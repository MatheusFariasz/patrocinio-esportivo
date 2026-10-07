package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class ContratoInexistenteError extends RuntimeException {

    public ContratoInexistenteError(String message) {
        super(message);
    }
}