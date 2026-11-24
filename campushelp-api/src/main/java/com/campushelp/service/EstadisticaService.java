package com.campushelp.service;

import com.campushelp.dto.Reportes.CargaResponsable;
import com.campushelp.dto.Reportes.Estadisticas;
import com.campushelp.dto.Reportes.LugarConProblemas;
import com.campushelp.dto.Reportes.LugarMapa;
import com.campushelp.dto.Reportes.Mapa;
import com.campushelp.dto.Reportes.Marcador;
import com.campushelp.model.ColorMapa;
import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Lugar;
import com.campushelp.model.Rol;
import com.campushelp.model.Usuario;
import com.campushelp.repository.IncidenciaRepository;
import com.campushelp.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Estadisticas para el administrador y mapa del campus con el color de cada lugar. */
@Service
public class EstadisticaService {

    private final IncidenciaRepository incidencias;
    private final UsuarioRepository usuarios;
    private final LugarService lugarService;
    private final UsuarioService usuarioService;

    public EstadisticaService(IncidenciaRepository incidencias, UsuarioRepository usuarios, LugarService lugarService,
                              UsuarioService usuarioService) {
        this.incidencias = incidencias;
        this.usuarios = usuarios;
        this.lugarService = lugarService;
        this.usuarioService = usuarioService;
    }

    /** Mapa del campus: cada lugar con su color (rojo urgente, amarillo en proceso, verde solucionado, gris sin problemas). */
    public Mapa mapa() {
        Map<String, List<Incidencia>> porLugar = incidencias.findAll().stream()
                .collect(Collectors.groupingBy(Incidencia::getLugarId));
        List<LugarMapa> puntos = lugarService.listar().stream().map(l -> punto(l, porLugar.getOrDefault(l.getId(), List.of())))
                .toList();
        return new Mapa(puntos.size(), contar(puntos, ColorMapa.ROJO), contar(puntos, ColorMapa.AMARILLO),
                contar(puntos, ColorMapa.VERDE), puntos);
    }

    private static LugarMapa punto(Lugar l, List<Incidencia> lista) {
        List<Incidencia> visibles = lista.stream().filter(i -> i.getEstado() != EstadoIncidencia.CERRADA).toList();
        long urgentes = visibles.stream().filter(i -> i.colorMapa() == ColorMapa.ROJO).count();
        long enProceso = visibles.stream().filter(i -> i.colorMapa() == ColorMapa.AMARILLO).count();
        long solucionadas = visibles.stream().filter(i -> i.colorMapa() == ColorMapa.VERDE).count();
        ColorMapa color = urgentes > 0 ? ColorMapa.ROJO : enProceso > 0 ? ColorMapa.AMARILLO
                : solucionadas > 0 ? ColorMapa.VERDE : ColorMapa.GRIS;
        List<Marcador> marcadores = visibles.stream()
                .sorted(Comparator.comparing(Incidencia::getFechaReporte).reversed())
                .map(i -> new Marcador(i.getId(), i.getCodigo(), i.getTipo(), i.getPrioridad(), i.getEstado(), i.colorMapa()))
                .toList();
        return new LugarMapa(l.getId(), l.getCodigo(), l.getNombre(), l.getEdificio(), l.getPiso(), l.getX(), l.getY(),
                color, urgentes, enProceso, solucionadas, marcadores);
    }

    private static long contar(List<LugarMapa> puntos, ColorMapa c) {
        return puntos.stream().filter(p -> p.color() == c).count();
    }

    /** Estadisticas generales: solo para el administrador. */
    public Estadisticas estadisticas(String adminId) {
        usuarioService.exigirRol(adminId, Rol.ADMINISTRADOR);
        List<Incidencia> todas = incidencias.findAll();
        Map<String, Lugar> lugares = lugarService.listar().stream().collect(Collectors.toMap(Lugar::getId, Function.identity()));

        List<LugarConProblemas> top = todas.stream().collect(Collectors.groupingBy(Incidencia::getLugarId)).entrySet().stream()
                .map(e -> {
                    Lugar l = lugares.get(e.getKey());
                    return new LugarConProblemas(e.getKey(), l == null ? "?" : l.getCodigo(), l == null ? "?" : l.getNombre(),
                            e.getValue().size(), e.getValue().stream().filter(i -> i.getEstado().esActiva()).count());
                })
                .sorted(Comparator.comparingLong(LugarConProblemas::total).reversed()).limit(5).toList();

        Map<String, Double> porTipo = new TreeMap<>();
        todas.stream().collect(Collectors.groupingBy(i -> i.getTipo().name())).forEach((tipo, lista) -> {
            Double p = promedio(lista);
            if (p != null) porTipo.put(tipo, p);
        });

        List<CargaResponsable> carga = usuarios.findByRolAndActivoTrue(Rol.MANTENIMIENTO).stream()
                .map(m -> carga(m, todas)).sorted(Comparator.comparingLong(CargaResponsable::asignadas).reversed()).toList();

        return new Estadisticas(todas.size(),
                todas.stream().filter(i -> i.getEstado().esActiva()).count(),
                todas.stream().filter(i -> i.getEstado() == EstadoIncidencia.SOLUCIONADA).count(),
                todas.stream().filter(i -> i.getEstado() == EstadoIncidencia.CERRADA).count(),
                contarPor(todas, i -> i.getEstado().name()), contarPor(todas, i -> i.getTipo().name()),
                contarPor(todas, i -> i.getPrioridad().name()), promedio(todas), porTipo, top, carga);
    }

    private static CargaResponsable carga(Usuario m, List<Incidencia> todas) {
        List<Incidencia> suyas = todas.stream().filter(i -> m.getId().equals(i.getResponsableId())).toList();
        return new CargaResponsable(m.getId(), m.getNombre(),
                suyas.stream().filter(i -> i.getEstado() == EstadoIncidencia.ASIGNADA).count(),
                suyas.stream().filter(i -> i.getEstado() == EstadoIncidencia.EN_REPARACION).count(),
                suyas.stream().filter(i -> i.getEstado() == EstadoIncidencia.SOLUCIONADA).count(),
                promedio(suyas));
    }

    private static Map<String, Long> contarPor(List<Incidencia> lista, Function<Incidencia, String> clave) {
        return lista.stream().collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    private static Double promedio(List<Incidencia> lista) {
        OptionalDouble p = lista.stream().map(Incidencia::horasHastaSolucion).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        return p.isPresent() ? Math.round(p.getAsDouble() * 10.0) / 10.0 : null;
    }
}
