# Consulta Vehicular – SRI + ANT con caché en la nube
#Link de icepanel.io: https://s.icepanel.io/VCdSiDyqdjOmdW/dFxW 

ISWZ2202 – Diseño y Arquitectura de Software. Frontend React, backend Java (Spring Boot), caché Redis Cloud.

```
sri-ant-c4/
├── backend/    Spring Boot 3 (Java 17)
├── frontend/   React + Vite
└── docs/C4-IcePanel.md   Modelo C4 para dibujar en IcePanel
```

## Patrón de caché (ANT con baja disponibilidad)

Cache-Aside + Fallback (stale-if-error), implementado en `AntService`:

1. Caché fresco (< 10 min) → responde sin consultar la ANT.
2. Si no, consulta la ANT (timeout 5 s); si la respuesta es válida la guarda en Redis (TTL 30 días).
3. Si la ANT falla → responde el último dato válido guardado, marcado `CACHE_RESPALDO`.
4. Sin ANT y sin caché → 503.

Si Redis falla, la consulta sigue funcionando (sin caché).

## Ejecutar

### 1. Caché Redis en la nube
Crea una base gratuita en **Redis Cloud** o **Upstash** y copia la URL:

```
export REDIS_URL="rediss://default:TU_PASSWORD@TU_HOST:PUERTO"
```

### 2. Backend
```
cd backend
mvn spring-boot:run
```
Variables: `REDIS_URL`, `CORS_ORIGINS` (por defecto `http://localhost:5173`).

### 3. Frontend
```
cd frontend
npm install
npm run dev
```
Variable opcional: `VITE_API_URL` (por defecto `http://localhost:8080`).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/contribuyentes/verificar` | `{email, ruc}` → existe, persona natural, datos |
| GET | `/api/vehiculos/{placa}` | Datos del vehículo (SRI) |
| GET | `/api/licencias/{cedula}` | Puntos de licencia (ANT con caché) |

## Cosas que debes verificar

- **Parser de la ANT** (`AntService.PUNTOS`): es una expresión regular genérica sobre el texto de la página. Abre la URL de la ANT con una cédula real, revisa cómo aparecen los puntos y ajusta el patrón si hace falta.
- **Acceso a SRI/ANT**: estos sitios a veces bloquean IPs fuera de Ecuador o certificados no reconocidos por Java. Pruébalo desde tu red.
- **Persona natural**: se valida con el tercer dígito del RUC (< 6).
