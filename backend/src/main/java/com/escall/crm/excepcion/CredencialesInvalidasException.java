package com.escall.crm.excepcion;

/** Usuario o contraseña incorrectos, o sesión inválida (responde 401). */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos");
    }
}
