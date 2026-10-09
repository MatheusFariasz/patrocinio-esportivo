package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class ParcelaInexistenteError extends RuntimeException {

    public ParcelaInexistenteError(String mensagem) {
        super(mensagem);
    }
}