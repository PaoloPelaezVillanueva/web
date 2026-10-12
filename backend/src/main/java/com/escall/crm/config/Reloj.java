package com.escall.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Fecha y hora "de la operación" (Lima por defecto), sin depender de la zona del servidor. */
@Component
public class Reloj {

    private final ZoneId zona;

    public Reloj(@Value("${app.zona-horaria}") String zonaHoraria) {
        this.zona = ZoneId.of(zonaHoraria);
    }

    public LocalDate hoy() {
        return LocalDate.now(zona);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(zona);
    }
}
