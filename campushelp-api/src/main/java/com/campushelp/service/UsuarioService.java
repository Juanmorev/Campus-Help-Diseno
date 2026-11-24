package com.campushelp.service;

import com.campushelp.dto.Solicitudes.UsuarioRequest;
import com.campushelp.exception.AccesoDenegadoException;
import com.campushelp.exception.RecursoNoEncontradoException;
import com.campushelp.exception.ReglaNegocioException;
import com.campushelp.model.Rol;
import com.campushelp.model.Usuario;
import com.campushelp.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/** Registro de usuarios y control de roles. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario crear(UsuarioRequest req) {
        usuarios.findByEmail(req.email()).ifPresent(u -> {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + req.email());
        });
        usuarios.findByCodigo(req.codigo()).ifPresent(u -> {
            throw new ReglaNegocioException("Ya existe un usuario con el código " + req.codigo());
        });
        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setEmail(req.email());
        u.setCodigo(req.codigo());
        u.setRol(req.rol());
        u.setEspecialidad(req.rol() == Rol.MANTENIMIENTO ? req.especialidad() : null);
        return usuarios.save(u);
    }

    public List<Usuario> listar(Rol rol) {
        return rol == null ? usuarios.findAll() : usuarios.findByRol(rol);
    }

    public Usuario obtener(String id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    /** Baja logica: el usuario se conserva para no perder el seguimiento de sus incidencias. */
    public void desactivar(String id, String adminId) {
        exigirRol(adminId, Rol.ADMINISTRADOR);
        Usuario u = obtener(id);
        u.setActivo(false);
        usuarios.save(u);
    }

    /** Devuelve el usuario si esta activo y tiene alguno de los roles indicados; si no, 403. */
    public Usuario exigirRol(String usuarioId, Rol... roles) {
        Usuario u = obtener(usuarioId);
        if (!u.isActivo()) {
            throw new AccesoDenegadoException("El usuario " + u.getNombre() + " está inactivo");
        }
        if (roles.length > 0 && Arrays.stream(roles).noneMatch(u::tieneRol)) {
            throw new AccesoDenegadoException("Esta operación es para " + nombres(roles) + " y " + u.getNombre()
                    + " es " + u.getRol().name().toLowerCase());
        }
        return u;
    }

    private static String nombres(Rol... roles) {
        return String.join(" o ", Arrays.stream(roles).map(r -> switch (r) {
            case ESTUDIANTE -> "estudiantes";
            case ADMINISTRADOR -> "el administrador";
            case MANTENIMIENTO -> "el personal de mantenimiento";
        }).toList());
    }
}
