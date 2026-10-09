package br.ifsp.edu.scl.patrocinioesportivo.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.*;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PendenciaFinanceiraError;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;

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
    private PeriodoContratual periodoContratual;
    private MetaContratual metaContratual;
    private final List<ParcelaDePagamento> parcelas;
    private final List<HistoricoDePeriodo> historico;
    private BigDecimal multaRescisoria;
    private BigDecimal exposicaoAcumulada;
    private BigDecimal valorTotal;

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
        this.historico = new ArrayList<>();
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
        if (status != StatusContrato.ATIVO) {
            throw new ContratoNaoAtivoError(
                    "Apenas contratos ativos podem receber parcelas."
            );
        }

        if (parcelas.contains(parcela)) {
            throw new ParcelaDuplicadaError(
                    "Já existe uma parcela com o número "
                            + parcela.getNumero() + " neste contrato."
            );
        }

        parcelas.add(parcela);
    }

    public List<ParcelaDePagamento> getParcelas() {
        return List.copyOf(parcelas);
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

    public void editar(BigDecimal novoValor, LocalDate novoInicio, LocalDate novoTermino, BigDecimal novaMeta) {
        if (status != StatusContrato.PENDENTE) {
            throw new TransicaoDeStatusInvalidaError("Propostas já avaliadas não podem ser editadas.");
        }

        if (novoValor == null || novoValor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValorInvalidoError("O valor do patrocínio deve ser maior que zero.");
        }

        PeriodoContratual novoPeriodo = new PeriodoContratual(novoInicio, novoTermino);
        if (novoPeriodo.termino().isBefore(LocalDate.now())) {
            throw new PeriodoInvalidoError("O período contratual é inválido.");
        }
        MetaContratual novaMetaContratual = new MetaContratual(novaMeta);

        valorTotal = novoValor;
        periodoContratual = novoPeriodo;
        metaContratual = novaMetaContratual;
    }

    public void renovar(Integer duracaoMeses, BigDecimal novaMeta) {
        if (status != StatusContrato.ATIVO && status != StatusContrato.EM_RISCO) {
            throw new TransicaoDeStatusInvalidaError("Apenas contratos ativos ou em risco podem ser renovados.");
        }

        if (duracaoMeses == null || duracaoMeses <= 0) {
            throw new PeriodoInvalidoError("A duração do novo período deve ser maior que zero.");
        }

        if (!periodoContratual.termino().isBefore(LocalDate.now())) {
            throw new PeriodoInvalidoError("Não é possível fazer uma renovação antecipada.");
        }

        if (parcelas.stream().anyMatch(parcela -> !parcela.isPaga())) {
            throw new PendenciaFinanceiraError("Existem pendências financeiras no período vigente.");
        }

        if (!metaFoiAtingida()) {
            status = StatusContrato.EM_RISCO;
            return;
        }

        LocalDate novoInicio = periodoContratual.termino().plusDays(1);
        PeriodoContratual novoPeriodo = new PeriodoContratual(novoInicio, novoInicio.plusMonths(duracaoMeses));
        MetaContratual novaMetaContratual = new MetaContratual(novaMeta);
        int ultimoNumero = parcelas.stream().mapToInt(ParcelaDePagamento::getNumero).max().orElse(0);
        List<ParcelaDePagamento> novasParcelas = GeradorDeParcelas.gerar(valorTotal, novoPeriodo, ultimoNumero + 1);

        historico.add(new HistoricoDePeriodo(periodoContratual, metaContratual, exposicaoAcumulada, parcelas));
        periodoContratual = novoPeriodo;
        metaContratual = novaMetaContratual;
        parcelas.clear();
        exposicaoAcumulada = BigDecimal.ZERO;
        status = StatusContrato.ATIVO;
        novasParcelas.forEach(this::adicionarParcela);
    }

    public List<HistoricoDePeriodo> getHistorico() {
        return List.copyOf(historico);
    }

    public void recusar() {
        if (status != StatusContrato.PENDENTE) {
            throw new TransicaoDeStatusInvalidaError(
                    "Apenas propostas pendentes podem ser recusadas."
            );
        }

        status = StatusContrato.RECUSADO;
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
