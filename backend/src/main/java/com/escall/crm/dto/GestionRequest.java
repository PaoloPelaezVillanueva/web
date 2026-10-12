package com.escall.crm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos de una gestión (RF07). Fecha y monto de compromiso solo si la tipificación lo exige (RF08). */
public record GestionRequest(
        @NotNull(message = "Elige la cuenta gestionada") Long cuentaId,
        LocalDateTime fechaHora,
        @NotBlank(message = "Indica el teléfono utilizado")
        @Pattern(regexp = "^[0-9+ ]{6,20}$", message = "Teléfono inválido") String telefono,
        @NotNull(message = "Elige el resultado (nivel 2)") Long tipificacionN2Id,
        @NotBlank(message = "Escribe una observación") @Size(max = 500) String observacion,
        LocalDate fechaCompromiso,
        @DecimalMin(value = "0.01", message = "El monto comprometido debe ser mayor a 0") BigDecimal montoCompromiso
) {
}
