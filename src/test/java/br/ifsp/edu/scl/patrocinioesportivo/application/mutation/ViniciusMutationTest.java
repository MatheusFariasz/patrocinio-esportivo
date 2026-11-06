package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.security.config.JwtAuthenticationFilter;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.JwtService;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.usuario;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@Tag("UnitTest")
@Tag("Mutation")
public class ViniciusMutationTest {

    @Test
    void tokenDeOutroUsuarioDeveSerRecusado() {
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

    @Test
    void filtroDeveConservarIpESessaoNaAutenticacao() throws Exception {
        var jwt = mock(JwtService.class);
        var users = mock(UserDetailsService.class);
        var usuario = usuario(Role.USER);
        when(jwt.extractUsername("token-valido")).thenReturn(usuario.getEmail());
        when(users.loadUserByUsername(usuario.getEmail())).thenReturn(usuario);
        when(jwt.isTokenValid("token-valido", usuario)).thenReturn(true);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.setSession(new MockHttpSession(null, "sessao-teste"));
        request.addHeader("Authorization", "Bearer token-valido");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(jwt, users).doFilter(request, response, chain);

        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getPrincipal()).isInstanceOf(User.class);
        assertThat(autenticacao.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        var detalhes = (WebAuthenticationDetails) autenticacao.getDetails();
        assertThat(detalhes.getRemoteAddress()).isEqualTo("192.0.2.10");
        assertThat(detalhes.getSessionId()).isEqualTo("sessao-teste");
        verify(chain).doFilter(request, response);
    }
}
