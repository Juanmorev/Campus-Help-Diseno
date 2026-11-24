# CampusHelp: sistema de gestión de incidencias del campus (API REST)

Cuando un estudiante encuentra un problema en el campus (una lámpara dañada, una silla rota, un proyector que no
funciona o un baño en mal estado), lo reporta en CampusHelp. El administrador lo revisa y, si es válido, lo asigna al
personal de mantenimiento, que lo repara y lo marca como solucionado. El estudiante recibe cada actualización, y un
mapa muestra dónde se concentran los problemas del campus.

**Tecnologías:** Java 21 · Spring Boot 3.5 · Spring Data MongoDB (con GridFS para las fotos) · Bean Validation ·
springdoc-openapi (Swagger) · JUnit 5 con MongoDB embebido para las pruebas.

```
Cliente (navegador / Postman)  --HTTP/JSON-->  Servidor Spring Boot (:8082)  --TCP 27017-->  MongoDB (campushelp_db)
```

Los 5 diagramas de diseño (contexto, funcional, lógico, desarrollo y despliegue) están en `../diagramas`.

## Alcance

**Sí hace:**
- Registrar incidencias con lugar, tipo de problema, descripción, fotografía, fecha y prioridad.
- Flujo de atención: Reportada → En revisión → (no válida) Cerrada / (válida) Asignada → En reparación → Solucionada.
- Permisos por rol: cada operación indica quién la hace (`usuarioId`) y la API comprueba su rol.
- Notificaciones al estudiante en cada cambio.
- Mapa del campus: cada lugar con su color (rojo urgente, amarillo en revisión o proceso, verde solucionado).
- Estadísticas para el administrador.

**No hace (para mantener el alcance):** inicio de sesión con contraseña, aplicación móvil, correos o mensajes push, chat.

## Usuarios y lo que pueden hacer

| Rol | Puede |
|---|---|
| **Estudiante** | Registrar un problema, consultar sus reportes, ver el estado, agregar información y fotos, ver sus notificaciones |
| **Administrador** | Ver todos los reportes, revisarlos, validarlos o cerrarlos, asignar y reasignar responsables, cambiar estados, ver estadísticas, registrar lugares |
| **Mantenimiento** | Ver los problemas asignados, iniciar la reparación, registrar avances, marcar como solucionado |

Si alguien intenta una operación que no le corresponde, la API responde **403** con el motivo.

## Cómo ejecutarla

Requisitos: JDK 21, Maven 3.9 y MongoDB (local en `mongodb://localhost:27017` o Atlas con la variable `MONGODB_URI`).

```bash
mvn spring-boot:run
```

- API: <http://localhost:8082> (se puede cambiar con la variable `PORT`).
- Documentación interactiva: <http://localhost:8082/swagger-ui.html>.
- La primera vez, con la base vacía, carga datos de demostración: 1 administrador, 3 estudiantes, 4 personas de
  mantenimiento, 11 lugares del campus y 13 incidencias en todos los estados (algunas con foto). Para no cargarlos:
  `APP_DATOS_DEMO=false`.

### Postman

Importa `postman/CampusHelp.postman_collection.json` y ejecuta las carpetas en orden (1 a 6). Las variables se llenan
solas y los emails usan `{{$timestamp}}`, así que se puede repetir. En "Subir foto de evidencia", si Postman no
encuentra el archivo, selecciona `postman/foto-ejemplo.png`.

### Pruebas automáticas

```bash
mvn test
```

Son 5 pruebas unitarias del flujo y 2 de integración contra un MongoDB embebido. Cubren:
- el flujo completo por rol;
- las negativas de permisos (403);
- la subida y descarga de una foto real;
- el rechazo de fotos de más de 10 MB con mensaje en español (413);
- el mapa y las estadísticas.

## Endpoints

### Usuarios y notificaciones
| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| POST | `/api/usuarios` | — | Registrar `{nombre, email, codigo, rol, especialidad}` |
| GET | `/api/usuarios?rol=` | — | Listar (filtro opcional por rol) |
| GET | `/api/usuarios/{id}` | — | Consultar |
| DELETE | `/api/usuarios/{id}?adminId=` | Administrador | Desactivar (baja lógica) |
| GET | `/api/usuarios/{id}/notificaciones?soloNoLeidas=` | — | Actualizaciones recibidas |
| PATCH | `/api/notificaciones/{id}/leida?usuarioId=` | Dueño | Marcar como leída |

### Lugares y mapa
| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| POST | `/api/lugares` | Administrador | Registrar `{usuarioId, codigo, nombre, edificio, piso, x, y}` (x, y de 0 a 100 en el plano) |
| GET | `/api/lugares` | — | Listar |
| GET | `/api/mapa` | — | Cada lugar con su color y sus problemas |

### Incidencias
| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| POST | `/api/incidencias` | Estudiante | Registrar `{estudianteId, lugarId, tipo, descripcion, prioridad}` |
| POST | `/api/incidencias/{id}/evidencias?usuarioId=` | Estudiante, admin o responsable | Foto (multipart, campo `archivo`, JPG/PNG/WEBP/GIF, máx. 10 MB) |
| GET | `/api/incidencias/{id}/evidencias/{evidenciaId}` | — | Descargar la foto |
| POST | `/api/incidencias/{id}/informacion` | Quien puede verla | Agregar información `{usuarioId, texto}` |
| GET | `/api/incidencias?usuarioId=&estado=&tipo=&prioridad=&lugarId=` | Todos | Estudiante: las suyas · Mantenimiento: las asignadas · Admin: todas |
| GET | `/api/incidencias/{id o CH-000001}?usuarioId=` | Quien puede verla | Detalle con estado, color y seguimiento |
| GET | `/api/incidencias/{id}/seguimiento?usuarioId=` | Quien puede verla | Historial de cambios |
| PATCH | `/api/incidencias/{id}/revision` | Administrador | Empezar la revisión |
| PATCH | `/api/incidencias/{id}/validacion` | Administrador | `{valida: true, responsableId, prioridad?}` asigna; `{valida: false, motivo}` cierra |
| PATCH | `/api/incidencias/{id}/responsable` | Administrador | Reasignar |
| PATCH | `/api/incidencias/{id}/estado` | Administrador | Cambio manual (reabrir o cerrar) |
| PATCH | `/api/incidencias/{id}/reparacion` | Responsable | Iniciar la reparación |
| POST | `/api/incidencias/{id}/progreso` | Responsable | Registrar un avance |
| PATCH | `/api/incidencias/{id}/solucion` | Responsable | Marcar como solucionado `{usuarioId, descripcion}` |

### Estadísticas
| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| GET | `/api/estadisticas?usuarioId=` | Administrador | Totales por estado, tipo y prioridad, tiempo promedio de solución, lugares con más problemas y carga del personal |

### Errores
Formato `{status, error, mensaje, ruta, detalles, fecha}`:
- **400:** datos inválidos o una foto que no es imagen.
- **403:** el rol no corresponde o el usuario no puede ver esa incidencia.
- **404:** no existe.
- **409:** paso no permitido en el flujo.
- **413:** la foto pesa más de 10 MB.

## Estructura

```
com.campushelp
├── controller   UsuarioController, IncidenciaController, LugarController, EstadisticaController
├── service      IncidenciaService (flujo y permisos), UsuarioService, LugarService, EvidenciaService (GridFS),
│                NotificacionService, EstadisticaService (mapa y estadísticas), GeneradorCodigoService
├── repository   Usuario, Lugar, Incidencia (+ consultas con filtros) y Notificacion
├── model        Incidencia, Usuario, Lugar, Evidencia, Actualizacion, Notificacion, Contador y enums
├── dto          Solicitudes, IncidenciaResponse, Reportes, FiltroIncidencias, ErrorResponse
├── exception    ManejadorGlobalErrores y excepciones del negocio
└── config       ConfiguracionApi (OpenAPI y CORS), DatosDemoInicializador
```
