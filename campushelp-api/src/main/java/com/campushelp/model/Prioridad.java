package com.campushelp.model;

/** Nivel de prioridad que indica el estudiante (el administrador puede ajustarlo al validar). */
public enum Prioridad {
    BAJA,
    MEDIA,
    ALTA,
    /** Riesgo para las personas o impide usar un espacio: se pinta en rojo en el mapa. */
    URGENTE
}
