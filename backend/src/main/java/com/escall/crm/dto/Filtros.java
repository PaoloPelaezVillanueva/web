package com.escall.crm.dto;

import java.time.LocalDate;

/**
 * Filtros comunes de Dashboard y Reportes (RF11).
 * periodo: HOY, SEMANA o MES. Si se envían desde/hasta, mandan sobre el periodo.
 */
public record Filtros(String periodo, LocalDate desde, LocalDate hasta, Long asesorId, String cartera,
                      String cosecha) {
}
