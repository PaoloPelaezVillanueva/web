package com.escall.crm.config;

import com.escall.crm.modelo.*;
import com.escall.crm.repositorio.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Se ejecuta al arrancar. Solo crea lo que falta (nunca duplica ni borra):
 *  1. El usuario administrador inicial (si no hay usuarios).
 *  2. El árbol de tipificaciones base (si está vacío).
 *  3. Datos de demostración (si app.datos-demo=true y no hay clientes).
 */
@Component
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);
    private static final String CLAVE_DEMO = "Clave12345";

    private final UsuarioRepository usuarios;
    private final TipificacionN1Repository n1Repo;
    private final TipificacionN2Repository n2Repo;
    private final ClienteRepository clientes;
    private final CuentaRepository cuentas;
    private final AsignacionRepository asignaciones;
    private final GestionRepository gestiones;
    private final PagoRepository pagos;
    private final PasswordEncoder passwordEncoder;
    private final Reloj reloj;

    @Value("${app.admin.usuario}")
    private String adminUsuario;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.datos-demo}")
    private boolean datosDemo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            usuario(adminUsuario, "Administrador del sistema", Rol.ADMIN, null, adminPassword);
            log.warn("Administrador creado: usuario '{}'. ¡Cambia la contraseña!", adminUsuario);
        }
        if (n1Repo.count() == 0) {
            crearArbolTipificaciones();
            log.info("Árbol de tipificaciones base creado");
        }
        if (datosDemo && clientes.count() == 0) {
            crearDatosDemo();
            log.info("Datos de demostración creados (contraseña de usuarios demo: {})", CLAVE_DEMO);
        }
    }

    // ------------------------------------------------------------------ Tipificaciones

    private void crearArbolTipificaciones() {
        TipificacionN1 directo = n1("Contacto directo", true, 1);
        n2(directo, "Promesa de pago", true, 1);
        n2(directo, "Ya pagó", false, 2);
        n2(directo, "Solicita refinanciación", false, 3);
        n2(directo, "Negativa de pago", false, 4);
        n2(directo, "Volver a llamar", false, 5);

        TipificacionN1 indirecto = n1("Contacto indirecto", true, 2);
        n2(indirecto, "Mensaje con familiar", false, 1);
        n2(indirecto, "Mensaje con tercero", false, 2);
        n2(indirecto, "Promesa de pago por tercero", true, 3);

        TipificacionN1 noContacto = n1("No contacto", false, 3);
        n2(noContacto, "No contesta", false, 1);
        n2(noContacto, "Buzón de voz", false, 2);
        n2(noContacto, "Teléfono apagado", false, 3);
        n2(noContacto, "Cuelga la llamada", false, 4);

        TipificacionN1 ilocalizado = n1("Ilocalizado", false, 4);
        n2(ilocalizado, "Número equivocado", false, 1);
        n2(ilocalizado, "Número no existe", false, 2);
        n2(ilocalizado, "Titular fallecido", false, 3);
    }

    private TipificacionN1 n1(String nombre, boolean esContacto, int orden) {
        TipificacionN1 t = new TipificacionN1();
        t.setNombre(nombre);
        t.setEsContacto(esContacto);
        t.setOrden(orden);
        return n1Repo.save(t);
    }

    private void n2(TipificacionN1 n1, String nombre, boolean requiereCompromiso, int orden) {
        TipificacionN2 t = new TipificacionN2();
        t.setN1(n1);
        t.setNombre(nombre);
        t.setRequiereCompromiso(requiereCompromiso);
        t.setOrden(orden);
        n1.getResultados().add(n2Repo.save(t));
    }

    // ------------------------------------------------------------------ Demo

    private void crearDatosDemo() {
        Usuario supA = usuario("sgarcia", "Sandra García", Rol.SUPERVISOR, null, CLAVE_DEMO);
        Usuario supB = usuario("mrojas", "Miguel Rojas", Rol.SUPERVISOR, null, CLAVE_DEMO);
        List<Usuario> asesores = List.of(
                usuario("asesor1", "Lucía Torres", Rol.ASESOR, supA, CLAVE_DEMO),
                usuario("asesor2", "Carlos Medina", Rol.ASESOR, supA, CLAVE_DEMO),
                usuario("asesor3", "Valeria Quispe", Rol.ASESOR, supB, CLAVE_DEMO));

        String[][] personas = {
                {"45879632", "Juan Carlos Pérez Díaz", "987654321"}, {"41236587", "María Elena Flores Ramos", "912345678"},
                {"70125489", "José Luis Castillo Vega", "956321478"}, {"46985231", "Ana Lucía Gutiérrez Soto", "998877665"},
                {"10458796", "Pedro Antonio Huamán Cruz", "945612378"}, {"72563148", "Rosa María Chávez León", "934567812"},
                {"43698521", "Luis Alberto Mendoza Ríos", "976543210"}, {"47852369", "Carmen Rosa Vargas Ortiz", "923456789"},
                {"40123698", "Jorge Enrique Salazar Paz", "965478123"}, {"73654128", "Patricia Isabel Navarro Gil", "987123456"},
                {"44589632", "Ricardo Manuel Ponce Lara", "912987654"}, {"48741236", "Silvia Teresa Campos Rivas", "943218765"}};
        String[][] carteras = {
                {"Banco Andino", "Tarjeta de crédito", "Banco Andino - Tarjetas"},
                {"Financiera Sol", "Préstamo personal", "Financiera Sol - Consumo"},
                {"Caja Norte", "Crédito vehicular", "Caja Norte - Vehicular"}};
        String[] cosechas = {"2026-07", "2026-08", "2026-09"};

        Random azar = new Random(42); // semilla fija: siempre los mismos datos de ejemplo
        LocalDate hoy = reloj.hoy();
        List<Cuenta> todas = new ArrayList<>();
        int operacion = 100200;
        for (int i = 0; i < personas.length; i++) {
            Cliente c = new Cliente();
            c.setDocumento(personas[i][0]);
            c.setNombres(personas[i][1]);
            c.agregarTelefono(personas[i][2], "CELULAR");
            if (i % 3 == 0) c.agregarTelefono("01" + personas[i][0].substring(0, 7), "FIJO");
            c.setDireccion("Lima, Perú");
            clientes.save(c);

            int numCuentas = i % 4 == 0 ? 2 : 1;
            for (int k = 0; k < numCuentas; k++) {
                String[] cart = carteras[(i + k) % carteras.length];
                Cuenta cu = new Cuenta();
                cu.setCliente(c);
                cu.setNumeroOperacion("OP-" + (operacion++));
                cu.setEntidad(cart[0]);
                cu.setProducto(cart[1]);
                cu.setCartera(cart[2]);
                cu.setCosecha(cosechas[(i + k) % cosechas.length]);
                cu.setDeudaTotal(BigDecimal.valueOf(800 + azar.nextInt(12000)).setScale(2, RoundingMode.HALF_UP));
                cu.setDiasMora(30 + azar.nextInt(300));
                cuentas.save(cu);
                todas.add(cu);

                Asignacion a = new Asignacion();
                a.setCuenta(cu);
                a.setAsesor(asesores.get(todas.size() % asesores.size()));
                a.setFechaAsignacion(hoy.minusDays(20));
                asignaciones.save(a);
            }
        }

        // Gestiones de los últimos días para que el dashboard tenga información
        List<TipificacionN1> arbol = n1Repo.findAllByOrderByOrdenAscNombreAsc();
        for (int d = 9; d >= 0; d--) {
            for (Cuenta cu : todas) {
                if (azar.nextInt(10) < 6) continue;
                Usuario asesor = asesores.get((todas.indexOf(cu) + 1) % asesores.size());
                TipificacionN1 n1 = arbol.get(azar.nextInt(arbol.size()));
                TipificacionN2 n2 = n1.getResultados().get(azar.nextInt(n1.getResultados().size()));

                Gestion g = new Gestion();
                g.setCuenta(cu);
                g.setAsesor(asesor);
                g.setFechaHora(hoy.minusDays(d).atTime(9 + azar.nextInt(9), azar.nextInt(60)));
                g.setTelefono(cu.getCliente().getTelefonos().getFirst().getNumero());
                g.setTipificacionN1(n1);
                g.setTipificacionN2(n2);
                g.setObservacion(n2.isRequiereCompromiso()
                        ? "Cliente se compromete a pagar en la fecha indicada."
                        : "Gestión telefónica: " + n2.getNombre().toLowerCase() + ".");
                if (n2.isRequiereCompromiso()) {
                    Promesa p = new Promesa();
                    p.setGestion(g);
                    p.setCuenta(cu);
                    p.setAsesor(asesor);
                    p.setFechaCompromiso(hoy.minusDays(d).plusDays(3 + azar.nextInt(5)));
                    p.setMonto(cu.getDeudaTotal().multiply(new BigDecimal("0.2")).setScale(2, RoundingMode.HALF_UP));
                    // Algunas promesas antiguas ya se cumplieron con un pago
                    if (d > 4 && azar.nextBoolean()) {
                        p.setEstado(EstadoPromesa.CUMPLIDA);
                        Pago pago = new Pago();
                        pago.setCuenta(cu);
                        pago.setFechaPago(hoy.minusDays(d - 2));
                        pago.setMonto(p.getMonto());
                        pago.setReferencia("DEMO-" + cu.getNumeroOperacion() + "-" + d);
                        pagos.save(pago);
                    }
                    g.setPromesa(p);
                }
                gestiones.save(g);
            }
        }
    }

    private Usuario usuario(String usuario, String nombre, Rol rol, Usuario supervisor, String clave) {
        Usuario u = new Usuario();
        u.setUsuario(usuario);
        u.setNombre(nombre);
        u.setEmail(usuario + "@escall.pe");
        u.setPasswordHash(passwordEncoder.encode(clave));
        u.setRol(rol);
        u.setSupervisor(supervisor);
        return usuarios.save(u);
    }
}
