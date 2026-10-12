package com.escall.crm.servicio;

import com.escall.crm.config.Reloj;
import com.escall.crm.dto.CargaDto;
import com.escall.crm.excepcion.ReglaNegocioException;
import com.escall.crm.modelo.*;
import com.escall.crm.repositorio.*;
import com.escall.crm.seguridad.UsuarioActual;
import com.escall.crm.util.LectorCsv;
import com.escall.crm.util.LectorCsv.Fila;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Integración de Datos (RF18 - RF21): carga de asignaciones y de pagos desde archivos CSV.
 * Cada fila se valida por separado: las válidas se guardan y las inválidas se reportan
 * con su número de fila y el motivo, sin detener la carga completa.
 */
@Service
@RequiredArgsConstructor
public class IntegracionService {

    public static final Set<String> COLUMNAS_ASIGNACION = Set.of("documento", "nombres", "numero_operacion",
            "entidad", "producto", "cartera", "cosecha", "deuda", "dias_mora", "asesor");
    public static final Set<String> COLUMNAS_PAGO = Set.of("numero_operacion", "fecha_pago", "monto");

    private static final int MAX_ERRORES_GUARDADOS = 500;
    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"));

    private final ClienteRepository clientes;
    private final CuentaRepository cuentas;
    private final AsignacionRepository asignaciones;
    private final PagoRepository pagos;
    private final PromesaRepository promesas;
    private final UsuarioRepository usuarios;
    private final CargaRepository cargas;
    private final UsuarioActual usuarioActual;
    private final AuditoriaService auditoria;
    private final Reloj reloj;

    // =================================================================== ASIGNACIONES

    @Transactional
    public CargaDto cargarAsignaciones(MultipartFile archivo) {
        List<Fila> filas = leer(archivo, COLUMNAS_ASIGNACION);
        Carga carga = iniciarCarga(Carga.Tipo.ASIGNACION, archivo, filas.size());
        LocalDate hoy = reloj.hoy();

        Map<String, Usuario> asesoresPorUsuario = new HashMap<>();
        Set<String> operacionesEnArchivo = new HashSet<>();
        List<String> errores = new ArrayList<>();
        int aceptados = 0;

        for (Fila f : filas) {
            try {
                String faltante = COLUMNAS_ASIGNACION.stream().filter(c -> f.get(c) == null).sorted().findFirst().orElse(null);
                if (faltante != null) {
                    throw new ReglaNegocioException("falta el campo \"" + faltante + "\"");
                }
                String operacion = f.get("numero_operacion");
                if (!operacionesEnArchivo.add(operacion)) {
                    throw new ReglaNegocioException("la operación " + operacion + " está repetida en el archivo");
                }
                String documento = f.get("documento");
                if (!documento.matches("[A-Za-z0-9-]{6,20}")) {
                    throw new ReglaNegocioException("documento inválido \"" + documento + "\"");
                }
                BigDecimal deuda = decimal(f.get("deuda"), "deuda");
                if (deuda.signum() < 0) {
                    throw new ReglaNegocioException("la deuda no puede ser negativa");
                }
                int diasMora = entero(f.get("dias_mora"), "dias_mora");
                String usuarioAsesor = f.get("asesor").toLowerCase(Locale.ROOT);
                Usuario asesor = asesoresPorUsuario.computeIfAbsent(usuarioAsesor,
                        clave -> usuarios.findByUsuarioIgnoreCase(clave)
                                .filter(u -> u.getRol() == Rol.ASESOR && u.isActivo()).orElse(null));
                if (asesor == null) {
                    throw new ReglaNegocioException("el asesor \"" + usuarioAsesor + "\" no existe o no está activo");
                }

                // Antes de guardar nada: la operación no puede pertenecer a otro cliente
                Cuenta cuenta = cuentas.findByNumeroOperacion(operacion).orElseGet(Cuenta::new);
                if (cuenta.getId() != null && !cuenta.getCliente().getDocumento().equals(documento)) {
                    throw new ReglaNegocioException("la operación " + operacion + " ya pertenece a otro cliente");
                }

                // Cliente: se crea o se actualiza por documento
                Cliente cliente = clientes.findByDocumento(documento).orElseGet(Cliente::new);
                cliente.setDocumento(documento);
                cliente.setTipoDocumento(Optional.ofNullable(f.get("tipo_documento")).orElse("DNI").toUpperCase(Locale.ROOT));
                cliente.setNombres(f.get("nombres"));
                if (f.get("email") != null) cliente.setEmail(f.get("email"));
                if (f.get("direccion") != null) cliente.setDireccion(f.get("direccion"));
                cliente.agregarTelefono(f.get("telefono"), "CELULAR");
                cliente = clientes.save(cliente);

                // Cuenta: se crea o se actualiza por número de operación
                cuenta.setCliente(cliente);
                cuenta.setNumeroOperacion(operacion);
                cuenta.setEntidad(f.get("entidad"));
                cuenta.setProducto(f.get("producto"));
                cuenta.setCartera(f.get("cartera"));
                cuenta.setCosecha(f.get("cosecha"));
                cuenta.setDeudaTotal(deuda);
                cuenta.setDiasMora(diasMora);
                cuenta = cuentas.save(cuenta);

                // Asignación: si cambia de asesor, la anterior se cierra y queda como historial
                Optional<Asignacion> actual = asignaciones.findByCuentaIdAndActivaTrue(cuenta.getId());
                if (actual.isEmpty() || !actual.get().getAsesor().getId().equals(asesor.getId())) {
                    actual.ifPresent(a -> {
                        a.setActiva(false);
                        a.setFechaFin(hoy);
                    });
                    Asignacion nueva = new Asignacion();
                    nueva.setCuenta(cuenta);
                    nueva.setAsesor(asesor);
                    nueva.setFechaAsignacion(hoy);
                    nueva.setCarga(carga);
                    asignaciones.save(nueva);
                }
                aceptados++;
            } catch (ReglaNegocioException e) {
                errores.add("Fila " + f.numero() + ": " + e.getMessage());
            }
        }
        return cerrarCarga(carga, aceptados, errores);
    }

    // =================================================================== PAGOS

    @Transactional
    public CargaDto cargarPagos(MultipartFile archivo) {
        List<Fila> filas = leer(archivo, COLUMNAS_PAGO);
        Carga carga = iniciarCarga(Carga.Tipo.PAGO, archivo, filas.size());
        LocalDate hoy = reloj.hoy();

        Set<String> clavesEnArchivo = new HashSet<>();
        List<String> errores = new ArrayList<>();
        int aceptados = 0;

        for (Fila f : filas) {
            try {
                String faltante = COLUMNAS_PAGO.stream().filter(c -> f.get(c) == null).sorted().findFirst().orElse(null);
                if (faltante != null) {
                    throw new ReglaNegocioException("falta el campo \"" + faltante + "\"");
                }
                String operacion = f.get("numero_operacion");
                Cuenta cuenta = cuentas.findByNumeroOperacion(operacion)
                        .orElseThrow(() -> new ReglaNegocioException("la operación " + operacion + " no existe"));
                LocalDate fecha = fecha(f.get("fecha_pago"));
                if (fecha.isAfter(hoy)) {
                    throw new ReglaNegocioException("la fecha de pago no puede ser futura");
                }
                BigDecimal monto = decimal(f.get("monto"), "monto");
                if (monto.signum() <= 0) {
                    throw new ReglaNegocioException("el monto debe ser mayor a 0");
                }
                String referencia = Optional.ofNullable(f.get("referencia")).orElse("");

                String clave = cuenta.getId() + "|" + fecha + "|" + monto.stripTrailingZeros().toPlainString() + "|" + referencia;
                if (!clavesEnArchivo.add(clave)
                        || pagos.existsByCuentaIdAndFechaPagoAndMontoAndReferencia(cuenta.getId(), fecha, monto, referencia)) {
                    throw new ReglaNegocioException("pago duplicado (misma operación, fecha, monto y referencia)");
                }

                Pago p = new Pago();
                p.setCuenta(cuenta);
                p.setFechaPago(fecha);
                p.setMonto(monto);
                p.setReferencia(referencia);
                p.setCarga(carga);
                pagos.save(p);

                // Un pago cumple las promesas pendientes que se hicieron antes de esa fecha
                promesas.marcarCumplidas(cuenta.getId(), fecha.plusDays(1).atStartOfDay());
                aceptados++;
            } catch (ReglaNegocioException e) {
                errores.add("Fila " + f.numero() + ": " + e.getMessage());
            }
        }
        return cerrarCarga(carga, aceptados, errores);
    }

    @Transactional(readOnly = true)
    public List<CargaDto> historial() {
        return cargas.findTop20ByOrderByCreadoEnDesc().stream().map(CargaDto::desde).toList();
    }

    // =================================================================== auxiliares

    private List<Fila> leer(MultipartFile archivo, Set<String> columnas) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Selecciona un archivo");
        }
        String nombre = Optional.ofNullable(archivo.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!nombre.endsWith(".csv") && !nombre.endsWith(".txt")) {
            throw new ReglaNegocioException("Formato no soportado: usa un archivo .csv (en Excel: Guardar como > CSV UTF-8)");
        }
        try {
            return LectorCsv.leer(archivo.getInputStream(), columnas);
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo");
        }
    }

    private Carga iniciarCarga(Carga.Tipo tipo, MultipartFile archivo, int total) {
        Carga c = new Carga();
        c.setTipo(tipo);
        String nombre = Optional.ofNullable(archivo.getOriginalFilename()).orElse("archivo.csv");
        c.setNombreArchivo(nombre.length() > 200 ? nombre.substring(0, 200) : nombre);
        c.setUsuario(usuarioActual.obtener());
        c.setTotal(total);
        return cargas.save(c);
    }

    private CargaDto cerrarCarga(Carga c, int aceptados, List<String> errores) {
        c.setAceptados(aceptados);
        c.setRechazados(errores.size());
        List<String> guardados = errores.size() > MAX_ERRORES_GUARDADOS
                ? new ArrayList<>(errores.subList(0, MAX_ERRORES_GUARDADOS)) : errores;
        if (errores.size() > MAX_ERRORES_GUARDADOS) {
            guardados.add("... y " + (errores.size() - MAX_ERRORES_GUARDADOS) + " errores más");
        }
        c.setDetalleErrores(String.join("\n", guardados));
        auditoria.registrar(c.getUsuario().getUsuario(), "CARGA_" + c.getTipo(), "Carga", c.getId(),
                c.getNombreArchivo() + ": " + aceptados + " aceptados, " + errores.size() + " rechazados");
        return CargaDto.desde(c);
    }

    private static BigDecimal decimal(String valor, String campo) {
        try {
            return new BigDecimal(valor.replace(" ", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("\"" + campo + "\" debe ser un número (ej: 1250.50)");
        }
    }

    private static int entero(String valor, String campo) {
        try {
            int n = Integer.parseInt(valor.trim());
            if (n < 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("\"" + campo + "\" debe ser un número entero positivo");
        }
    }

    private static LocalDate fecha(String valor) {
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(valor.trim(), formato);
            } catch (DateTimeParseException ignorada) {
                // probamos el siguiente formato
            }
        }
        throw new ReglaNegocioException("fecha inválida \"" + valor + "\" (usa AAAA-MM-DD o DD/MM/AAAA)");
    }
}
