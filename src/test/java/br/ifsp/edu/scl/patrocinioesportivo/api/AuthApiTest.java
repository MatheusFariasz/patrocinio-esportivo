package br.ifsp.edu.scl.patrocinioesportivo.api;

import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthRequest;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.RegisterUserRequest;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@Tag("ApiTest")
@Tag("IntegrationTest")
@DisplayName("Testes de integração da API de Autenticação e Usuario")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")


public class AuthApiTest {
    private static final String DATABASE_URL = SqliteTestDatabase.create();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", ()-> DATABASE_URL);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deve cadastrar usuário válido retornando 201 com ID no payload e persistir no banco")
    void shouldRegisterUserAndReturn201WithGeneratedId() throws Exception{
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var request = new RegisterUserRequest("Lucas", "Santos", email, "SenhaForte123!");
        String responseBody = mvc.perform(post("/api/v1/register")
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID returnedId = UUID.fromString(json.readTree(responseBody).get("id").asText());
        var persistedUser = userRepository.findByEmail(email);

        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getId()).isEqualTo(returnedId);
        assertThat(persistedUser.get().getName()).isEqualTo("Lucas");
        assertThat(persistedUser.get().getLastname()).isEqualTo("Santos");
        assertThat(persistedUser.get().getRole()).isEqualTo(Role.USER);
        assertThat(persistedUser.get().getPassword()).isNotEqualTo("SenhaForte123!");
        assertThat(passwordEncoder.matches("SenhaForte123!", persistedUser.get().getPassword())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterRequests")
    @DisplayName("Deve retornar 400 e payload de erro quando dados de cadastro forem inválidos")
    void shouldReturn400WhenRegisterPayloadIsInvalid(RegisterUserRequest invalidRequest) throws Exception{
        mvc.perform(post("/api/v1/register")
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.developerMessage").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    static Stream<Arguments> invalidRegisterRequests() {
        return Stream.of(
                Arguments.of(new RegisterUserRequest(null, "Santos", "valido@ifsp.edu.br", "Senha123!")),
                Arguments.of(new RegisterUserRequest(" ", "Santos", "valido@ifsp.edu.br", "Senha123!")),
                Arguments.of(new RegisterUserRequest("Lucas", "", "valido@ifsp.edu.br", "Senha123!")),
                Arguments.of(new RegisterUserRequest("Lucas", "Santos", "", "Senha123!")),
                Arguments.of(new RegisterUserRequest("Lucas", "Santos", "email-nao-validor", "Senha123!")),
                Arguments.of(new RegisterUserRequest("Lucas", "Santos", "valido@ifsp.edu.br", ""))
        );
    }

    @Test
    @DisplayName("Deve retornar 409 e manter usuario original ao tentar cadastrar email duplicado")
    void shouldReturn409WhenRegisteringDuplicateEmail() throws Exception{
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var firstRequest = new RegisterUserRequest("Original", "User", email, "Senha123!");
        var duplicateRequest = new RegisterUserRequest("Impostor", "Outro", email, "OutraSenha99!");

        mvc.perform(post("/api/v1/register")
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(firstRequest)
                ))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/register")
                .contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(duplicateRequest)
                ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.developerMessage").value("EntityAlreadyExistsException"))
                .andExpect(jsonPath("$.message").value("Email already registered: " + email));

        var persisted = userRepository.findByEmail(email).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Original");
        assertThat(persisted.getLastname()).isEqualTo("User");
    }

    @Test
    @DisplayName("Deve autenticar usuário com credenciais validas retornando 200 e token JWT no payload")
    void shouldAuthenticateValidUserAndReturn200WithJwtToken() throws Exception {
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var registerRequest = new RegisterUserRequest("Lucas", "Santos", email, "SenhaForte123!");

        mvc.perform(post("/api/v1/register")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(registerRequest)
                        ))
                        .andExpect(status().isCreated());

        var authRequest = new AuthRequest(email, "SenhaForte123!");

        String responseBody = mvc.perform(post("/api/v1/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(authRequest)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = json.readTree(responseBody).get("token").asText();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @ParameterizedTest
    @MethodSource("invalidAuthRequests")
    @DisplayName("Deve retornar 400 e payload de erro quando dados de autenticação forem invalidos")
    void shouldReturn400WhenAuthenticatePayloadIsInvalid(AuthRequest invalidRequest) throws Exception {
        mvc.perform(post("/api/v1/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(invalidRequest)
                        ))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                        .andExpect(jsonPath("$.developerMessage").value("MethodArgumentNotValidException"))
                        .andExpect(jsonPath("$.message").isNotEmpty());
    }

    static Stream<Arguments> invalidAuthRequests() {
        return Stream.of(
                Arguments.of(new AuthRequest(null, "Senha123!")),
                Arguments.of(new AuthRequest(" ", "Senha123!")),
                Arguments.of(new AuthRequest("formato-nao-valido", "Senha123!")),
                Arguments.of(new AuthRequest("valido@ifsp.edu.br", null)),
                Arguments.of(new AuthRequest("valido@ifsp.edu.br", ""))
        );
    }

    @Test
    @DisplayName("Deve retornar 401 e um payload de erro ao autenticar com senha incorret ou email nao existente")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var registerRequest = new RegisterUserRequest("Lucas", "Santos", email, "SenhaCorreta123!");

        mvc.perform(post("/api/v1/register")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(registerRequest)))
                        .andExpect(status().isCreated());

        var wrongPasswordRequest = new AuthRequest(email, "SenhaErrada999!");
        mvc.perform(post("/api/v1/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(wrongPasswordRequest)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                        .andExpect(jsonPath("$.message").value("Credenciais inválidas."))
                        .andExpect(jsonPath("$.developerMessage").isNotEmpty());

        var unknownEmailRequest = new AuthRequest("naoexiste-" + UUID.randomUUID() + "@ifsp.edu.br", "SenhaCorreta123!");
        mvc.perform(post("/api/v1/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(unknownEmailRequest)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                        .andExpect(jsonPath("$.message").value("Credenciais inválidas."))
                        .andExpect(jsonPath("$.developerMessage").isNotEmpty());
    }

    @Test
    @DisplayName("Deve retornar 200 e saudaçao com ID do usuario ao acessar hello com token valid")
    void shouldReturn200WithGreetingAndUserIdWhenAuthenticated() throws Exception {
        String email = UUID.randomUUID() + "@ifsp.edu.br";
        var registerRequest = new RegisterUserRequest("Lucas", "Santos", email, "SenhaForte123!");

        String registerResponse = mvc.perform(post("/api/v1/register")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(registerRequest)
                        ))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String userId = json.readTree(registerResponse).get("id").asText();
        String authResponse = mvc.perform(post("/api/v1/authenticate")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(new AuthRequest(email, "SenhaForte123!"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = json.readTree(authResponse).get("token").asText();

        mvc.perform(get("/api/v1/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello: " + userId));
    }

    @Test
    @DisplayName("Deve retornar 401 ao acessar hello sem token, com prefixo invalido ou com JWT invalido")
    void shouldReturn401OnHelloWithoutTokenOrWithInvalidToken() throws Exception {
        mvc.perform(get("/api/v1/hello"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/hello")
                        .header("Authorization", "Basic credencial-invalida"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/hello")
                        .header("Authorization", "Bearer token.jwt.invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("JWT is not valid or is expired."));
    }
}
