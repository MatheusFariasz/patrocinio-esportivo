package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ClubeRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.PatrocinadorRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.SubmeterPropostaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class SubmeterPropostaServiceFunctionalTest {
    @Mock
    private ClubeRepository clubeRepository;
    @Mock
    private PatrocinadorRepository patrocinadorRepository;
    @Mock
    private ContratoDePatrocinioRepository contratoRepository;
    @InjectMocks
    private SubmeterPropostaService service;

    private void prepararRepositorios() {
        when(clubeRepository.existePorId(10L)).thenReturn(true);
        when(patrocinadorRepository.existePorId(20L)).thenReturn(true);
        when(contratoRepository.salvar(any(ContratoDePatrocinio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest(name = "valor={0}, meta={1}")
    @CsvSource({"0.01, 500", "1000, 0.01", "0.01, 0.01"})
    @DisplayName("#20, #26 e #54 - VL - aceita valor e meta positivos próximos de zero")
    void aceitaPequenosValoresPositivos(BigDecimal valor, BigDecimal meta) {
        prepararRepositorios();
        LocalDate inicio = LocalDate.now().plusDays(2);
        LocalDate termino = inicio.plusDays(1);

        ContratoDePatrocinio proposta = service.submeter(
                PerfilUsuario.COMERCIAL, 10L, 20L, valor, inicio, termino, meta);

        assertThat(proposta.getStatus()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(proposta.getValorTotal()).isEqualByComparingTo(valor);
        assertThat(proposta.getMetaContratual().valor()).isEqualByComparingTo(meta);
        assertThat(proposta.getPeriodoContratual().inicio()).isEqualTo(inicio);
        assertThat(proposta.getPeriodoContratual().termino()).isEqualTo(termino);
        assertThat(proposta.getParcelas()).isEmpty();
        verify(contratoRepository).salvar(proposta);
    }
}
