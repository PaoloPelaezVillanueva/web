package com.escall.crm.dto;

import java.util.List;

/** Todo lo que el asesor necesita ver de un cliente en una sola pantalla (RF04 - RF06). */
public record ClienteDetalleDto(Long id, String tipoDocumento, String documento, String nombres, String email,
                                String direccion, List<TelefonoDto> telefonos, List<CuentaDto> cuentas,
                                List<GestionDto> gestiones, List<PagoDto> pagos, List<PromesaDto> promesas) {
}
