---
service: "inventory-service"
primary_exception: "RedisConnectionFailureException"
title: "Redis Cache Connection Refused"
---

## Symptoms & Error Signatures
`RedisConnectionFailureException` in inventory-service.

## Root Cause Analysis
Redis cluster unreachable.

## Step-by-Step Remediation
Verify cluster nodes and restart cache service.
