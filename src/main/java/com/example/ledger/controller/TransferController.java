package com.example.ledger.controller;

import com.example.ledger.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final LedgerService ledgerService;

    public TransferController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<TransferResult> executeTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequest request) {

        TransferResult result = ledgerService.processTransfer(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.currency(),
                idempotencyKey
        );

        return ResponseEntity.ok(result);
    }
}