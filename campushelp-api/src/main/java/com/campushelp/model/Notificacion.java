package com.campushelp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Aviso para el estudiante cada vez que su incidencia cambia ("el estudiante recibe actualizacion"). */
@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;
    @Indexed
    private String usuarioId;
    private String incidenciaId;
    private String codigoIncidencia;
    private String mensaje;
    private Instant fecha = Instant.now();
    private boolean leida;

    public Notificacion() {
    }

    public Notificacion(String usuarioId, String incidenciaId, String codigoIncidencia, String mensaje) {
        this.usuarioId = usuarioId;
        this.incidenciaId = incidenciaId;
        this.codigoIncidencia = codigoIncidencia;
        this.mensaje = mensaje;
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getIncidenciaId() { return incidenciaId; }
    public void setIncidenciaId(String incidenciaId) { this.incidenciaId = incidenciaId; }
    public String getCodigoIncidencia() { return codigoIncidencia; }
    public void setCodigoIncidencia(String codigoIncidencia) { this.codigoIncidencia = codigoIncidencia; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}
