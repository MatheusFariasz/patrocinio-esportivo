package br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc;

import br.ifsp.edu.scl.patrocinioesportivo.repository.ClubeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClubeRepository implements ClubeRepository {
    private final JdbcTemplate jdbc;

    public JdbcClubeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existePorId(Long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM clube WHERE id = ?)", Boolean.class, id));
    }
}
