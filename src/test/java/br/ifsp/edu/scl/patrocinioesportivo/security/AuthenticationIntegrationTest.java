package br.ifsp.edu.scl.patrocinioesportivo.security;

import br.ifsp.edu.scl.patrocinioesportivo.controller.TransactionController;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ApiExceptionHandler;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("IntegrationTest")
@DisplayName("Autenticação com JWT e persistência SQLite JDBC")
@SpringBootTest(classes = AuthenticationIntegrationTest.AuthConfiguration.class)
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {
    private static final String DATABASE_URL = temporaryDatabaseUrl();

    @Configuration
    @EnableAutoConfiguration
    @Import({ApplicationConfig.class, SecurityConfiguration.class, JwtAuthenticationFilter.class,
            JwtService.class, AuthenticationService.class, AuthenticationInfoService.class,
            UserController.class, TransactionController.class, ApiExceptionHandler.class,
            JdbcUserRepository.class})
    static class AuthConfiguration {
    }

    private static String temporaryDatabaseUrl() {
        try {
            var file = Files.createTempFile("patrocinio-auth-", ".db");
            file.toFile().deleteOnExit();
            return "jdbc:sqlite:" + file.toAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o banco temporário", e);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    private String register(String email) throws Exception {
        var request = new RegisterUserRequest("Clube", "Teste", email, "Senha123!");
        var response = mvc.perform(post("/api/v1/register").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    @Test
    @DisplayName("Cadastrar, autenticar e acessar rota protegida usando o usuário persistido")
    void registerAndAuthenticate() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        String id = register(email);

        var independentDataSource = new DriverManagerDataSource(DATABASE_URL);
        var persisted = new JdbcUserRepository(new JdbcTemplate(independentDataSource))
                .findByEmail(email).orElseThrow();
        assertThat(persisted.getId()).isEqualTo(UUID.fromString(id));
        assertThat(persisted.getName()).isEqualTo("Clube");
        assertThat(persisted.getLastname()).isEqualTo("Teste");
        assertThat(persisted.getRole()).isEqualTo(Role.USER);
        assertThat(persisted.getPassword()).isNotEqualTo("Senha123!");
        assertThat(passwords.matches("Senha123!", persisted.getPassword())).isTrue();

        var response = mvc.perform(post("/api/v1/authenticate").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new AuthRequest(email, "Senha123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();
        mvc.perform(get("/api/v1/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("Hello: " + id));

        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(independentDataSource);
        assertThat(users.findByEmail(email)).isPresent();
    }

    @Test
    @DisplayName("Rejeitar cadastro duplicado com HTTP 409 e manter um único usuário")
    void duplicateEmail() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        String id = register(email);
        mvc.perform(post("/api/v1/register").contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterUserRequest("Outro", "Nome", email, "Senha123!"))))
                .andExpect(status().isConflict());
        assertThat(users.findByEmail(email).orElseThrow().getId()).isEqualTo(UUID.fromString(id));
    }

    @Test
    @DisplayName("O banco rejeita email duplicado mesmo sem a consulta prévia do serviço")
    void uniqueEmailInDatabase() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        register(email);
        assertThatThrownBy(() -> users.save(new User(UUID.randomUUID(), "Outro", "Nome", email,
                passwords.encode("OutraSenha"), Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Rejeitar senha incorreta e usuário inexistente com HTTP 401")
    void invalidCredentials() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        register(email);
        for (var request : new AuthRequest[]{new AuthRequest(email, "errada"),
                new AuthRequest("inexistente@teste.com", "Senha123!")}) {
            mvc.perform(post("/api/v1/authenticate").contentType(APPLICATION_JSON)
                            .content(json.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Rejeitar acesso sem token ou com JWT inválido")
    void protectedRoute() throws Exception {
        mvc.perform(get("/api/v1/hello")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/hello").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }
}
