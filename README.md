# CacheIQ - LLM Cost Optimization Layer

[![Version](https://img.shields.io/badge/version-4.0.2-blue)
[![Kotlin](https://img.shields.io/badge/kotlin-21-blue)
[![Spring Boot](https://img.shields.io/badge/spring_boot-3.2-green)
[![Docker](https://img.shields.io/badge/docker-ready-blue)

CacheIQ es un proxy de cache semántico que reduce costos de LLM almacenando respuestas basadas en embeddings.

## Estado - V4 Completo (Groq API Real)

### Features implementadas
- ✅ Proxy de cache semántico (pgvector + cosine similarity)
- ✅ Embedding: Hash (default) o ONNX (opcional)
- ✅ Groq API real (no mock)
- ✅ Métricas (Micrometer + Prometheus + Grafana)
- ✅ PII Router Inteligente (None/Structured/Contextual)
- ✅ Tests (104 tests, >65% coverage)

## Inicio Rápido (Elige tu opción)

### Opción A: Ejecutar con Docker (Recomendado)

**Importante:** Antes de ejecutar, crear el archivo `.env` basado en `.env.example`:

```bash
cp .env.example .env
# Editar .env y agregar tu GROQ_API_KEY
```

```bash
# 1. Clonar el repositorio
git clone https://github.com/Mar10-Labs/cache-iq.git
cd cache-iq

# 2. Ejecutar servicios
docker compose up -d

# 3. Verificar que esté corriendo
docker compose ps

# 4. Probar el endpoint
curl -X POST http://localhost:8081/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

**Servicios disponibles:**
| Servicio | URL |
|----------|-----|
| App | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| PostgreSQL | localhost:5434 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3002 (admin/admin) |

---

### Opción B: Ejecutar Local (IntelliJ/IDE)

#### Prerrequisitos
- JDK 21
- PostgreSQL 16+ con extensión pgvector

#### Paso 1: Iniciar PostgreSQL con Docker
```bash
# Solo PostgreSQL
docker run -d \
  --name cacheiq-postgres \
  -e POSTGRES_DB=cacheiq \
  -e POSTGRES_USER=cacheiq \
  -e POSTGRES_PASSWORD=cacheiq \
  -p 5434:5432 \
  pgvector/pgvector:pg16
```

#### Paso 2: Configurar IDE (IntelliJ)

**Run Configuration:**
- Main class: `com.cacheiq.CacheIqApplicationKt`
- VM options: (none needed)
- Environment variables: (none needed - application.yml tiene defaults)

**O si preferís:**
```bash
# Desde terminal
export GROQ_API_KEY=your_key_here
./gradlew bootRun
```

#### Paso 3: Ejecutar
```bash
# En IntelliJ: Shift+F10
# O terminal:
./gradlew bootRun
```

#### Paso 4: Probar
```bash
curl -X POST http://localhost:8080/proxy/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -d '{"messages":[{"role":"user","content":"Hello"}], "model":"llama-3.3-70b-versatile"}'
```

---

### Con modelos ONNX reales (opcional)

Los modelos ONNX (~90MB) se incluyen automáticamente en el build de Docker. Para local:

```bash
# Los modelos ya están en src/main/resources/models/
# Solo ejecutar

./gradlew bootRun
```

Para rebuild con Docker:
```bash
docker compose build --no-cache
docker compose up -d
```

---

## Modelos de Embedding

### Configuración actual

El proyecto incluye el modelo `all-MiniLM-L6-v2` (384 dimensiones, ~90MB) que convierte texto en vectores semánticos.

```
src/main/resources/models/
├── model.onnx              (90MB - Red neuronal)
├── tokenizer.json         (466KB - Vocabulario: palabra → ID)
└── tokenizer_config.json  (350B - Configuración del tokenizador)
```

### Cómo cambiar de modelo

Si necesitás usar un modelo de embedding diferente (ej: `paraphrase-mpnet-base-v2` con 768 dimensiones), debés cambiar **los tres archivos**:

| Archivo | Qué es | Necesario cambiarlo |
|---------|--------|---------------------|
| `model.onnx` | La red neuronal | ✅ Sí |
| `tokenizer.json` | Vocabulario del modelo | ✅ Sí |
| `tokenizer_config.json` | Config del tokenizer | ✅ Sí |

**Los tres deben ser del mismo modelo** - no se pueden mezclar.

### Ejemplo: Cambiar a modelo de 768 dimensiones

1. Descargar los 3 archivos del nuevo modelo (ej: de HuggingFace)
2. Reemplazar los archivos en `src/main/resources/models/`
3. Actualizar `application.yml`:
   ```yaml
   embedding:
     model:
       dimensions: 768  # cambiar de 384 a 768
   ```
4. Rebuild del proyecto

### Por qué los tres archivos

- `model.onnx` → genera los embeddings
- `tokenizer.json` → sabe cómo dividir el texto en tokens
- `tokenizer_config.json → sabe cómo procesar esos tokens

Si cambias `model.onnx` pero dejás el tokenizer del modelo anterior, no va a funcionar correctamente.

---

## Response Headers

| Header | Description |
|--------|-------------|
| `X-Cache` | HIT or MISS |
| `X-Cache-Llm-Model` | LLM model used |
| `X-Cache-Llm-Provider` | Provider (groq, claude, etc.) |
| `X-Cache-Embedding-Model` | Embedding model |

## Por qué el modelo viaje en el request

El modelo forma parte de la **cache key**. Si un usuario usa "Hello" con `llama-3.3` y otro con `gpt-4`, las respuestas pueden ser distintas - se guardan separadas en cache.

La búsqueda en cache usa:
1. **Embedding del prompt** - búsqueda semántica
2. **Modelo** - qué modelo LLM generó la respuesta
3. **Provider** - proveedor (groq, openai, etc.)
4. **Tenant** - aislamiento entre clientes

Si no se incluyera el modelo, un usuario con `gpt-4` recibiría respuestas de `llama-3.3` - incorrecto.

El modelo es configurable via `application.yml` o variables de entorno.

## Service URLs

| Service | URL |
|----------|-----|
| Proxy API (Docker) | http://localhost:8081 |
| Proxy API (Local) | http://localhost:8080 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3002 (admin/admin) |
| PostgreSQL | localhost:5434 |

## Hexagonal Architecture

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