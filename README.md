# Jona Labels Backend

API REST para la gestión de cotizaciones, pedidos y reseñas de etiquetas textiles personalizadas. Plataforma B2B que conecta a marcas de ropa con proveedores de producción de etiquetas de satén y colgantes.

## Tech Stack

- **Java 21** + **Spring Boot 4.1.0**
- **Spring Security** — Autenticación JWT basada en cookies HttpOnly
- **Spring Data JPA** + **Hibernate** — Capa de persistencia
- **PostgreSQL** — Base de datos relacional (compatible con Neon, Supabase, RDS)
- **Flyway** — Migraciones de esquema controladas por versión
- **Cloudinary** — Almacenamiento y entrega de imágenes en la nuba
- **Spring Mail** — Notificaciones por correo electrónico
- **Maven** — Gestión de dependencias y build
- **Lombok** — Reducción de boilerplate

## Arquitectura

```
com.jonalabels
├── security/
│   ├── config/SecurityConfig          # CORS, filtros JWT, reglas de acceso
│   └── jwt/JwtService, JwtFilter      # Generación, validación y extracción de tokens
├── auth/
│   ├── controller/AuthController      # login, refresh, logout, registro
│   ├── service/AuthServiceImpl        # Lógica de autenticación y tokens
│   ├── domain/Usuario                 # Entidad (implementa UserDetails)
│   └── config/AdminUserSeeder         # Seed del usuario admin
├── pedido/
│   ├── controller/CotizacionController    # CRUD de cotizaciones (público + autenticado)
│   ├── controller/PedidoController        # Flujo de estados del pedido
│   ├── service/CotizacionServiceImpl      # Crear, paginar, exportar CSV, métricas
│   ├── service/PedidoServiceImpl          # Máquina de estados ESPERANDO → COTIZADO → PAGADO
│   ├── domain/Cotizacion, Pedido          # Entidades
│   └── domain/EstadoCotizacion            # Enum: NUEVO, CONTACTADO, COTIZADO, CERRADO
├── resena/
│   ├── controller/ResenaController        # Crear, moderar, listar aprobadas
│   ├── service/ResenaServiceImpl          # Validación de propiedad y estado del pedido
│   └── domain/Resena, EstadoModeracion    # Entidad + Enum: PENDIENTE, APROBADA, RECHAZADA
├── archivo/
│   ├── controller/ArchivoController       # Subida y descarga de archivos
│   └── service/LocalFileSystemStorageService  # Almacenamiento local en disco
├── cloudinary/
│   ├── config/CloudinaryConfig            # Bean condicional (requiere CLOUDINARY_URL)
│   └── service/CloudinaryService          # Upload a Cloudinary → secure_url
└── email/
    └── service/EmailService               # Notificación async de nuevas cotizaciones
```

## Módulos

| Módulo | Descripción |
|--------|-------------|
| **Auth** | Registro, login, refresh y logout. JWT en cookies HttpOnly con SameSite configurable. |
| **Cotizaciones** | Formulario público de clientes + dashboard administrativo con paginación, filtros, métricas y exportación CSV. |
| **Pedidos** | Flujo de estados: `ESPERANDO_FACTIBILIDAD` → `COTIZADO` → `PAGADO`. Validación estricta de transiciones. |
| **Reseñas** | Clientes crean reseñas (solo pedidos pagados). Admin modera: pendiente → aprobada/rechazada. |
| **Archivos** | Subida y descarga local. Extensiones permitidas: jpg, jpeg, png, pdf. |
| **Cloudinary** | Upload de diseños de clientes a CDN. Bean condicional: solo activo con `CLOUDINARY_URL`. |

## Requisitos Previos

- **Java 21** (JDK, no JRE)
- **Maven 3.9+**
- **PostgreSQL 15+** (local o servicio managed)
- **Docker** (opcional, para PostgreSQL local)

## Variables de entorno

```yaml
# Base de datos
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/etiquetas_rfq_db
SPRING_DATASOURCE_USERNAME: tu_usuario
SPRING_DATASOURCE_PASSWORD: tu_contraseña

# JWT
jwt:
  secret: <base64-encoded-secret>
  cookie-secure: "false"          # true en producción
  cookie-same-site: Lax           # None en producción (cross-origin)

# Correo
MAIL_HOST: smtp.gmail.com
MAIL_PORT: 587
MAIL_USERNAME: tu@email.com
MAIL_PASSWORD: tu-app-password

# Cloudinary (opcional)
CLOUDINARY_URL: cloudinary://api_key:api_secret@cloud_name

# Admin seed (solo desarrollo; deshabilitado en prod)
ADMIN_EMAIL: admin@jonalabels.com
APP_ADMIN_SEED_ENABLED: "true"
APP_ADMIN_SEED_PASSWORD: tu_password_seguro

# Rate limiting
APP_RATE_LIMIT_MAX: 30
APP_RATE_LIMIT_WINDOW: 60
```

Documentación interactiva disponible en `/swagger-ui.html` cuando la app está corriendo.

Las variables con `${VAR:default}` usan el valor por defecto en local. En producción, establece cada una como variable de entorno (especialmente `JWT_SECRET`, credenciales de BD y `APP_ADMIN_SEED_PASSWORD` vacío/deshabilitado).

## Ejecución Local

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/jonalabels-backend.git
cd jonalabels-backend

# 2. Levantar PostgreSQL (si no tienes uno local)
docker compose up -d

# 3. Compilar e instalar dependencias
mvn clean install

# 4. Ejecutar la aplicación
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080`.

El usuario admin se crea automáticamente al iniciar **solo si** `APP_ADMIN_SEED_PASSWORD` está definido:
- **Email:** `admin@jonalabels.com` (configurable con `ADMIN_EMAIL`)
- **Password:** valor de `APP_ADMIN_SEED_PASSWORD`

## Migraciones Flyway

| Versión | Archivo | Descripción |
|---------|---------|-------------|
| V1 | `V1__init_schema.sql` | Esquema inicial: usuarios, productos, talleres, diseños, pedidos, reseñas |
| V2 | `V2__resena_estado_moderacion.sql` | Renombra columna `estado` → `estado_moderacion` en reseñas |
| V3 | `V3__usuario_telefono.sql` | Agrega campo `telefono` a usuarios |
| V4 | `V4__pedido_url_diseno.sql` | Agrega campo `url_diseno` a pedidos |
| V5 | `V5__create_cotizaciones.sql` | Crea tabla `cotizaciones` con campos de contacto y diseño |
| V6 | `V6__fix_cotizaciones_estado_column.sql` | Elimina columna redundante `estado_cotizacion` (solo `estado` está mapeada en JPA) |

## Endpoints

### Autenticación

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/v1/auth/registro` | Público | Registrar nuevo usuario |
| `POST` | `/api/v1/auth/login` | Público | Iniciar sesión (Set-Cookie: access_token, refresh_token) |
| `POST` | `/api/v1/auth/refresh` | Público | Refrescar tokens (requiere cookie refresh_token) |
| `POST` | `/api/v1/auth/logout` | Público | Cerrar sesión (limpia cookies) |

### Cotizaciones

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/v1/cotizaciones` | **Público** | Crear cotización (multipart: `data` JSON + `archivo`) |
| `GET` | `/api/v1/cotizaciones` | Autenticado | Listar cotizaciones paginadas (page, size, busqueda, estado) |
| `GET` | `/api/v1/cotizaciones/metricas` | Autenticado | Métricas del dashboard (total, piezas, nuevos) |
| `GET` | `/api/v1/cotizaciones/exportar` | Autenticado | Exportar CSV con filtros |
| `PATCH` | `/api/v1/cotizaciones/{id}/estado` | Autenticado | Cambiar estado (RequestParam: estado) |

### Pedidos

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/v1/pedidos` | CLIENTE | Crear solicitud de pedido |
| `PATCH` | `/api/v1/pedidos/{id}/cotizacion` | ADMIN | Cotizar pedido (asignar taller, costos) |
| `PATCH` | `/api/v1/pedidos/{id}/pago` | CLIENTE | Registrar pago del pedido |

### Reseñas

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/v1/resenas` | CLIENTE | Crear reseña (requiere pedido PAGADO) |
| `PATCH` | `/api/v1/resenas/{id}/moderacion` | ADMIN | Moderar reseña (aprobar/rechazar) |
| `GET` | `/api/v1/resenas` | **Público** | Listar reseñas aprobadas |

### Archivos

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| `POST` | `/api/v1/archivos` | CLIENTE | Subir archivo (jpg, png, pdf) |
| `GET` | `/api/v1/archivos/{nombre}` | **Público** | Descargar archivo |

## CORS

Las solicitudes cross-origin están habilitadas para:

```java
allowedOrigins:
  http://localhost:5173      // Vite dev server
  http://localhost:3000      // alternativa local
  https://jonalabels.vercel.app  // frontend en producción
  https://jonalabels.com         // dominio personalizado
```

## Producción

Variables mínimas requeridas para despliegue:

```bash
# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://tu-host/tu-db?sslmode=require
SPRING_DATASOURCE_USERNAME=tu_usuario
SPRING_DATASOURCE_PASSWORD=tu_password

# JWT (obligatorio para cross-origin)
jwt.cookie-secure=true
jwt.cookie-same-site=None
jwt.secret=<genera-un-secret-base64-seguro>

# Correo
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu@email.com
MAIL_PASSWORD=tu-app-password

# Cloudinary
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
```

> **Nota:** `SameSite=None` requiere `Secure=true`. Ambos valores deben establecerse en producción para que las cookies funcionen entre dominios distintos (frontend en Vercel, backend en plataforma separada).

## Licencia

Proyecto privado — Jona Labels © 2026
