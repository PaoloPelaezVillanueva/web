package com.escall.crm.dto;

import com.escall.crm.modelo.Auditoria;

import java.time.Instant;

public record AuditoriaDto(Long id, String usuario, String accion, String entidad, String entidadId, String detalle,
                           Instant fecha) {

    public static AuditoriaDto desde(Auditoria a) {
        return new AuditoriaDto(a.getId(), a.getUsuario(), a.getAccion(), a.getEntidad(), a.getEntidadId(),
                a.getDetalle(), a.getFecha());
    }
}
