package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ValorInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.EditarPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class EditarPropostaServiceFunctionalTest {

    @Mock
    private ContratoDePatrocinioRepository repository;

    @InjectMocks
    private EditarPropostaService service;

    private ContratoDePatrocinio proposta(StatusContrato status) {
        LocalDate inicio = LocalDate.now().plusDays(2);
        return new ContratoDePatrocinio(status, new PeriodoContratual(inicio, inicio.plusMonths(3)),
                new MetaContratual(new BigDecimal("500")), new BigDecimal("1000"));
    }

    @ParameterizedTest(name = "valor={0}, meta={1}")
    @CsvSource({"-0.01, 0.01, true", "0.01, -0.01, false"})
    @DisplayName("#47 - VL - rejeição próxima de zero não compromete edição seguinte")
    void preservaPropostaAposRejeicaoEPermiteNovaEdicao(
            BigDecimal valor, BigDecimal meta, boolean valorInvalido) {
        ContratoDePatrocinio proposta = proposta(StatusContrato.PENDENTE);
        PeriodoContratual anterior = proposta.getPeriodoContratual();
        MetaContratual metaAnterior = proposta.getMetaContratual();
        LocalDate novoInicio = anterior.inicio().plusDays(1);
        LocalDate novoTermino = novoInicio.plusDays(1);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(proposta));

        assertThatThrownBy(() -> service.editar(1L, valor, novoInicio, novoTermino, meta))
                .isInstanceOf(valorInvalido ? ValorInvalidoError.class : MetaInvalidaError.class);

        assertThat(proposta.getValorTotal()).isEqualByComparingTo("1000");
        assertThat(proposta.getPeriodoContratual()).isEqualTo(anterior);
        assertThat(proposta.getMetaContratual()).isEqualTo(metaAnterior);
        assertThat(proposta.getStatus()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(proposta.getParcelas()).isEmpty();
        verify(repository, never()).salvar(proposta);

        service.editar(1L, new BigDecimal("0.01"), novoInicio, novoTermino, new BigDecimal("0.01"));

        assertThat(proposta.getValorTotal()).isEqualByComparingTo("0.01");
        assertThat(proposta.getPeriodoContratual()).isEqualTo(new PeriodoContratual(novoInicio, novoTermino));
        assertThat(proposta.getMetaContratual().valor()).isEqualByComparingTo("0.01");
        assertThat(proposta.getStatus()).isEqualTo(StatusContrato.PENDENTE);
        verify(repository).salvar(proposta);
    }

    @ParameterizedTest
    @EnumSource(value = StatusContrato.class, names = {"PENDENTE"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("#44 e #45 - TD - proposta avaliada rejeita edição mesmo com valor inválido")
    void priorizaEstadoDaPropostaAntesDeAlterarDados(StatusContrato status) {
        ContratoDePatrocinio proposta = proposta(status);
        PeriodoContratual anterior = proposta.getPeriodoContratual();
        MetaContratual metaAnterior = proposta.getMetaContratual();
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(proposta));

        assertThatThrownBy(() -> service.editar(1L, new BigDecimal("-0.01"),
                anterior.inicio(), anterior.termino(), new BigDecimal("0.01")))
                .isInstanceOf(TransicaoDeStatusInvalidaError.class);

        assertThat(proposta.getStatus()).isEqualTo(status);
        assertThat(proposta.getValorTotal()).isEqualByComparingTo("1000");
        assertThat(proposta.getPeriodoContratual()).isEqualTo(anterior);
        assertThat(proposta.getMetaContratual()).isEqualTo(metaAnterior);
        verify(repository, never()).salvar(proposta);
    }
}
