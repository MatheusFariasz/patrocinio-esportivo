
package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PatrocinadorInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ClubeInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ValorInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ClubeObrigatorioError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PatrocinadorObrigatorioError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.PartesIguaisError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaInvalidaError;
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
        if (clubeId == null) {
            throw new ClubeObrigatorioError("O identificador do clube é obrigatório.");
        }

        if (patrocinadorId == null) {
            throw new PatrocinadorObrigatorioError("O identificador do patrocinador é obrigatório.");
        }

        if (clubeId.equals(patrocinadorId)) {
            throw new PartesIguaisError("Clube e patrocinador devem ser partes diferentes.");
        }

        if (!clubeRepository.existePorId(clubeId)) {
            throw new ClubeInexistenteError("Clube não encontrado.");
        }

        if (!patrocinadorRepository.existePorId(patrocinadorId)) {
            throw new PatrocinadorInexistenteError("Patrocinador não encontrado.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoError("O valor do patrocínio deve ser maior que zero.");
        }

        if (inicio == null || termino == null
                || !termino.isAfter(inicio)
                || termino.isBefore(LocalDate.now())) {
            throw new PeriodoInvalidoError("O período contratual é inválido.");
        }

        if (meta == null) {
            throw new MetaObrigatoriaError("A meta contratual é obrigatória.");
        }

        if (meta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MetaInvalidaError("A meta contratual deve ser maior que zero.");
        }

        ContratoDePatrocinio proposta =
                new ContratoDePatrocinio(StatusContrato.PENDENTE);

        return contratoRepository.salvar(proposta);
    }
}
