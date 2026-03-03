# 🚦 Distributed Rate Limiter Service
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-Backend-green)
![Redis](https://img.shields.io/badge/Redis-Distributed-red)
![Status](https://img.shields.io/badge/Status-Active-blue)

A production-style **Rate Limiting Service** built with **Spring Boot + Redis**, implementing industry-standard algorithms like **Fixed Window** and **Token Bucket (Lua-powered, atomic execution)**.

Designed to simulate how real API gateways and backend systems protect services from abuse, traffic spikes, and overload.

---

## 🧠 The Problem

Modern backend systems face:

- Traffic bursts  
- API abuse  
- Distributed server deployments  
- Concurrency at scale  
- Race conditions in shared counters  

A naive in-memory limiter:

- ❌ Fails in distributed environments  
- ❌ Breaks under concurrency  
- ❌ Causes inconsistent rate limits  
- ❌ Cannot support real-world API traffic  

---

## 💡 The Solution

This project implements a **Redis-backed distributed rate limiter** with:

- Atomic operations using **Lua scripts**
- HTTP-compliant **429 Too Many Requests**
- Retry headers
- Configurable algorithm switching
- Production-style layered architecture

---

## 🏗️ Architecture Overview
```
Client → REST API → RateLimiterService
                         ↓
              Strategy Pattern (Config-based)
                ↙                    ↘
       Fixed Window          Token Bucket (Lua + Redis)
```

### 🔑 Key Design Decisions

- Redis used as centralized distributed storage  
- Lua scripts ensure atomic multi-step token bucket operations  
- Strategy pattern allows switching algorithms via config  
- Proper HTTP semantics (429 + Retry-After headers)

---

## 🚀 Features

### ✅ 1. Redis-Backed Distributed Storage

- Shared counters across multiple instances  
- No in-memory state  
- Horizontally scalable  

---

### ✅ 2. Token Bucket (Industry Standard)

- Smooth traffic shaping  
- Burst-friendly  
- Long-term average rate enforcement  
- Implemented using Redis + Lua for atomicity  

---

### ✅ 3. Fixed Window

- Simple, predictable limits  
- Useful for strict per-minute quotas  

---

### ✅ 4. Concurrency Safe

- No race conditions  
- Atomic Redis operations  
- Lua ensures multi-step atomic execution  

---

### ✅ 5. REST API Service

#### `POST /rate-limit/check`

**Request:**

```json
{
  "clientId": "user-123",
  "endpoint": "/login"
}
```

**Response:**

```json
{
  "allowed": true,
  "remaining": 3,
  "retryAfterMs": 0
}
```
If blocked:

- HTTP `429 Too Many Requests`
- `Retry-After` header included

---

### ✅ 6. Strategy-Based Configuration

Switch algorithms without changing code:

```properties
rate.limit.strategy=fixed
```

or

```properties
rate.limit.strategy=token
```

---

## 🔬 Why Lua?

Token Bucket requires multiple operations:

- Read tokens  
- Calculate refill  
- Update tokens  
- Deduct if allowed  
- Return retry time  

If executed separately → race conditions.

Using Lua:

- All operations run atomically inside Redis  
- Guaranteed consistency  
- Safe under high concurrency  

---

## 🛠️ Tech Stack

- Java 17  
- Spring Boot  
- Redis  
- Lua (Redis scripting)  
- Maven  

---

## 📦 Running Locally

### 1️⃣ Start Redis

Using Redis directly:

```bash
redis-server
```

Or using Docker:

```bash
docker run -p 6379:6379 redis
```

---

### 2️⃣ Run the Application

```bash
./mvnw spring-boot:run
```

Application runs at:

```
http://localhost:8080
```

---

### 3️⃣ Test With cURL

```bash
curl -X POST http://localhost:8080/rate-limit/check \
-H "Content-Type: application/json" \
-d '{"clientId":"test-user","endpoint":"/demo"}'
```

---

## 📊 Production Considerations Covered

✔ Distributed state  
✔ Atomic operations  
✔ Burst handling  
✔ HTTP 429 compliance  
✔ Strategy pattern  
✔ Config-driven behavior  
✔ Clean architecture separation  

---

## 🧱 Project Structure

```
controller/         → REST endpoints
service/            → Business logic
service/limiter/    → Algorithm implementations
config/             → Redis + Lua configuration
model/              → Request / Response models
resources/          → Lua scripts + properties
```

---

## 📈 Real-World Applications

This design mirrors systems used in:

- API Gateways  
- Authentication services  
- Payment systems  
- Public REST APIs  
- Microservice architectures  

---

## 🎯 What This Project Demonstrates

- Understanding of distributed systems fundamentals  
- Concurrency and race condition handling  
- Atomic scripting with Redis Lua  
- Clean backend architecture  
- HTTP protocol correctness  
- Production-level system design thinking

---

## 📊 Observability

Integrated **Spring Boot Actuator + Micrometer** with custom business metrics:

- `rate_limiter.total_requests`
- `rate_limiter.blocked_requests`

Example output from `/actuator/metrics`:

![Metrics](docs/images/ratelimiter_metrics.png)
---

## 🔮 Future Improvements

- **Prometheus & Grafana Integration**  
  Expose Micrometer metrics for scraping and build real-time dashboards for traffic patterns and rate-limit behavior.

- **Advanced Monitoring & Alerting**  
  Add alert rules for abnormal traffic spikes, high block rates, or Redis latency issues.

- **Per-User & Per-Endpoint Dynamic Limits**  
  Support configurable limits per client or endpoint via database or centralized config service.

- **Redis Cluster Support**  
  Enable high-availability and horizontal scalability using Redis Cluster or Sentinel.

- **Integration & Load Testing**  
  Add automated tests and simulate high-concurrency scenarios to validate performance under load.

- **Authentication & API Gateway Integration**  
  Integrate with JWT or API gateway systems to simulate real production environments. 

---

## 🏁 Summary

A distributed, Redis-backed rate limiting service implementing industry-standard algorithms with atomic Lua execution and HTTP-compliant responses.

Designed to reflect real-world backend infrastructure principles including concurrency safety, scalability, and clean architectural design.
