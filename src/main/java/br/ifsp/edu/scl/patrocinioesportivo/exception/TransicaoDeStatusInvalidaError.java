package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class TransicaoDeStatusInvalidaError extends RuntimeException {
    public TransicaoDeStatusInvalidaError(String message) {
        super(message);
    }
}