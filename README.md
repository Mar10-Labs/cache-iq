# CacheIQ - LLM Cost Optimization Layer

[![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
[![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
[![Docker](https://img.shields.io/badge/docker-ready-blue)

CacheIQ es un proxy de cache semántico que reduce costos de LLM almacenando respuestas basadas en embeddings. Si un usuario hace una pregunta similar a otra anterior, se devuelve la respuesta guardada sin llamar al LLM.

## Inicio Rápido

### Opción A: Docker (todo incluido)

```bash
# 1. Clonar y ejecutar
git clone https://github.com/Mar10-Labs/cache-iq.git
cd cache-iq
cp .env.example .env  # agregar GROQ_API_KEY
docker compose up -d
```

**Servicios:**
| Servicio | URL |
|----------|-----|
| App | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Grafana Dashboard | http://localhost:3002/dashboards (admin/admin) |
| Prometheus | http://localhost:9090 |
| PostgreSQL | localhost:5434 (cacheiq/cacheiq) |

---

### Opción B: Local (IntelliJ/IDE)

1. Levantar servicios: `docker compose up -d postgres prometheus grafana`
2. IDE: Run config → Main class: `com.cacheiq.CacheIqApplicationKt`, Environment: `GROQ_API_KEY=tu_key`
3. Ejecutar: `./gradlew bootRun` (puerto 8080)

---

## Probar el Proyecto

### Verificar que funciona

```bash
#Primera pregunta (MISS - llama a Groq)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Que es Kotlin?"}], "model":"llama-3.3-70b-versatile"}'

#Repetir pregunta (HIT - usa cache, no llama a Groq)
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Que es Kotlin?"}], "model":"llama-3.3-70b-versatile"}'
```

### Verificar resultados

| Herramienta | Qué ver |
|-------------|---------|
| **Grafana** | http://localhost:3002/dashboards → "CacheIQ Dashboard" - HIT/MISS, tokens ahorrados |
| **Swagger** | http://localhost:8081/swagger-ui.html - probá endpoints |
| **PostgreSQL** | localhost:5434 - tabla `cache_entries` con respuestas cacheadas |
| **Response Headers** | `X-Cache: HIT` o `X-Cache: MISS` |

---

## Modelo de Embedding (ONNX)

El proyecto usa **all-MiniLM-L6-v2** (~90MB, 384 dimensiones) para convertir texto en vectores.

**Fuente:** [HuggingFace](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

Este modelo se選擇 por ser liviano y rápido. Genera embeddings de 384 dimensiones para buscar similitud semántica en PostgreSQL.

**Archivos en el proyecto:**
```
src/main/resources/models/
├── model.onnx           (red neuronal que genera vectores)
├── tokenizer.json       (divide texto en tokens)
└── tokenizer_config.json (config del tokenizer)
```

Los tres deben ser del mismo modelo.

---

## Por qué el modelo viaja en el request

El modelo es parte de la **cache key**. La misma pregunta puede dar diferentes respuestas según el modelo:

| Pregunta | Modelo | Respuesta |
|----------|--------|-----------|
| "Hello" | llama-3.3 | "Hi, how can I help?" |
| "Hello" | gpt-4 | "Hello! What can I do for you?" |

Si un usuario usa un modelo diferente, no debería recibir respuestas cacheadas de otro modelo. Por eso la cache key incluye: **embedding + modelo + provider + tenant**.

El modelo se pasa en el request y se configurable via `application.yml`.

---

## Detección de PII (Datos Sensibles)

El proyecto incluye detección de datos sensibles en prompts para no guardarlos en cache.

**RegexPiiDetector:** Rápido, sin dependencias, para demo.

**Presidio:** Más avanzado (ML-based), pero requiere imagen Docker de ~1.3GB. El código existe pero no está habilitado.

---

## Response Headers

| Header | Description |
|--------|-------------|
| `X-Cache` | HIT (respuesta desde cache) o MISS (llamó al LLM) |
| `X-Cache-Llm-Model` | Modelo LLM usado |
| `X-Cache-Llm-Provider` | Proveedor (groq, openai, etc.) |
| `X-Cache-Embedding-Model` | Modelo de embedding |

---

## Configuración

### Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `GROQ_API_KEY` | - | API key de Groq (requerido) |
| `LLM_MODEL` | llama-3.3-70b-versatile | Modelo por defecto |
| `SIMILARITY_THRESHOLD` | 0.5 | Umbral de similitud (0-1) |

### Docker
```bash
export GROQ_API_KEY=gsk_...
docker compose up -d
```

### Local (IntelliJ)
Run Configuration → Environment Variables → `GROQ_API_KEY=gsk_...`

---

## Stack

| Technology | Purpose |
|------------|--------|
| Kotlin 21 + Spring Boot 3.2 | App |
| PostgreSQL + pgvector | Cache semántico |
| ONNX Runtime | Embeddings |
| Micrometer + Prometheus | Métricas |
| Grafana | Dashboard |
| Groq API | LLM (gratuito) |
| Docker Compose | Orquestación |

## Tests

```bash
./gradlew test
./gradlew jacocoTestReport
```

---

MIT - github.com/Mar10-Labs/cache-iq