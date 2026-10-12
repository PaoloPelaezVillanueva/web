package com.escall.crm.dto;

import java.math.BigDecimal;

/** Cuenta con su saldo calculado y el asesor que la tiene asignada. */
public record CuentaDto(Long id, String numeroOperacion, String entidad, String producto, String cartera,
                        String cosecha, BigDecimal deudaTotal, int diasMora, BigDecimal totalPagado,
                        BigDecimal saldo, Long asesorId, String asesorNombre,
                        Long clienteId, String clienteDocumento, String clienteNombres) {
}
