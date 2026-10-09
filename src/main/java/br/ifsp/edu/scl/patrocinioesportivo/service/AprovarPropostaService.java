package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PermissaoNegadaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;

@Service
public class AprovarPropostaService {

    private final ContratoDePatrocinioRepository repository;

    public AprovarPropostaService(ContratoDePatrocinioRepository repository) {
        this.repository = repository;
    }

    public void aprovar(PerfilUsuario perfil, Long contratoId) {
        if (perfil != PerfilUsuario.DIRETOR_FINANCEIRO) {
            throw new PermissaoNegadaError(
                    "Apenas o diretor financeiro pode aprovar propostas."
            );
        }
        repository.buscarPorId(contratoId)
                .orElseThrow(() ->
                        new ContratoInexistenteError(
                                "Proposta não encontrada."
                        ));
    }
}