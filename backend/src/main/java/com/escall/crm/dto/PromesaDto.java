package com.escall.crm.dto;

import com.escall.crm.modelo.EstadoPromesa;
import com.escall.crm.modelo.Promesa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** estado: PENDIENTE, CUMPLIDA o VENCIDA (pendiente cuya fecha ya pasó). */
public record PromesaDto(Long id, Long cuentaId, String numeroOperacion, String cartera, LocalDate fechaCompromiso,
                         BigDecimal monto, String estado, String asesorNombre, LocalDateTime fechaGestion,
                         Long clienteId, String clienteDocumento, String clienteNombres) {

    public static PromesaDto desde(Promesa p, LocalDate hoy, boolean conCliente) {
        String estado = p.getEstado() == EstadoPromesa.PENDIENTE && p.getFechaCompromiso().isBefore(hoy)
                ? "VENCIDA" : p.getEstado().name();
        return new PromesaDto(p.getId(), p.getCuenta().getId(), p.getCuenta().getNumeroOperacion(),
                p.getCuenta().getCartera(), p.getFechaCompromiso(), p.getMonto(), estado,
                p.getAsesor().getNombre(), p.getGestion().getFechaHora(),
                conCliente ? p.getCuenta().getCliente().getId() : null,
                conCliente ? p.getCuenta().getCliente().getDocumento() : null,
                conCliente ? p.getCuenta().getCliente().getNombres() : null);
    }
}
