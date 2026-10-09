package br.ifsp.edu.scl.patrocinioesportivo.controller;

import br.ifsp.edu.scl.patrocinioesportivo.controller.dto.*;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contratos")
@Tag(name = "Contratos de patrocínio")
public class ContratoController {
    private final ConsultarContratoService consultar;
    private final RegistrarPagamentoService pagamento;
    private final RegistrarExposicaoService exposicao;
    private final EncerrarContratoService encerrar;
    private final RenovarContratoDePatrocinioService renovar;

    public ContratoController(ConsultarContratoService consultar, RegistrarPagamentoService pagamento,
                              RegistrarExposicaoService exposicao, EncerrarContratoService encerrar,
                              RenovarContratoDePatrocinioService renovar) {
        this.consultar = consultar;
        this.pagamento = pagamento;
        this.exposicao = exposicao;
        this.encerrar = encerrar;
        this.renovar = renovar;
    }

    @GetMapping("/{id}")
    public ContratoResponse consultar(@PathVariable Long id) {
        return ContratoResponse.de(consultar.consultar(id));
    }

    @PostMapping("/{id}/parcelas/{numero}/pagamentos")
    public ContratoResponse registrarPagamento(@PathVariable Long id, @PathVariable int numero) {
        pagamento.registrar(id, numero);
        return consultar(id);
    }

    @PostMapping("/{id}/exposicoes")
    public ContratoResponse registrarExposicao(@PathVariable Long id, @Valid @RequestBody ExposicaoRequest request) {
        exposicao.registrar(id, request.valor());
        return consultar(id);
    }

    @PostMapping("/{id}/encerramento")
    public ContratoResponse encerrar(@PathVariable Long id) {
        encerrar.encerrar(id);
        return consultar(id);
    }

    @PostMapping("/{id}/renovacao")
    public ContratoResponse renovar(@PathVariable Long id, @Valid @RequestBody RenovarContratoRequest request) {
        renovar.renovar(id, request.duracaoMeses(), request.novaMeta());
        return consultar(id);
    }
}
