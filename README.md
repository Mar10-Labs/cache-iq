# CacheIQ - LLM Cost Optimization Layer

[![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
[![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
[![Docker](https://img.shields.io/badge/docker-ready-blue)
[![Medium](https://img.shields.io/badge/medium-article-blue)](https://medium.com/@magam.2004/your-llm-is-answering-the-same-question-over-and-over-796aabddbfc1)

CacheIQ es un proxy de cache semántico que reduce costos de LLM almacenando respuestas basadas en embeddings. Si un usuario hace una pregunta similar a otra anterior, se devuelve la respuesta guardada sin llamar al LLM.

## Inicio Rápido

### Requisitos
- Docker y Docker Compose
- JDK 21
- Groq API Key (gratuita en console.groq.com)

### Pasos

```bash
# 1. Clonar y configurar
git clone https://github.com/Mar10-Labs/cache-iq.git
cd cache-iq
cp .env.example .env

# 2. Editar .env y agregar GROQ_API_KEY
# Obtener key gratuita en: https://console.groq.com

# 3. Levantar servicios (infraestructura + Presidio)
docker compose up -d

# 4. Levantar aplicación (usa script que carga .env automáticamente)
./start.sh
```

**Servicios:**
| Servicio | URL |
|----------|-----|
| App | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Presidio (PII) | http://localhost:3001 |
| Grafana Dashboard | http://localhost:3004 (admin/admin) |
| Prometheus | http://localhost:9092 |
| PostgreSQL | localhost:5435 (cacheiq/cacheiq)

---

## Probar el Proyecto

### Verificar que funciona (prompt idéntico)

```bash
#Primera pregunta (MISS - llama a Groq)
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Que es Kotlin?"}], "model":"llama-3.3-70b-versatile"}'

#Repetir pregunta exacta (HIT - usa cache, no llama a Groq)
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Que es Kotlin?"}], "model":"llama-3.3-70b-versatile"}'
```

### Verificar que funciona (prompt similar - búsqueda semántica)

```bash
#Primera pregunta (MISS - llama a Groq)
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Que es Kotlin?"}], "model":"llama-3.3-70b-versatile"}'

#Segunda pregunta con significado similar (HIT - usa cache semántico)
#El sistema convierte ambos a embeddings y busca similitud > 80%
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Qué es el lenguaje Kotlin?"}], "model":"llama-3.3-70b-versatile"}'
```

**Por qué es importante:** El cache no busca texto exacto, sino **significado semántico**. 
- `"Que es Kotlin?"` y `"Qué es el lenguaje Kotlin?"` son diferentes como texto, 
- pero significado similar → embeddings similares → HIT en cache.

### Verificar resultados

| Herramienta | Qué ver |
|-------------|---------|
| **Grafana** | http://localhost:3004 → "CacheIQ Dashboard" - HIT/MISS, tokens ahorrados |
| **Swagger** | http://localhost:8080/swagger-ui.html - probá endpoints |
| **PostgreSQL** | localhost:5434 - tabla `cache_entries` con respuestas cacheadas |
| **Response Headers** | `X-Cache: HIT` o `X-Cache: MISS` |

---

## Modelo de Embedding (ONNX)

El proyecto usa **all-MiniLM-L6-v2** (~90MB, 384 dimensiones) para convertir texto en vectores.

**Fuente:** [HuggingFace](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

Este modelo se selecciona por ser liviano y rápido. Genera embeddings de 384 dimensiones para buscar similitud semántica en PostgreSQL.

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

### Enfoque Híbrido: Regex + Presidio

El sistema usa un enfoque de dos capas para detectar PII:

| Prompt | Clasificación | Detector usado |
|--------|--------------|--------------|
| `"What is the weather in NYC?"` | NONE (técnico) | ❌ Sin detección |
| `"My email is john@company.com"` | STRUCTURED (pattern `@`) | **Regex** → detecta email |
| `"My phone is 11 9999-8888"` | STRUCTURED (pattern `\d{10,11}`) | **Regex** → detecta teléfono |
| `"My CBU is 1234567890123456789012"` | STRUCTURED (pattern `\d{13,16}`) | **Regex** → detecta CBU |
| `"I want to cancel my subscription"` | CONTEXTUAL (indicador personal) | **Regex** primero → no encuentra → **Presidio** |
| `"Tell me my account balance"` | CONTEXTUAL (indicador personal) | **Regex** primero → no encuentra → **Presidio** |
| `"Please update my data"` | CONTEXTUAL (indicador personal) | **Regex** primero → no encuentra → **Presidio** |

### Flujo de Detección

```
Prompt
   ↓
¿Tiene pattern estructurado? (@, phone, CBU, DNI...)
   ├── SÍ → Regex → listo
   └── NO
        ↓
   ¿Tiene indicador personal? (my, account, cancel...)
   ├── SÍ → Regex primero
   │         └── Si no encuentra → Presidio
   └── NO → STRUCTURED (default)
```

### Por qué no usar Presidio siempre

```
┌─────────────────────────────────────────────────────────────┐
│  DATOS SENSIBLES = NO salen de tu infra                     │
└─────────────────────────────────────────────────────────────┘
```

| Enfoque | Pros | Contras |
|--------|------|---------|
| **Regex** | Rápido, sin dependencia externa, sin datos sensibles fuera | Menos preciso en patrones complejos |
| **Presidio** | Mejor precisión (ML) | Datos sensibles salen a servicio externo |

### Presidio

**Presidio:** Servicio de PII detection (ML-based) que se levanta automáticamente con Docker Compose en `http://localhost:3000`.

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