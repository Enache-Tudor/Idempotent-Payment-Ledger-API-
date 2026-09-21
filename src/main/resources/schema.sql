CREATE TYPE idempotency_status AS ENUM ('PROCESSING', 'SUCCESS', 'FAILED');
CREATE TYPE ledger_direction AS ENUM ('DEBIT', 'CREDIT');

CREATE TABLE IF NOT EXISTS idempotency_keys (
                                                key VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    status idempotency_status NOT NULL,
    response_code INT,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE TABLE IF NOT EXISTS accounts (
                                        id VARCHAR(36) PRIMARY KEY,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT balance_non_negative CHECK (balance >= 0)
    );

CREATE TABLE IF NOT EXISTS ledger_transactions (
                                                   id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE REFERENCES idempotency_keys(key),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );

CREATE TABLE IF NOT EXISTS ledger_entries (
                                              id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL REFERENCES ledger_transactions(id),
    account_id VARCHAR(36) NOT NULL REFERENCES accounts(id),
    amount NUMERIC(19, 4) NOT NULL,
    direction ledger_direction NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                             );