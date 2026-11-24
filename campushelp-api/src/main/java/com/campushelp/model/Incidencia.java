package com.campushelp.model;

import com.campushelp.exception.ReglaNegocioException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Problema reportado en el campus. Es la raiz del agregado: las evidencias y el seguimiento viven
 * embebidos en el mismo documento. Las reglas del flujo (revisar, validar o cerrar, asignar,
 * reparar y solucionar) estan aqui para que ninguna capa deje la incidencia en un estado invalido.
 */
@Document(collection = "incidencias")
public class Incidencia {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    @Indexed
    private String lugarId;
    private TipoProblema tipo;
    private String descripcion;
    private Prioridad prioridad;
    @Indexed
    private EstadoIncidencia estado;

    @Indexed
    private String reportadaPorId;
    @Indexed
    private String responsableId;

    private Instant fechaReporte;
    private Instant fechaRevision;
    private Instant fechaAsignacion;
    private Instant fechaInicioReparacion;
    private Instant fechaSolucion;
    private Instant fechaCierre;

    private String motivoCierre;
    private String solucion;

    private List<Evidencia> evidencias = new ArrayList<>();
    private List<Actualizacion> seguimiento = new ArrayList<>();

    // ------------------------------------------------------------------ flujo principal

    /** El estudiante registra el problema: queda REPORTADA. */
    public void registrar(String codigo, Usuario estudiante) {
        this.codigo = codigo;
        this.reportadaPorId = estudiante.getId();
        this.estado = EstadoIncidencia.REPORTADA;
        this.fechaReporte = Instant.now();
        anotar(TipoActualizacion.REGISTRO, estudiante.getId(), "Problema reportado", null, estado);
    }

    /** El administrador empieza a revisar el reporte. */
    public void iniciarRevision(Usuario admin) {
        cambiar(EstadoIncidencia.EN_REVISION, TipoActualizacion.REVISION, admin.getId(), "El administrador está revisando el reporte");
        this.fechaRevision = Instant.now();
    }

    /** Reporte valido: se asigna a un responsable de mantenimiento (puede ajustarse la prioridad). */
    public void validarYAsignar(Usuario admin, Usuario responsable, Prioridad prioridadAjustada) {
        exigirResponsableValido(responsable);
        if (prioridadAjustada != null && prioridadAjustada != prioridad) {
            anotar(TipoActualizacion.VALIDACION, admin.getId(),
                    "Prioridad ajustada de " + prioridad + " a " + prioridadAjustada, estado, estado);
            this.prioridad = prioridadAjustada;
        }
        anotar(TipoActualizacion.VALIDACION, admin.getId(), "Reporte validado", estado, estado);
        if (fechaRevision == null) fechaRevision = Instant.now();
        this.responsableId = responsable.getId();
        this.fechaAsignacion = Instant.now();
        cambiar(EstadoIncidencia.ASIGNADA, TipoActualizacion.ASIGNACION, admin.getId(),
                "Asignada a " + responsable.getNombre());
    }

    /** Reporte no valido (duplicado, no es un problema real...): se cierra con un motivo. */
    public void rechazar(Usuario admin, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Para cerrar un reporte no válido hay que indicar el motivo");
        }
        if (estado != EstadoIncidencia.REPORTADA && estado != EstadoIncidencia.EN_REVISION) {
            throw new ReglaNegocioException("Solo se puede rechazar un reporte que todavía no ha sido validado");
        }
        this.motivoCierre = motivo;
        if (fechaRevision == null) fechaRevision = Instant.now();
        this.fechaCierre = Instant.now();
        cambiar(EstadoIncidencia.CERRADA, TipoActualizacion.RECHAZO, admin.getId(), "Reporte cerrado: " + motivo);
    }

    /** El administrador cambia el responsable de una incidencia ya asignada. */
    public void reasignar(Usuario admin, Usuario responsable) {
        if (estado != EstadoIncidencia.ASIGNADA && estado != EstadoIncidencia.EN_REPARACION) {
            throw new ReglaNegocioException("Solo se puede reasignar una incidencia asignada o en reparación");
        }
        exigirResponsableValido(responsable);
        String anterior = responsableId;
        this.responsableId = responsable.getId();
        this.fechaAsignacion = Instant.now();
        anotar(TipoActualizacion.ASIGNACION, admin.getId(), "Reasignada a " + responsable.getNombre()
                + (anterior == null ? "" : " (antes: " + anterior + ")"), estado, estado);
    }

    /** El responsable empieza la reparacion. */
    public void iniciarReparacion(Usuario mantenimiento, String nota) {
        exigirResponsable(mantenimiento);
        if (estado != EstadoIncidencia.ASIGNADA) {
            // Reabrir una incidencia solucionada lo decide el administrador (cambio de estado manual)
            throw new ReglaNegocioException("La reparación se inicia cuando la incidencia está ASIGNADA (estado actual: "
                    + estado + ")");
        }
        this.fechaInicioReparacion = Instant.now();
        cambiar(EstadoIncidencia.EN_REPARACION, TipoActualizacion.INICIO_REPARACION, mantenimiento.getId(),
                nota == null || nota.isBlank() ? "Reparación iniciada" : nota);
    }

    /** El responsable informa un avance sin cambiar el estado. */
    public void registrarProgreso(Usuario mantenimiento, String nota) {
        exigirResponsable(mantenimiento);
        if (estado != EstadoIncidencia.EN_REPARACION) {
            throw new ReglaNegocioException("El progreso se registra cuando la incidencia está en reparación");
        }
        if (nota == null || nota.isBlank()) throw new ReglaNegocioException("Describe el avance");
        anotar(TipoActualizacion.PROGRESO, mantenimiento.getId(), nota, estado, estado);
    }

    /** El responsable marca el problema como solucionado. */
    public void solucionar(Usuario mantenimiento, String descripcionSolucion) {
        exigirResponsable(mantenimiento);
        if (descripcionSolucion == null || descripcionSolucion.isBlank()) {
            throw new ReglaNegocioException("Describe qué se hizo para solucionar el problema");
        }
        this.solucion = descripcionSolucion;
        this.fechaSolucion = Instant.now();
        cambiar(EstadoIncidencia.SOLUCIONADA, TipoActualizacion.SOLUCION, mantenimiento.getId(), descripcionSolucion);
    }

    /** Cambio de estado manual del administrador (por ejemplo, reabrir o cerrar), validando las transiciones. */
    public void cambiarEstadoManual(Usuario admin, EstadoIncidencia nuevo, String motivo) {
        if (nuevo == EstadoIncidencia.ASIGNADA) {
            throw new ReglaNegocioException("Para asignar use la validación indicando el responsable");
        }
        if (nuevo == EstadoIncidencia.SOLUCIONADA) {
            throw new ReglaNegocioException("Solo el responsable puede marcar la incidencia como solucionada");
        }
        if (nuevo == EstadoIncidencia.CERRADA) {
            if (motivo == null || motivo.isBlank()) throw new ReglaNegocioException("Indica el motivo del cierre");
            this.motivoCierre = motivo;
            this.fechaCierre = Instant.now();
        }
        if (nuevo == EstadoIncidencia.EN_REPARACION && estado == EstadoIncidencia.SOLUCIONADA) {
            this.fechaSolucion = null;
            this.solucion = null;
        }
        cambiar(nuevo, TipoActualizacion.CAMBIO_ESTADO, admin.getId(),
                motivo == null || motivo.isBlank() ? "Cambio de estado" : motivo);
    }

    /** El estudiante (o el personal) agrega informacion adicional. */
    public void agregarInformacion(Usuario autor, String texto) {
        if (estado == EstadoIncidencia.CERRADA) {
            throw new ReglaNegocioException("La incidencia " + codigo + " está cerrada y no admite cambios");
        }
        if (texto == null || texto.isBlank()) throw new ReglaNegocioException("Escribe la información a agregar");
        anotar(TipoActualizacion.INFORMACION_ADICIONAL, autor.getId(), texto, estado, estado);
    }

    public void agregarEvidencia(Evidencia evidencia, Usuario autor) {
        if (estado == EstadoIncidencia.CERRADA) {
            throw new ReglaNegocioException("La incidencia " + codigo + " está cerrada y no admite evidencias");
        }
        evidencias.add(evidencia);
        anotar(TipoActualizacion.EVIDENCIA, autor.getId(), "Fotografía agregada: " + evidencia.getNombreArchivo(), estado, estado);
    }

    // ------------------------------------------------------------------ consultas

    /** Color del problema en el mapa: rojo urgente, amarillo en revision o proceso, verde solucionado. */
    public ColorMapa colorMapa() {
        if (estado == EstadoIncidencia.SOLUCIONADA) return ColorMapa.VERDE;
        if (estado == EstadoIncidencia.CERRADA) return ColorMapa.GRIS;
        return prioridad == Prioridad.URGENTE ? ColorMapa.ROJO : ColorMapa.AMARILLO;
    }

    /** Horas entre el reporte y la solucion (null si no esta solucionada). */
    public Double horasHastaSolucion() {
        if (fechaSolucion == null || fechaReporte == null) return null;
        return Duration.between(fechaReporte, fechaSolucion).toMinutes() / 60.0;
    }

    public boolean esDe(Usuario u) {
        return u.getId().equals(reportadaPorId);
    }

    public boolean estaAsignadaA(Usuario u) {
        return u.getId().equals(responsableId);
    }

    // ------------------------------------------------------------------ auxiliares

    private void exigirResponsableValido(Usuario responsable) {
        if (!responsable.tieneRol(Rol.MANTENIMIENTO) || !responsable.isActivo()) {
            throw new ReglaNegocioException(responsable.getNombre() + " no es personal de mantenimiento activo");
        }
    }

    private void exigirResponsable(Usuario u) {
        if (!estaAsignadaA(u)) {
            throw new ReglaNegocioException("La incidencia " + codigo + " no está asignada a " + u.getNombre());
        }
    }

    private void cambiar(EstadoIncidencia nuevo, TipoActualizacion tipo, String autorId, String mensaje) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new ReglaNegocioException("No se puede pasar de " + estado + " a " + nuevo
                    + ". Pasos permitidos: " + estado.siguientesPermitidos());
        }
        EstadoIncidencia anterior = estado;
        this.estado = nuevo;
        anotar(tipo, autorId, mensaje, anterior, nuevo);
    }

    private void anotar(TipoActualizacion tipo, String autorId, String mensaje,
                        EstadoIncidencia anterior, EstadoIncidencia nuevo) {
        seguimiento.add(new Actualizacion(tipo, autorId, mensaje, anterior, nuevo));
    }

    // ------------------------------------------------------------------ getters / setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getLugarId() { return lugarId; }
    public void setLugarId(String lugarId) { this.lugarId = lugarId; }
    public TipoProblema getTipo() { return tipo; }
    public void setTipo(TipoProblema tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public EstadoIncidencia getEstado() { return estado; }
    public void setEstado(EstadoIncidencia estado) { this.estado = estado; }
    public String getReportadaPorId() { return reportadaPorId; }
    public void setReportadaPorId(String reportadaPorId) { this.reportadaPorId = reportadaPorId; }
    public String getResponsableId() { return responsableId; }
    public void setResponsableId(String responsableId) { this.responsableId = responsableId; }
    public Instant getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(Instant fechaReporte) { this.fechaReporte = fechaReporte; }
    public Instant getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(Instant fechaRevision) { this.fechaRevision = fechaRevision; }
    public Instant getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(Instant fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    public Instant getFechaInicioReparacion() { return fechaInicioReparacion; }
    public void setFechaInicioReparacion(Instant fechaInicioReparacion) { this.fechaInicioReparacion = fechaInicioReparacion; }
    public Instant getFechaSolucion() { return fechaSolucion; }
    public void setFechaSolucion(Instant fechaSolucion) { this.fechaSolucion = fechaSolucion; }
    public Instant getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Instant fechaCierre) { this.fechaCierre = fechaCierre; }
    public String getMotivoCierre() { return motivoCierre; }
    public void setMotivoCierre(String motivoCierre) { this.motivoCierre = motivoCierre; }
    public String getSolucion() { return solucion; }
    public void setSolucion(String solucion) { this.solucion = solucion; }
    public List<Evidencia> getEvidencias() { return evidencias; }
    public void setEvidencias(List<Evidencia> evidencias) { this.evidencias = evidencias; }
    public List<Actualizacion> getSeguimiento() { return seguimiento; }
    public void setSeguimiento(List<Actualizacion> seguimiento) { this.seguimiento = seguimiento; }
}
