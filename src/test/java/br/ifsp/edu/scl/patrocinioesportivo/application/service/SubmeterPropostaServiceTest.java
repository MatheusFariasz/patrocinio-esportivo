
package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ClubeRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.PatrocinadorRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.SubmeterPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubmeterPropostaServiceTest {

    @Mock
    private ContratoDePatrocinioRepository contratoRepository;

    @Mock
    private ClubeRepository clubeRepository;

    @Mock
    private PatrocinadorRepository patrocinadorRepository;

    @InjectMocks
    private SubmeterPropostaService service;

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#20 - deve criar proposta PENDENTE sem parcelas")
    void deveCriarPropostaPendenteSemParcelas() {

        when(clubeRepository.existePorId(1L))
                .thenReturn(true);

        when(patrocinadorRepository.existePorId(2L))
                .thenReturn(true);

        when(contratoRepository.salvar(any(ContratoDePatrocinio.class)))
                .thenAnswer(invocation -> {
                    ContratoDePatrocinio proposta = invocation.getArgument(0);
                    proposta.definirId(10L);
                    return proposta;
                });

        ContratoDePatrocinio resultado = service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                new BigDecimal("500")
        );

        assertThat(resultado.getId())
                .isEqualTo(10L);

        assertThat(resultado.getStatus())
                .isEqualTo(StatusContrato.PENDENTE);

        assertThat(resultado.getParcelas())
                .isEmpty();
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#21 - deve lançar erro ao submeter proposta com patrocinador inexistente")
    void deveLancarErroAoSubmeterPropostaComPatrocinadorInexistente() {

        when(clubeRepository.existePorId(1L))
                .thenReturn(true);

        when(patrocinadorRepository.existePorId(2L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                new BigDecimal("500")
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("PatrocinadorInexistenteError"));
    }

    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#25 - deve lançar erro ao submeter proposta com clube inexistente")
    void deveLancarErroAoSubmeterPropostaComClubeInexistente() {

        when(clubeRepository.existePorId(1L))
                .thenReturn(false);

        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                new BigDecimal("500")
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("ClubeInexistenteError"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-100.00"})
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#26 - deve lancar erro para valor invalido")
    void deveLancarErroAoSubmeterPropostaComValorInvalido(BigDecimal valor) {

        when(clubeRepository.existePorId(1L))
                .thenReturn(true);

        when(patrocinadorRepository.existePorId(2L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                valor,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                new BigDecimal("500")
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("ValorInvalidoError"));

        verifyNoInteractions(contratoRepository);
    }


    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#27 - deve lancar erro para periodo com termino anterior ao inicio")
    void deveLancarErroAoSubmeterPropostaComTerminoAnteriorAoInicio() {

        when(clubeRepository.existePorId(1L))
                .thenReturn(true);

        when(patrocinadorRepository.existePorId(2L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(1),
                new BigDecimal("500")
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("PeriodoInvalidoError"));

        verifyNoInteractions(contratoRepository);
    }


    @Test
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#28 - deve lancar erro para meta obrigatoria")
    void deveLancarErroAoSubmeterPropostaSemMeta() {

        when(clubeRepository.existePorId(1L))
                .thenReturn(true);

        when(patrocinadorRepository.existePorId(2L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                1L,
                2L,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                null
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo("MetaObrigatoriaError"));

        verifyNoInteractions(contratoRepository);
    }


    @ParameterizedTest
    @MethodSource("partesInvalidas")
    @Tag("UnitTest")
    @Tag("TDD")
    @DisplayName("#52 - deve lancar erro para identificadores obrigatorios e partes iguais")
    void deveLancarErroAoSubmeterPropostaComPartesInvalidas(Long clubeId, Long patrocinadorId, String erroEsperado) {


        assertThatThrownBy(() -> service.submeter(
                PerfilUsuario.COMERCIAL,
                clubeId,
                patrocinadorId,
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(3),
                new BigDecimal("500")
        ))
                .isInstanceOf(RuntimeException.class)
                .satisfies(erro -> assertThat(erro.getClass().getSimpleName())
                        .isEqualTo(erroEsperado));

        verifyNoInteractions(contratoRepository);
    }

    static Stream<Arguments> partesInvalidas() {
        return Stream.of(
                Arguments.of(null, 2L, "ClubeObrigatorioError"),
                Arguments.of(1L, null, "PatrocinadorObrigatorioError"),
                Arguments.of(1L, 1L, "PartesIguaisError")
        );
    }
}
