package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PagamentoJaRegistradoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ParcelaInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarPagamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RegistrarPagamentoServiceTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#15 - deve registrar pagamento de parcela de contrato ativo")
    void deveRegistrarPagamentoDeParcelaDeContratoAtivo() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 10, 1)
                );

        contrato.adicionarParcela(parcela);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        when(repository.salvar(contrato))
                .thenReturn(contrato);

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        LocalDate dataAntesDoRegistro = LocalDate.now();

        service.registrar(1L, 1);

        LocalDate dataDepoisDoRegistro = LocalDate.now();

        assertThat(parcela.isPaga()).isTrue();

        assertThat(parcela.getDataPagamento())
                .isBetween(dataAntesDoRegistro, dataDepoisDoRegistro);

        verify(repository).salvar(contrato);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#16 - deve registrar pagamento de parcela vencida com a data real do pagamento")
    void deveRegistrarPagamentoDeParcelaVencidaComDataRealDoPagamento() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.now().minusDays(5)
                );

        contrato.adicionarParcela(parcela);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        when(repository.salvar(contrato))
                .thenReturn(contrato);

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        LocalDate dataAntesDoPagamento = LocalDate.now();

        service.registrar(1L, 1);

        LocalDate dataDepoisDoPagamento = LocalDate.now();

        assertThat(parcela.isPaga()).isTrue();

        assertThat(parcela.getDataPagamento())
                .isNotNull()
                .isBetween(dataAntesDoPagamento, dataDepoisDoPagamento);

        assertThat(parcela.isEmAtraso(LocalDate.now()))
                .isFalse();

        verify(repository).salvar(contrato);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#17 - deve impedir pagamento duplicado e preservar a data original")
    void deveImpedirPagamentoDuplicadoEPreservarDataOriginal() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 10, 1)
                );

        LocalDate dataPagamentoOriginal =
                LocalDate.of(2026, 10, 5);

        parcela.registrarPagamento(dataPagamentoOriginal);
        contrato.adicionarParcela(parcela);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        assertThatThrownBy(() -> service.registrar(1L, 1))
                .isInstanceOf(PagamentoJaRegistradoError.class);

        assertThat(parcela.isPaga()).isTrue();

        assertThat(parcela.getDataPagamento())
                .isEqualTo(dataPagamentoOriginal);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {
            "PENDENTE",
            "ENCERRADO",
            "RECUSADO",
            "CANCELADO"
    })
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#18 - deve impedir pagamento quando o contrato não permite")
    void deveImpedirPagamentoQuandoContratoNaoPermite(StatusContrato status) {
        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.now().minusDays(5)
                );

        ContratoDePatrocinio contrato = ContratoDePatrocinio.reconstituir(
                status,
                null,
                null,
                new BigDecimal("1000.00"),
                List.of(parcela)
        );

        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        assertThatThrownBy(() -> service.registrar(1L, 1))
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(parcela.isPaga()).isFalse();
        assertThat(parcela.getDataPagamento()).isNull();

        verify(repository, never()).salvar(any(ContratoDePatrocinio.class));
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#55 - deve lançar erro ao pagar parcela inexistente")
    void deveLancarErroAoPagarParcelaInexistente() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        contrato.adicionarParcela(
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.now().plusDays(10)
                )
        );

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        assertThatThrownBy(() -> service.registrar(1L, 2))
                .isInstanceOf(ParcelaInexistenteError.class);

        assertThat(contrato.getParcelas())
                .filteredOn(parcela -> parcela.getNumero() == 1)
                .singleElement()
                .satisfies(parcela -> {
                    assertThat(parcela.isPaga()).isFalse();
                    assertThat(parcela.getDataPagamento()).isNull();
                });

        verify(repository, never()).salvar(any(ContratoDePatrocinio.class));
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#56 - deve permitir pagamento em contrato em risco")
    void devePermitirPagamentoEmContratoEmRisco() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        LocalDate.now().minusDays(5)
                );

        ContratoDePatrocinio contrato =
                ContratoDePatrocinio.reconstituir(
                        StatusContrato.EM_RISCO,
                        null,
                        null,
                        new BigDecimal("1000.00"),
                        List.of(parcela)
                );

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        RegistrarPagamentoService service =
                new RegistrarPagamentoService(repository);

        service.registrar(1L, 1);

        assertThat(parcela.isPaga()).isTrue();
        assertThat(parcela.getDataPagamento()).isEqualTo(LocalDate.now());
        assertThat(contrato.getStatus()).isEqualTo(StatusContrato.EM_RISCO);

        verify(repository).salvar(contrato);
    }
}
