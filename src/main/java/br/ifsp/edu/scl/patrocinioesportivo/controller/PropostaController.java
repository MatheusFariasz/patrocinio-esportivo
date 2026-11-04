package br.ifsp.edu.scl.patrocinioesportivo.controller;

import br.ifsp.edu.scl.patrocinioesportivo.controller.dto.*;
import br.ifsp.edu.scl.patrocinioesportivo.security.auth.AuthenticationInfoService;
import br.ifsp.edu.scl.patrocinioesportivo.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/propostas")
@Tag(name = "Propostas de patrocínio")
public class PropostaController {
    private final SubmeterPropostaService submeter;
    private final EditarPropostaService editar;
    private final AprovarPropostaService aprovar;
    private final RecusarPropostaService recusar;
    private final CancelarPropostaService cancelar;
    private final ConsultarContratoService consultar;
    private final AuthenticationInfoService authentication;

    public PropostaController(SubmeterPropostaService submeter, EditarPropostaService editar,
                             AprovarPropostaService aprovar, RecusarPropostaService recusar,
                             CancelarPropostaService cancelar, ConsultarContratoService consultar,
                             AuthenticationInfoService authentication) {
        this.submeter = submeter;
        this.editar = editar;
        this.aprovar = aprovar;
        this.recusar = recusar;
        this.cancelar = cancelar;
        this.consultar = consultar;
        this.authentication = authentication;
    }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Proposta criada")
    public ResponseEntity<ContratoResponse> submeter(@Valid @RequestBody SubmeterPropostaRequest request) {
        var contrato = submeter.submeter(authentication.getAuthenticatedUserProfile(), request.clubeId(),
                request.patrocinadorId(), request.valor(), request.inicio(), request.termino(), request.meta());
        return ResponseEntity.created(URI.create("/api/v1/contratos/" + contrato.getId()))
                .body(ContratoResponse.de(contrato));
    }

    @PutMapping("/{id}")
    public ContratoResponse editar(@PathVariable Long id, @Valid @RequestBody EditarPropostaRequest request) {
        editar.editar(id, request.valor(), request.inicio(), request.termino(), request.meta());
        return resposta(id);
    }

    @PostMapping("/{id}/aprovacao")
    public ContratoResponse aprovar(@PathVariable Long id) {
        aprovar.aprovar(authentication.getAuthenticatedUserProfile(), id);
        return resposta(id);
    }

    @PostMapping("/{id}/recusa")
    public ContratoResponse recusar(@PathVariable Long id) {
        recusar.recusar(authentication.getAuthenticatedUserProfile(), id);
        return resposta(id);
    }

    @PostMapping("/{id}/cancelamento")
    public ContratoResponse cancelar(@PathVariable Long id) {
        cancelar.cancelar(id);
        return resposta(id);
    }

    private ContratoResponse resposta(Long id) {
        return ContratoResponse.de(consultar.consultar(id));
    }
}
