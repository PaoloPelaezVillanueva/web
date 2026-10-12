package com.escall.crm.dto;

import com.escall.crm.modelo.Gestion;
import com.escall.crm.modelo.Promesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GestionDto(Long id, Long cuentaId, String numeroOperacion, String cartera, LocalDateTime fechaHora,
                         String telefono, String nivel1, String nivel2, String observacion, String asesorNombre,
                         LocalDate fechaCompromiso, BigDecimal montoCompromiso,
                         String clienteDocumento, String clienteNombres) {

    public static GestionDto desde(Gestion g, boolean conCliente) {
        Promesa p = g.getPromesa();
        return new GestionDto(g.getId(), g.getCuenta().getId(), g.getCuenta().getNumeroOperacion(),
                g.getCuenta().getCartera(), g.getFechaHora(), g.getTelefono(),
                g.getTipificacionN1().getNombre(), g.getTipificacionN2().getNombre(), g.getObservacion(),
                g.getAsesor().getNombre(),
                p != null ? p.getFechaCompromiso() : null, p != null ? p.getMonto() : null,
                conCliente ? g.getCuenta().getCliente().getDocumento() : null,
                conCliente ? g.getCuenta().getCliente().getNombres() : null);
    }
}
