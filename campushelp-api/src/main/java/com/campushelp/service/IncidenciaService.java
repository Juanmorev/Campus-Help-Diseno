package com.campushelp.service;

import com.campushelp.dto.FiltroIncidencias;
import com.campushelp.dto.Solicitudes.CambioEstadoRequest;
import com.campushelp.dto.Solicitudes.IncidenciaRequest;
import com.campushelp.dto.Solicitudes.InformacionRequest;
import com.campushelp.dto.Solicitudes.NotaRequest;
import com.campushelp.dto.Solicitudes.ResponsableRequest;
import com.campushelp.dto.Solicitudes.SolucionRequest;
import com.campushelp.dto.Solicitudes.ValidacionRequest;
import com.campushelp.exception.AccesoDenegadoException;
import com.campushelp.exception.RecursoNoEncontradoException;
import com.campushelp.exception.ReglaNegocioException;
import com.campushelp.model.Evidencia;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Lugar;
import com.campushelp.model.Rol;
import com.campushelp.model.Usuario;
import com.campushelp.repository.IncidenciaRepository;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Casos de uso de CampusHelp por rol:
 * - Estudiante: registrar problema, consultar sus reportes, ver estado, agregar informacion y fotos.
 * - Administrador: ver todos, revisar, validar o cerrar, asignar responsable y cambiar estados.
 * - Mantenimiento: ver sus asignaciones, iniciar la reparacion, registrar avances y marcar solucionado.
 * Cada cambio genera una notificacion para el estudiante que reporto.
 */
@Service
public class IncidenciaService {

    private final IncidenciaRepository incidencias;
    private final UsuarioService usuarioService;
    private final LugarService lugarService;
    private final NotificacionService notificaciones;
    private final EvidenciaService evidencias;
    private final GeneradorCodigoService codigos;

    public IncidenciaService(IncidenciaRepository incidencias, UsuarioService usuarioService, LugarService lugarService,
                             NotificacionService notificaciones, EvidenciaService evidencias, GeneradorCodigoService codigos) {
        this.incidencias = incidencias;
        this.usuarioService = usuarioService;
        this.lugarService = lugarService;
        this.notificaciones = notificaciones;
        this.evidencias = evidencias;
        this.codigos = codigos;
    }

    // ------------------------------------------------------------------ estudiante

    public Incidencia registrar(IncidenciaRequest req) {
        Usuario estudiante = usuarioService.exigirRol(req.estudianteId(), Rol.ESTUDIANTE);
        Lugar lugar = lugarService.obtener(req.lugarId());
        Incidencia i = new Incidencia();
        i.setLugarId(lugar.getId());
        i.setTipo(req.tipo());
        i.setDescripcion(req.descripcion().trim());
        i.setPrioridad(req.prioridad());
        i.registrar(codigos.siguienteCodigo(), estudiante);
        Incidencia guardada = incidencias.save(i);
        notificaciones.notificarReportante(guardada, "recibimos tu reporte en " + lugar.getNombre() + ". Está pendiente de revisión.");
        return guardada;
    }

    public Incidencia agregarInformacion(String id, InformacionRequest req) {
        Incidencia i = obtenerCualquiera(id);
        Usuario autor = usuarioService.exigirRol(req.usuarioId());
        exigirVisible(i, autor);
        i.agregarInformacion(autor, req.texto());
        return incidencias.save(i);
    }

    public Incidencia agregarEvidencia(String id, String usuarioId, MultipartFile archivo) {
        Incidencia i = obtenerCualquiera(id);
        Usuario autor = usuarioService.exigirRol(usuarioId);
        exigirVisible(i, autor);
        if (i.getEstado() == com.campushelp.model.EstadoIncidencia.CERRADA) {
            throw new ReglaNegocioException("La incidencia " + i.getCodigo() + " está cerrada y no admite evidencias");
        }
        Evidencia e = evidencias.guardar(archivo, autor.getId());
        i.agregarEvidencia(e, autor);
        return incidencias.save(i);
    }

    public GridFsResource foto(String id, String evidenciaId) {
        Incidencia i = obtenerCualquiera(id);
        boolean pertenece = i.getEvidencias().stream().anyMatch(e -> e.getId().equals(evidenciaId));
        if (!pertenece) throw new RecursoNoEncontradoException("Fotografía", evidenciaId);
        return evidencias.cargar(evidenciaId);
    }

    // ------------------------------------------------------------------ consultas segun rol

    /** Estudiante: solo sus reportes. Mantenimiento: solo lo asignado. Administrador: todas, con filtros. */
    public List<Incidencia> listar(String usuarioId, FiltroIncidencias filtro) {
        Usuario u = usuarioService.exigirRol(usuarioId);
        FiltroIncidencias f = switch (u.getRol()) {
            case ESTUDIANTE -> filtro.conReportante(u.getId());
            case MANTENIMIENTO -> filtro.conResponsable(u.getId());
            case ADMINISTRADOR -> filtro;
        };
        return incidencias.buscar(f);
    }

    public Incidencia obtener(String id, String usuarioId) {
        Incidencia i = obtenerCualquiera(id);
        exigirVisible(i, usuarioService.exigirRol(usuarioId));
        return i;
    }

    public Incidencia obtenerCualquiera(String idOCodigo) {
        return incidencias.findById(idOCodigo)
                .or(() -> incidencias.findByCodigo(idOCodigo.toUpperCase()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Incidencia", idOCodigo));
    }

    // ------------------------------------------------------------------ administrador

    public Incidencia iniciarRevision(String id, String adminId) {
        Usuario admin = usuarioService.exigirRol(adminId, Rol.ADMINISTRADOR);
        Incidencia i = obtenerCualquiera(id);
        i.iniciarRevision(admin);
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "el administrador está revisando tu reporte.");
        return g;
    }

    public Incidencia validar(String id, ValidacionRequest req) {
        Usuario admin = usuarioService.exigirRol(req.usuarioId(), Rol.ADMINISTRADOR);
        Incidencia i = obtenerCualquiera(id);
        if (Boolean.TRUE.equals(req.valida())) {
            if (req.responsableId() == null || req.responsableId().isBlank()) {
                throw new ReglaNegocioException("Para validar el reporte indica el responsable de mantenimiento (responsableId)");
            }
            Usuario responsable = usuarioService.obtener(req.responsableId());
            i.validarYAsignar(admin, responsable, req.prioridad());
            Incidencia g = incidencias.save(i);
            notificaciones.notificarReportante(g, "tu reporte fue validado y asignado a " + responsable.getNombre() + ".");
            return g;
        }
        i.rechazar(admin, req.motivo());
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "tu reporte se cerró. Motivo: " + req.motivo());
        return g;
    }

    public Incidencia reasignar(String id, ResponsableRequest req) {
        Usuario admin = usuarioService.exigirRol(req.usuarioId(), Rol.ADMINISTRADOR);
        Usuario responsable = usuarioService.obtener(req.responsableId());
        Incidencia i = obtenerCualquiera(id);
        i.reasignar(admin, responsable);
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "ahora lo atiende " + responsable.getNombre() + ".");
        return g;
    }

    public Incidencia cambiarEstado(String id, CambioEstadoRequest req) {
        Usuario admin = usuarioService.exigirRol(req.usuarioId(), Rol.ADMINISTRADOR);
        Incidencia i = obtenerCualquiera(id);
        i.cambiarEstadoManual(admin, req.estado(), req.motivo());
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "cambió de estado a " + g.getEstado() + ".");
        return g;
    }

    // ------------------------------------------------------------------ mantenimiento

    public Incidencia iniciarReparacion(String id, NotaRequest req) {
        Usuario m = usuarioService.exigirRol(req.usuarioId(), Rol.MANTENIMIENTO);
        Incidencia i = obtenerCualquiera(id);
        i.iniciarReparacion(m, req.nota());
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "está en reparación.");
        return g;
    }

    public Incidencia registrarProgreso(String id, NotaRequest req) {
        Usuario m = usuarioService.exigirRol(req.usuarioId(), Rol.MANTENIMIENTO);
        Incidencia i = obtenerCualquiera(id);
        i.registrarProgreso(m, req.nota());
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "avance de la reparación: " + req.nota());
        return g;
    }

    public Incidencia solucionar(String id, SolucionRequest req) {
        Usuario m = usuarioService.exigirRol(req.usuarioId(), Rol.MANTENIMIENTO);
        Incidencia i = obtenerCualquiera(id);
        i.solucionar(m, req.descripcion());
        Incidencia g = incidencias.save(i);
        notificaciones.notificarReportante(g, "¡solucionado! " + req.descripcion());
        return g;
    }

    // ------------------------------------------------------------------ permisos de lectura

    private void exigirVisible(Incidencia i, Usuario u) {
        boolean puede = switch (u.getRol()) {
            case ADMINISTRADOR -> true;
            case ESTUDIANTE -> i.esDe(u);
            case MANTENIMIENTO -> i.estaAsignadaA(u);
        };
        if (!puede) {
            throw new AccesoDenegadoException(u.getNombre() + " no puede ver la incidencia " + i.getCodigo());
        }
    }
}
