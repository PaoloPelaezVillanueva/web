package com.escall.crm.servicio;

import com.escall.crm.dto.DashboardDto;
import com.escall.crm.dto.Filtros;
import com.escall.crm.dto.OpcionesFiltroDto;
import com.escall.crm.modelo.*;
import com.escall.crm.repositorio.*;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Dashboard operativo (RF10 - RF12): gestiones, contactos, promesas y pagos. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final PageRequest SIN_LIMITE_PRACTICO = PageRequest.of(0, 100_000);

    private final GestionRepository gestiones;
    private final PromesaRepository promesas;
    private final PagoRepository pagos;
    private final UsuarioRepository usuarios;
    private final CuentaRepository cuentas;
    private final UsuarioActual usuarioActual;
    private final AlcanceConsulta alcanceConsulta;

    @Transactional(readOnly = true)
    public DashboardDto resumen(Filtros filtros) {
        AlcanceConsulta.Alcance a = alcanceConsulta.resolver(filtros);
        var inicio = a.desde().atStartOfDay();
        var fin = a.hasta().plusDays(1).atStartOfDay();

        List<Gestion> listaGestiones = gestiones.delPeriodo(inicio, fin, a.asesores(), a.cartera(), a.cosecha(),
                SIN_LIMITE_PRACTICO);
        List<Promesa> listaPromesas = promesas.delPeriodo(inicio, fin, a.asesores(), a.cartera(), a.cosecha(),
                SIN_LIMITE_PRACTICO);
        List<Object[]> listaPagos = pagos.delPeriodo(a.desde(), a.hasta(), a.todos(), a.asesores(), a.cartera(),
                a.cosecha(), SIN_LIMITE_PRACTICO);

        long contactos = listaGestiones.stream().filter(g -> g.getTipificacionN1().isEsContacto()).count();
        BigDecimal montoPromesas = sumar(listaPromesas.stream().map(Promesa::getMonto));
        BigDecimal montoPagos = sumar(listaPagos.stream().map(f -> ((Pago) f[0]).getMonto()));

        return new DashboardDto(a.desde(), a.hasta(),
                listaGestiones.size(), contactos, porcentaje(contactos, listaGestiones.size()),
                listaPromesas.size(), montoPromesas, listaPagos.size(), montoPagos,
                porAsesor(a.asesores(), listaGestiones, listaPromesas, listaPagos),
                porDia(a.desde(), a.hasta(), listaGestiones),
                porTipificacion(listaGestiones));
    }

    /** Opciones de los desplegables de filtro según lo que el usuario puede ver. */
    @Transactional(readOnly = true)
    public OpcionesFiltroDto opciones() {
        List<Long> visibles = usuarioActual.asesoresVisibles(usuarioActual.obtener());
        List<OpcionesFiltroDto.Opcion> asesores = usuarios.findAllById(visibles).stream()
                .sorted(Comparator.comparing(Usuario::getNombre))
                .map(u -> new OpcionesFiltroDto.Opcion(u.getId(), u.getNombre() + (u.isActivo() ? "" : " (inactivo)")))
                .toList();
        return new OpcionesFiltroDto(cuentas.carteras(), cuentas.cosechas(), asesores);
    }

    // ---------------------------------------------------------------- cálculos

    private List<DashboardDto.FilaAsesor> porAsesor(List<Long> ids, List<Gestion> gs, List<Promesa> ps,
                                                    List<Object[]> pagosConAsesor) {
        Map<Long, List<Gestion>> gPorAsesor = gs.stream().collect(Collectors.groupingBy(g -> g.getAsesor().getId()));
        Map<Long, List<Promesa>> pPorAsesor = ps.stream().collect(Collectors.groupingBy(p -> p.getAsesor().getId()));
        Map<Long, List<Pago>> pagoPorAsesor = pagosConAsesor.stream().filter(f -> f[1] != null)
                .collect(Collectors.groupingBy(f -> ((Usuario) f[1]).getId(),
                        Collectors.mapping(f -> (Pago) f[0], Collectors.toList())));

        return usuarios.findAllById(ids).stream()
                .sorted(Comparator.comparing(Usuario::getNombre))
                .map(u -> {
                    List<Gestion> g = gPorAsesor.getOrDefault(u.getId(), List.of());
                    List<Promesa> p = pPorAsesor.getOrDefault(u.getId(), List.of());
                    List<Pago> pg = pagoPorAsesor.getOrDefault(u.getId(), List.of());
                    return new DashboardDto.FilaAsesor(u.getId(), u.getNombre(), g.size(),
                            g.stream().filter(x -> x.getTipificacionN1().isEsContacto()).count(),
                            p.size(), sumar(p.stream().map(Promesa::getMonto)),
                            pg.size(), sumar(pg.stream().map(Pago::getMonto)));
                })
                .toList();
    }

    private List<DashboardDto.Dia> porDia(LocalDate desde, LocalDate hasta, List<Gestion> gs) {
        if (ChronoUnit.DAYS.between(desde, hasta) > 62) {
            return List.of(); // para rangos largos el gráfico diario deja de ser legible
        }
        Map<LocalDate, List<Gestion>> agrupadas = gs.stream()
                .collect(Collectors.groupingBy(g -> g.getFechaHora().toLocalDate()));
        List<DashboardDto.Dia> dias = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            List<Gestion> delDia = agrupadas.getOrDefault(d, List.of());
            dias.add(new DashboardDto.Dia(d, delDia.size(),
                    delDia.stream().filter(g -> g.getTipificacionN1().isEsContacto()).count(),
                    delDia.stream().filter(g -> g.getPromesa() != null).count()));
        }
        return dias;
    }

    private List<DashboardDto.Conteo> porTipificacion(List<Gestion> gs) {
        return gs.stream()
                .collect(Collectors.groupingBy(g -> g.getTipificacionN1().getNombre() + " › " + g.getTipificacionN2().getNombre(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new DashboardDto.Conteo(e.getKey(), e.getValue()))
                .toList();
    }

    private static BigDecimal sumar(java.util.stream.Stream<BigDecimal> montos) {
        return montos.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double porcentaje(long parte, long total) {
        return total == 0 ? 0 : BigDecimal.valueOf(parte * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
