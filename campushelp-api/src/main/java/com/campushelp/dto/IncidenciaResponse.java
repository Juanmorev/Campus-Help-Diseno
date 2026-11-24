package com.campushelp.dto;

import com.campushelp.model.Actualizacion;
import com.campushelp.model.ColorMapa;
import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Lugar;
import com.campushelp.model.Prioridad;
import com.campushelp.model.TipoProblema;

import java.time.Instant;
import java.util.List;

/** Incidencia tal como la devuelve la API, con el lugar, el color del mapa y la URL de cada foto. */
public record IncidenciaResponse(
        String id,
        String codigo,
        LugarResumen lugar,
        TipoProblema tipo,
        String descripcion,
        Prioridad prioridad,
        EstadoIncidencia estado,
        ColorMapa colorMapa,
        String reportadaPorId,
        String responsableId,
        Instant fechaReporte,
        Instant fechaRevision,
        Instant fechaAsignacion,
        Instant fechaInicioReparacion,
        Instant fechaSolucion,
        Instant fechaCierre,
        String motivoCierre,
        String solucion,
        Double horasHastaSolucion,
        List<EstadoIncidencia> siguientesEstados,
        List<EvidenciaResponse> evidencias,
        List<Actualizacion> seguimiento) {

    public record LugarResumen(String id, String codigo, String nombre, String edificio, int piso) {
    }

    public record EvidenciaResponse(String id, String nombreArchivo, String tipoContenido, long tamanoBytes,
                                    Instant fecha, String url) {
    }

    public static IncidenciaResponse de(Incidencia i, Lugar lugar) {
        LugarResumen l = lugar == null ? null
                : new LugarResumen(lugar.getId(), lugar.getCodigo(), lugar.getNombre(), lugar.getEdificio(), lugar.getPiso());
        List<EvidenciaResponse> fotos = i.getEvidencias().stream()
                .map(e -> new EvidenciaResponse(e.getId(), e.getNombreArchivo(), e.getTipoContenido(), e.getTamanoBytes(),
                        e.getFecha(), "/api/incidencias/" + i.getId() + "/evidencias/" + e.getId()))
                .toList();
        return new IncidenciaResponse(i.getId(), i.getCodigo(), l, i.getTipo(), i.getDescripcion(), i.getPrioridad(),
                i.getEstado(), i.colorMapa(), i.getReportadaPorId(), i.getResponsableId(), i.getFechaReporte(),
                i.getFechaRevision(), i.getFechaAsignacion(), i.getFechaInicioReparacion(), i.getFechaSolucion(),
                i.getFechaCierre(), i.getMotivoCierre(), i.getSolucion(), i.horasHastaSolucion(),
                i.getEstado().siguientesPermitidos().stream().toList(), fotos, i.getSeguimiento());
    }
}
