package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcClubeRepository;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("PersistenceTest")
@Tag("IntegrationTest")
@DisplayName("Testes de persistência JdbcClubeRepository")
class JdbcClubeRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcClubeRepository clubeRepository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data-demo.sql")
        ).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.clubeRepository = new JdbcClubeRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quand o clube existir no banco por ID")
    void shouldReturnTrueWhenClubeExistsById() {
        jdbcTemplate.update("INSERT INTO clube (id, nome) VALUES (?, ?)", 30L, "Clube Atletico Sao Carlos");

        assertThat(clubeRepository.existePorId(30L)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 999L})
    @DisplayName("Deve retornar falso quando o ID do clube nao existir no banco ou for nulo")
    void shouldReturnFalseWhenClubeDoesNotExistOrIdIsNull(Long id) {
        assertThat(clubeRepository.existePorId(id)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeita clube com ID duplicado ou nome nulo pelas constraint da tabela")
    void shouldRejectDuplicateIdOrNullNameInClubeTable() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO clube (id, nome) VALUES (?, ?)", 10L, "Clube Duplicado"))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO clube (id, nome) VALUES (?, ?)", 99L, null))
                .isInstanceOf(DataAccessException.class);

        assertThat(clubeRepository.existePorId(99L)).isFalse();
    }
}
