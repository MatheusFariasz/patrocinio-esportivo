package br.ifsp.edu.scl.patrocinioesportivo.model;

import java.math.BigDecimal;

public class ParcelaDePagamento {

    private final int numero;
    private final BigDecimal valor;
    private boolean paga;

    public ParcelaDePagamento(int numero, BigDecimal valor) {
        this.numero = numero;
        this.valor = valor;
        this.paga = false;
    }

    public void registrarPagamento() {
        this.paga = true;
    }

    public boolean isPaga() {
        return paga;
    }

    public BigDecimal getValor() {
        return valor;
    }
}