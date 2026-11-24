package com.campushelp.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de una incidencia (flujo principal de CampusHelp):
 * REPORTADA -> EN_REVISION -> (no valida) CERRADA
 *                          -> (valida)    ASIGNADA -> EN_REPARACION -> SOLUCIONADA
 */
public enum EstadoIncidencia {
    REPORTADA,
    EN_REVISION,
    CERRADA,
    ASIGNADA,
    EN_REPARACION,
    SOLUCIONADA;

    public Set<EstadoIncidencia> siguientesPermitidos() {
        return switch (this) {
            case REPORTADA -> EnumSet.of(EN_REVISION, CERRADA, ASIGNADA);
            case EN_REVISION -> EnumSet.of(CERRADA, ASIGNADA);
            case ASIGNADA -> EnumSet.of(EN_REPARACION, CERRADA);
            case EN_REPARACION -> EnumSet.of(SOLUCIONADA);
            // Si la reparacion no funciono, el administrador puede reabrirla
            case SOLUCIONADA -> EnumSet.of(EN_REPARACION);
            case CERRADA -> EnumSet.noneOf(EstadoIncidencia.class);
        };
    }

    public boolean puedeCambiarA(EstadoIncidencia destino) {
        return siguientesPermitidos().contains(destino);
    }

    /** Incidencia que todavia requiere trabajo (cuenta para el mapa en amarillo o rojo). */
    public boolean esActiva() {
        return this == REPORTADA || this == EN_REVISION || this == ASIGNADA || this == EN_REPARACION;
    }
}
