package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.security.config.JwtService;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.usuario;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Tag("UnitTest")
@Tag("Mutation")
public class ViniciusMutationTest {

    @Test
    void TokenDeOutroUsuarioDeveSerRecusado() {
        var jwt = new JwtService();
        String chave = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwt, "secretKey", chave);
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 600_000L);
        var titular = usuario(Role.USER);
        var outro = new User(titular.getId(), "Outro", "Usuario", "outro@teste.com", "hash", Role.USER);
        String token = jwt.generateToken(titular);

        assertThat(jwt.isTokenValid(token, titular)).isTrue();
        assertThat(jwt.isTokenValid(token, outro)).isFalse();
    }
}
