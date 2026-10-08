package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.PerfilUsuario;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ClubeRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.PatrocinadorRepository;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SubmeterPropostaService {

    public SubmeterPropostaService(
            ClubeRepository clubeRepository,
            PatrocinadorRepository patrocinadorRepository,
            ContratoDePatrocinioRepository contratoRepository
    ) {
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
        return null;
    }
}