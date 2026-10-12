package com.escall.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edición de datos de contacto (CRUD - Actualizar). El documento no cambia: es la clave con la que llegan las cargas. */
public record ClienteEdicionRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombres,
        @Email(message = "El correo no es válido") @Size(max = 150) String email,
        @Size(max = 300) String direccion
) {
}
