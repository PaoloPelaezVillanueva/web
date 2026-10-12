package com.escall.crm.servicio;

import com.escall.crm.config.Reloj;
import com.escall.crm.dto.*;
import com.escall.crm.excepcion.RecursoNoEncontradoException;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.*;
import com.escall.crm.repositorio.*;
import com.escall.crm.seguridad.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Módulo Resumen (RF03 - RF06) y cartera del asesor. */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int MAX_RESULTADOS = 30;

    private final ClienteRepository clientes;
    private final CuentaRepository cuentas;
    private final AsignacionRepository asignaciones;
    private final GestionRepository gestiones;
    private final PagoRepository pagos;
    private final PromesaRepository promesas;
    private final UsuarioActual usuarioActual;
    private final Reloj reloj;
    private final AuditoriaService auditoria;

    /** RF03: busca por documento, número de operación o nombre. */
    @Transactional(readOnly = true)
    public List<ClienteResumenDto> buscar(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() < 3) {
            throw new ReglaNegocioException("Escribe al menos 3 caracteres para buscar");
        }
        String patron = "%" + limpio.toLowerCase(Locale.ROOT) + "%";
        Usuario u = usuarioActual.obtener();
        PageRequest limite = PageRequest.of(0, MAX_RESULTADOS);
        List<Cliente> encontrados = u.getRol() == Rol.ADMIN
                ? clientes.buscar(patron, limite)
                : clientes.buscarEnCartera(patron, UsuarioActual.noVacia(usuarioActual.asesoresVisibles(u)), limite);

        return encontrados.stream().map(c -> {
            List<Cuenta> deCliente = cuentas.findByClienteIdOrderByNumeroOperacionAsc(c.getId());
            BigDecimal deuda = deCliente.stream().map(Cuenta::getDeudaTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new ClienteResumenDto(c.getId(), c.getTipoDocumento(), c.getDocumento(), c.getNombres(),
                    deCliente.size(), deuda);
        }).toList();
    }

    /** RF04 - RF06: datos, cuentas, historial de gestiones, pagos y promesas del cliente. */
    @Transactional(readOnly = true)
    public ClienteDetalleDto detalle(Long clienteId) {
        Cliente c = clientes.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        verificarAcceso(clienteId);

        List<Cuenta> deCliente = cuentas.findByClienteIdOrderByNumeroOperacionAsc(clienteId);
        List<Pago> pagosCliente = pagos.delCliente(clienteId);
        Map<Long, Usuario> asesorPorCuenta = asesorPorCuenta(deCliente);
        List<CuentaDto> cuentasDto = deCliente.stream()
                .map(cu -> aDto(cu, totalPagado(cu.getId(), pagosCliente), asesorPorCuenta.get(cu.getId()), false))
                .toList();

        return new ClienteDetalleDto(c.getId(), c.getTipoDocumento(), c.getDocumento(), c.getNombres(), c.getEmail(),
                c.getDireccion(),
                c.getTelefonos().stream().filter(Telefono::isActivo).map(TelefonoDto::desde).toList(),
                cuentasDto,
                gestiones.historialDelCliente(clienteId).stream().map(g -> GestionDto.desde(g, false)).toList(),
                pagosCliente.stream().map(p -> PagoDto.desde(p, asesorPorCuenta.get(p.getCuenta().getId()), false)).toList(),
                promesas.delCliente(clienteId).stream().map(p -> PromesaDto.desde(p, reloj.hoy(), false)).toList());
    }

    /** Cartera del asesor: sus cuentas asignadas, ordenadas por días de mora. */
    @Transactional(readOnly = true)
    public List<CuentaDto> cartera() {
        Usuario asesor = usuarioActual.obtener();
        List<Asignacion> lista = asignaciones.findByAsesorIdAndActivaTrueOrderByCuentaDiasMoraDesc(asesor.getId());
        List<Long> ids = lista.stream().map(a -> a.getCuenta().getId()).toList();
        Map<Long, BigDecimal> pagado = ids.isEmpty() ? Map.of() : pagos.totalesPorCuenta(ids).stream()
                .collect(Collectors.toMap(f -> (Long) f[0], f -> (BigDecimal) f[1]));
        return lista.stream()
                .map(a -> aDto(a.getCuenta(), pagado.getOrDefault(a.getCuenta().getId(), BigDecimal.ZERO), asesor, true))
                .toList();
    }

    /** Promesas pendientes del asesor (incluye las vencidas) para su seguimiento diario. */
    @Transactional(readOnly = true)
    public List<PromesaDto> agenda() {
        Usuario asesor = usuarioActual.obtener();
        return promesas.delAsesorPorEstado(asesor.getId(), EstadoPromesa.PENDIENTE).stream()
                .map(p -> PromesaDto.desde(p, reloj.hoy(), true)).toList();
    }

    /** CRUD - Crear (solo administrador). El cliente nace sin cuentas; las cuentas llegan por Integración de Datos. */
    @Transactional
    public ClienteDetalleDto crear(ClienteRequest req) {
        String documento = req.documento().trim();
        if (clientes.existsByDocumento(documento)) {
            throw new ReglaNegocioException("Ya existe un cliente con ese documento");
        }
        Cliente c = new Cliente();
        c.setTipoDocumento(limpiar(req.tipoDocumento()) == null ? "DNI" : limpiar(req.tipoDocumento()));
        c.setDocumento(documento);
        c.setNombres(req.nombres().trim());
        c.setEmail(limpiar(req.email()));
        c.setDireccion(limpiar(req.direccion()));
        c = clientes.save(c);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), "CREAR", "Cliente", c.getId(), c.getDocumento());
        return detalle(c.getId());
    }

    /** CRUD - Actualizar: corrige datos de contacto. Lo puede hacer quien ya ve al cliente (asesor de su cartera, supervisor de su equipo, administrador). */
    @Transactional
    public ClienteDetalleDto actualizar(Long id, ClienteEdicionRequest req) {
        Cliente c = clientes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        verificarAcceso(id);
        c.setNombres(req.nombres().trim());
        c.setEmail(limpiar(req.email()));
        c.setDireccion(limpiar(req.direccion()));
        auditoria.registrar(usuarioActual.obtener().getUsuario(), "EDITAR", "Cliente", id, c.getDocumento());
        return detalle(id);
    }

    /** CRUD - Eliminar (solo administrador). Solo si no tiene cuentas: así nunca se pierde deuda ni historial. */
    @Transactional
    public void eliminar(Long id) {
        Cliente c = clientes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        if (cuentas.existsByClienteId(id)) {
            throw new ReglaNegocioException("El cliente tiene cuentas registradas; no se puede eliminar");
        }
        String documento = c.getDocumento();
        clientes.delete(c);
        auditoria.registrar(usuarioActual.obtener().getUsuario(), "ELIMINAR", "Cliente", id, documento);
    }

    // ---------------------------------------------------------------- auxiliares

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private void verificarAcceso(Long clienteId) {
        Usuario u = usuarioActual.obtener();
        if (u.getRol() != Rol.ADMIN
                && !clientes.esVisiblePara(clienteId, UsuarioActual.noVacia(usuarioActual.asesoresVisibles(u)))) {
            // Mismo mensaje que "no existe": no revelamos clientes de otras carteras
            throw new RecursoNoEncontradoException("Cliente no encontrado");
        }
    }

    private Map<Long, Usuario> asesorPorCuenta(List<Cuenta> lista) {
        if (lista.isEmpty()) {
            return Map.of();
        }
        return asignaciones.findByCuentaIdInAndActivaTrue(lista.stream().map(Cuenta::getId).toList()).stream()
                .collect(Collectors.toMap(a -> a.getCuenta().getId(), Asignacion::getAsesor, (a, b) -> a));
    }

    private static BigDecimal totalPagado(Long cuentaId, List<Pago> pagosCliente) {
        return pagosCliente.stream().filter(p -> p.getCuenta().getId().equals(cuentaId))
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static CuentaDto aDto(Cuenta c, BigDecimal pagado, Usuario asesor, boolean conCliente) {
        BigDecimal saldo = c.getDeudaTotal().subtract(pagado).max(BigDecimal.ZERO);
        return new CuentaDto(c.getId(), c.getNumeroOperacion(), c.getEntidad(), c.getProducto(), c.getCartera(),
                c.getCosecha(), c.getDeudaTotal(), c.getDiasMora(), pagado, saldo,
                asesor != null ? asesor.getId() : null, asesor != null ? asesor.getNombre() : null,
                conCliente ? c.getCliente().getId() : null,
                conCliente ? c.getCliente().getDocumento() : null,
                conCliente ? c.getCliente().getNombres() : null);
    }
}
