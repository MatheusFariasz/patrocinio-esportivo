
package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PatrocinadorInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ClubeRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.PatrocinadorRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SubmeterPropostaService {

    private final ClubeRepository clubeRepository;
    private final PatrocinadorRepository patrocinadorRepository;
    private final ContratoDePatrocinioRepository contratoRepository;

    public SubmeterPropostaService(
            ClubeRepository clubeRepository,
            PatrocinadorRepository patrocinadorRepository,
            ContratoDePatrocinioRepository contratoRepository
    ) {
        this.clubeRepository = clubeRepository;
        this.patrocinadorRepository = patrocinadorRepository;
        this.contratoRepository = contratoRepository;
    }

    public ContratoDePatrocinio submeter(
            PerfilUsuario perfil,
            Long clubeId,
            Long patrocinadorId,
            BigDecimal valor,
            LocalDate inicio,
            LocalDate termino,
            BigDecimal meta
    ) {
        if (!clubeRepository.existePorId(clubeId)) {
            throw new IllegalArgumentException("Clube não encontrado.");
        }

        if (!patrocinadorRepository.existePorId(patrocinadorId)) {
            throw new PatrocinadorInexistenteError("Patrocinador não encontrado.");
        }

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE);

        return contratoRepository.salvar(proposta);
    }
}
