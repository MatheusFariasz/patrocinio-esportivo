package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PermissaoNegadaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

@Service
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

        ContratoDePatrocinio proposta = repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError(
                                "Proposta não encontrada."
                        ));

        proposta.recusar();

    }
}