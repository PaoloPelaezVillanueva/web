package com.escall.crm.servicio;

import com.escall.crm.config.Reloj;
import com.escall.crm.dto.Filtros;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.Rol;
import com.escall.crm.modelo.Usuario;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Traduce los filtros del usuario (periodo, asesor, cartera, cosecha) a un rango de fechas
 * y a la lista de asesores que REALMENTE puede consultar según su rol (RF11, RF14, RF15).
 */
@Component
@RequiredArgsConstructor
public class AlcanceConsulta {

    private static final int DIAS_MAXIMOS = 366;

    private final UsuarioActual usuarioActual;
    private final Reloj reloj;

    public record Alcance(LocalDate desde, LocalDate hasta, List<Long> asesores, boolean todos,
                          String cartera, String cosecha) {
    }

    public Alcance resolver(Filtros f) {
        LocalDate hoy = reloj.hoy();
        LocalDate desde;
        LocalDate hasta;
        if (f.desde() != null || f.hasta() != null) {
            desde = f.desde() != null ? f.desde() : f.hasta();
            hasta = f.hasta() != null ? f.hasta() : hoy;
        } else {
            hasta = hoy;
            desde = switch (f.periodo() == null ? "MES" : f.periodo().toUpperCase()) {
                case "HOY" -> hoy;
                case "SEMANA" -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                default -> hoy.withDayOfMonth(1);
            };
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser mayor que la final");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS) {
            throw new ReglaNegocioException("El rango máximo de consulta es de un año");
        }

        Usuario u = usuarioActual.obtener();
        List<Long> visibles = usuarioActual.asesoresVisibles(u);
        List<Long> asesores = visibles;
        if (f.asesorId() != null) {
            if (!visibles.contains(f.asesorId())) {
                throw new ReglaNegocioException("Ese asesor no pertenece a tu equipo");
            }
            asesores = List.of(f.asesorId());
        }
        boolean todos = u.getRol() == Rol.ADMIN && f.asesorId() == null;
        return new Alcance(desde, hasta, UsuarioActual.noVacia(asesores), todos, vacioANull(f.cartera()),
                vacioANull(f.cosecha()));
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
