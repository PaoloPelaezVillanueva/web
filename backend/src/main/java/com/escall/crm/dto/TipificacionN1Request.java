package com.escall.crm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipificacionN1Request(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        boolean esContacto,
        @Min(0) int orden,
        boolean activo
) {
}
