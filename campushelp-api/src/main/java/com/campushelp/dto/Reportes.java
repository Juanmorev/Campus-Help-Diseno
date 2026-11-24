package com.campushelp.dto;

import com.campushelp.model.ColorMapa;
import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Prioridad;
import com.campushelp.model.TipoProblema;

import java.util.List;
import java.util.Map;

/** Respuestas del mapa del campus y de las estadisticas. */
public final class Reportes {

    private Reportes() {
    }

    /** Un problema dibujado como marcador en el mapa. */
    public record Marcador(String incidenciaId, String codigo, TipoProblema tipo, Prioridad prioridad,
                           EstadoIncidencia estado, ColorMapa color) {
    }

    /**
     * Un lugar del mapa con su color resumen: rojo si tiene algun problema urgente sin solucionar,
     * amarillo si tiene problemas en revision o proceso, verde si todos estan solucionados, gris si no tiene.
     */
    public record LugarMapa(String lugarId, String codigo, String nombre, String edificio, int piso, double x, double y,
                            ColorMapa color, long urgentes, long enProceso, long solucionadas, List<Marcador> problemas) {
    }

    public record Mapa(long lugares, long lugaresEnRojo, long lugaresEnAmarillo, long lugaresEnVerde,
                       List<LugarMapa> puntos) {
    }

    public record LugarConProblemas(String lugarId, String codigo, String nombre, long total, long activas) {
    }

    public record CargaResponsable(String responsableId, String nombre, long asignadas, long enReparacion,
                                   long solucionadas, Double promedioHorasSolucion) {
    }

    public record Estadisticas(long total, long activas, long solucionadas, long cerradas,
                               Map<String, Long> porEstado, Map<String, Long> porTipo, Map<String, Long> porPrioridad,
                               Double promedioHorasSolucion, Map<String, Double> promedioHorasSolucionPorTipo,
                               List<LugarConProblemas> lugaresConMasProblemas, List<CargaResponsable> cargaMantenimiento) {
    }
}
