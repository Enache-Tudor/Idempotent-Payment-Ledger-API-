package com.example.ledger.repository;

import com.example.ledger.model.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, String> {
}
