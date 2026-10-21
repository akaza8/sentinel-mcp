---
service: "order-service"
primary_exception: "PSQLException"
title: "HikariCP Connection Pool Exhaustion"
---

## Symptoms & Error Signatures
Logs show `PSQLException` and connection timeouts in order-service.

## Root Cause Analysis
Connection pool exhaustion due to unclosed connections.

## Step-by-Step Remediation
Restart service and increase HikariCP pool size.
