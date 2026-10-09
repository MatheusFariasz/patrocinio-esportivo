package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RenovarContratoDePatrocinioService {

    private final ContratoDePatrocinioRepository repository;

    public RenovarContratoDePatrocinioService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void renovar(Long contratoId, Integer duracaoMeses, BigDecimal novaMeta) {
        if (contratoId == null) {
            throw new IdentificacaoObrigatoriaError("A identificação do contrato é obrigatória.");
        }

        ContratoDePatrocinio contrato = repository.buscarPorId(contratoId)
                .orElseThrow(() -> new ContratoInexistenteError("Contrato não encontrado."));

        contrato.renovar(duracaoMeses, novaMeta);
        repository.salvar(contrato);
    }
}
