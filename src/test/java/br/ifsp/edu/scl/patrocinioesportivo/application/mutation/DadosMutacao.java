package br.ifsp.edu.scl.patrocinioesportivo.application.mutation;

import br.ifsp.edu.scl.patrocinioesportivo.controller.ContratoController;
import br.ifsp.edu.scl.patrocinioesportivo.controller.PropostaController;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationInfoService;
import br.ifsp.edu.scl.patrocinioesportivo.security.config.ApplicationConfig;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.Role;
import br.ifsp.edu.scl.patrocinioesportivo.security.user.User;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

final class DadosMutacao {

    private DadosMutacao() {}

    static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    static User usuario(Role role) {
        return new User(
                UUID.fromString("12345678-1234-1234-1234-123456789012"),
                "Ana",
                "Silva",
                "ana@teste.com",
                "hash",
                role
        );
    }

    static HistoricoDePeriodo historico(int ciclo) {
        LocalDate inicio = LocalDate.of(2023, 1, 1)
                .plusMonths(ciclo * 3L);
        LocalDate termino = inicio.plusMonths(3);

        var parcela = ParcelaDePagamento.reconstituir(
                ciclo + 1,
                valor("300"),
                termino,
                true,
                termino.minusDays(1)
        );

        return new HistoricoDePeriodo(
                new PeriodoContratual(inicio, termino),
                new MetaContratual(valor("100")),
                valor("150"),
                List.of(parcela)
        );
    }

    static ContratoDePatrocinio contrato(StatusContrato status) {
        LocalDate inicio = LocalDate.of(2025, 5, 1);
        LocalDate termino = inicio.plusMonths(3);

        boolean proposta = status == StatusContrato.PENDENTE
                || status == StatusContrato.CANCELADO;

        boolean paga = status != StatusContrato.ENCERRADO;

        var parcela = ParcelaDePagamento.reconstituir(
                7,
                valor("1000"),
                termino,
                paga,
                paga ? termino.minusDays(1) : null
        );

        return ContratoDePatrocinio.reconstituir(
                7L,
                status,
                new PeriodoContratual(inicio, termino),
                new MetaContratual(valor("500")),
                valor("1000"),
                10L,
                20L,
                proposta ? BigDecimal.ZERO : valor("600"),
                status == StatusContrato.ENCERRADO
                        ? valor("200")
                        : BigDecimal.ZERO,
                proposta ? List.of() : List.of(parcela),
                proposta ? List.of() : List.of(historico(0))
        );
    }

    static PropostaController propostas(
            ConsultarContratoService consultar,
            EditarPropostaService editar,
            AprovarPropostaService aprovar,
            CancelarPropostaService cancelar,
            AuthenticationInfoService auth
    ) {
        return new PropostaController(
                mock(SubmeterPropostaService.class),
                editar,
                aprovar,
                mock(RecusarPropostaService.class),
                cancelar,
                consultar,
                auth
        );
    }

    static ContratoController contratos(
            ConsultarContratoService consultar,
            RegistrarPagamentoService pagamento,
            EncerrarContratoService encerrar,
            RenovarContratoDePatrocinioService renovar
    ) {
        return new ContratoController(
                consultar,
                pagamento,
                mock(RegistrarExposicaoService.class),
                encerrar,
                renovar
        );
    }

    static AuthenticationProvider provider(ApplicationConfig config) {
        return ReflectionTestUtils.invokeMethod(
                config,
                "authenticatorProvider"
        );
    }
}