package com.escall.crm;

import com.escall.crm.repositorio.ClienteRepository;
import com.escall.crm.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-test",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secreto=clave-de-firma-exclusiva-de-pruebas",
        "app.admin.password=ClaveInicialSoloParaPruebas",
        "app.datos-demo=true",
        "app.cors.origenes=https://crm.example.test"
})
@ActiveProfiles("prod")
class ProduccionTests {

    @Autowired private WebApplicationContext contexto;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;

    @Test
    void arranqueSinCarteraNiUsuariosDeDemostracion() {
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(clientes.count()).isZero();
    }

    @Test
    void readinessDisponibleYConsolaInaccesible() throws Exception {
        mvc().perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mvc().perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        mvc().perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPermiteSoloElOrigenConfigurado() throws Exception {
        mvc().perform(options("/api/clientes")
                        .header("Origin", "https://crm.example.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://crm.example.test"));
        mvc().perform(options("/api/clientes")
                        .header("Origin", "https://otro.example.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }
}
