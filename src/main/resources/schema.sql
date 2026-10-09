CREATE TABLE IF NOT EXISTS app_user (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    lastname TEXT NOT NULL,
    email TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX IF NOT EXISTS app_user_email_unique ON app_user (email);

CREATE TABLE IF NOT EXISTS clube (
    id INTEGER PRIMARY KEY,
    nome TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS patrocinador (
    id INTEGER PRIMARY KEY,
    nome TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS contrato_patrocinio (
    id INTEGER PRIMARY KEY,
    clube_id INTEGER NOT NULL REFERENCES clube(id),
    patrocinador_id INTEGER NOT NULL REFERENCES patrocinador(id),
    status TEXT NOT NULL CHECK (status IN ('PENDENTE', 'ATIVO', 'EM_RISCO', 'ENCERRADO', 'CANCELADO', 'RECUSADO')),
    inicio TEXT NOT NULL,
    termino TEXT NOT NULL,
    meta TEXT NOT NULL,
    valor_total TEXT NOT NULL,
    exposicao_acumulada TEXT NOT NULL,
    multa_rescisoria TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS historico_periodo (
    contrato_id INTEGER NOT NULL REFERENCES contrato_patrocinio(id) ON DELETE CASCADE,
    ciclo INTEGER NOT NULL CHECK (ciclo > 0),
    inicio TEXT NOT NULL,
    termino TEXT NOT NULL,
    meta TEXT NOT NULL,
    exposicao_acumulada TEXT NOT NULL,
    PRIMARY KEY (contrato_id, ciclo)
);

CREATE TABLE IF NOT EXISTS parcela_pagamento (
    contrato_id INTEGER NOT NULL REFERENCES contrato_patrocinio(id) ON DELETE CASCADE,
    ciclo INTEGER NOT NULL CHECK (ciclo >= 0),
    numero INTEGER NOT NULL CHECK (numero > 0),
    valor TEXT NOT NULL,
    vencimento TEXT,
    paga INTEGER NOT NULL CHECK (paga IN (0, 1)),
    data_pagamento TEXT,
    PRIMARY KEY (contrato_id, ciclo, numero)
);
