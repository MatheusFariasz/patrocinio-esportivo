package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
public class EditarPropostaService {

    private final ContratoDePatrocinioRepository repository;

    public EditarPropostaService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void editar(Long propostaId, BigDecimal valor, LocalDate inicio, LocalDate termino, BigDecimal meta) {
        if (propostaId == null) {
            throw new IdentificacaoObrigatoriaError("A identificação da proposta é obrigatória.");
        }

        ContratoDePatrocinio proposta = repository.buscarPorId(propostaId)
                .orElseThrow(() -> new ContratoInexistenteError("Proposta não encontrada."));

        proposta.editar(valor, inicio, termino, meta);
        repository.salvar(proposta);
    }
}
