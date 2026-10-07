package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

@Service
public class CancelarPropostaService {

    private final ContratoDePatrocinioRepository repository;

    public CancelarPropostaService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void cancelar(Long propostaId) {
        ContratoDePatrocinio proposta = repository.buscarPorId(propostaId)
                .orElseThrow(() ->
                        new ContratoInexistenteError("Proposta não encontrada."));

        proposta.cancelar();
    }
}