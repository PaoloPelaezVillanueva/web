package com.escall.crm.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingresa tu usuario") String usuario,
        @NotBlank(message = "Ingresa tu contraseña") String password
) {
}
