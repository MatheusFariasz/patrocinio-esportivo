package br.ifsp.edu.scl.patrocinioesportivo.application.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
@Tag("Functional")
class IdentificacaoContratoServiceFunctionalTest {
    @Mock private ContratoDePatrocinioRepository repository;

    @ParameterizedTest(name = "{0} sem identificação")
    @ValueSource(strings = {"aprovar", "recusar", "pagar", "encerrar"})
    @DisplayName("#100 / #7 - PE - rejeitar identificação ausente antes de consultar ou salvar")
    void rejeitarIdentificacaoAusente(String operacao) {
        assertThatThrownBy(() -> executar(operacao)).isInstanceOf(IdentificacaoObrigatoriaError.class);
        verifyNoInteractions(repository);
    }

    private void executar(String operacao) {
        switch (operacao) {
            case "aprovar" -> new AprovarPropostaService(repository).aprovar(PerfilUsuario.DIRETOR_FINANCEIRO, null);
            case "recusar" -> new RecusarPropostaService(repository).recusar(PerfilUsuario.DIRETOR_FINANCEIRO, null);
            case "pagar" -> new RegistrarPagamentoService(repository).registrar(null, 1);
            case "encerrar" -> new EncerrarContratoService(repository).encerrar(null);
            default -> throw new IllegalArgumentException("Operação desconhecida: " + operacao);
        }
    }
}
