package com.campushelp.controller;

import com.campushelp.dto.Solicitudes.UsuarioRequest;
import com.campushelp.model.Notificacion;
import com.campushelp.model.Rol;
import com.campushelp.model.Usuario;
import com.campushelp.service.NotificacionService;
import com.campushelp.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios y notificaciones", description = "Estudiantes, administradores, mantenimiento y sus avisos")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public UsuarioController(UsuarioService usuarioService, NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un usuario (estudiante, administrador o mantenimiento)")
    public Usuario crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Listar usuarios, opcionalmente por rol")
    public List<Usuario> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Consultar un usuario")
    public Usuario obtener(@PathVariable String id) {
        return usuarioService.obtener(id);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un usuario (solo administrador; baja lógica)")
    public void desactivar(@PathVariable String id, @RequestParam String adminId) {
        usuarioService.desactivar(id, adminId);
    }

    @GetMapping("/usuarios/{id}/notificaciones")
    @Operation(summary = "Actualizaciones que ha recibido el usuario sobre sus incidencias")
    public List<Notificacion> notificaciones(@PathVariable String id,
                                             @RequestParam(defaultValue = "false") boolean soloNoLeidas) {
        usuarioService.obtener(id);
        return notificacionService.listar(id, soloNoLeidas);
    }

    @PatchMapping("/notificaciones/{id}/leida")
    @Operation(summary = "Marcar una notificación como leída")
    public Notificacion marcarLeida(@PathVariable String id, @RequestParam String usuarioId) {
        return notificacionService.marcarLeida(id, usuarioId);
    }
}
