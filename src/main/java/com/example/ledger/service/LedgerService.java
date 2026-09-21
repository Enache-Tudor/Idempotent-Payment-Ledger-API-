package com.example.ledger.service;

import com.example.ledger.controller.TransferResult;
import com.example.ledger.model.Account;
import com.example.ledger.model.LedgerDirection;
import com.example.ledger.model.LedgerTransaction;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
import com.example.ledger.repository.LedgerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.example.ledger.model.LedgerEntry;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository txRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(AccountRepository accountRepository,
                         LedgerTransactionRepository txRepository,
                         LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.txRepository = txRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResult processTransfer(String fromAccountId, String toAccountId, BigDecimal amount, String currency, String idempotencyKey) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        // 1. Sort lexicographically to prevent database deadlocks
        String firstLockId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        String secondLockId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        // 2. Acquire Pessimistic Locks in strictly sorted order
        Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstLockId));
        Account secondAccount = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondLockId));

        // 3. Re-assign to Source and Target
        Account source = firstLockId.equals(fromAccountId) ? firstAccount : secondAccount;
        Account target = firstLockId.equals(toAccountId) ? firstAccount : secondAccount;

        // 4. Currency and Balance Checks
        if (!source.getCurrency().equals(currency) || !target.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        // 5. Update Balances
        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        // 6. Double-Entry Bookkeeping
        LedgerTransaction tx = new LedgerTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        txRepository.save(tx);

        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setId(UUID.randomUUID().toString());
        debitEntry.setTransactionId(tx.getId());
        debitEntry.setAccountId(source.getId());
        debitEntry.setAmount(amount);
        debitEntry.setDirection(LedgerDirection.DEBIT);
        entryRepository.save(debitEntry);

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setId(UUID.randomUUID().toString());
        creditEntry.setTransactionId(tx.getId());
        creditEntry.setAccountId(target.getId());
        creditEntry.setAmount(amount);
        creditEntry.setDirection(LedgerDirection.CREDIT);
        entryRepository.save(creditEntry);

        return new TransferResult(tx.getId(), tx.getAmount(), tx.getCurrency(), "SUCCESS");
    }
}