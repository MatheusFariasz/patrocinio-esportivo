package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ValorInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.EditarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("TDD")
class EditarPropostaServiceTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private EditarPropostaService service;

    static Stream<Arguments> dadosInvalidos() {
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate termino = inicio.plusMonths(3);
        BigDecimal valor = new BigDecimal("2000.00");
        BigDecimal meta = new BigDecimal("800");
        return Stream.of(
                Arguments.of(null, inicio, termino, meta, ValorInvalidoError.class),
                Arguments.of(BigDecimal.ZERO, inicio, termino, meta, ValorInvalidoError.class),
                Arguments.of(new BigDecimal("-1"), inicio, termino, meta, ValorInvalidoError.class),
                Arguments.of(valor, null, termino, meta, PeriodoInvalidoError.class),
                Arguments.of(valor, inicio, null, meta, PeriodoInvalidoError.class),
                Arguments.of(valor, inicio, inicio, meta, PeriodoInvalidoError.class),
                Arguments.of(valor, inicio, inicio.minusDays(1), meta, PeriodoInvalidoError.class),
                Arguments.of(valor, inicio.minusMonths(6), inicio.minusDays(2), meta, PeriodoInvalidoError.class),
                Arguments.of(valor, inicio, termino, null, MetaObrigatoriaError.class),
                Arguments.of(valor, inicio, termino, BigDecimal.ZERO, MetaInvalidaError.class),
                Arguments.of(valor, inicio, termino, new BigDecimal("-1"), MetaInvalidaError.class)
        );
    }

    private void verificarStatusInvalido(StatusContrato status) {
        ContratoDePatrocinio proposta = proposta(status);
        PeriodoContratual periodo = proposta.getPeriodoContratual();
        MetaContratual meta = proposta.getMetaContratual();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(proposta));

        assertThatThrownBy(() -> service.editar(1L, new BigDecimal("2000.00"),
                LocalDate.now().plusDays(2), LocalDate.now().plusMonths(6), new BigDecimal("800")))
                .isInstanceOf(TransicaoDeStatusInvalidaError.class)
                .hasMessageContaining("avaliadas");

        assertThat(proposta.getStatus()).isEqualTo(status);
        assertThat(proposta.getPeriodoContratual()).isSameAs(periodo);
        assertThat(proposta.getMetaContratual()).isSameAs(meta);
        assertThat(proposta.getValorTotal()).isEqualByComparingTo("1000.00");
        verify(repository, never()).salvar(proposta);
    }

    private ContratoDePatrocinio proposta(StatusContrato status) {
        LocalDate inicio = LocalDate.now().plusDays(1);
        return new ContratoDePatrocinio(status, new PeriodoContratual(inicio, inicio.plusMonths(3)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("#43 - deve editar os dados da proposta e manter status pendente")
    void deveEditarPropostaPendente() {
        ContratoDePatrocinio proposta = proposta(StatusContrato.PENDENTE);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(proposta));
        LocalDate inicio = LocalDate.now().plusDays(2);
        LocalDate termino = inicio.plusMonths(6);

        service.editar(1L, new BigDecimal("2000.00"), inicio, termino, new BigDecimal("800"));

        assertThat(proposta.getValorTotal()).isEqualByComparingTo("2000.00");
        assertThat(proposta.getPeriodoContratual()).isEqualTo(new PeriodoContratual(inicio, termino));
        assertThat(proposta.getMetaContratual()).isEqualTo(new MetaContratual(new BigDecimal("800")));
        assertThat(proposta.getStatus()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(proposta.getParcelas()).isEmpty();
        verify(repository).salvar(proposta);
    }

    @Test
    @DisplayName("#7 e #24 - deve exigir identificação ao editar")
    void deveExigirIdentificacaoAoEditar() {
        assertThatThrownBy(() -> service.editar(null, new BigDecimal("2000.00"),
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), new BigDecimal("800")))
                .isInstanceOf(IdentificacaoObrigatoriaError.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO", "ENCERRADO", "RECUSADO"})
    @DisplayName("#44 - não deve editar proposta que já foi avaliada")
    void naoDeveEditarPropostaAvaliada(StatusContrato status) {
        verificarStatusInvalido(status);
    }

    @Test
    @DisplayName("#45 - não deve editar proposta cancelada")
    void naoDeveEditarPropostaCancelada() {
        verificarStatusInvalido(StatusContrato.CANCELADO);
    }

    @Test
    @DisplayName("#46 - deve informar proposta inexistente ao editar")
    void deveInformarPropostaInexistente() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(1L, new BigDecimal("2000.00"),
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), new BigDecimal("800")))
                .isInstanceOf(ContratoInexistenteError.class);

        verify(repository, never()).salvar(org.mockito.ArgumentMatchers.any());
    }
}
