package com.escall.crm.servicio;

import com.escall.crm.config.Reloj;
import com.escall.crm.dto.*;
import com.escall.crm.modelo.Pago;
import com.escall.crm.modelo.Usuario;
import com.escall.crm.repositorio.GestionRepository;
import com.escall.crm.repositorio.PagoRepository;
import com.escall.crm.repositorio.PromesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reportes de gestiones, pagos y promesas (RF13 - RF15), limitados según el rol. */
@Service
@RequiredArgsConstructor
public class ReporteService {

    /** Límite de filas por reporte para no saturar el navegador. */
    private static final PageRequest LIMITE = PageRequest.of(0, 5_000);

    private final GestionRepository gestiones;
    private final PagoRepository pagos;
    private final PromesaRepository promesas;
    private final AlcanceConsulta alcanceConsulta;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public List<GestionDto> gestiones(Filtros f) {
        AlcanceConsulta.Alcance a = alcanceConsulta.resolver(f);
        return gestiones.delPeriodo(a.desde().atStartOfDay(), a.hasta().plusDays(1).atStartOfDay(), a.asesores(),
                        a.cartera(), a.cosecha(), LIMITE)
                .stream().map(g -> GestionDto.desde(g, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<PromesaDto> promesas(Filtros f) {
        AlcanceConsulta.Alcance a = alcanceConsulta.resolver(f);
        return promesas.delPeriodo(a.desde().atStartOfDay(), a.hasta().plusDays(1).atStartOfDay(), a.asesores(),
                        a.cartera(), a.cosecha(), LIMITE)
                .stream().map(p -> PromesaDto.desde(p, reloj.hoy(), true)).toList();
    }

    @Transactional(readOnly = true)
    public List<PagoDto> pagos(Filtros f) {
        AlcanceConsulta.Alcance a = alcanceConsulta.resolver(f);
        return pagos.delPeriodo(a.desde(), a.hasta(), a.todos(), a.asesores(), a.cartera(), a.cosecha(), LIMITE)
                .stream().map(fila -> PagoDto.desde((Pago) fila[0], (Usuario) fila[1], true)).toList();
    }
}
