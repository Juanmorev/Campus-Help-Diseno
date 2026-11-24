package com.campushelp.service;

import com.campushelp.dto.Solicitudes.LugarRequest;
import com.campushelp.exception.RecursoNoEncontradoException;
import com.campushelp.exception.ReglaNegocioException;
import com.campushelp.model.Lugar;
import com.campushelp.model.Rol;
import com.campushelp.repository.LugarRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Lugares del campus (aulas, banos, laboratorios...) con su posicion en el mapa. */
@Service
public class LugarService {

    private final LugarRepository lugares;
    private final UsuarioService usuarioService;

    public LugarService(LugarRepository lugares, UsuarioService usuarioService) {
        this.lugares = lugares;
        this.usuarioService = usuarioService;
    }

    public Lugar crear(LugarRequest req) {
        usuarioService.exigirRol(req.usuarioId(), Rol.ADMINISTRADOR);
        String codigo = req.codigo().trim().toUpperCase();
        lugares.findByCodigo(codigo).ifPresent(l -> {
            throw new ReglaNegocioException("Ya existe un lugar con el código " + codigo);
        });
        Lugar l = new Lugar();
        l.setCodigo(codigo);
        l.setNombre(req.nombre());
        l.setEdificio(req.edificio());
        l.setPiso(req.piso());
        l.setX(req.x());
        l.setY(req.y());
        return lugares.save(l);
    }

    public List<Lugar> listar() {
        return lugares.findByActivoTrueOrderByEdificioAscPisoAscCodigoAsc();
    }

    public Lugar obtener(String id) {
        return lugares.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Lugar", id));
    }
}
