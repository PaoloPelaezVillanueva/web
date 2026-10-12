package com.escall.crm.excepcion;

/** Se lanza cuando se viola una regla del negocio, ej: "no hay stock suficiente" (responde 409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
