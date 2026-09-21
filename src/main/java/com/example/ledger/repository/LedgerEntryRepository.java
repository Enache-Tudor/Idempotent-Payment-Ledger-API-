package com.example.ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<com.example.ledger.model.LedgerEntry, String> {
}
