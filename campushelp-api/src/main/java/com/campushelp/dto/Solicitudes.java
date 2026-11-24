package com.campushelp.dto;

import com.campushelp.model.EstadoIncidencia;
import com.campushelp.model.Prioridad;
import com.campushelp.model.Rol;
import com.campushelp.model.TipoProblema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos JSON que recibe la API. No hay inicio de sesion: cada operacion indica en "usuarioId"
 * quien la realiza, y la API comprueba que tenga el rol adecuado.
 */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record UsuarioRequest(
            @NotBlank(message = "es obligatorio") String nombre,
            @NotBlank(message = "es obligatorio") @Email(message = "no es un email válido") String email,
            @NotBlank(message = "es obligatorio") String codigo,
            @NotNull(message = "es obligatorio") Rol rol,
            TipoProblema especialidad) {
    }

    public record LugarRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotBlank(message = "es obligatorio") String codigo,
            @NotBlank(message = "es obligatorio") String nombre,
            @NotBlank(message = "es obligatorio") String edificio,
            int piso,
            @DecimalMin(value = "0", message = "debe estar entre 0 y 100") @DecimalMax(value = "100", message = "debe estar entre 0 y 100") double x,
            @DecimalMin(value = "0", message = "debe estar entre 0 y 100") @DecimalMax(value = "100", message = "debe estar entre 0 y 100") double y) {
    }

    public record IncidenciaRequest(
            @NotBlank(message = "es obligatorio") String estudianteId,
            @NotBlank(message = "es obligatorio") String lugarId,
            @NotNull(message = "es obligatorio") TipoProblema tipo,
            @NotBlank(message = "es obligatoria") @Size(max = 500, message = "máximo 500 caracteres") String descripcion,
            @NotNull(message = "es obligatoria") Prioridad prioridad) {
    }

    public record UsuarioAccion(@NotBlank(message = "es obligatorio") String usuarioId) {
    }

    public record InformacionRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotBlank(message = "es obligatorio") String texto) {
    }

    /** Validacion del administrador: valida=true asigna responsable; valida=false cierra con motivo. */
    public record ValidacionRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotNull(message = "es obligatorio") Boolean valida,
            String responsableId,
            Prioridad prioridad,
            String motivo) {
    }

    public record ResponsableRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotBlank(message = "es obligatorio") String responsableId) {
    }

    public record NotaRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            String nota) {
    }

    public record SolucionRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotBlank(message = "es obligatoria") String descripcion) {
    }

    public record CambioEstadoRequest(
            @NotBlank(message = "es obligatorio") String usuarioId,
            @NotNull(message = "es obligatorio") EstadoIncidencia estado,
            String motivo) {
    }
}
