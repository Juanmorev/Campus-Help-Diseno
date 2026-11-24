package com.campushelp.controller;

import com.campushelp.dto.Reportes.Mapa;
import com.campushelp.dto.Solicitudes.LugarRequest;
import com.campushelp.model.Lugar;
import com.campushelp.service.EstadisticaService;
import com.campushelp.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Lugares y mapa", description = "Lugares del campus y mapa de problemas por ubicación")
public class LugarController {

    private final LugarService lugarService;
    private final EstadisticaService estadisticaService;

    public LugarController(LugarService lugarService, EstadisticaService estadisticaService) {
        this.lugarService = lugarService;
        this.estadisticaService = estadisticaService;
    }

    @PostMapping("/lugares")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un lugar del campus con su posición en el mapa (solo administrador)")
    public Lugar crear(@Valid @RequestBody LugarRequest req) {
        return lugarService.crear(req);
    }

    @GetMapping("/lugares")
    @Operation(summary = "Listar los lugares del campus")
    public List<Lugar> listar() {
        return lugarService.listar();
    }

    @GetMapping("/lugares/{id}")
    @Operation(summary = "Consultar un lugar")
    public Lugar obtener(@PathVariable String id) {
        return lugarService.obtener(id);
    }

    @GetMapping("/mapa")
    @Operation(summary = "Mapa del campus: cada lugar con su color (ROJO urgente, AMARILLO en revisión/proceso, VERDE solucionado)")
    public Mapa mapa() {
        return estadisticaService.mapa();
    }
}
