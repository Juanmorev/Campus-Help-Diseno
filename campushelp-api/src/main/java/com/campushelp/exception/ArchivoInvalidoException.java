package com.campushelp.exception;

/** El archivo subido no es una imagen valida (HTTP 400). */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
