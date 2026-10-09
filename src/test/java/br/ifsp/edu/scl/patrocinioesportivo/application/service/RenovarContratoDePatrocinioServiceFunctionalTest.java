package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PendenciaFinanceiraError;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class RenovarContratoDePatrocinioServiceFunctionalTest {
    @Mock
    private ContratoDePatrocinioRepository repository;
    @InjectMocks
    private RenovarContratoDePatrocinioService service;

    private ContratoDePatrocinio contrato(
            StatusContrato status, boolean paga, BigDecimal exposicao) {
        LocalDate termino = LocalDate.now().minusDays(2);
        ParcelaDePagamento parcela = new ParcelaDePagamento(
                7, new BigDecimal("1000"), termino);
        if (paga) {
            parcela.registrarPagamento();
        }
        ContratoDePatrocinio contrato = ContratoDePatrocinio.reconstituir(
                status, new PeriodoContratual(termino.minusMonths(3), termino),
                new MetaContratual(new BigDecimal("500")),
                new BigDecimal("1000"), List.of(parcela));
        contrato.definirId(1L);
        if (status == StatusContrato.ATIVO || status == StatusContrato.EM_RISCO) {
            contrato.registrarExposicao(exposicao);
        }
        return contrato;
    }

    @ParameterizedTest(name = "{0}: exposição {1}, renova={2}")
    @CsvSource({
            "ATIVO, 499.99, false", "ATIVO, 500, true", "ATIVO, 500.01, true",
            "EM_RISCO, 499.99, false", "EM_RISCO, 500, true", "EM_RISCO, 500.01, true"
    })
    @DisplayName("#38, #40, #42 e #69 - VL - avalia exposição abaixo, igual e acima da meta")
    void avaliaLimiteDaMeta(StatusContrato status, BigDecimal exposicao, boolean renova) {
        ContratoDePatrocinio contrato = contrato(status, true, exposicao);
        PeriodoContratual anterior = contrato.getPeriodoContratual();
        MetaContratual metaAnterior = contrato.getMetaContratual();
        List<ParcelaDePagamento> parcelasAnteriores = List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.renovar(1L, 3, new BigDecimal("800"));

        if (renova) {
            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
            assertThat(contrato.getPeriodoContratual().inicio())
                    .isEqualTo(anterior.termino().plusDays(1));
            assertThat(contrato.getMetaContratual().valor()).isEqualByComparingTo("800");
            assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo("0");
            assertThat(contrato.getHistorico()).hasSize(1);
            assertThat(contrato.getHistorico().getFirst().exposicaoAcumulada())
                    .isEqualByComparingTo(exposicao);
            assertThat(contrato.getParcelas()).extracting(ParcelaDePagamento::getNumero)
                    .containsExactly(8, 9, 10);
        } else {
            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.EM_RISCO);
            assertThat(contrato.getPeriodoContratual()).isEqualTo(anterior);
            assertThat(contrato.getMetaContratual()).isEqualTo(metaAnterior);
            assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(exposicao);
            assertThat(contrato.getParcelas()).containsExactlyElementsOf(parcelasAnteriores);
            assertThat(contrato.getHistorico()).isEmpty();
        }
        verify(repository).salvar(contrato);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("#38 e #66 - VL - aceita renovação com duração de um mês")
    void aceitaDuracaoDeUmMes(StatusContrato status) {
        ContratoDePatrocinio contrato = contrato(status, true, new BigDecimal("500"));
        LocalDate novoInicio = contrato.getPeriodoContratual().termino().plusDays(1);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.renovar(1L, 1, new BigDecimal("800"));

        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(contrato.getPeriodoContratual().inicio()).isEqualTo(novoInicio);
        assertThat(contrato.getPeriodoContratual().termino()).isEqualTo(novoInicio.plusMonths(1));
        assertThat(contrato.getParcelas()).hasSize(1);
        assertThat(contrato.getParcelas().getFirst().getNumero()).isEqualTo(8);
        assertThat(contrato.getParcelas().getFirst().getValor()).isEqualByComparingTo("1000");
        assertThat(contrato.getParcelas().getFirst().isPaga()).isFalse();
        verify(repository).salvar(contrato);
    }

    @ParameterizedTest(name = "{0} com dívida e exposição {1}")
    @CsvSource({"ATIVO, 499.99", "ATIVO, 500", "EM_RISCO, 499.99", "EM_RISCO, 500"})
    @DisplayName("#39 e #68 - TD R3/R4/R7/R8 - dívida impede renovação com ou sem meta")
    void impedeRenovacaoComDivida(StatusContrato status, String valorExposicao) {
        BigDecimal exposicao = new BigDecimal(valorExposicao);
        ContratoDePatrocinio contrato = contrato(status, false, exposicao);
        PeriodoContratual anterior = contrato.getPeriodoContratual();
        MetaContratual metaAnterior = contrato.getMetaContratual();
        List<ParcelaDePagamento> parcelasAnteriores = List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.renovar(1L, 3, new BigDecimal("800")))
                .isInstanceOf(PendenciaFinanceiraError.class);

        assertThat(contrato.getStatus()).isEqualTo(status);
        assertThat(contrato.getPeriodoContratual()).isEqualTo(anterior);
        assertThat(contrato.getMetaContratual()).isEqualTo(metaAnterior);
        assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo(exposicao);
        assertThat(contrato.getParcelas()).containsExactlyElementsOf(parcelasAnteriores);
        assertThat(contrato.getParcelas().getFirst().isPaga()).isFalse();
        assertThat(contrato.getHistorico()).isEmpty();
        verify(repository, never()).salvar(contrato);
    }

    @ParameterizedTest(name = "término {0} dia(s), renova={1}")
    @CsvSource({"-1, true", "0, false", "1, false"})
    @DisplayName("#38 e #64 - VL - avalia término ontem, hoje e amanhã")
    void avaliaFronteiraDoTermino(int dias, boolean renova) {
        LocalDate hoje = LocalDate.now();
        LocalDate termino = hoje.plusDays(dias);
        ParcelaDePagamento parcela = new ParcelaDePagamento(7, new BigDecimal("1000"), termino);
        parcela.registrarPagamento();
        ContratoDePatrocinio contrato = ContratoDePatrocinio.reconstituir(
                StatusContrato.ATIVO, new PeriodoContratual(termino.minusMonths(3), termino),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"), List.of(parcela));
        contrato.registrarExposicao(new BigDecimal("500"));
        PeriodoContratual anterior = contrato.getPeriodoContratual();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        if (renova) {
            service.renovar(1L, 1, new BigDecimal("800"));
            assertThat(contrato.getPeriodoContratual().inicio()).isEqualTo(termino.plusDays(1));
            assertThat(contrato.getHistorico()).hasSize(1);
            verify(repository).salvar(contrato);
        } else {
            assertThatThrownBy(() -> service.renovar(1L, 1, new BigDecimal("800")))
                    .isInstanceOf(PeriodoInvalidoError.class);
            assertThat(contrato.getPeriodoContratual()).isEqualTo(anterior);
            assertThat(contrato.getHistorico()).isEmpty();
            assertThat(contrato.getExposicaoAcumulada()).isEqualByComparingTo("500");
            verify(repository, never()).salvar(contrato);
        }
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
    }
}
