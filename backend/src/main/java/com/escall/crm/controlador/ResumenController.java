package com.escall.crm.controlador;

import com.escall.crm.dto.*;
import com.escall.crm.servicio.ClienteService;
import com.escall.crm.servicio.GestionService;
import com.escall.crm.servicio.TipificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Módulos Resumen (RF03 - RF06) y Gestiones (RF07 - RF09). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResumenController {

    private final ClienteService clientes;
    private final GestionService gestiones;
    private final TipificacionService tipificaciones;

    /** Ej: GET /api/clientes?q=45879632  (documento, operación o nombre) */
    @GetMapping("/clientes")
    public List<ClienteResumenDto> buscar(@RequestParam String q) {
        return clientes.buscar(q);
    }

    @GetMapping("/clientes/{id}")
    public ClienteDetalleDto detalle(@PathVariable Long id) {
        return clientes.detalle(id);
    }

    /** CRUD - Crear: alta de cliente (solo administrador). */
    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteDetalleDto crearCliente(@Valid @RequestBody ClienteRequest req) {
        return clientes.crear(req);
    }

    /** CRUD - Actualizar: corrige datos de contacto de un cliente que el usuario puede ver. */
    @PutMapping("/clientes/{id}")
    public ClienteDetalleDto actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteEdicionRequest req) {
        return clientes.actualizar(id, req);
    }

    /** CRUD - Eliminar: solo clientes sin cuentas (solo administrador). */
    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminarCliente(@PathVariable Long id) {
        clientes.eliminar(id);
    }

    /** Árbol de tipificaciones activas para el formulario de gestión. */
    @GetMapping("/tipificaciones")
    public List<TipificacionDto> tipificaciones() {
        return tipificaciones.arbol(true);
    }

    @PostMapping("/gestiones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ASESOR')")
    public GestionDto registrarGestion(@Valid @RequestBody GestionRequest req) {
        return gestiones.registrar(req);
    }

    /** Cuentas asignadas al asesor que inició sesión. */
    @GetMapping("/cartera")
    @PreAuthorize("hasRole('ASESOR')")
    public List<CuentaDto> cartera() {
        return clientes.cartera();
    }

    /** Promesas pendientes y vencidas del asesor. */
    @GetMapping("/cartera/agenda")
    @PreAuthorize("hasRole('ASESOR')")
    public List<PromesaDto> agenda() {
        return clientes.agenda();
    }
}
