package com.example.ledger.filter;

import com.example.ledger.model.IdempotencyKey;
import com.example.ledger.model.IdempotencyStatus;
import com.example.ledger.service.IdempotencyManager;
import com.example.ledger.util.CachedBodyHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyManager idempotencyManager;

    public IdempotencyFilter(IdempotencyManager idempotencyManager) {
        this.idempotencyManager = idempotencyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String requestHash = computeSha256(wrappedRequest.getCachedBody());

        IdempotencyKey keyState = idempotencyManager.initiateOrRecover(idempotencyKey, requestHash);

        // If keyState is NOT null, it means this is a duplicate/retry request
        if (keyState != null) {
            if (keyState.getStatus() == IdempotencyStatus.PROCESSING) {
                response.setStatus(HttpStatus.CONFLICT.value());
                response.getWriter().write("{\"error\": \"Concurrent request in flight\"}");
                return;
            }

            // It already finished previously. Return the cached response.
            response.setStatus(keyState.getResponseCode());
            response.setContentType("application/json");
            response.getWriter().write(keyState.getResponseBody());
            return;
        }

        // If keyState IS null, it means it's a brand new request. Proceed to the Ledger!
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);

            int status = wrappedResponse.getStatus();
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            boolean isSuccess = status >= 200 && status < 300;

            idempotencyManager.complete(idempotencyKey, status, responseBody, isSuccess);
        } catch (Exception e) {
            idempotencyManager.complete(idempotencyKey, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", false);
            throw e;
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String computeSha256(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(payload);
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}