package com.example.ledger.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id private String id;
    @Column(name = "transaction_id") private String transactionId;
    @Column(name = "account_id") private String accountId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // <-- ADD THIS LINE
    private LedgerDirection direction;

    // ... getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LedgerDirection getDirection() {
        return direction;
    }

    public void setDirection(LedgerDirection direction) {
        this.direction = direction;
    }
}