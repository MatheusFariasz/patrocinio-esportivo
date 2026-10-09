package br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc;

import br.ifsp.edu.scl.patrocinioesportivo.repository.PatrocinadorRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPatrocinadorRepository implements PatrocinadorRepository {
    private final JdbcTemplate jdbc;

    public JdbcPatrocinadorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existePorId(Long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM patrocinador WHERE id = ?)", Boolean.class, id));
    }
}
