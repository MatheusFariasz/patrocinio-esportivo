package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PagamentoJaRegistradoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ParcelaInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarPagamentoService;
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
class RegistrarPagamentoServiceFunctionalTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private RegistrarPagamentoService service;

    private ContratoDePatrocinio contrato(StatusContrato status, LocalDate vencimento) {
        LocalDate inicio = vencimento.minusMonths(1);
        return ContratoDePatrocinio.reconstituir(status,
                new PeriodoContratual(inicio, vencimento.plusMonths(2)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"),
                List.of(new ParcelaDePagamento(7, new BigDecimal("500"), vencimento),
                        new ParcelaDePagamento(8, new BigDecimal("500"), vencimento.plusMonths(1))));
    }

    @ParameterizedTest(name = "{0}, vencimento {1} dia(s)")
    @CsvSource({
            "ATIVO, -1", "ATIVO, 0", "ATIVO, 1",
            "EM_RISCO, -1", "EM_RISCO, 0", "EM_RISCO, 1"
    })
    @DisplayName("#15, #16 e #56 - VL/TD - paga na fronteira do vencimento sem alterar outras parcelas")
    void registraNaFronteiraDoVencimento(StatusContrato status, int dias) {
        LocalDate antes = LocalDate.now();
        LocalDate vencimento = antes.plusDays(dias);
        ContratoDePatrocinio contrato = contrato(status, vencimento);
        ParcelaDePagamento escolhida = contrato.getParcelas().getFirst();
        ParcelaDePagamento outra = contrato.getParcelas().getLast();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.registrar(1L, 7);

        assertThat(escolhida.isPaga()).isTrue();
        assertThat(escolhida.getDataPagamento()).isBetween(antes, LocalDate.now());
        assertThat(escolhida.getVencimento()).isEqualTo(vencimento);
        assertThat(escolhida.getValor()).isEqualByComparingTo("500");
        assertThat(outra.isPaga()).isFalse();
        assertThat(outra.getDataPagamento()).isNull();
        assertThat(contrato.getStatus()).isEqualTo(status);
        verify(repository).salvar(contrato);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("#17 e #56 - TD - rejeita repetição e conserva as demais obrigações")
    void preservaContratoAoRejeitarPagamentoRepetido(StatusContrato status) {
        LocalDate ontem = LocalDate.now().minusDays(1);
        ContratoDePatrocinio contrato = contrato(status, ontem.minusDays(1));
        ParcelaDePagamento escolhida = contrato.getParcelas().getFirst();
        ParcelaDePagamento outra = contrato.getParcelas().getLast();
        escolhida.registrarPagamento(ontem);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.registrar(1L, 7))
                .isInstanceOf(PagamentoJaRegistradoError.class);

        assertThat(escolhida.getDataPagamento()).isEqualTo(ontem);
        assertThat(escolhida.isPaga()).isTrue();
        assertThat(outra.isPaga()).isFalse();
        assertThat(outra.getDataPagamento()).isNull();
        assertThat(contrato.getStatus()).isEqualTo(status);
        verify(repository, never()).salvar(contrato);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"ATIVO", "EM_RISCO"})
    @DisplayName("#55 e #56 - TD - parcela inexistente não altera as obrigações do contrato")
    void preservaParcelasAoRejeitarNumeroInexistente(StatusContrato status) {
        ContratoDePatrocinio contrato = contrato(status, LocalDate.now());
        List<ParcelaDePagamento> parcelas = List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.registrar(1L, 9))
                .isInstanceOf(ParcelaInexistenteError.class);

        assertThat(contrato.getParcelas()).containsExactlyElementsOf(parcelas);
        assertThat(contrato.getParcelas()).allSatisfy(parcela -> {
            assertThat(parcela.isPaga()).isFalse();
            assertThat(parcela.getDataPagamento()).isNull();
        });
        assertThat(contrato.getStatus()).isEqualTo(status);
        verify(repository, never()).salvar(contrato);
    }
}
