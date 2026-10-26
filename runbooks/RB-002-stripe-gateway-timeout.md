---
service: "payment-service"
primary_exception: "ApiConnectionException"
title: "Stripe Payment Gateway Timeouts"
---

## Symptoms & Error Signatures
`ApiConnectionException` from payment-service.

## Root Cause Analysis
Network latency and gateway timeout.

## Step-by-Step Remediation
Check gateway health and retry with backoff.
