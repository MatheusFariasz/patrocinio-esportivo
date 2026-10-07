package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

@Service
public class ConsultarContratoService {

    private final ContratoDePatrocinioRepository repository;

    public ConsultarContratoService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public ContratoDePatrocinio consultar(Long contratoId) {
        return repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError("Contrato não encontrado."));
    }
}
