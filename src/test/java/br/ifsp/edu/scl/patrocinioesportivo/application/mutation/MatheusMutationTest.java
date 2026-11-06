package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.controller.dto.EditarPropostaRequest;
import br.ifsp.edu.scl.patrocinioesportivo.controller.dto.HistoricoResponse;
import br.ifsp.edu.scl.patrocinioesportivo.controller.dto.RenovarContratoRequest;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ApiExceptionHandler;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ValorInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc.JdbcContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationInfoService;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.ApplicationConfig;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.OpenApiConfiguration;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.UserRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("UnitTest")
@Tag("Mutation")
class MatheusMutationTest {

    @Test
    void DuracaoZeroNaoDeveRenovarNemMudarParaEmRisco() {
        for (BigDecimal exposicao : List.of(BigDecimal.ZERO, valor("500"))) {
            var repository = mock(ContratoDePatrocinioRepository.class);
            var service = new RenovarContratoDePatrocinioService(repository);
            LocalDate termino = LocalDate.now().minusDays(1);
            var periodo = new PeriodoContratual(termino.minusMonths(3), termino);
            var meta = new MetaContratual(valor("500"));
            var parcela = ParcelaDePagamento.reconstituir(
                    1, valor("1000"), termino, true, termino);
            var contrato = ContratoDePatrocinio.reconstituir(
                    1L, StatusContrato.ATIVO, periodo, meta, valor("1000"),
                    10L, 20L, exposicao, BigDecimal.ZERO, List.of(parcela), List.of());
            when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

            assertThatThrownBy(() -> service.renovar(1L, 0, valor("800")))
                    .isInstanceOf(PeriodoInvalidoError.class)
                    .hasMessage("A duração do novo período deve ser maior que zero.");

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
            assertThat(contrato.getPeriodoContratual()).isSameAs(periodo);
            assertThat(contrato.getMetaContratual()).isSameAs(meta);
            assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(exposicao);
            assertThat(contrato.getValorTotal()).isEqualByComparingTo("1000");
            assertThat(contrato.getMultaRescisoria()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(contrato.getParcelas()).containsExactly(parcela);
            assertThat(parcela.isPaga()).isTrue();
            assertThat(parcela.getDataPagamento()).isEqualTo(termino);
            assertThat(contrato.getHistorico()).isEmpty();
            verify(repository, never()).salvar(any());
        }
    }

    @Test
    void DeveGravarDoisCiclosDistintosNoHistorico() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        var atual = contrato(StatusContrato.ATIVO);
        var comDoisHistoricos = ContratoDePatrocinio.reconstituir(7L, atual.getStatus(),
                atual.getPeriodoContratual(), atual.getMetaContratual(), atual.getValorTotal(),
                10L, 20L, atual.getExposicaoAcumulada(), BigDecimal.ZERO,
                atual.getParcelas(), List.of(historico(0), historico(1)));

        assertThat(new JdbcContratoDePatrocinioRepository(jdbc).salvar(comDoisHistoricos))
                .isSameAs(comDoisHistoricos);

        var periodos = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(2)).update(contains("INSERT INTO historico_periodo"), periodos.capture());
        assertThat(periodos.getAllValues()).extracting(args -> args[1]).containsExactly(1, 2);
        assertThat(periodos.getAllValues()).extracting(args -> args[2])
                .containsExactly(historico(0).periodo().inicio().toString(),
                        historico(1).periodo().inicio().toString());

        var parcelas = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, times(3)).update(contains("INSERT INTO parcela_pagamento"), parcelas.capture());
        assertThat(parcelas.getAllValues()).extracting(args -> args[1]).containsExactly(0, 1, 2);
        assertThat(parcelas.getAllValues()).extracting(args -> args[2]).containsExactly(7, 1, 2);
    }

    @Test
    void EdicaoDeveRetornarOsDadosDaProposta() {
        var consultar = mock(ConsultarContratoService.class);
        var editar = mock(EditarPropostaService.class);
        var proposta = contrato(StatusContrato.PENDENTE);
        when(consultar.consultar(7L)).thenReturn(proposta);
        var controller = propostas(consultar, editar, mock(AprovarPropostaService.class),
                mock(CancelarPropostaService.class), mock(AuthenticationInfoService.class));
        var request = new EditarPropostaRequest(proposta.getValorTotal(),
                proposta.getPeriodoContratual().inicio(), proposta.getPeriodoContratual().termino(),
                proposta.getMetaContratual().valor());

        var resposta = controller.editar(7L, request);

        assertThat(resposta).isNotNull();
        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.status()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(resposta.valorTotal()).isEqualByComparingTo(request.valor());
        assertThat(resposta.inicio()).isEqualTo(request.inicio());
        assertThat(resposta.termino()).isEqualTo(request.termino());
        assertThat(resposta.meta()).isEqualByComparingTo(request.meta());
        verify(editar).editar(7L, request.valor(), request.inicio(), request.termino(), request.meta());
    }

    @Test
    void RenovacaoDeveRetornarContratoParcelasEHistorico() {
        var consultar = mock(ConsultarContratoService.class);
        var renovar = mock(RenovarContratoDePatrocinioService.class);
        var renovado = contrato(StatusContrato.ATIVO);
        when(consultar.consultar(7L)).thenReturn(renovado);
        var controller = contratos(consultar, mock(RegistrarPagamentoService.class),
                mock(EncerrarContratoService.class), renovar);

        var resposta = controller.renovar(7L, new RenovarContratoRequest(3, valor("500")));

        assertThat(resposta).isNotNull();
        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.status()).isEqualTo(StatusContrato.ATIVO);
        assertThat(resposta.inicio()).isEqualTo(renovado.getPeriodoContratual().inicio());
        assertThat(resposta.termino()).isEqualTo(renovado.getPeriodoContratual().termino());
        assertThat(resposta.meta()).isEqualByComparingTo("500");
        assertThat(resposta.parcelas()).hasSize(1);
        assertThat(resposta.parcelas().getFirst().numero()).isEqualTo(7);
        assertThat(resposta.historico()).hasSize(1);
        assertThat(resposta.historico().getFirst().inicio()).isEqualTo(historico(0).periodo().inicio());
        verify(renovar).renovar(7L, 3, valor("500"));
    }

    @Test
    void DadoInvalidoDeveRetornarCorpoComErro400() {
        var resposta = new ApiExceptionHandler().invalid(new ValorInvalidoError("valor inválido"));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message()).isEqualTo("valor inválido");
        assertThat(resposta.getBody().status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().developerMessage()).isEqualTo("ValorInvalidoError");
    }

    private static void entrada(Object request) {}

    @Test
    void ValidacaoDeveInformarNomeEMensagemDeCadaCampo() throws Exception {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "valor", "deve ser positivo"));
        binding.addError(new FieldError("request", "meta", "deve ser positiva"));
        var parametro = new MethodParameter(
                MatheusMutationTest.class.getDeclaredMethod("entrada", Object.class), 0);
        var erro = new MethodArgumentNotValidException(parametro, binding);

        var resposta = new ApiExceptionHandler().validation(erro);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().message())
                .isEqualTo("meta: deve ser positiva; valor: deve ser positivo");
    }

    @Test
    void DeveRetornarOAuthenticationManagerConfigurado() throws Exception {
        var origem = mock(AuthenticationConfiguration.class);
        var manager = mock(AuthenticationManager.class);
        when(origem.getAuthenticationManager()).thenReturn(manager);
        var config = new ApplicationConfig(mock(UserRepository.class));
        assertThat(config.authenticationManager(origem)).isSameAs(manager);
    }

    @Test
    void DeveBuscarUsuarioPeloEmail() {
        var repository = mock(UserRepository.class);
        var usuario = usuario(Role.USER);
        when(repository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        var service = new ApplicationConfig(repository).userDetailsService();
        assertThat(service).isNotNull();
        assertThat(service.loadUserByUsername(usuario.getEmail())).isSameAs(usuario);
        verify(repository).findByEmail(usuario.getEmail());
    }

    @Test
    void UsuarioAusenteDeveGerarAExcecaoCorreta() {
        var repository = mock(UserRepository.class);
        when(repository.findByEmail("ausente@teste.com")).thenReturn(Optional.empty());
        var service = new ApplicationConfig(repository).userDetailsService();
        assertThat(service).isNotNull();
        assertThatThrownBy(() -> service.loadUserByUsername("ausente@teste.com"))
                .isInstanceOf(UsernameNotFoundException.class).hasMessage("User not found!");
    }

    @Test
    void SwaggerDeveTerInformacoesEAutenticacaoBearer() {
        var api = new OpenApiConfiguration().patrocinioOpenApi();
        assertThat(api).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Patrocínio esportivo");
        assertThat(api.getInfo().getVersion()).isEqualTo("v1");
        var scheme = api.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void RespostaDoHistoricoDevePreservarTodosOsDados() {
        var original = historico(1);
        var resposta = HistoricoResponse.de(original);
        assertThat(resposta).isNotNull();
        assertThat(resposta.inicio()).isEqualTo(original.periodo().inicio());
        assertThat(resposta.termino()).isEqualTo(original.periodo().termino());
        assertThat(resposta.meta()).isEqualByComparingTo(original.meta().valor());
        assertThat(resposta.exposicaoAcumulada()).isEqualByComparingTo(original.exposicaoAcumulada());
        assertThat(resposta.parcelas()).hasSize(1);
        var parcela = resposta.parcelas().getFirst();
        var esperada = original.parcelas().getFirst();
        assertThat(parcela.numero()).isEqualTo(esperada.getNumero());
        assertThat(parcela.valor()).isEqualByComparingTo(esperada.getValor());
        assertThat(parcela.vencimento()).isEqualTo(esperada.getVencimento());
        assertThat(parcela.paga()).isTrue();
        assertThat(parcela.dataPagamento()).isEqualTo(esperada.getDataPagamento());
    }
}
