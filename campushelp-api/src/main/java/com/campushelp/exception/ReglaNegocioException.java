package com.campushelp.exception;

/** Operacion que viola una regla del flujo de CampusHelp (HTTP 409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
