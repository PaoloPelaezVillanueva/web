package com.escall.crm.dto;

import com.escall.crm.modelo.Telefono;

public record TelefonoDto(Long id, String numero, String tipo) {

    public static TelefonoDto desde(Telefono t) {
        return new TelefonoDto(t.getId(), t.getNumero(), t.getTipo());
    }
}
