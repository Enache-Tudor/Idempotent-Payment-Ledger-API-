package com.example.ledger.controller;

import java.math.BigDecimal;

public record TransferRequest(String fromAccountId, String toAccountId, BigDecimal amount, String currency) {
}
