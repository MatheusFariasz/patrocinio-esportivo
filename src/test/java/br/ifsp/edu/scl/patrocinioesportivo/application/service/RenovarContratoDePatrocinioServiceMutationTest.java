package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RenovarContratoDePatrocinioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("UnitTest")
@Tag("Mutation")
class RenovarContratoDePatrocinioServiceMutationTest {

    @ParameterizedTest(name = "meta atingida: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("Duração zero deve ser rejeitada antes de avaliar a meta, preservando o contrato")
    void deveRejeitarDuracaoZeroIndependentementeDaMeta(boolean metaAtingida) {
        var repository = mock(ContratoDePatrocinioRepository.class);
        var service = new RenovarContratoDePatrocinioService(repository);
        LocalDate termino = LocalDate.now().minusDays(1);
        var periodo = new PeriodoContratual(termino.minusMonths(3), termino);
        var meta = new MetaContratual(new BigDecimal("500"));
        BigDecimal exposicao = metaAtingida ? new BigDecimal("500") : BigDecimal.ZERO;
        var parcela = ParcelaDePagamento.reconstituir(
                1, new BigDecimal("1000"), termino, true, termino);
        var contrato = ContratoDePatrocinio.reconstituir(
                1L, StatusContrato.ATIVO, periodo, meta, new BigDecimal("1000"),
                10L, 20L, exposicao, BigDecimal.ZERO, List.of(parcela), List.of());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.renovar(1L, 0, new BigDecimal("800")))
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
