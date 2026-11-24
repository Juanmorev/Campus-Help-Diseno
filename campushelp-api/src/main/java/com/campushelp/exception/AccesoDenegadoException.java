package com.campushelp.exception;

/** El usuario no tiene el rol o el permiso necesario para la operacion (HTTP 403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
