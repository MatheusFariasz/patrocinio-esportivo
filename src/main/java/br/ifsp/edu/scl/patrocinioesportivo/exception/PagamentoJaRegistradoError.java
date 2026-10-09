package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class PagamentoJaRegistradoError extends RuntimeException {

    public PagamentoJaRegistradoError(String message) {
        super(message);
    }
}