package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RenovarContratoDePatrocinioService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.ifsp.edu.scl.patrocinioesportivo.application.mutation.DadosMutacao.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
