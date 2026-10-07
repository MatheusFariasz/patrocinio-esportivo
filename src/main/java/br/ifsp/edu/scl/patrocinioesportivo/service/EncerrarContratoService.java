package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

@Service
public class EncerrarContratoService {

    private final ContratoDePatrocinioRepository repository;

    public EncerrarContratoService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void encerrar(Long contratoId) {
        ContratoDePatrocinio contrato = repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError(
                                "Contrato não encontrado."
                        ));

        contrato.encerrar();
    }
}