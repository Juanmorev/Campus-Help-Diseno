package com.campushelp.service;

import com.campushelp.exception.AccesoDenegadoException;
import com.campushelp.exception.RecursoNoEncontradoException;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Notificacion;
import com.campushelp.repository.NotificacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Avisos al estudiante cuando su incidencia cambia ("el estudiante recibe actualizacion"). */
@Service
public class NotificacionService {

    private final NotificacionRepository notificaciones;

    public NotificacionService(NotificacionRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    public void notificarReportante(Incidencia i, String mensaje) {
        notificaciones.save(new Notificacion(i.getReportadaPorId(), i.getId(), i.getCodigo(), i.getCodigo() + ": " + mensaje));
    }

    public List<Notificacion> listar(String usuarioId, boolean soloNoLeidas) {
        return soloNoLeidas ? notificaciones.findByUsuarioIdAndLeidaFalseOrderByFechaDesc(usuarioId)
                : notificaciones.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    public Notificacion marcarLeida(String notificacionId, String usuarioId) {
        Notificacion n = notificaciones.findById(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación", notificacionId));
        if (!n.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Esa notificación es de otro usuario");
        }
        n.marcarLeida();
        return notificaciones.save(n);
    }
}
