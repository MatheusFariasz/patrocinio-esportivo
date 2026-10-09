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
import java.util.List;
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

        ParcelaDePagamento parcela =
                new ParcelaDePagamento(
                        1,
                        new BigDecimal("1000.00")
                );

        parcela.registrarPagamento();

        ContratoDePatrocinio contrato =
                ContratoDePatrocinio.reconstituir(
                        StatusContrato.ENCERRADO,
                        null,
                        null,
                        null,
                        List.of(parcela)
                );

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
}
