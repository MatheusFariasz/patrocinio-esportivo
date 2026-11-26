package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.security.user.JdbcUserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;


@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcUserRepository")
class JdbcUserRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcUserRepository userRepository;

    @BeforeEach
    void setUp(){
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.userRepository = new JdbcUserRepository(jdbcTemplate);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Deve salvar usuário e recupera-lo por email preservando todos seus atributos e perfil")
    void
    shouldSaveAndFindUserByEmailForEachRole(Role role) {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Seu",
                "Ze", "seuze@ifsp.edu.br",
                "senhaBoa123", role);
        userRepository.save(user);

        var userfound = userRepository.findByEmail("seuze@ifsp.edu.br");
        assertThat(userfound).isPresent();
        assertThat(userfound.get().getId()).isEqualTo(id);
        assertThat(userfound.get().getName()).isEqualTo("Seu");
        assertThat(userfound.get().getLastname()).isEqualTo("Ze");
        assertThat(userfound.get().getEmail()).isEqualTo("seuze@ifsp.edu.br");
        assertThat(userfound.get().getPassword()).isEqualTo("senhaBoa123");
        assertThat(userfound.get().getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"naoexiste@ifsp.edu.br", "   ", "LUCAS@IFSP.EDU.BR"})
    @DisplayName("Deve retornar vazio ao buscar por email nao registrado, vazio ou nulo")
    void shouldReturnEmptyWhenEmailDoesNotExistOrIsInvalid(String emailBusca) {
        userRepository.save(new User(UUID.randomUUID(), "Lucas", "Santos", "lucas@ifsp.edu.br", "hash", Role.USER));
        var result = userRepository.findByEmail(emailBusca);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeita usuario com email duplicado e manter o registrado original inalterado")
    void shouldThrowDuplicateKeyExceptionWhenSavingDuplicateEmail(){
        UUID originalId = UUID.randomUUID();
        User original = new User(originalId, "Original", "User", "duplicado@ifsp.edu.br", "hash1", Role.USER);
        User duplicado = new User(UUID.randomUUID(), "Outro", "Nome", "duplicado@ifsp.edu.br", "hash2", Role.ADMIN);

        userRepository.save(original);

        assertThatThrownBy(() -> userRepository.save(duplicado))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("duplicado@ifsp.edu.br");

        var persisted = userRepository.findByEmail("duplicado@ifsp.edu.br").orElseThrow();
        assertThat(persisted.getId()).isEqualTo(originalId);
        assertThat(persisted.getName()).isEqualTo("Original");
        assertThat(persisted.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Deve rejeitar inserção de usuario com ID ja existente pela restrição de chave primária")
    void shouldRejectUserWithDuplicatedPrimaryKeyId(){
        UUID sameId = UUID.randomUUID();
        User first = new User(sameId, "Primeiro", "User", "primeiro@ifsp.edu.br",
                "hash1", Role.USER);
        User second = new User(sameId, "Segundo", "User", "segundo@ifsp.edu.br",
                "hash2", Role.USER);

        userRepository.save(first);

        assertThatThrownBy(()-> userRepository.save(second))
                .isInstanceOf(DataAccessException.class);
        assertThat(userRepository.findByEmail("segundo@ifsp.edu.br")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("userWithNullRequiredColumn")
    @DisplayName("Deve rejeitar usuário quando qualquer coluna not null do banco for nula")
    void shouldRejectUserWhenRequiredColumIsNull(User invalidUser){
        assertThatThrownBy(()-> userRepository.save(invalidUser))
                .isInstanceOf(DataAccessException.class);
    }

    static Stream<Arguments> userWithNullRequiredColumn() {
        UUID randomId = UUID.randomUUID();
        return Stream.of(
                Arguments.of(new User(randomId, null, "Santos", "a@ifsp.edu.br", "hash", Role.USER)),
                Arguments.of(new User(randomId, "Lucas", null, "b@ifsp.edu.br", "hash", Role.USER)),
                Arguments.of(new User(randomId, "Lucas", "Santos", null, "hash", Role.USER)),
                Arguments.of(new User(randomId, "Lucas", "Santos", "d@ifsp.edu.br", null, Role.USER))
        );
    }


    @Test
    @DisplayName("Deve rejeitr no banco a insercao de role fora do dominio USER e ADMIN")
    void shouldRejectInvalidRoleByDatabaseCheckConstraint(){
        assertThatThrownBy(()-> jdbcTemplate.update(
                "INSERT INTO app_user (id, name, lastname, email, password, role) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), "Lucas", "Santos", "invalid@ifsp.edu.br", "hash", "GUEST"
        )).isInstanceOf(DataAccessException.class);

        assertThat(userRepository.findByEmail("invalid@ifsp.edu.br")).isEmpty();
    }
}