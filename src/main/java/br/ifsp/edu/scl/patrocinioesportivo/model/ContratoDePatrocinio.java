package br.ifsp.edu.scl.patrocinioesportivo.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContratoDePatrocinio {

    private static final BigDecimal PERCENTUAL_MULTA =
            new BigDecimal("0.20");

    private StatusContrato status;
    private final List<ParcelaDePagamento> parcelas;
    private BigDecimal multaRescisoria;

    public ContratoDePatrocinio(StatusContrato status) {
        this.status = status;
        this.parcelas = new ArrayList<>();
        this.multaRescisoria = BigDecimal.ZERO;
    }

    public void adicionarParcela(ParcelaDePagamento parcela) {
        parcelas.add(parcela);
    }

    public void cancelar() {
        if (status == StatusContrato.CANCELADO) {
            throw new OperacaoRedundanteError(
                    "A proposta já está cancelada."
            );
        }

        if (status != StatusContrato.PENDENTE) {
            throw new TransicaoDeStatusInvalidaError(
                    "Apenas propostas pendentes podem ser canceladas."
            );
        }

        status = StatusContrato.CANCELADO;
    }

    public void encerrar() {
        if (status == StatusContrato.ENCERRADO) {
            throw new OperacaoRedundanteError(
                    "O contrato já está encerrado."
            );
        }

        if (status != StatusContrato.ATIVO) {
            throw new TransicaoDeStatusInvalidaError(
                    "Apenas contratos ativos podem ser encerrados."
            );
        }

        BigDecimal valorParcelasPendentes = parcelas.stream()
                .filter(parcela -> !parcela.isPaga())
                .map(ParcelaDePagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        multaRescisoria = valorParcelasPendentes
                .multiply(PERCENTUAL_MULTA);

        status = StatusContrato.ENCERRADO;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public BigDecimal getMultaRescisoria() {
        return multaRescisoria;
    }
}