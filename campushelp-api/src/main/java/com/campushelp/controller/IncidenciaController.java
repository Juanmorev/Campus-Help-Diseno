package com.campushelp.controller;

import com.campushelp.dto.FiltroIncidencias;
import com.campushelp.dto.IncidenciaResponse;
import com.campushelp.dto.Solicitudes.CambioEstadoRequest;
import com.campushelp.dto.Solicitudes.IncidenciaRequest;
import com.campushelp.dto.Solicitudes.InformacionRequest;
import com.campushelp.dto.Solicitudes.NotaRequest;
import com.campushelp.dto.Solicitudes.ResponsableRequest;
import com.campushelp.dto.Solicitudes.SolucionRequest;
import com.campushelp.dto.Solicitudes.UsuarioAccion;
import com.campushelp.dto.Solicitudes.ValidacionRequest;
import com.campushelp.model.Actualizacion;
import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Incidencia;
import com.campushelp.model.Lugar;
import com.campushelp.model.Prioridad;
import com.campushelp.model.TipoProblema;
import com.campushelp.service.IncidenciaService;
import com.campushelp.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incidencias")
@Tag(name = "Incidencias", description = "Registro, revisión, asignación, reparación y solución de problemas del campus")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final LugarService lugarService;

    public IncidenciaController(IncidenciaService incidenciaService, LugarService lugarService) {
        this.incidenciaService = incidenciaService;
        this.lugarService = lugarService;
    }

    // ------------------------------------------------------------------ estudiante

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Estudiante: registrar un problema (lugar, tipo, descripción y prioridad)")
    public IncidenciaResponse registrar(@Valid @RequestBody IncidenciaRequest req) {
        return respuesta(incidenciaService.registrar(req));
    }

    @PostMapping(value = "/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adjuntar una fotografía de evidencia (multipart: archivo + usuarioId; máximo 10 MB)")
    public IncidenciaResponse subirEvidencia(@PathVariable String id, @RequestParam String usuarioId,
                                             @RequestPart("archivo") MultipartFile archivo) {
        return respuesta(incidenciaService.agregarEvidencia(id, usuarioId, archivo));
    }

    @GetMapping("/{id}/evidencias/{evidenciaId}")
    @Operation(summary = "Descargar una fotografía de evidencia")
    public ResponseEntity<InputStreamResource> foto(@PathVariable String id, @PathVariable String evidenciaId) throws IOException {
        GridFsResource r = incidenciaService.foto(id, evidenciaId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(r.getContentType()))
                .contentLength(r.contentLength())
                .body(new InputStreamResource(r.getInputStream()));
    }

    @PostMapping("/{id}/informacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar información adicional a una incidencia")
    public IncidenciaResponse informacion(@PathVariable String id, @Valid @RequestBody InformacionRequest req) {
        return respuesta(incidenciaService.agregarInformacion(id, req));
    }

    // ------------------------------------------------------------------ consultas

    @GetMapping
    @Operation(summary = "Listar incidencias: el estudiante ve las suyas, mantenimiento las asignadas y el administrador todas")
    public List<IncidenciaResponse> listar(@RequestParam String usuarioId,
                                           @RequestParam(required = false) EstadoIncidencia estado,
                                           @RequestParam(required = false) TipoProblema tipo,
                                           @RequestParam(required = false) Prioridad prioridad,
                                           @RequestParam(required = false) String lugarId) {
        Map<String, Lugar> lugares = lugarService.listar().stream().collect(Collectors.toMap(Lugar::getId, Function.identity()));
        return incidenciaService.listar(usuarioId, new FiltroIncidencias(estado, tipo, prioridad, lugarId, null, null))
                .stream().map(i -> IncidenciaResponse.de(i, lugares.get(i.getLugarId()))).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una incidencia por id o código (CH-000001), con su estado y seguimiento")
    public IncidenciaResponse obtener(@PathVariable String id, @RequestParam String usuarioId) {
        return respuesta(incidenciaService.obtener(id, usuarioId));
    }

    @GetMapping("/{id}/seguimiento")
    @Operation(summary = "Historial de cambios de la incidencia")
    public List<Actualizacion> seguimiento(@PathVariable String id, @RequestParam String usuarioId) {
        return incidenciaService.obtener(id, usuarioId).getSeguimiento();
    }

    // ------------------------------------------------------------------ administrador

    @PatchMapping("/{id}/revision")
    @Operation(summary = "Administrador: empezar a revisar el reporte (pasa a EN_REVISION)")
    public IncidenciaResponse revisar(@PathVariable String id, @Valid @RequestBody UsuarioAccion req) {
        return respuesta(incidenciaService.iniciarRevision(id, req.usuarioId()));
    }

    @PatchMapping("/{id}/validacion")
    @Operation(summary = "Administrador: ¿es válido? Sí: asigna responsable (ASIGNADA). No: cierra con motivo (CERRADA)")
    public IncidenciaResponse validar(@PathVariable String id, @Valid @RequestBody ValidacionRequest req) {
        return respuesta(incidenciaService.validar(id, req));
    }

    @PatchMapping("/{id}/responsable")
    @Operation(summary = "Administrador: reasignar el responsable de mantenimiento")
    public IncidenciaResponse reasignar(@PathVariable String id, @Valid @RequestBody ResponsableRequest req) {
        return respuesta(incidenciaService.reasignar(id, req));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Administrador: cambiar el estado manualmente (por ejemplo reabrir o cerrar)")
    public IncidenciaResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambioEstadoRequest req) {
        return respuesta(incidenciaService.cambiarEstado(id, req));
    }

    // ------------------------------------------------------------------ mantenimiento

    @PatchMapping("/{id}/reparacion")
    @Operation(summary = "Mantenimiento: iniciar la reparación (pasa a EN_REPARACION)")
    public IncidenciaResponse iniciarReparacion(@PathVariable String id, @Valid @RequestBody NotaRequest req) {
        return respuesta(incidenciaService.iniciarReparacion(id, req));
    }

    @PostMapping("/{id}/progreso")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Mantenimiento: registrar un avance de la reparación")
    public IncidenciaResponse progreso(@PathVariable String id, @Valid @RequestBody NotaRequest req) {
        return respuesta(incidenciaService.registrarProgreso(id, req));
    }

    @PatchMapping("/{id}/solucion")
    @Operation(summary = "Mantenimiento: marcar como solucionado (pasa a SOLUCIONADA)")
    public IncidenciaResponse solucionar(@PathVariable String id, @Valid @RequestBody SolucionRequest req) {
        return respuesta(incidenciaService.solucionar(id, req));
    }

    private IncidenciaResponse respuesta(Incidencia i) {
        return IncidenciaResponse.de(i, lugarService.obtener(i.getLugarId()));
    }
}
