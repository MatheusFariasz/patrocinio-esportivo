package br.ifsp.edu.scl.patrocinioesportivo.persistence;

import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcPatrocinadorRepository;
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
@DisplayName("Testes de persistência JdbcPatrocinadorRepository")
class JdbcPatrocinadorRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcPatrocinadorRepository patrocinadorRepository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create());
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data-demo.sql")
        ).execute(dataSource);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.patrocinadorRepository = new JdbcPatrocinadorRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o patrocinador existir no banco pelo ID")
    void shouldReturnTrueWhenPatrocinadorExistsById() {
        jdbcTemplate.update("INSERT INTO patrocinador (id, nome) VALUES (?, ?)", 50L, "Patrocinador Esporte Max");

        assertThat(patrocinadorRepository.existePorId(20L)).isTrue();
        assertThat(patrocinadorRepository.existePorId(50L)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {-1L, 0L, 999L})
    @DisplayName("Deve retornar falso quando o ID do patrocinador não existir no banco ou for nulo")
    void shouldReturnFalseWhenPatrocinadorDoesNotExistOrIdIsNull(Long id) {
        assertThat(patrocinadorRepository.existePorId(id)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar patrocinador com ID duplicado ou nome nulo pelas constraints da tabela")
    void shouldRejectDuplicateIdOrNullNameInPatrocinadorTable() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO patrocinador (id, nome) VALUES (?, ?)", 20L, "Duplicado"))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO patrocinador (id, nome) VALUES (?, ?)", 99L, null))
                .isInstanceOf(DataAccessException.class);

        assertThat(patrocinadorRepository.existePorId(99L)).isFalse();
    }
}

