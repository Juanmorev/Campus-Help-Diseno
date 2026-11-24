package com.campushelp.model;

import java.time.Instant;

/**
 * Fotografia de evidencia de una incidencia. El archivo se guarda en MongoDB GridFS;
 * aqui solo quedan sus datos (embebidos en la incidencia).
 */
public class Evidencia {

    /** Id del archivo en GridFS. */
    private String id;
    private String nombreArchivo;
    private String tipoContenido;
    private long tamanoBytes;
    private String subidaPorId;
    private Instant fecha;

    public Evidencia() {
    }

    public Evidencia(String id, String nombreArchivo, String tipoContenido, long tamanoBytes, String subidaPorId) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.tipoContenido = tipoContenido;
        this.tamanoBytes = tamanoBytes;
        this.subidaPorId = subidaPorId;
        this.fecha = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getTipoContenido() { return tipoContenido; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }
    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }
    public String getSubidaPorId() { return subidaPorId; }
    public void setSubidaPorId(String subidaPorId) { this.subidaPorId = subidaPorId; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
