---
service: "stripe-gateway"
primary_exception: "ApiConnectionException"
title: "Stripe Payment Gateway Timeouts"
---

## Symptoms & Error Signatures
`ApiConnectionException` from stripe-gateway.

## Root Cause Analysis
Network latency and gateway timeout.

## Step-by-Step Remediation
Check gateway health and retry with backoff.
