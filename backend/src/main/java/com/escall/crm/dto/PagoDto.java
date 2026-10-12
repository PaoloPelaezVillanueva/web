package com.escall.crm.dto;

import com.escall.crm.modelo.Pago;
import com.escall.crm.modelo.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoDto(Long id, Long cuentaId, String numeroOperacion, String cartera, LocalDate fechaPago,
                      BigDecimal monto, String referencia, String clienteDocumento, String clienteNombres,
                      String asesorNombre) {

    public static PagoDto desde(Pago p, Usuario asesor, boolean conCliente) {
        return new PagoDto(p.getId(), p.getCuenta().getId(), p.getCuenta().getNumeroOperacion(),
                p.getCuenta().getCartera(), p.getFechaPago(), p.getMonto(), p.getReferencia(),
                conCliente ? p.getCuenta().getCliente().getDocumento() : null,
                conCliente ? p.getCuenta().getCliente().getNombres() : null,
                asesor != null ? asesor.getNombre() : null);
    }
}
