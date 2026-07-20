package com.jonalabels.common.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String nombreRecurso, Long id) {
        super(String.format("%s con id %d no encontrado", nombreRecurso, id));
    }
}
