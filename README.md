# CacheIQ - LLM Cost Optimization Layer

[![Version](https://img.shields.io/badge/version-4.0.0-blue)
![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
![Docker](https://img.shields.io/badge/docker-ready-blue)

CacheIQ es un proxy de cache semántico que reduce los costos de inferencia LLM mediante cache de respuestas basado en embeddings.

## Estado - V1 ✅ Funcional

### Implementado
- ✅ Proxy con cache semántico (pgvector + similaridad coseno)
- ✅ Mock de embedding (hash-based)
- ✅ Mock de Groq API (respuestas predefinidas)
- ✅ Métricas (Micrometer + Prometheus + Grafana)
- ✅ Swagger UI para testing

### Pendiente V2
- 🚧 Embedding real (ONNX all-MiniLM-L6-v2)
- 🚧 Groq API real (WebClient)
- 🚧 PII Detection real
- 🚧 Tests unitarios

## Quick Start - Docker Compose

```bash
# 1. Clonar y entrar
git clone https://github.com/kaeron-dev/cacheiq && cd cacheiq

# 2. Configurar (opcional - ya incluye mock)
cp .env.example .env

# 3. Levantar servicios
docker compose up -d

# 4. Verificar servicios
curl http://localhost:8081/actuator/health

# 5. Primera llamada - MISS
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hola"}], "model":"llama-3.3-70b-versatile"}'

# 6. Segunda llamada - HIT (usa cache)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hola"}], "model":"llama-3.3-70b-versatile"}'
```

## URLs de Servicios

| Servicio | URL |
|----------|-----|
| Proxy API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:8081/actuator/prometheus |
| Grafana | http://localhost:3002 (admin/admin) |

## Arquitectura Hexagonal

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

### Capas (Arquitectura Hexagonal)
- **API**: ProxyController - recibe requests HTTP
- **Application**: SemanticCacheUseCase - lógica de negocio
- **Domain**: Entidades, puertos de entrada/salida
- **Infrastructure**: Adaptadores (Groq, PostgreSQL, Metrics)

## Stack

| Tecnología | Propósito |
|------------|-----------|
| Kotlin 21 | Lenguaje |
| Spring Boot 3.2 | Framework |
| PostgreSQL + pgvector | Cache de embeddings |
| Redis | Sesiones (presente) |
| Micrometer + Prometheus | Métricas |
| Grafana | Dashboard |
| Docker Compose | Orquestación |

## Desarrollo Local

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

## Licencia

MIT - github.com/kaeron-dev/cacheiq