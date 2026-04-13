# CacheIQ - LLM Cost Optimization Layer

[![Version](https://img.shields.io/badge/version-4.1.0-blue)
[![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
[![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
[![Docker](https://img.shields.io/badge/docker-ready-blue)

CacheIQ es un proxy de cache semántico que reduce costos de LLM almacenando respuestas basadas en embeddings.

## Estado - Completo ✅

### Features implementadas
- ✅ Proxy de cache semántico (pgvector + cosine similarity)
- ✅ Embedding: ONNX all-MiniLM-L6-v2 (384 dims, ~90MB)
- ✅ Groq API real (no mock)
- ✅ Métricas (Micrometer + Prometheus + Grafana)
- ✅ PII Router (Regex detector - para demo)
- ✅ Tests (104 tests, >65% coverage)
- ✅ Grafana Dashboard automático

## Inicio Rápido (Elige tu opción)

### Opción A: Ejecutar todo con Docker

**Importante:** Antes de ejecutar, crear el archivo `.env` basado en `.env.example`:

```bash
cp .env.example .env
# Editar .env y agregar tu GROQ_API_KEY
```

```bash
# 1. Clonar y ejecutar
git clone https://github.com/Mar10-Labs/cache-iq.git
cd cache-iq
docker compose up -d

# 2. Probar
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

**Servicios:** App (8081), PostgreSQL (5434), Prometheus (9090), Grafana (3002)

---

### Opción B: Ejecutar Local (IntelliJ)

**Servicios necesarios:** PostgreSQL, Prometheus, Grafana

```bash
# 1. Levantar servicios (sin la app)
docker compose up -d postgres prometheus grafana
```

**IDE (IntelliJ):**
- Main class: `com.cacheiq.CacheIqApplicationKt`
- Environment: `GROQ_API_KEY=tu_key`

```bash
# 2. Ejecutar
./gradlew bootRun

# 3. Probar (puerto 8080)
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

---

## Probar el Cache (Casos de uso)

```bash
# MISS - primera vez (llama a Groq)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'

# HIT - repetición (usa cache, no llama a Groq)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'

# Headers de respuesta muestran HIT/MISS
curl -i -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}' \
  | grep X-Cache
```

---

## Modelo de Embedding

El proyecto usa **all-MiniLM-L6-v2** (~90MB, 384 dimensiones) para convertir texto en vectores.

**Fuente:** [HuggingFace - sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

Este modelo se chose por ser liviano y rápido, ideal para demos. Modelos más grandes (ej: 768 dims)dan mejores resultados pero pesan más.

**Archivos necesarios (ya incluidos):**
```
src/main/resources/models/
├── model.onnx           (red neuronal)
├── tokenizer.json       (vocabulario)
└── tokenizer_config.json (config)
```

**Detección de PII:** El proyecto incluye RegexPiiDetector para detectar datos sensibles en prompts. Para producción, existe Presidio (más avanzado pero ~1.3GB).

---

## Response Headers

| Header | Description |
|--------|-------------|
| `X-Cache` | HIT or MISS |
| `X-Cache-Llm-Model` | LLM model used |
| `X-Cache-Llm-Provider` | Provider (groq, etc.) |
| `X-Cache-Embedding-Model` | Embedding model |

## Por qué el modelo viaje en el request

El modelo forma parte de la **cache key**. Si "Hello" con `llama-3.3` da diferente respuesta que con `gpt-4`, se guardan separadas.

Cache key = embedding + modelo + provider + tenant

## Service URLs

| Service | URL |
|----------|-----|
| Proxy API (Docker) | http://localhost:8081 |
| Proxy API (Local) | http://localhost:8080 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana (Dashboard) | http://localhost:3002 (admin/admin) |
| PostgreSQL | localhost:5434 |

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  ProxyController│────▶│SemanticCacheUse │────▶│   GroqAdapter   │
│      (API)      │     │    (UseCase)    │     │  (Groq API)     │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                   │
                      ┌────────────┼────────────┐
                      ▼            ▼            ▼
             ┌────────────┐ ┌────────────┐ ┌─────────────┐
             │EmbeddingAdp│ │PgVectorCache│ │CacheMetrics │
             │(ONNX/Hash) │ │  Adapter   │ │  Adapter    │
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
| PostgreSQL + pgvector | Embedding cache (similarity search) |
| Micrometer + Prometheus | Metrics |
| Grafana | Dashboard |
| Docker Compose | Orchestration |
| Groq API | LLM (gratuito, ~9-10 req/min) |

## Local Development

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test
```

## Configuración

### Variables de entorno requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `GROQ_API_KEY` | API key de Groq | `gsk_...` |
| `POSTGRES_HOST` | Host de PostgreSQL | `localhost` |
| `POSTGRES_PORT` | Puerto de PostgreSQL | `5434` |
| `POSTGRES_USER` | Usuario de PostgreSQL | `cacheiq` |
| `POSTGRES_PASSWORD` | Password de PostgreSQL | `cacheiq` |

### Configuración de Docker

```bash
# Con archivo .env
export GROQ_API_KEY="gsk_..."
docker compose up -d

# O inline
GROQ_API_KEY="gsk_..." docker compose up -d
```

### Configuración Local (IntelliJ)

1. Run Configuration → Environment Variables
2. Agregar: `GROQ_API_KEY=gsk_...;POSTGRES_HOST=localhost;POSTGRES_PORT=5434;POSTGRES_USER=cacheiq;POSTGRES_PASSWORD=cacheiq`

## License

MIT - github.com/Mar10-Labs/cache-iq