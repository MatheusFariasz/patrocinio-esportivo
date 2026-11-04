package br.ifsp.edu.scl.patrocinioesportivo.security.user;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, name, lastname, email, password, role FROM app_user WHERE email = ?",
                (rs, rowNum) -> new User(UUID.fromString(rs.getString("id")),
                        rs.getString("name"), rs.getString("lastname"), rs.getString("email"),
                        rs.getString("password"), Role.valueOf(rs.getString("role"))),
                email).stream().findFirst();
    }

    @Override
    public void save(User user) {
        int inserted = jdbcTemplate.update(
                "INSERT INTO app_user (id, name, lastname, email, password, role) VALUES (?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(email) DO NOTHING",
                user.getId().toString(), user.getName(), user.getLastname(), user.getEmail(),
                user.getPassword(), user.getRole().name());
        if (inserted == 0) {
            throw new DuplicateKeyException("Email already registered: " + user.getEmail());
        }
    }
}
