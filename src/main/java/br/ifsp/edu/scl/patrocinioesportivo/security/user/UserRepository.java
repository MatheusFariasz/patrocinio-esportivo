package br.ifsp.edu.scl.patrocinioesportivo.security.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    void save(User user);
}
