package br.ifsp.edu.scl.patrocinioesportivo.service;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.IdentificacaoObrigatoriaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ParcelaInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class RegistrarPagamentoService {

    private final ContratoDePatrocinioRepository repository;

    public RegistrarPagamentoService(
            ContratoDePatrocinioRepository repository
    ) {
        this.repository = repository;
    }

    public void registrar(Long contratoId, int numeroParcela) {
        if (contratoId == null) {
            throw new IdentificacaoObrigatoriaError("A identificação do contrato é obrigatória.");
        }

        ContratoDePatrocinio contrato =
                repository.buscarPorId(contratoId)
                        .orElseThrow(() ->
                                new ContratoInexistenteError(
                                        "Contrato não encontrado."
                                ));

        if (contrato.getStatus() != StatusContrato.ATIVO
                && contrato.getStatus() != StatusContrato.EM_RISCO) {
            throw new TransicaoDeStatusInvalidaError(
                    "O contrato não permite registrar pagamentos."
            );
        }

        ParcelaDePagamento parcela = contrato.getParcelas().stream()
                .filter(item -> item.getNumero() == numeroParcela)
                .findFirst()
                .orElseThrow(() ->
                        new ParcelaInexistenteError(
                                "Parcela não encontrada no contrato."
                        ));

        parcela.registrarPagamento(LocalDate.now());

        repository.salvar(contrato);
    }
}

