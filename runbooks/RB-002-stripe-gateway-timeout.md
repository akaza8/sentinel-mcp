---
service: "payment-service"
primary_exception: "SocketTimeoutException"
title: "Stripe Payment Gateway Timeouts"
---

## Symptoms & Error Signatures
`SocketTimeoutException` from stripe-gateway.

## Root Cause Analysis
Network latency and gateway timeout.

## Step-by-Step Remediation
Check gateway health and retry with backoff.
