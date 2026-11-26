package br.ifsp.edu.scl.patrocinioesportivo.support;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthRequest;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationService;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
public abstract class BaseApiIntegrationTest {
    private static final String DATABASE_URL = SqliteTestDatabase.create();
    private static final String DEFAULT_PASSWORD = "Senha123!";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected AuthenticationService authenticationService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ContratoDePatrocinioRepository contratoRepository;

    protected RequestSpecification givenAnonymous() {
        return RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    protected RequestSpecification givenAuth(String token) {
        return givenAnonymous()
                .header("Authorization", "Bearer " + token);
    }

    protected String authenticateAs(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@teste.com";
        userRepository.save(new User(
                UUID.randomUUID(),
                "Usuario",
                role.name(),
                email,
                passwordEncoder.encode(DEFAULT_PASSWORD),
                role
        ));
        return authenticationService.authenticate(new AuthRequest(email, DEFAULT_PASSWORD)).token();
    }

    protected long criarPropostaPendente(String token) {
        return criarPropostaPendente(token, new BigDecimal("1000.01"), 2, new BigDecimal("100.00"));
    }

    protected long criarPropostaPendente(String token, BigDecimal valor, int duracaoMeses, BigDecimal meta) {
        LocalDate inicio = LocalDate.now();
        LocalDate termino = inicio.plusMonths(duracaoMeses);
        Map<String, Object> body = Map.of(
                "clubeId", ContratoTestFixtures.CLUBE_DEMO_ID,
                "patrocinadorId", ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                "valor", valor,
                "inicio", inicio.toString(),
                "termino", termino.toString(),
                "meta", meta
        );

        Response response = givenAuth(token)
                .body(body)
                .post("/api/v1/propostas");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        return response.jsonPath().getLong("id");
    }

    protected long criarContratoAtivo(String adminToken) {
        long propostaId = criarPropostaPendente(adminToken);
        Response response = givenAuth(adminToken)
                .post("/api/v1/propostas/{id}/aprovacao", propostaId);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return propostaId;
    }

    protected long salvarContratoReconstituido(
            StatusContrato status,
            LocalDate inicio,
            LocalDate termino,
            BigDecimal meta,
            BigDecimal valorTotal,
            BigDecimal exposicaoAcumulada,
            List<ParcelaDePagamento> parcelas,
            List<HistoricoDePeriodo> historico
    ) {
        var contrato = ContratoDePatrocinio.reconstituir(
                null,
                status,
                new PeriodoContratual(inicio, termino),
                new MetaContratual(meta),
                valorTotal,
                ContratoTestFixtures.CLUBE_DEMO_ID,
                ContratoTestFixtures.PATROCINADOR_DEMO_ID,
                exposicaoAcumulada,
                BigDecimal.ZERO,
                parcelas,
                historico
        );
        return contratoRepository.salvar(contrato).getId();
    }

    protected void assertApiError(
            SoftAssertions softly,
            Response response,
            HttpStatus expectedStatus,
            String expectedDeveloperMessage,
            String expectedMessageContains
    ) {
        softly.assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        var json = response.jsonPath();
        softly.assertThat(json.getString("status")).isEqualTo(expectedStatus.name());
        softly.assertThat(json.getString("developerMessage")).isEqualTo(expectedDeveloperMessage);
        softly.assertThat(json.getString("message")).contains(expectedMessageContains);
        softly.assertThat(json.getString("timestamp")).isNotBlank();
    }
}
