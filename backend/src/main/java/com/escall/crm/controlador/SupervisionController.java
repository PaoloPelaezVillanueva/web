package com.escall.crm.controlador;

import com.escall.crm.dto.*;
import com.escall.crm.servicio.DashboardService;
import com.escall.crm.servicio.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dashboard (RF10 - RF12) y Reportes (RF13 - RF15). Solo supervisor y administrador.
 * Filtros por query string, ej: /api/dashboard?periodo=SEMANA&asesorId=4&cartera=Tarjetas
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
public class SupervisionController {

    private final DashboardService dashboard;
    private final ReporteService reportes;

    @GetMapping("/dashboard")
    public DashboardDto dashboard(Filtros filtros) {
        return dashboard.resumen(filtros);
    }

    @GetMapping("/filtros")
    public OpcionesFiltroDto opcionesDeFiltro() {
        return dashboard.opciones();
    }

    @GetMapping("/reportes/gestiones")
    public List<GestionDto> reporteGestiones(Filtros filtros) {
        return reportes.gestiones(filtros);
    }

    @GetMapping("/reportes/pagos")
    public List<PagoDto> reportePagos(Filtros filtros) {
        return reportes.pagos(filtros);
    }

    @GetMapping("/reportes/promesas")
    public List<PromesaDto> reportePromesas(Filtros filtros) {
        return reportes.promesas(filtros);
    }
}
