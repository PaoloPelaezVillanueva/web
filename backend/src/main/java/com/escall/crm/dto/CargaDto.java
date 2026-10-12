package com.escall.crm.dto;

import com.escall.crm.modelo.Carga;

import java.time.Instant;
import java.util.List;

/** Resultado de una carga: registros aceptados, rechazados y el motivo de cada rechazo (RF21). */
public record CargaDto(Long id, String tipo, String nombreArchivo, String usuario, int total, int aceptados,
                       int rechazados, List<String> errores, Instant creadoEn) {

    public static CargaDto desde(Carga c) {
        List<String> errores = c.getDetalleErrores() == null || c.getDetalleErrores().isBlank()
                ? List.of() : List.of(c.getDetalleErrores().split("\n"));
        return new CargaDto(c.getId(), c.getTipo().name(), c.getNombreArchivo(), c.getUsuario().getNombre(),
                c.getTotal(), c.getAceptados(), c.getRechazados(), errores, c.getCreadoEn());
    }
}
