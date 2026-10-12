package com.escall.crm.modelo;

/**
 * ADMIN: acceso total. SUPERVISOR: dashboard, reportes y administra a los asesores de su equipo.
 * ASESOR: consulta y gestiona las cuentas de su cartera.
 */
public enum Rol {
    ADMIN,
    SUPERVISOR,
    ASESOR
}
