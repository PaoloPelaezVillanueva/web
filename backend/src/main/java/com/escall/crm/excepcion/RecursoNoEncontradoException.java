package com.escall.crm.excepcion;

/** Se lanza cuando algo que se busca no existe (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
