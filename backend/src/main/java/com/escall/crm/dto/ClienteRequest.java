package com.escall.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de cliente (CRUD - Crear). El documento identifica al cliente y no puede repetirse. */
public record ClienteRequest(
        @Size(max = 10) String tipoDocumento,
        @NotBlank(message = "El documento es obligatorio") @Size(max = 20) String documento,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombres,
        @Email(message = "El correo no es válido") @Size(max = 150) String email,
        @Size(max = 300) String direccion
) {
}
