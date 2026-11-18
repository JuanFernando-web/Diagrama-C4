# Modelo C4 para IcePanel – Consulta Vehicular (SRI + ANT)

Patrón de resiliencia: **Cache-Aside + Fallback (stale-if-error)** con Redis en la nube.
Cada respuesta válida de la ANT se guarda en Redis; si la ANT cae, se responde con el último dato guardado.

---

## Nivel 1 – Contexto (Context diagram)

| Tipo | Nombre | Descripción |
|---|---|---|
| Actor | **Usuario** | Persona que consulta su contribuyente, vehículo y puntos de licencia |
| Sistema | **Sistema de Consulta Vehicular** | Valida contribuyente, muestra datos del vehículo y puntos de licencia |
| Sistema externo | **SRI – Catastro de Contribuyentes** | API REST: existePorNumeroRuc / obtenerPorNumerosRuc |
| Sistema externo | **SRI – Matriculación Vehicular** | API REST: obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn |
| Sistema externo | **ANT – Consulta Web** | Página web con citaciones y puntos. **Baja disponibilidad** |
| Sistema externo | **Redis Cloud** | Caché administrado en la nube |

**Conexiones (Level 1)**

1. Usuario → Sistema de Consulta Vehicular: "Consulta contribuyente, vehículo y puntos" (HTTPS)
2. Sistema → SRI Catastro: "Verifica contribuyente y obtiene datos" (HTTPS/REST)
3. Sistema → SRI Matriculación: "Obtiene datos del vehículo" (HTTPS/REST)
4. Sistema → ANT: "Consulta puntos de licencia" (HTTPS/HTML)
5. Sistema → Redis Cloud: "Guarda y lee respuestas de la ANT" (TLS/Redis)

---

## Nivel 2 – Contenedores (Container diagram)

Dentro del sistema, crear como **Apps/Stores**:

| Tipo | Nombre | Tecnología | Responsabilidad |
|---|---|---|---|
| App | **Web App (SPA)** | React + Vite | Formulario de correo/RUC, matrícula y presentación de resultados |
| App | **API Backend** | Java 17, Spring Boot 3 | Orquesta SRI y ANT, aplica el patrón de caché |
| Store | **Caché de licencias** | Redis Cloud | Último dato válido de la ANT por cédula (TTL 30 días, fresco 10 min) |

**Conexiones (Level 2)**

1. Usuario → Web App: "Usa" (HTTPS)
2. Web App → API Backend: "Llama a /api/..." (HTTPS/JSON)
3. API Backend → SRI Catastro: "Verifica RUC y obtiene datos" (HTTPS/JSON)
4. API Backend → SRI Matriculación: "Obtiene vehículo" (HTTPS/JSON)
5. API Backend → ANT: "Consulta puntos" (HTTPS/HTML, timeout 5 s)
6. API Backend → Caché de licencias: "Lee/escribe entradas `ant:licencia:{cedula}`" (TLS/Redis)

---

## Nivel 3 – Componentes (dentro de API Backend)

| Componente | Clase | Responsabilidad |
|---|---|---|
| Controlador REST | `ConsultaController` | Expone `/api/contribuyentes/verificar`, `/api/vehiculos/{placa}`, `/api/licencias/{cedula}` |
| Cliente SRI | `SriService` | Existe contribuyente → persona natural → datos; datos del vehículo |
| Cliente ANT con caché | `AntService` | Cache-Aside + Fallback; parsea el HTML y extrae los puntos |
| Adaptador de caché | `CacheService` | Lee/escribe en Redis; si Redis falla no rompe la consulta |
| Manejo de errores | `GlobalExceptionHandler` | Traduce excepciones a 400/404/422/503 |

**Conexiones (Level 3)**

- Web App → ConsultaController
- ConsultaController → SriService
- ConsultaController → AntService
- AntService → CacheService
- SriService → SRI Catastro / SRI Matriculación
- AntService → ANT
- CacheService → Caché de licencias (Redis)

---

## Flujo para el diagrama (Flows / Diagrams de IcePanel)

1. Usuario ingresa correo + RUC → Web App
2. Web App → API: `POST /contribuyentes/verificar`
3. API → SRI: `existePorNumeroRuc`; si es `false` → 404
4. API valida persona natural (3.er dígito del RUC < 6); si no → 422
5. API → SRI: `obtenerPorNumerosRuc` → datos al usuario
6. Usuario ingresa matrícula → `GET /vehiculos/{placa}` → SRI Matriculación
7. En paralelo `GET /licencias/{cedula}` (cédula = 10 primeros dígitos del RUC):
   - **a)** Caché fresco (< 10 min) → responde `CACHE_FRESCO`
   - **b)** Si no: consulta ANT → válida → guarda en Redis → `ANT_EN_VIVO`
   - **c)** ANT falla → responde último dato de Redis → `CACHE_RESPALDO`
   - **d)** ANT falla y no hay caché → 503

---

## Enlazar con GitHub en IcePanel

1. Sube el proyecto a GitHub (un repo con `backend/` y `frontend/`, o dos repos separados).
2. En IcePanel, selecciona cada objeto (**Web App**, **API Backend**) y en su panel de detalle agrega un **link** a la URL del repositorio/carpeta:
   - Web App → `https://github.com/<usuario>/<repo>/tree/main/frontend`
   - API Backend → `https://github.com/<usuario>/<repo>/tree/main/backend`
3. Opcional: en los componentes del Nivel 3 enlaza cada clase (`.../backend/src/main/java/ec/udla/consulta/service/AntService.java`).
4. En la descripción de cada objeto indica la tecnología (React, Spring Boot, Redis Cloud).

> Los nombres exactos de los menús de IcePanel pueden variar; busca la opción de links/enlaces externos del objeto.
