package br.ifsp.edu.scl.patrocinioesportivo.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.OperacaoRedundanteError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.TransicaoDeStatusInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.ValorInvalidoError;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ContratoDePatrocinio {

    private static final BigDecimal PERCENTUAL_MULTA =
            new BigDecimal("0.20");

    private Long id;
    private StatusContrato status;
    private final PeriodoContratual periodoContratual;
    private final MetaContratual metaContratual;
    private final List<ParcelaDePagamento> parcelas;
    private BigDecimal multaRescisoria;
    private BigDecimal exposicaoAcumulada;
    private final BigDecimal valorTotal;

    public ContratoDePatrocinio(StatusContrato status) {
        this(status, null, null);
    }

    public ContratoDePatrocinio(
            StatusContrato status,
            PeriodoContratual periodoContratual,
            MetaContratual metaContratual
    ) {
        this(status, periodoContratual, metaContratual, null);
    }

    public ContratoDePatrocinio(
            StatusContrato status,
            PeriodoContratual periodoContratual,
            MetaContratual metaContratual,
            BigDecimal valorTotal
    ) {
        this.periodoContratual = periodoContratual;
        this.metaContratual = metaContratual;
        this.valorTotal = valorTotal;
        this.status = status;
        this.parcelas = new ArrayList<>();
        this.multaRescisoria = BigDecimal.ZERO;
        this.exposicaoAcumulada = BigDecimal.ZERO;
    }

    public static ContratoDePatrocinio reconstituir(
            StatusContrato status,
            PeriodoContratual periodoContratual,
            MetaContratual metaContratual,
            BigDecimal valorTotal,
            List<ParcelaDePagamento> parcelas
    ) {
        ContratoDePatrocinio contrato = new ContratoDePatrocinio(
                status,
                periodoContratual,
                metaContratual,
                valorTotal
        );

        contrato.parcelas.addAll(parcelas);

        return contrato;
    }

    public void adicionarParcela(ParcelaDePagamento parcela) {
        parcelas.add(parcela);
    }

    public List<ParcelaDePagamento> getParcelas() {
        return parcelas;
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

        if (status != StatusContrato.ATIVO
                && status != StatusContrato.EM_RISCO) {
            throw new TransicaoDeStatusInvalidaError(
                    "Apenas contratos ativos ou em risco podem ser encerrados."
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

    public void registrarExposicao(BigDecimal valor) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoError(
                    "O valor da exposição deve ser maior que zero."
            );
        }

        if (status != StatusContrato.ATIVO
                && status != StatusContrato.EM_RISCO) {
            throw new TransicaoDeStatusInvalidaError(
                    "O contrato não permite registrar exposição midiática."
            );
        }

        exposicaoAcumulada = exposicaoAcumulada.add(valor);
    }

    public void aprovar() {
        if (status != StatusContrato.PENDENTE) {
            throw new TransicaoDeStatusInvalidaError(
                    "Apenas propostas pendentes podem ser aprovadas."
            );
        }

        status = StatusContrato.ATIVO;

        GeradorDeParcelas.gerar(valorTotal, periodoContratual)
                .forEach(this::adicionarParcela);
    }

    public BigDecimal getExposicaoAcumulada() {
        return exposicaoAcumulada;
    }

    public boolean metaFoiAtingida() {
        return exposicaoAcumulada.compareTo(metaContratual.valor()) >= 0;
    }

    public PeriodoContratual getPeriodoContratual() {
        return periodoContratual;
    }

    public MetaContratual getMetaContratual() {
        return metaContratual;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public BigDecimal getMultaRescisoria() {
        return multaRescisoria;
    }

    public void definirId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

}