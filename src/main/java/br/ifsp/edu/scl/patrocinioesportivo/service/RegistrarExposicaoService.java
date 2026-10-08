package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RegistrarExposicaoService {

    private final ContratoDePatrocinioRepository repository;

    public RegistrarExposicaoService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void registrar(Long contratoId, BigDecimal valor) {
        ContratoDePatrocinio contrato = repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError("Contrato não encontrado."));

        contrato.registrarExposicao(valor);
    }
}
