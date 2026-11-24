package com.campushelp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Lugar del campus donde puede ocurrir un problema (aula, bano, laboratorio...).
 * Las coordenadas x, y (0 a 100) ubican el lugar sobre el plano del campus para el mapa.
 */
@Document(collection = "lugares")
public class Lugar {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    private String nombre;
    private String edificio;
    private int piso;
    private double x;
    private double y;
    private boolean activo = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEdificio() { return edificio; }
    public void setEdificio(String edificio) { this.edificio = edificio; }
    public int getPiso() { return piso; }
    public void setPiso(int piso) { this.piso = piso; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
