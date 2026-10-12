package com.escall.crm.servicio;

import com.escall.crm.config.Reloj;
import com.escall.crm.dto.GestionDto;
import com.escall.crm.dto.GestionRequest;
import com.escall.crm.excepcion.RecursoNoEncontradoException;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.*;
import com.escall.crm.repositorio.AsignacionRepository;
import com.escall.crm.repositorio.CuentaRepository;
import com.escall.crm.repositorio.GestionRepository;
import com.escall.crm.repositorio.TipificacionN2Repository;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro de gestiones (RF07 - RF09). */
@Service
@RequiredArgsConstructor
public class GestionService {

    /** Plazo máximo para una promesa de pago, en días. */
    static final int DIAS_MAXIMOS_COMPROMISO = 30;

    private final GestionRepository gestiones;
    private final CuentaRepository cuentas;
    private final AsignacionRepository asignaciones;
    private final TipificacionN2Repository tipificaciones;
    private final UsuarioActual usuarioActual;
    private final Reloj reloj;

    @Transactional
    public GestionDto registrar(GestionRequest req) {
        Usuario asesor = usuarioActual.obtener();
        Cuenta cuenta = cuentas.findById(req.cuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));

        // Solo se gestionan cuentas de la propia cartera
        Asignacion asignacion = asignaciones.findByCuentaIdAndActivaTrue(cuenta.getId())
                .filter(a -> a.getAsesor().getId().equals(asesor.getId()))
                .orElseThrow(() -> new ReglaNegocioException("Esta cuenta no está asignada a tu cartera"));

        // RF09: el nivel 2 elegido define automáticamente su nivel 1
        TipificacionN2 n2 = tipificaciones.findWithN1ById(req.tipificacionN2Id())
                .filter(t -> t.isActivo() && t.getN1().isActivo())
                .orElseThrow(() -> new ReglaNegocioException("La tipificación elegida no existe o está inactiva"));

        LocalDateTime ahora = reloj.ahora();
        LocalDateTime fechaHora = req.fechaHora() != null ? req.fechaHora() : ahora;
        if (fechaHora.isAfter(ahora.plusMinutes(5))) {
            throw new ReglaNegocioException("La fecha de la gestión no puede ser futura");
        }

        Gestion g = new Gestion();
        g.setCuenta(asignacion.getCuenta());
        g.setAsesor(asesor);
        g.setFechaHora(fechaHora);
        g.setTelefono(req.telefono().trim());
        g.setTipificacionN1(n2.getN1());
        g.setTipificacionN2(n2);
        g.setObservacion(req.observacion().trim());

        // RF08: si la tipificación lo exige, se registra la promesa de pago
        if (n2.isRequiereCompromiso()) {
            g.setPromesa(crearPromesa(req, g, cuenta, asesor));
        }

        // Si el teléfono usado es nuevo, se guarda en la ficha del cliente
        cuenta.getCliente().agregarTelefono(g.getTelefono(), "GESTION");

        return GestionDto.desde(gestiones.save(g), false);
    }

    private Promesa crearPromesa(GestionRequest req, Gestion g, Cuenta cuenta, Usuario asesor) {
        if (req.fechaCompromiso() == null || req.montoCompromiso() == null) {
            throw new ReglaNegocioException("Esta tipificación requiere fecha y monto de compromiso");
        }
        LocalDate hoy = reloj.hoy();
        if (req.fechaCompromiso().isBefore(hoy)) {
            throw new ReglaNegocioException("La fecha de compromiso no puede ser anterior a hoy");
        }
        if (req.fechaCompromiso().isAfter(hoy.plusDays(DIAS_MAXIMOS_COMPROMISO))) {
            throw new ReglaNegocioException("La fecha de compromiso no puede superar " + DIAS_MAXIMOS_COMPROMISO + " días");
        }
        if (req.montoCompromiso().compareTo(cuenta.getDeudaTotal()) > 0) {
            throw new ReglaNegocioException("El monto comprometido no puede superar la deuda de la cuenta");
        }
        Promesa p = new Promesa();
        p.setGestion(g);
        p.setCuenta(cuenta);
        p.setAsesor(asesor);
        p.setFechaCompromiso(req.fechaCompromiso());
        p.setMonto(req.montoCompromiso());
        return p;
    }
}
