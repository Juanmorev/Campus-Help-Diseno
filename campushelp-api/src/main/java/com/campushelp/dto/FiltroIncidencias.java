package com.campushelp.dto;

import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Prioridad;
import com.campushelp.model.TipoProblema;

/** Filtros opcionales para listar incidencias (todos pueden ser null). */
public record FiltroIncidencias(EstadoIncidencia estado, TipoProblema tipo, Prioridad prioridad, String lugarId,
                                String reportadaPorId, String responsableId) {

    public FiltroIncidencias conReportante(String id) {
        return new FiltroIncidencias(estado, tipo, prioridad, lugarId, id, responsableId);
    }

    public FiltroIncidencias conResponsable(String id) {
        return new FiltroIncidencias(estado, tipo, prioridad, lugarId, reportadaPorId, id);
    }
}
