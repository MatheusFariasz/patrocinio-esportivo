package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.ConsultarContratoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarContratoServiceTest {

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#9 - deve consultar contrato ativo")
    void deveConsultarContratoAtivo() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        ConsultarContratoService service =
                new ConsultarContratoService(repository);

        ContratoDePatrocinio resultado =
                service.consultar(1L);

        assertThat(resultado)
                .isSameAs(contrato);

        assertThat(resultado.getStatus())
                .isEqualTo(StatusContrato.ATIVO);
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#10 - deve consultar contrato encerrado com histórico de pagamentos")
    void deveConsultarContratoEncerradoComHistoricoDePagamentos() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ENCERRADO);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00")
                );

        parcela.registrarPagamento();

        contrato.adicionarParcela(parcela);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        ConsultarContratoService service =
                new ConsultarContratoService(repository);

        ContratoDePatrocinio resultado =
                service.consultar(1L);

        assertThat(resultado.getStatus())
                .isEqualTo(StatusContrato.ENCERRADO);

        assertThat(resultado.getParcelas().size())
                .isEqualTo(1);

        assertThat(resultado.getParcelas().get(0).isPaga())
                .isTrue();
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#11 - deve consultar os dados completos das parcelas")
    void deveConsultarDadosCompletosDasParcelas() {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(StatusContrato.ATIVO);

        LocalDate vencimento =
                LocalDate.of(2026, 11, 15);

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00"),
                        vencimento
                );

        contrato.adicionarParcela(parcela);

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        ConsultarContratoService service =
                new ConsultarContratoService(repository);

        ContratoDePatrocinio resultado =
                service.consultar(1L);

        assertThat(resultado.getParcelas().size())
                .isEqualTo(1);

        ParcelaDePagamento parcelaConsultada =
                resultado.getParcelas().get(0);

        assertThat(parcelaConsultada.getNumero())
                .isEqualTo(1);

        assertThat(parcelaConsultada.getValor())
                .isEqualByComparingTo("1000.00");

        assertThat(parcelaConsultada.getVencimento())
                .isEqualTo(vencimento);

        assertThat(parcelaConsultada.isPaga())
                .isFalse();
    }
}
