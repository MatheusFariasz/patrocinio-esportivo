package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarPagamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
