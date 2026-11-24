package com.campushelp.model;

import java.time.Instant;

/** Evento del seguimiento de una incidencia (quien hizo que y cuando). Se guarda embebido. */
public class Actualizacion {

    private Instant fecha;
    private TipoActualizacion tipo;
    private String autorId;
    private String mensaje;
    private EstadoIncidencia estadoAnterior;
    private EstadoIncidencia estadoNuevo;

    public Actualizacion() {
    }

    public Actualizacion(TipoActualizacion tipo, String autorId, String mensaje,
                         EstadoIncidencia estadoAnterior, EstadoIncidencia estadoNuevo) {
        this.fecha = Instant.now();
        this.tipo = tipo;
        this.autorId = autorId;
        this.mensaje = mensaje;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
    }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
    public TipoActualizacion getTipo() { return tipo; }
    public void setTipo(TipoActualizacion tipo) { this.tipo = tipo; }
    public String getAutorId() { return autorId; }
    public void setAutorId(String autorId) { this.autorId = autorId; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public EstadoIncidencia getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoIncidencia estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoIncidencia getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoIncidencia estadoNuevo) { this.estadoNuevo = estadoNuevo; }
}
