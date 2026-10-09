package br.ifsp.edu.scl.patrocinioesportivo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class ParcelaDePagamento {

    private final int numero;
    private final BigDecimal valor;
    private final LocalDate vencimento;
    private boolean paga;

    public ParcelaDePagamento(int numero, BigDecimal valor) {
        this(numero, valor, null);
    }

    public ParcelaDePagamento(int numero, BigDecimal valor, LocalDate vencimento) {
        this.numero = numero;
        this.valor = valor;
        this.vencimento = vencimento;
        this.paga = false;
    }

    public void registrarPagamento() {
        this.paga = true;
    }

    public boolean isPaga() {
        return paga;
    }

    public int getNumero() {
        return numero;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ParcelaDePagamento that = (ParcelaDePagamento) o;
        return numero == that.numero;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(numero);
    }

    public boolean isEmAtraso(LocalDate dataReferencia) {
        return !paga
                && vencimento != null
                && vencimento.isBefore(dataReferencia);
    }
}