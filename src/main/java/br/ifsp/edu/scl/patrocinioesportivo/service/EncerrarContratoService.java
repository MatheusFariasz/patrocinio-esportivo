package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EncerrarContratoService {

    private final ContratoDePatrocinioRepository repository;

    public EncerrarContratoService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void encerrar(Long contratoId) {
        if (contratoId == null) {
            throw new IdentificacaoObrigatoriaError("A identificação do contrato é obrigatória.");
        }

        ContratoDePatrocinio contrato = repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError(
                                "Contrato não encontrado."
                        ));

        contrato.encerrar();
        repository.salvar(contrato);
    }
}