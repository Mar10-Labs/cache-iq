# CacheIQ - AGENTS.md

## Build & Run

```bash
# Build (skip tests)
./gradlew build -x test

# Run tests
./gradlew test

# Coverage report
./gradlew jacocoTestReport

# Docker (all services)
docker compose up -d

# Single service rebuild
docker compose build --no-cache
```

## Ports

- App: `8081` (not 8080)
- PostgreSQL: `5434`
- Prometheus: `9090`
- Grafana: `3002`

## API

```bash
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

Response headers: `X-Cache` (HIT/MISS), `X-Cache-Llm-Model`, `X-Cache-Llm-Provider`

## Architecture

Hexagonal (ports & adapters):
- `src/main/kotlin/com/cacheiq/domain/` - models, ports
- `src/main/kotlin/com/cacheiq/application/` - use cases
- `src/main/kotlin/com/cacheiq/infrastructure/` - adapters
- `src/main/kotlin/com/cacheiq/api/` - controllers

## Tech Stack

- Kotlin 21 (not Java 17)
- Spring Boot 3.2.0
- Gradle 8.5
- PostgreSQL + pgvector
- ONNX Runtime 1.19.2 (for V2 embeddings)

## V2 Priorities

1. Real ONNX embeddings (`all-MiniLM-L6-v2`)
2. Real Groq API client (WebClient)
3. Real PII detection
4. More tests

## CI

- GitHub Actions: `.github/workflows/ci.yml`
- Runs: build → test → jacoco → docker-build
- JDK 21 required