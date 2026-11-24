# Historias de usuario: CampusHelp

Plataforma para que los estudiantes reporten problemas del campus (una lámpara dañada, una silla rota, un proyector que
no funciona o un baño en mal estado). El administrador revisa cada reporte; si es válido lo asigna al personal de
mantenimiento, que lo repara y lo marca como solucionado. El estudiante recibe cada actualización, y un mapa del campus
muestra con colores dónde están los problemas.

## Roles

Son los actores del diagrama de contexto. En cada operación de la API se indica quién la hace (`usuarioId`) y el
sistema comprueba su rol.

| Rol | Quién es | Rol en el sistema |
|---|---|---|
| **Estudiante** | Persona que encuentra el problema en el campus y lo reporta | `ESTUDIANTE` |
| **Administrador** | Encargado de la planta física: revisa, valida y asigna | `ADMINISTRADOR` |
| **Mantenimiento** | Personal que repara (electricista, carpintero, soporte técnico, plomero…) | `MANTENIMIENTO` |

**Formato:** *Como* [rol] *quiero* [acción] *para* [beneficio]. Los criterios de aceptación usan *Dado / Cuando / Entonces*.
**Prioridad:** MoSCoW (Debe, Debería, Podría). **Estimación:** puntos de historia (1, 2, 3, 5, 8).

## Resumen

| ID | Historia | Rol | Prioridad | Puntos |
|---|---|---|---|---|
| **Épica 1: Reporte de problemas** |||||
| HU-01 | Reportar un problema del campus | Estudiante | Debe | 3 |
| HU-02 | Adjuntar una fotografía como evidencia | Estudiante | Debe | 3 |
| HU-03 | Consultar mis reportes y su estado | Estudiante | Debe | 2 |
| HU-04 | Agregar información a un reporte | Estudiante | Debería | 2 |
| **Épica 2: Revisión y validación** |||||
| HU-05 | Ver todos los reportes y filtrarlos | Administrador | Debe | 3 |
| HU-06 | Revisar un reporte | Administrador | Debe | 1 |
| HU-07 | Cerrar un reporte no válido con su motivo | Administrador | Debe | 2 |
| HU-08 | Validar un reporte y asignar un responsable | Administrador | Debe | 3 |
| HU-09 | Reasignar el responsable | Administrador | Debería | 2 |
| HU-10 | Cambiar el estado manualmente (reabrir o cerrar) | Administrador | Podría | 2 |
| **Épica 3: Reparación** |||||
| HU-11 | Ver los problemas que tengo asignados | Mantenimiento | Debe | 2 |
| HU-12 | Iniciar la reparación | Mantenimiento | Debe | 1 |
| HU-13 | Registrar avances de la reparación | Mantenimiento | Debería | 2 |
| HU-14 | Marcar el problema como solucionado | Mantenimiento | Debe | 2 |
| **Épica 4: Notificaciones y seguimiento** |||||
| HU-15 | Recibir una notificación en cada cambio | Estudiante | Debe | 3 |
| HU-16 | Marcar notificaciones como leídas | Estudiante | Podría | 1 |
| HU-17 | Ver el historial de un reporte | Estudiante | Debe | 2 |
| **Épica 5: Mapa del campus** |||||
| HU-18 | Registrar los lugares del campus en el plano | Administrador | Debe | 2 |
| HU-19 | Ver el mapa con colores por estado | Estudiante | Debe | 5 |
| **Épica 6: Estadísticas** |||||
| HU-20 | Consultar estadísticas generales | Administrador | Debería | 5 |
| HU-21 | Ver la carga de trabajo del personal de mantenimiento | Administrador | Debería | 3 |
| **Épica 7: Gestión de usuarios** |||||
| HU-22 | Registrar usuarios con su rol | Administrador | Debe | 2 |
| HU-23 | Desactivar un usuario sin perder su historial | Administrador | Podría | 1 |
| **Épica 8: Seguridad y verificación** |||||
| HU-24 | Impedir operaciones que no corresponden al rol | Administrador | Debe | 3 |
| HU-25 | Probar la API con Postman y Swagger | Desarrollador | Debe | 3 |
| | | | **Total** | **60** |

## Flujo de estados

```
REPORTADA ──► EN_REVISION ──► ASIGNADA ──► EN_REPARACION ──► SOLUCIONADA
    │              │              │                              │
    └──────────────┴──► CERRADA ◄─┘          (reabrir) ◄─────────┘
```

| Color en el mapa | Significado |
|---|---|
| 🔴 Rojo | Hay al menos un problema **urgente** sin solucionar |
| 🟡 Amarillo | Hay problemas en revisión, asignados o en reparación |
| 🟢 Verde | Los problemas del lugar ya están solucionados |
| ⚪ Gris | El lugar no tiene problemas reportados |

---

## Épica 1: Reporte de problemas

### HU-01: Reportar un problema del campus
**Como** estudiante **quiero** reportar un problema indicando el lugar, el tipo, una descripción y la prioridad
**para** que la universidad lo repare.

- Dado que indico lugar, tipo (eléctrico, mobiliario, tecnología, sanitario, infraestructura, limpieza u otro),
  descripción y prioridad (baja, media, alta o urgente), cuando envío el reporte, entonces se crea en estado
  **Reportada** con un código consecutivo `CH-000001`.
- El sistema guarda la fecha del reporte y el primer registro del historial.
- Recibo una notificación: "recibimos tu reporte en [lugar]. Está pendiente de revisión."
- Dado que falta un dato obligatorio o el lugar no existe, entonces recibo un error 400 o 404 que dice qué falta.
- Dado que quien reporta no es estudiante, entonces recibo un error 403.

*Implementación:* `POST /api/incidencias`.

### HU-02: Adjuntar una fotografía como evidencia
**Como** estudiante **quiero** adjuntar una foto del problema **para** que el administrador y el personal de
mantenimiento vean qué pasa sin tener que ir al lugar.

- Dado que subo una imagen JPG, PNG, WEBP o GIF de hasta 10 MB, entonces queda guardada en el reporte (GridFS) y
  se puede descargar.
- Dado que el archivo no es una imagen, entonces recibo un error 400; si pesa más de 10 MB, un error 413 con el
  mensaje en español.
- También pueden subir fotos el administrador y el responsable asignado (por ejemplo, la foto del arreglo).
- Dado que el reporte está **Cerrado**, entonces no admite más fotos (409).

*Implementación:* `POST /api/incidencias/{id}/evidencias` (multipart, campo `archivo`) ·
`GET /api/incidencias/{id}/evidencias/{evidenciaId}`.

### HU-03: Consultar mis reportes y su estado
**Como** estudiante **quiero** ver la lista de mis reportes y el detalle de cada uno **para** saber en qué va.

- Cuando consulto la lista, entonces solo veo los reportes que yo hice, con su estado y su color.
- Puedo consultar un reporte por su id o por su código (`CH-000001`).
- Dado que intento ver el reporte de otro estudiante, entonces recibo un error 403.

*Implementación:* `GET /api/incidencias?usuarioId=` · `GET /api/incidencias/{id o código}?usuarioId=`.

### HU-04: Agregar información a un reporte
**Como** estudiante **quiero** agregar más detalles a mi reporte **para** aclarar el problema si cambia o empeora.

- Dado que el reporte no está cerrado, cuando agrego un texto, entonces queda en el historial con mi nombre y la fecha.
- Dado que el texto está vacío o el reporte está cerrado, entonces recibo un error que lo explica.

*Implementación:* `POST /api/incidencias/{id}/informacion`.

## Épica 2: Revisión y validación

### HU-05: Ver todos los reportes y filtrarlos
**Como** administrador **quiero** ver todos los reportes y filtrarlos por estado, tipo, prioridad o lugar **para**
decidir qué atender primero.

- Dado que soy administrador, entonces veo los reportes de todos los estudiantes.
- Puedo combinar los filtros `estado`, `tipo`, `prioridad` y `lugarId`.

*Implementación:* `GET /api/incidencias?usuarioId=&estado=&tipo=&prioridad=&lugarId=`.

### HU-06: Revisar un reporte
**Como** administrador **quiero** marcar que estoy revisando un reporte **para** que el estudiante sepa que ya lo vieron.

- Dado un reporte en estado **Reportada**, cuando inicio la revisión, entonces pasa a **En revisión**.
- El estudiante recibe la notificación "el administrador está revisando tu reporte".

*Implementación:* `PATCH /api/incidencias/{id}/revision`.

### HU-07: Cerrar un reporte no válido con su motivo
**Como** administrador **quiero** cerrar los reportes que no son válidos (duplicados, falsos o fuera del campus)
**para** que no ocupen al personal de mantenimiento.

- Dado un reporte que todavía no fue validado, cuando lo marco como no válido con un motivo, entonces pasa a **Cerrada**.
- Dado que no escribo el motivo, entonces recibo un error 409 que lo pide.
- El estudiante recibe la notificación con el motivo del cierre.

*Implementación:* `PATCH /api/incidencias/{id}/validacion` con `{valida: false, motivo}`.

### HU-08: Validar un reporte y asignar un responsable
**Como** administrador **quiero** validar un reporte y asignarlo a una persona de mantenimiento **para** que lo repare.

- Dado un reporte válido, cuando indico el responsable (y si quiero ajusto la prioridad), entonces pasa a **Asignada**.
- Dado que el responsable no es personal de mantenimiento activo, entonces recibo un error 409.
- El estudiante recibe la notificación "tu reporte fue validado y asignado a [nombre]".

*Implementación:* `PATCH /api/incidencias/{id}/validacion` con `{valida: true, responsableId, prioridad?}`.

### HU-09: Reasignar el responsable
**Como** administrador **quiero** cambiar el responsable de un reporte **para** repartir mejor el trabajo o cubrir
ausencias.

- Dado un reporte **Asignado** o **En reparación**, cuando elijo otro responsable, entonces queda asignado a él.
- Dado un reporte en otro estado, entonces recibo un error 409.
- El estudiante recibe la notificación "ahora lo atiende [nombre]".

*Implementación:* `PATCH /api/incidencias/{id}/responsable`.

### HU-10: Cambiar el estado manualmente (reabrir o cerrar)
**Como** administrador **quiero** reabrir un problema que volvió a fallar o cerrar uno con motivo **para** corregir
casos especiales.

- Dado un reporte **Solucionado**, cuando lo reabro, entonces vuelve a **En reparación**.
- Solo se permiten los cambios del flujo; si no, recibo un error 409 que dice a qué estados sí puede pasar.
- No se puede marcar como solucionado desde aquí: eso lo hace el responsable (HU-14).

*Implementación:* `PATCH /api/incidencias/{id}/estado`.

## Épica 3: Reparación

### HU-11: Ver los problemas que tengo asignados
**Como** persona de mantenimiento **quiero** ver solo los problemas que me asignaron **para** organizar mi trabajo.

- Dado que soy de mantenimiento, cuando consulto la lista, entonces veo únicamente los reportes asignados a mí.
- Puedo filtrar por estado y prioridad para atender primero los urgentes.

*Implementación:* `GET /api/incidencias?usuarioId=`.

### HU-12: Iniciar la reparación
**Como** persona de mantenimiento **quiero** marcar que empecé a reparar **para** que el estudiante sepa que ya se
está atendiendo.

- Dado un reporte **Asignado** a mí, cuando inicio la reparación, entonces pasa a **En reparación**.
- Dado que el reporte está asignado a otra persona, entonces recibo un error 409.

*Implementación:* `PATCH /api/incidencias/{id}/reparacion`.

### HU-13: Registrar avances de la reparación
**Como** persona de mantenimiento **quiero** anotar los avances (por ejemplo, "se pidió el repuesto") **para** que
quede constancia y el estudiante esté informado.

- Dado un reporte **En reparación**, cuando escribo un avance, entonces queda en el historial y se notifica al estudiante.
- Dado que el texto está vacío o el reporte no está en reparación, entonces recibo un error que lo explica.

*Implementación:* `POST /api/incidencias/{id}/progreso`.

### HU-14: Marcar el problema como solucionado
**Como** persona de mantenimiento **quiero** marcar el problema como solucionado y describir qué hice **para** cerrar
el ciclo.

- Dado un reporte **En reparación** asignado a mí, cuando escribo la solución, entonces pasa a **Solucionada** y se
  guarda la fecha de solución.
- El lugar se pinta en verde en el mapa si ya no tiene otros problemas pendientes.
- El estudiante recibe la notificación "¡solucionado!" con la descripción.

*Implementación:* `PATCH /api/incidencias/{id}/solucion`.

## Épica 4: Notificaciones y seguimiento

### HU-15: Recibir una notificación en cada cambio
**Como** estudiante **quiero** recibir una notificación cada vez que mi reporte cambia **para** no tener que estar
preguntando.

- Recibo una notificación al reportar, al iniciar la revisión, al validar o cerrar, al reasignar, al iniciar la
  reparación, con cada avance y al solucionar.
- Cada notificación incluye el código del reporte y la fecha.
- Puedo pedir solo las que no he leído.

*Implementación:* `GET /api/usuarios/{id}/notificaciones?soloNoLeidas=`.

### HU-16: Marcar notificaciones como leídas
**Como** estudiante **quiero** marcar una notificación como leída **para** distinguir las nuevas.

- Cuando marco una notificación mía, entonces queda como leída.
- Dado que la notificación es de otro usuario, entonces recibo un error 403.

*Implementación:* `PATCH /api/notificaciones/{id}/leida?usuarioId=`.

### HU-17: Ver el historial de un reporte
**Como** estudiante **quiero** ver todo lo que ha pasado con mi reporte **para** saber quién lo atendió y cuándo.

- El historial muestra cada cambio con la fecha, el autor, el mensaje y el estado anterior y nuevo.
- Lo pueden ver el estudiante que reportó, el responsable asignado y el administrador.

*Implementación:* `GET /api/incidencias/{id}/seguimiento?usuarioId=`.

## Épica 5: Mapa del campus

### HU-18: Registrar los lugares del campus en el plano
**Como** administrador **quiero** registrar los lugares del campus (salones, laboratorios, baños, zonas comunes) con
su posición en el plano **para** que se puedan reportar y ubicar en el mapa.

- Dado código, nombre, edificio, piso y posición (x, y de 0 a 100), cuando lo registro, entonces queda disponible.
- Dado que el código ya existe, entonces recibo un error 409.
- Solo el administrador puede registrar lugares (403 para los demás).

*Implementación:* `POST /api/lugares` · `GET /api/lugares`.

### HU-19: Ver el mapa con colores por estado
**Como** estudiante **quiero** ver un mapa del campus con colores **para** saber dónde hay problemas y si ya se
están atendiendo.

- Cada lugar se pinta en **rojo** si tiene algún problema urgente sin solucionar, en **amarillo** si tiene problemas en
  revisión o en proceso, en **verde** si sus problemas ya están solucionados y en **gris** si no tiene reportes.
- Los reportes cerrados por no válidos no cuentan en el mapa.
- Cada lugar muestra cuántos problemas tiene de cada color y la lista de sus reportes.
- El mapa también trae el total de lugares en rojo, amarillo y verde.

*Implementación:* `GET /api/mapa`.

## Épica 6: Estadísticas

### HU-20: Consultar estadísticas generales
**Como** administrador **quiero** ver estadísticas de los reportes **para** detectar los lugares y tipos de problema
más frecuentes y mejorar el mantenimiento.

- Veo el total de reportes, cuántos están activos, solucionados y cerrados.
- Veo los totales por estado, por tipo y por prioridad.
- Veo el tiempo promedio de solución (en horas), en general y por tipo de problema.
- Veo los lugares con más problemas.
- Solo el administrador puede consultarlas (403 para los demás).

*Implementación:* `GET /api/estadisticas?usuarioId=`.

### HU-21: Ver la carga de trabajo del personal de mantenimiento
**Como** administrador **quiero** ver cuántos reportes tiene cada persona de mantenimiento **para** asignar al que
tenga menos trabajo.

- Por cada persona veo cuántos reportes tiene asignados, en reparación y solucionados, y su tiempo promedio de solución.

*Implementación:* `GET /api/estadisticas?usuarioId=` (campo `cargaMantenimiento`).

## Épica 7: Gestión de usuarios

### HU-22: Registrar usuarios con su rol
**Como** administrador **quiero** registrar estudiantes, administradores y personal de mantenimiento **para** que
puedan usar CampusHelp.

- Dado nombre, email, código y rol (y la especialidad si es de mantenimiento), cuando lo registro, entonces queda activo.
- Dado que el email o el código ya existen, entonces recibo un error 409.
- Puedo listar los usuarios y filtrarlos por rol.

*Implementación:* `POST /api/usuarios` · `GET /api/usuarios?rol=`.

### HU-23: Desactivar un usuario sin perder su historial
**Como** administrador **quiero** desactivar a un usuario que ya no está en la universidad **para** que no pueda
operar, sin borrar sus reportes.

- Cuando lo desactivo, entonces queda inactivo (baja lógica) y sus reportes se conservan.
- Dado que un usuario inactivo intenta una operación, entonces recibo un error 403.

*Implementación:* `DELETE /api/usuarios/{id}?adminId=`.

## Épica 8: Seguridad y verificación

### HU-24: Impedir operaciones que no corresponden al rol
**Como** administrador **quiero** que cada rol solo pueda hacer lo que le corresponde **para** que el flujo sea
confiable.

- Un estudiante no puede validar, asignar ni reparar; mantenimiento no puede validar ni ver reportes que no le asignaron.
- En esos casos la API responde **403** con el motivo en español.
- Los pasos fuera del flujo (por ejemplo, solucionar un reporte que no está en reparación) responden **409**.
- Todos los errores tienen el formato `{status, error, mensaje, ruta, detalles, fecha}`.

*Implementación:* `IncidenciaService` (permisos) · `ManejadorGlobalErrores`.

### HU-25: Probar la API con Postman y Swagger
**Como** desarrollador **quiero** probar todos los endpoints **para** comprobar que el flujo funciona de punta a punta.

- La colección de Postman recorre el flujo completo en carpetas ordenadas (1 a 6) y llena sus variables sola.
- Swagger documenta todos los endpoints en `/swagger-ui.html`.
- Las pruebas automáticas (`mvn test`) cubren el flujo por rol, los permisos, las fotos, el mapa y las estadísticas.
- La primera vez se cargan datos de demostración (usuarios, 11 lugares y 13 reportes en todos los estados).

*Implementación:* `campushelp-api/postman/` · springdoc-openapi · `src/test`.
