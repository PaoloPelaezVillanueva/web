package com.escall.crm.dto;

import com.escall.crm.modelo.Rol;
import jakarta.validation.constraints.*;

/** Crear o editar usuarios. Al editar, la contraseña se cambia aparte (CambioPasswordRequest). */
public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,40}$", message = "Usuario: de 3 a 40 letras, números, punto o guion")
        String usuario,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") @Size(max = 150) String email,
        @Size(max = 72) String password,
        @NotNull(message = "Elige un rol") Rol rol,
        Long supervisorId
) {
}
