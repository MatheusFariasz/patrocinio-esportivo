package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.EncerrarContratoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class EncerrarContratoServiceFunctionalTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private EncerrarContratoService service;

    private ContratoDePatrocinio contrato(StatusContrato status, boolean todasPagas) {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        ParcelaDePagamento paga = new ParcelaDePagamento(1, new BigDecimal("500"), inicio);
        paga.registrarPagamento(inicio);
        ParcelaDePagamento segunda = new ParcelaDePagamento(2, new BigDecimal("200"), inicio.plusMonths(1));
        ParcelaDePagamento terceira = new ParcelaDePagamento(3, new BigDecimal("300"), inicio.plusMonths(2));
        if (todasPagas) {
            segunda.registrarPagamento(inicio);
            terceira.registrarPagamento(inicio);
        }
        ContratoDePatrocinio contrato = ContratoDePatrocinio.reconstituir(status,
                new PeriodoContratual(inicio, inicio.plusMonths(3)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"),
                List.of(paga, segunda, terceira));
        contrato.definirId(1L);
        return contrato;
    }

    @ParameterizedTest(name = "{0}, quitadas={1}, multa={2}")
    @CsvSource({"ATIVO, false, 100", "ATIVO, true, 0", "EM_RISCO, false, 100", "EM_RISCO, true, 0"})
    @DisplayName("#94 - TD - grava o encerramento com a multa correspondente às parcelas pendentes")
    void gravaEncerramentoComMulta(StatusContrato status, boolean todasPagas, BigDecimal multa) {
        ContratoDePatrocinio contrato = contrato(status, todasPagas);
        PeriodoContratual periodo = contrato.getPeriodoContratual();
        MetaContratual meta = contrato.getMetaContratual();
        List<ParcelaDePagamento> parcelas = List.copyOf(contrato.getParcelas());
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        service.encerrar(1L);

        ArgumentCaptor<ContratoDePatrocinio> captor = ArgumentCaptor.forClass(ContratoDePatrocinio.class);
        verify(repository).salvar(captor.capture());
        ContratoDePatrocinio gravado = captor.getValue();
        assertThat(gravado.getId()).isEqualTo(1L);
        assertThat(gravado.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
        assertThat(gravado.getMultaRescisoria()).isEqualByComparingTo(multa);
        assertThat(gravado.getPeriodoContratual()).isEqualTo(periodo);
        assertThat(gravado.getMetaContratual()).isEqualTo(meta);
        assertThat(gravado.getValorTotal()).isEqualByComparingTo("1000");
        assertThat(gravado.getParcelas()).containsExactlyElementsOf(parcelas);
        assertThat(gravado.getParcelas().getFirst().isPaga()).isTrue();
        assertThat(gravado.getParcelas().get(1).isPaga()).isEqualTo(todasPagas);
        assertThat(gravado.getParcelas().getLast().isPaga()).isEqualTo(todasPagas);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"PENDENTE", "RECUSADO", "CANCELADO", "ENCERRADO"})
    @DisplayName("#94 - PE - estados inválidos não provocam gravação")
    void naoGravaQuandoEstadoNaoPermiteEncerrar(StatusContrato status) {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        ContratoDePatrocinio contrato = new ContratoDePatrocinio(status,
                new PeriodoContratual(inicio, inicio.plusMonths(3)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> service.encerrar(1L))
                .isInstanceOf(status == StatusContrato.ENCERRADO
                        ? OperacaoRedundanteError.class : TransicaoDeStatusInvalidaError.class);

        assertThat(contrato.getStatus()).isEqualTo(status);
        assertThat(contrato.getMultaRescisoria()).isEqualByComparingTo("0");
        verify(repository, never()).salvar(any(ContratoDePatrocinio.class));
    }

    @Test
    @DisplayName("#94 - PE - contrato inexistente não provoca gravação")
    void naoGravaQuandoContratoNaoExiste() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.encerrar(1L))
                .isInstanceOf(ContratoInexistenteError.class);

        verify(repository, never()).salvar(any(ContratoDePatrocinio.class));
    }
}
