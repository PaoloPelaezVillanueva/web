package com.escall.crm.modelo;

/**
 * Estado guardado de la promesa. "VENCIDA" no se guarda: se calcula cuando
 * una promesa PENDIENTE ya pasó su fecha de compromiso (ver PromesaDto).
 */
public enum EstadoPromesa {
    PENDIENTE,
    CUMPLIDA
}
