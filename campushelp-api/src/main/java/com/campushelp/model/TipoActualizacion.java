package com.campushelp.model;

/** Tipo de evento que queda en el seguimiento de una incidencia. */
public enum TipoActualizacion {
    REGISTRO,
    REVISION,
    VALIDACION,
    RECHAZO,
    ASIGNACION,
    INICIO_REPARACION,
    PROGRESO,
    SOLUCION,
    INFORMACION_ADICIONAL,
    EVIDENCIA,
    CAMBIO_ESTADO
}
