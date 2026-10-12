package com.escall.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Indicadores operativos del periodo (RF10 - RF12). */
public record DashboardDto(
        LocalDate desde,
        LocalDate hasta,
        long gestiones,
        long contactos,
        double tasaContacto,
        long promesas,
        BigDecimal montoPromesas,
        long pagos,
        BigDecimal montoPagos,
        List<FilaAsesor> porAsesor,
        List<Dia> porDia,
        List<Conteo> porTipificacion
) {

    public record FilaAsesor(Long asesorId, String asesor, long gestiones, long contactos, long promesas,
                             BigDecimal montoPromesas, long pagos, BigDecimal montoPagos) {
    }

    public record Dia(LocalDate fecha, long gestiones, long contactos, long promesas) {
    }

    public record Conteo(String nombre, long cantidad) {
    }
}
