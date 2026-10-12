package com.escall.crm.controlador;

import com.escall.crm.dto.*;
import com.escall.crm.servicio.AuditoriaService;
import com.escall.crm.servicio.IntegracionService;
import com.escall.crm.servicio.TipificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Funciones exclusivas del administrador: tipificaciones (RF22), Integración de Datos (RF18 - RF21) y auditoría (RF23). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdministracionController {

    private final TipificacionService tipificaciones;
    private final IntegracionService integracion;
    private final AuditoriaService auditoria;

    // ------------------------------------------------------------ Árbol de tipificaciones

    @GetMapping("/tipificaciones")
    public List<TipificacionDto> arbolCompleto() {
        return tipificaciones.arbol(false);
    }

    @PostMapping("/tipificaciones/n1")
    @ResponseStatus(HttpStatus.CREATED)
    public TipificacionDto crearN1(@Valid @RequestBody TipificacionN1Request req) {
        return tipificaciones.guardarN1(null, req);
    }

    @PutMapping("/tipificaciones/n1/{id}")
    public TipificacionDto editarN1(@PathVariable Long id, @Valid @RequestBody TipificacionN1Request req) {
        return tipificaciones.guardarN1(id, req);
    }

    @PostMapping("/tipificaciones/n2")
    @ResponseStatus(HttpStatus.CREATED)
    public TipificacionDto.Resultado crearN2(@Valid @RequestBody TipificacionN2Request req) {
        return tipificaciones.guardarN2(null, req);
    }

    @PutMapping("/tipificaciones/n2/{id}")
    public TipificacionDto.Resultado editarN2(@PathVariable Long id, @Valid @RequestBody TipificacionN2Request req) {
        return tipificaciones.guardarN2(id, req);
    }

    // Se desactiva en lugar de borrarse: el historial de gestiones conserva la tipificación original
    @DeleteMapping("/tipificaciones/n1/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarN1(@PathVariable Long id) {
        tipificaciones.desactivarN1(id);
    }

    @DeleteMapping("/tipificaciones/n2/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarN2(@PathVariable Long id) {
        tipificaciones.desactivarN2(id);
    }

    // ------------------------------------------------------------ Integración de Datos

    @PostMapping(value = "/integracion/asignaciones", consumes = "multipart/form-data")
    public CargaDto cargarAsignaciones(@RequestParam("archivo") MultipartFile archivo) {
        return integracion.cargarAsignaciones(archivo);
    }

    @PostMapping(value = "/integracion/pagos", consumes = "multipart/form-data")
    public CargaDto cargarPagos(@RequestParam("archivo") MultipartFile archivo) {
        return integracion.cargarPagos(archivo);
    }

    @GetMapping("/integracion/cargas")
    public List<CargaDto> historialDeCargas() {
        return integracion.historial();
    }

    // ------------------------------------------------------------ Auditoría

    @GetMapping("/auditoria")
    public Pagina<AuditoriaDto> auditoria(@RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "50") int tamano) {
        return auditoria.listar(pagina, tamano);
    }
}
