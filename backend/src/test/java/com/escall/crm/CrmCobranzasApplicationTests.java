package com.escall.crm;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración (TDD) sobre el perfil demo (H2 en memoria con datos de ejemplo).
 * Cada prueba indica el requisito funcional que verifica.
 */
@SpringBootTest
@ActiveProfiles("demo")
class CrmCobranzasApplicationTests {

    private static final String CLAVE_DEMO = "Clave12345";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void rf01_loginConUsuarioYContrasena() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"password\":\"Admin12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"));

        mvc.perform(get("/api/clientes").param("q", "Pérez")).andExpect(status().isUnauthorized());
    }

    @Test
    void rnf05_permisosPorRol() throws Exception {
        String asesor = login("asesor1", CLAVE_DEMO);
        String supervisor = login("sgarcia", CLAVE_DEMO);

        mvc.perform(get("/api/dashboard").header("Authorization", bearer(asesor))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/integracion/cargas").header("Authorization", bearer(supervisor)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").param("periodo", "SEMANA").header("Authorization", bearer(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porAsesor.length()").value(2)); // solo su equipo (RF14)
    }

    @Test
    void rf03_rf04_asesorSoloVeClientesDeSuCartera() throws Exception {
        String asesor1 = login("asesor1", CLAVE_DEMO);
        String asesor3 = login("asesor3", CLAVE_DEMO);

        List<Integer> clientesAsesor1 = JsonPath.read(getJson("/api/cartera", asesor1), "$[*].clienteId");
        List<Integer> clientesAsesor3 = JsonPath.read(getJson("/api/cartera", asesor3), "$[*].clienteId");
        Integer soloDeAsesor1 = clientesAsesor1.stream().filter(id -> !clientesAsesor3.contains(id)).findFirst().orElseThrow();

        mvc.perform(get("/api/clientes/" + soloDeAsesor1).header("Authorization", bearer(asesor1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentas").isNotEmpty());
        mvc.perform(get("/api/clientes/" + soloDeAsesor1).header("Authorization", bearer(asesor3)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf07_rf08_gestionConPromesaYPagoQueLaCumple() throws Exception {
        String asesor = login("asesor1", CLAVE_DEMO);
        String cartera = getJson("/api/cartera", asesor);
        Integer cuentaId = JsonPath.read(cartera, "$[0].id");
        Integer clienteId = JsonPath.read(cartera, "$[0].clienteId");
        String operacion = JsonPath.read(cartera, "$[0].numeroOperacion");
        Integer promesaN2 = JsonPath.<List<Integer>>read(getJson("/api/tipificaciones", asesor),
                "$[*].resultados[?(@.nombre == 'Promesa de pago')].id").getFirst();

        // RF08: sin fecha ni monto de compromiso se rechaza
        mvc.perform(post("/api/gestiones").header("Authorization", bearer(asesor)).contentType(MediaType.APPLICATION_JSON)
                        .content(gestion(cuentaId, promesaN2, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esta tipificación requiere fecha y monto de compromiso"));

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        mvc.perform(post("/api/gestiones").header("Authorization", bearer(asesor)).contentType(MediaType.APPLICATION_JSON)
                        .content(gestion(cuentaId, promesaN2, hoy.plusDays(2).toString(), "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel1").value("Contacto directo"))
                .andExpect(jsonPath("$.montoCompromiso").value(150.00));

        // RF20 - RF21: carga de pagos con una fila válida, una duplicada y una inexistente
        String admin = login("admin", "Admin12345");
        String csv = "numero_operacion;fecha_pago;monto;referencia\n"
                + operacion + ";" + hoy + ";150.00;VOUCHER-1\n"
                + operacion + ";" + hoy + ";150.00;VOUCHER-1\n"
                + "OP-NO-EXISTE;" + hoy + ";99;X\n";
        mvc.perform(multipart("/api/admin/integracion/pagos").file(archivo("pagos.csv", csv))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceptados").value(1))
                .andExpect(jsonPath("$.rechazados").value(2))
                .andExpect(jsonPath("$.errores[0]").value("Fila 3: pago duplicado (misma operación, fecha, monto y referencia)"));

        // El pago marcó como cumplida la promesa de esa cuenta
        String detalle = getJson("/api/clientes/" + clienteId, asesor);
        List<String> estados = JsonPath.read(detalle, "$.promesas[?(@.cuentaId == " + cuentaId + ")].estado");
        assertThat(estados).isNotEmpty().allMatch(e -> e.equals("CUMPLIDA"));
    }

    @Test
    void rf19_rf21_cargaDeAsignacionesValidaEstructuraYFilas() throws Exception {
        String admin = login("admin", "Admin12345");

        mvc.perform(multipart("/api/admin/integracion/asignaciones").file(archivo("malo.csv", "documento;nombres\n123;X\n"))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("Estructura inválida")));

        String csv = "documento;nombres;telefono;numero_operacion;entidad;producto;cartera;cosecha;deuda;dias_mora;asesor\n"
                + "99887766;Cliente Nuevo Prueba;999111222;OP-TEST-1;Banco Andino;Tarjeta;Banco Andino - Tarjetas;2026-10;2500.50;45;asesor3\n"
                + "99887767;Otro Cliente;999111223;OP-TEST-2;Banco Andino;Tarjeta;Banco Andino - Tarjetas;2026-10;100;10;no.existe\n";
        mvc.perform(multipart("/api/admin/integracion/asignaciones").file(archivo("asignacion.csv", csv))
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceptados").value(1))
                .andExpect(jsonPath("$.rechazados").value(1));

        String asesor3 = login("asesor3", CLAVE_DEMO);
        mvc.perform(get("/api/clientes").param("q", "99887766").header("Authorization", bearer(asesor3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombres").value("Cliente Nuevo Prueba"));
    }

    @Test
    void rf16_supervisorSoloAdministraSuEquipo() throws Exception {
        String supervisor = login("sgarcia", CLAVE_DEMO);
        String admin = login("admin", "Admin12345");
        Map<String, Integer> ids = idsDeUsuarios(admin);

        // Aunque pida rol ADMIN, el supervisor solo puede crear asesores de su equipo
        mvc.perform(post("/api/usuarios").header("Authorization", bearer(supervisor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"nuevo.asesor\",\"nombre\":\"Nuevo Asesor\",\"email\":\"nuevo@escall.pe\","
                                + "\"password\":\"Segura2026\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ASESOR"))
                .andExpect(jsonPath("$.supervisorNombre").value("Sandra García"));

        mvc.perform(patch("/api/usuarios/" + ids.get("asesor3") + "/estado").header("Authorization", bearer(supervisor))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isNotFound());

        // RF23: las acciones quedan en la auditoría
        mvc.perform(get("/api/admin/auditoria").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.accion == 'CREAR' && @.usuario == 'sgarcia')]").isNotEmpty());
    }

    @Test
    void rf22_tipificacionesSeDesactivanSinBorrarse() throws Exception {
        String admin = login("admin", "Admin12345");
        Integer n2Id = JsonPath.<List<Integer>>read(getJson("/api/admin/tipificaciones", admin),
                "$[*].resultados[?(@.nombre == 'Teléfono apagado')].id").getFirst();

        mvc.perform(delete("/api/admin/tipificaciones/n2/" + n2Id).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        // Sigue en el árbol del administrador, pero marcada como inactiva (no se pierde el historial)
        List<Boolean> activa = JsonPath.read(getJson("/api/admin/tipificaciones", admin),
                "$[*].resultados[?(@.id == " + n2Id + ")].activo");
        assertThat(activa).containsExactly(false);

        // El asesor ya no la recibe al tipificar una gestión
        String asesor = login("asesor1", CLAVE_DEMO);
        List<Integer> visibles = JsonPath.read(getJson("/api/tipificaciones", asesor),
                "$[*].resultados[?(@.id == " + n2Id + ")].id");
        assertThat(visibles).isEmpty();
    }

    @Test
    void crudClientes_rolesAlcanceYReglas() throws Exception {
        String admin = login("admin", "Admin12345");
        String asesor1 = login("asesor1", CLAVE_DEMO);
        String asesor3 = login("asesor3", CLAVE_DEMO);
        String nuevo = "{\"documento\":\"70707070\",\"nombres\":\"Cliente CRUD\",\"email\":\"crud@escall.pe\",\"direccion\":\"Av. Prueba 123\"}";

        // Crear: solo el administrador
        mvc.perform(post("/api/clientes").header("Authorization", bearer(asesor1)).contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isForbidden());
        String creado = mvc.perform(post("/api/clientes").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("70707070"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(creado, "$.id");
        mvc.perform(post("/api/clientes").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cliente con ese documento"));

        // Actualizar: un asesor que no tiene cartera con este cliente no lo ve (404); el administrador sí
        String cambio = "{\"nombres\":\"Cliente CRUD Editado\",\"email\":\"crud@escall.pe\",\"direccion\":\"Av. Nueva 456\"}";
        mvc.perform(put("/api/clientes/" + id).header("Authorization", bearer(asesor3)).contentType(MediaType.APPLICATION_JSON).content(cambio))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/clientes/" + id).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(cambio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Cliente CRUD Editado"));

        // Eliminar: no se permite si el cliente tiene cuentas (cliente de la cartera de asesor1)
        Integer conCuentas = JsonPath.read(getJson("/api/cartera", asesor1), "$[0].clienteId");
        mvc.perform(delete("/api/clientes/" + conCuentas).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        // El asesor sí puede corregir el contacto de un cliente de su cartera (conserva el nombre)
        String nombres = JsonPath.read(getJson("/api/clientes/" + conCuentas, asesor1), "$.nombres");
        mvc.perform(put("/api/clientes/" + conCuentas).header("Authorization", bearer(asesor1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"" + nombres + "\",\"email\":null,\"direccion\":\"Dirección actualizada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Dirección actualizada"));

        // Eliminar cliente sin cuentas: 204 y luego ya no existe
        mvc.perform(delete("/api/clientes/" + id).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id).header("Authorization", bearer(admin))).andExpect(status().isNotFound());
    }

    @Test
    void rf19_cargaSinArchivoResponde400NoError() throws Exception {
        String admin = login("admin", "Admin12345");
        mvc.perform(multipart("/api/admin/integracion/asignaciones").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("adjuntar el archivo")));
    }

    // ------------------------------------------------------------------ utilidades

    private String login(String usuario, String password) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String getJson(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Map<String, Integer> idsDeUsuarios(String token) throws Exception {
        String json = getJson("/api/usuarios", token);
        List<String> nombres = JsonPath.read(json, "$[*].usuario");
        List<Integer> ids = JsonPath.read(json, "$[*].id");
        java.util.HashMap<String, Integer> mapa = new java.util.HashMap<>();
        for (int i = 0; i < nombres.size(); i++) {
            mapa.put(nombres.get(i), ids.get(i));
        }
        return mapa;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static MockMultipartFile archivo(String nombre, String contenido) {
        return new MockMultipartFile("archivo", nombre, "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private static String gestion(Integer cuentaId, Integer n2, String fecha, String monto) {
        return """
                {"cuentaId":%d,"telefono":"987654321","tipificacionN2Id":%d,"observacion":"Cliente acepta pagar",
                 "fechaCompromiso":%s,"montoCompromiso":%s}
                """.formatted(cuentaId, n2, fecha == null ? "null" : "\"" + fecha + "\"", monto == null ? "null" : monto);
    }
}
