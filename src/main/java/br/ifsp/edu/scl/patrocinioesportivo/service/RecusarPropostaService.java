package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PermissaoNegadaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecusarPropostaService {

    private final ContratoDePatrocinioRepository repository;

    public RecusarPropostaService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void recusar(PerfilUsuario perfil, Long contratoId) {
        if (perfil != PerfilUsuario.DIRETOR_FINANCEIRO) {
            throw new PermissaoNegadaError(
                    "Apenas o diretor financeiro pode recusar propostas."
            );
        }

        if (contratoId == null) {
            throw new IdentificacaoObrigatoriaError("A identificação do contrato é obrigatória.");
        }

        ContratoDePatrocinio proposta = repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError(
                                "Proposta não encontrada."
                        ));

        proposta.recusar();
        repository.salvar(proposta);
    }
}