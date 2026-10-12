package com.escall.crm.dto;

import java.math.BigDecimal;

/** Fila del resultado de búsqueda en el módulo Resumen. */
public record ClienteResumenDto(Long id, String tipoDocumento, String documento, String nombres,
                                int cuentas, BigDecimal deudaTotal) {
}
