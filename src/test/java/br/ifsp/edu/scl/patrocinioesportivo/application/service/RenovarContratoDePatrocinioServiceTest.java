package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.GeradorDeParcelas;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RenovarContratoDePatrocinioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("TDD")
class RenovarContratoDePatrocinioServiceTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private RenovarContratoDePatrocinioService service;

    private void verificarPendenciaFinanceira(ContratoDePatrocinio contrato) {
        PeriodoContratual periodo = contrato.getPeriodoContratual();
        MetaContratual meta = contrato.getMetaContratual();
        BigDecimal exposicao = contrato.getExposicaoAcumulada();
        var parcelas = java.util.List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.renovar(1L, 3, new BigDecimal("800")))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("PendenciaFinanceiraError"));

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getPeriodoContratual()).isSameAs(periodo);
        assertThat(contrato.getMetaContratual()).isSameAs(meta);
        assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(exposicao);
        assertThat(contrato.getParcelas()).containsExactlyElementsOf(parcelas);
        assertThat(contrato.getHistorico()).isEmpty();
        verify(repository, never()).salvar(contrato);
    }

    private void verificarMetaNaoAtingida(ContratoDePatrocinio contrato) {
        PeriodoContratual periodo = contrato.getPeriodoContratual();
        MetaContratual meta = contrato.getMetaContratual();
        var parcelas = java.util.List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.renovar(1L, 3, new BigDecimal("800"));

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.EM_RISCO);
        assertThat(contrato.getPeriodoContratual()).isSameAs(periodo);
        assertThat(contrato.getMetaContratual()).isSameAs(meta);
        assertThat(contrato.getParcelas()).containsExactlyElementsOf(parcelas);
        assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(contrato.getHistorico()).isEmpty();
        verify(repository).salvar(contrato);
    }

    private ContratoDePatrocinio contrato(StatusContrato status, boolean pagas, boolean metaAtingida) {
        LocalDate termino = LocalDate.now().minusDays(1);
        var parcelas = new java.util.ArrayList<ParcelaDePagamento>();
        for (int numero = 1; numero <= 2; numero++) {
            ParcelaDePagamento parcela = new ParcelaDePagamento(numero, new BigDecimal("500.00"), termino);
            if (pagas) { parcela.registrarPagamento(); }
            parcelas.add(parcela);
        }
        ContratoDePatrocinio contrato = ContratoDePatrocinio.reconstituir(
                status, new PeriodoContratual(termino.minusMonths(3), termino),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000.00"), parcelas);
        if (metaAtingida && (status == StatusContrato.ATIVO || status == StatusContrato.EM_RISCO)) {
            contrato.registrarExposicao(new BigDecimal("500"));
        }
        return contrato;
    }

    @Test
    @DisplayName("#38 - deve renovar contrato ativo e preservar o histórico anterior")
    void deveRenovarContratoAtivoComHistorico() {
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, true, true);
        PeriodoContratual periodoAnterior = contrato.getPeriodoContratual();
        MetaContratual metaAnterior = contrato.getMetaContratual();
        var parcelasAnteriores = java.util.List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.renovar(1L, 3, new BigDecimal("800"));

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getPeriodoContratual().inicio())
                .isEqualTo(periodoAnterior.termino().plusDays(1));
        assertThat(contrato.getPeriodoContratual().termino())
                .isEqualTo(periodoAnterior.termino().plusDays(1).plusMonths(3));
        assertThat(contrato.getMetaContratual().valor()).isEqualByComparingTo("800");
        assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(contrato.getParcelas()).hasSize(3).allSatisfy(parcela -> {
            assertThat(parcela.isPaga()).isFalse();
            assertThat(parcela.getNumero()).isGreaterThan(2);
            assertThat(parcela.getVencimento()).isBetween(
                    contrato.getPeriodoContratual().inicio(), contrato.getPeriodoContratual().termino());
        });
        assertThat(contrato.getParcelas().stream().map(ParcelaDePagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000.00");
        assertThat(contrato.getHistorico()).hasSize(1);
        var historico = contrato.getHistorico().getFirst();
        assertThat(historico.periodo()).isSameAs(periodoAnterior);
        assertThat(historico.meta()).isSameAs(metaAnterior);
        assertThat(historico.exposicaoAcumulada()).isEqualByComparingTo("500");
        assertThat(historico.parcelas()).containsExactlyElementsOf(parcelasAnteriores);
        assertThat(parcelasAnteriores).allSatisfy(parcela -> assertThat(parcela.isPaga()).isTrue());
        verify(repository).salvar(contrato);
    }

    @Test
    @DisplayName("#38 - deve gerar parcelas a partir do número inicial sem alterar vencimentos ou total")
    void deveGerarParcelasComNumeroInicial() {
        LocalDate inicio = LocalDate.now();
        PeriodoContratual periodo = new PeriodoContratual(inicio, inicio.plusMonths(3));

        var parcelas = GeradorDeParcelas.gerar(new BigDecimal("1000.00"), periodo, 3);

        assertThat(parcelas).extracting(ParcelaDePagamento::getNumero).containsExactly(3, 4, 5);
        assertThat(parcelas).extracting(ParcelaDePagamento::getVencimento)
                .containsExactly(inicio.plusMonths(1), inicio.plusMonths(2), inicio.plusMonths(3));
        assertThat(parcelas.stream().map(ParcelaDePagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("#7 e #23 - deve exigir identificação ao renovar")
    void deveExigirIdentificacaoAoRenovar() {
        assertThatThrownBy(() -> service.renovar(null, 3, new BigDecimal("800")))
                .isInstanceOf(IdentificacaoObrigatoriaError.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("#23 - deve informar contrato inexistente ao renovar")
    void deveInformarContratoInexistenteAoRenovar() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.renovar(1L, 3, new BigDecimal("800")))
                .isInstanceOf(ContratoInexistenteError.class);
    }

    @Test
    @DisplayName("#39 - deve impedir renovação com parcela pendente e preservar o contrato")
    void deveImpedirRenovacaoComPendenciaFinanceira() {
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, true, true);
        contrato.getParcelas().add(new ParcelaDePagamento(3, new BigDecimal("100")));
        verificarPendenciaFinanceira(contrato);
    }

    @Test
    @DisplayName("#40 - deve mudar para em risco quando a meta não foi atingida")
    void deveMudarParaEmRiscoSemRenovar() {
        ContratoDePatrocinio contrato = contrato(StatusContrato.ATIVO, true, false);
        verificarMetaNaoAtingida(contrato);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ENCERRADO", "PENDENTE", "RECUSADO", "CANCELADO"})
    @DisplayName("#41 - deve impedir renovação de contrato com status inválido")
    void deveImpedirRenovacaoComStatusInvalido(StatusContrato status) {
        ContratoDePatrocinio contrato = contrato(status, true, false);
        PeriodoContratual periodo = contrato.getPeriodoContratual();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.renovar(1L, 3, new BigDecimal("800")))
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(contrato.getStatus()).isEqualTo(status);
        assertThat(contrato.getPeriodoContratual()).isSameAs(periodo);
        assertThat(contrato.getHistorico()).isEmpty();
        verify(repository, never()).salvar(contrato);
    }

    @Test
    @DisplayName("#42 - deve renovar contrato em risco e voltar ao status ativo")
    void deveRenovarContratoEmRisco() {
        ContratoDePatrocinio contrato = contrato(StatusContrato.EM_RISCO, true, true);
        PeriodoContratual periodo = contrato.getPeriodoContratual();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.renovar(1L, 3, new BigDecimal("800"));

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getPeriodoContratual().inicio()).isEqualTo(periodo.termino().plusDays(1));
        assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(contrato.getHistorico()).hasSize(1);
        verify(repository).salvar(contrato);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("#64 - não deve renovar antes de terminar o período vigente")
    void naoDeveRenovarAntecipadamente(int diasAteTermino) {
        LocalDate termino = LocalDate.now().plusDays(diasAteTermino);
        ContratoDePatrocinio contrato = new ContratoDePatrocinio(StatusContrato.ATIVO,
                new PeriodoContratual(termino.minusMonths(3), termino),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"));
        contrato.registrarExposicao(new BigDecimal("500"));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.renovar(1L, 3, new BigDecimal("800")))
                .isInstanceOf(PeriodoInvalidoError.class)
                .hasMessageContaining("antecipada");

        assertThat(contrato.getPeriodoContratual().termino()).isEqualTo(termino);
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getHistorico()).isEmpty();
        verify(repository, never()).salvar(contrato);
    }
}
