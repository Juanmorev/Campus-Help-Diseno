package com.campushelp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Estudiante, administrador o persona de mantenimiento. */
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String email;
    /** Codigo estudiantil o de empleado. */
    private String codigo;
    private Rol rol;
    /** Solo para mantenimiento: tipo de problema en el que se especializa. */
    private TipoProblema especialidad;
    private boolean activo = true;
    private Instant fechaRegistro = Instant.now();

    public boolean tieneRol(Rol r) {
        return rol == r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public TipoProblema getEspecialidad() { return especialidad; }
    public void setEspecialidad(TipoProblema especialidad) { this.especialidad = especialidad; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
