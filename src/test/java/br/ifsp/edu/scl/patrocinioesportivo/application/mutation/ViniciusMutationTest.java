package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.EntityAlreadyExistsException;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationService;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.RegisterUserRequest;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.ApplicationConfig;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.JwtAuthenticationFilter;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.JwtService;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarExposicaoService;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarPagamentoService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.util.ReflectionTestUtils;
import pitest.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.usuario;
import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.valor;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @Test
    void jwtInvalidoDeveRetornarErro() throws Exception {
        var jwt = mock(JwtService.class);
        when(jwt.extractUsername("invalido")).thenThrow(new JwtException("token inválido"));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalido");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(jwt, mock(UserDetailsService.class)).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        var json = new ObjectMapper().readTree(response.getContentAsString());
        assertThat(json.get("error").asText()).isEqualTo("JWT is not valid or is expired.");
        verifyNoInteractions(chain);
    }

    @Test
    void encoderDeveCodificarEConferirASenha() {
        var encoder = new ApplicationConfig(mock(UserRepository.class)).passwordEncoder();
        assertThat(encoder).isNotNull();
        String hash = encoder.encode("Senha123!");
        assertThat(hash).isNotEqualTo("Senha123!");
        assertThat(encoder.matches("Senha123!", hash)).isTrue();
        assertThat(encoder.matches("incorreta", hash)).isFalse();
    }

    @Test
    void cadastroDuplicadoNaoDeveCodificarNemSalvar() {
        var repository = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var existente = usuario(Role.USER);
        when(repository.findByEmail(existente.getEmail())).thenReturn(Optional.of(existente));
        var service = new AuthenticationService(repository, encoder, mock(JwtService.class),
                mock(AuthenticationManager.class));
        var request = new RegisterUserRequest("Ana", "Silva", existente.getEmail(), "Senha123!");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Email already registered: " + existente.getEmail());

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void pagamentoEmContratoAusenteDeveInformarOErroCorreto() {
        var repository = mock(ContratoDePatrocinioRepository.class);
        when(repository.buscarPorId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistrarPagamentoService(repository).registrar(7L, 1))
                .isInstanceOf(ContratoInexistenteError.class).hasMessage("Contrato não encontrado.");

        verify(repository, never()).salvar(any());
    }

    @Test
    void exposicaoEmContratoAusenteDeveInformarOErroCorreto() {
        var repository = mock(ContratoDePatrocinioRepository.class);
        when(repository.buscarPorId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistrarExposicaoService(repository).registrar(7L, valor("10")))
                .isInstanceOf(ContratoInexistenteError.class).hasMessage("Contrato não encontrado.");

        verify(repository, never()).salvar(any());
    }
}
