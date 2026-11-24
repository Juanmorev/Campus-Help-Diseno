package com.campushelp.exception;

/** El recurso solicitado no existe (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super(recurso + " no encontrado: " + id);
    }
}
