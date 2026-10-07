package br.ifsp.edu.scl.patrocinioesportivo.repository;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;

import java.util.Optional;

public interface ContratoDePatrocinioRepository {

    Optional<ContratoDePatrocinio> buscarPorId(Long id);
}