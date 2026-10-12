package com.escall.crm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TipificacionN2Request(
        @NotNull(message = "Elige el nivel 1") Long n1Id,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        boolean requiereCompromiso,
        @Min(0) int orden,
        boolean activo
) {
}
