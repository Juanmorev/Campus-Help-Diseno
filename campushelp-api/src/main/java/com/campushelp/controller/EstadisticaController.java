package com.campushelp.controller;

import com.campushelp.dto.Reportes.Estadisticas;
import com.campushelp.service.EstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = "Estadísticas", description = "Indicadores para el administrador")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping
    @Operation(summary = "Administrador: totales por estado, tipo y prioridad, tiempos de solución, lugares con más problemas y carga de mantenimiento")
    public Estadisticas estadisticas(@RequestParam String usuarioId) {
        return estadisticaService.estadisticas(usuarioId);
    }
}
