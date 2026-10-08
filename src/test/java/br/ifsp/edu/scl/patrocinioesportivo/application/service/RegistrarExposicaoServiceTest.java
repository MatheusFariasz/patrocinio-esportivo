package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;

import br.ifsp.edu.scl.patrocinioesportivo.service.RegistrarExposicaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrarExposicaoServiceTest {

    @ParameterizedTest
    @EnumSource(
            value = StatusContrato.class,
            names = {"ATIVO", "EM_RISCO"}
    )
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#65 - deve registrar exposição em contrato ativo ou em risco")
    void deveRegistrarExposicaoEmContratoValido(StatusContrato status) {
        ContratoDePatrocinioRepository repository =
                mock(ContratoDePatrocinioRepository.class);

        ContratoDePatrocinio contrato =
                new ContratoDePatrocinio(
                        status,
                        null,
                        new MetaContratual(new BigDecimal("1000"))
                );

        when(repository.buscarPorId(1L))
                .thenReturn(Optional.of(contrato));

        RegistrarExposicaoService service =
                new RegistrarExposicaoService(repository);

        service.registrar(
                1L,
                new BigDecimal("600")
        );

        service.registrar(
                1L,
                new BigDecimal("500")
        );

        assertThat(contrato.getExposicaoAcumulada())
                .isEqualByComparingTo("1100");

        assertThat(contrato.metaFoiAtingida())
                .isTrue();
    }
}