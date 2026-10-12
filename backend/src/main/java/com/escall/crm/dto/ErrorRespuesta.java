package com.escall.crm.dto;

import java.time.Instant;
import java.util.Map;

/** Formato único de error: el frontend siempre sabe dónde está el mensaje. */
public record ErrorRespuesta(int estado, String mensaje, Map<String, String> errores, Instant fecha) {

    public static ErrorRespuesta de(int estado, String mensaje) {
        return new ErrorRespuesta(estado, mensaje, Map.of(), Instant.now());
    }
}
