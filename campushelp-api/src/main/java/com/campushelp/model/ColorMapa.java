package com.campushelp.model;

/** Color con el que se marca un problema o un lugar en el mapa del campus. */
public enum ColorMapa {
    /** Problema urgente sin solucionar. */
    ROJO,
    /** Problema en revision o en proceso. */
    AMARILLO,
    /** Problema solucionado. */
    VERDE,
    /** Lugar sin problemas reportados (o solo con reportes cerrados por no ser validos). */
    GRIS
}
