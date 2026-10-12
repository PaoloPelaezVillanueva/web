package com.escall.crm.util;

public final class Textos {

    private Textos() {
    }

    /** Quita espacios sobrantes y convierte "" en null para no guardar textos vacíos. */
    public static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
