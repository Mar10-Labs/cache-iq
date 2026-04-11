# CacheIQ - LLM Cost Optimization Layer

[![Version](https://img.shields.io/badge/version-4.0.0-blue)
[![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
[![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
[![Docker](https://img.shields.io/badge/docker-ready-blue)

CacheIQ is a semantic cache proxy that reduces LLM inference costs by caching responses based on embeddings.

## Status - V2 In Progress

### Implemented (V1)
- ✅ Semantic cache proxy (pgvector + cosine similarity)
- ✅ Embedding mock (hash-based)
- ✅ Groq API mock (predefined responses)
- ✅ Metrics (Micrometer + Prometheus + Grafana)
- ✅ Swagger UI for testing

### V2 Progress
- 🚧 Real embedding (ONNX all-MiniLM-L6-v2) ✅ DONE
- 🚧 Real Groq API (WebClient)
- 🚧 Real PII Detection
- 🚧 Additional tests

## Quick Start - Docker Compose

```bash
# 1. Clone and enter
git clone https://github.com/Mar10-Labs/cache-iq && cd cache-iq

# 2. Setup (optional - already includes mock)
cp .env.example .env

# 3. Start services
docker compose up -d

# 4. Verify services
curl http://localhost:8081/actuator/health

# 5. First call - MISS
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'

# 6. Second call - HIT (uses cache)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

## Response Headers

| Header | Description |
|--------|-------------|
| `X-Cache` | HIT or MISS |
| `X-Cache-Llm-Model` | LLM model used |
| `X-Cache-Llm-Provider` | Provider (groq, claude, etc.) |
| `X-Cache-Embedding-Model` | Embedding model |

## Service URLs

| Service | URL |
|----------|-----|
| Proxy API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:8081/actuator/prometheus |
| Grafana | http://localhost:3002 (admin/admin) |
| PostgreSQL | localhost:5433 |
| Prometheus | localhost:9090 |

## Hexagonal Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  ProxyController│────▶│SemanticCacheUse │────▶│   GroqAdapter   │
│      (API)      │     │    (UseCase)    │     │  (Mock V1)      │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                  │
                     ┌────────────┼────────────┐
                     ▼            ▼            ▼
            ┌────────────┐ ┌────────────┐ ┌─────────────┐
            │EmbeddingAdp│ │PgVectorCache│ │CacheMetrics │
            │  (Mock)    │ │  Adapter   │ │  Adapter    │
            └────────────┘ └────────────┘ └─────────────┘
```

### Layers (Hexagonal Architecture)
- **API**: ProxyController - receives HTTP requests
- **Application**: SemanticCacheUseCase - business logic
- **Domain**: Entities, input/output ports
- **Infrastructure**: Adapters (Groq, PostgreSQL, Metrics)

## Stack

| Technology | Purpose |
|------------|-----------|
| Kotlin 21 | Language |
| Spring Boot 3.2 | Framework |
| PostgreSQL + pgvector | Embedding cache |
| Redis | Sessions (present) |
| Micrometer + Prometheus | Metrics |
| Grafana | Dashboard |
| Docker Compose | Orchestration |

## Local Development

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test
```

## License

MIT - github.com/Mar10-Labs/cache-iq