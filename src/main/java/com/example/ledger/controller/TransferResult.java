package com.example.ledger.controller;

import java.math.BigDecimal;

public record TransferResult(String transactionId, BigDecimal amount, String currency, String status) {
}