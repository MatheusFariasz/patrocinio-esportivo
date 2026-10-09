# Matriz de testes funcionais

Base da implementação: `matheus-branch`, a partir de `8173b6a`.
Todos os testes complementares têm `UnitTest` e `Functional`; os TDD existentes
conservam suas tags. JUnit 5, Mockito e AssertJ seguem o padrão do projeto.

PE = partição de equivalência; VL = valores-limite; TD = tabela de decisão.
As referências de issues ligam os casos às regras existentes e não indicam novos bugs.
MF/BUG são identificadores da matriz/documentação, não números de issues.

## Rastreamento da matriz inicial inteira

| ID | Técnica / regra | Cobertura funcional e encaminhamento |
|---|---|---|
| MF-01 | VL: valor da proposta | SubmeterPropostaServiceFunctionalTest.aceitaPequenosValoresPositivos: valor 0.01, combinado com metas 500/0.01. Inválidos continuam cobertos por TDD #26. |
| MF-02 | VL: período mínimo | PeriodoContratualFunctionalTest.aceitaPeriodoDeUmDia: fim um dia depois; submissão também usa período futuro de um dia. Ausência, igualdade e inversão cobertas por TDD #27/#53. |
| MF-03 | PE/VL: meta | Positivos 0.01 na submissão; MetaContratualFunctionalTest rejeita -0.01, zero e ausência. |
| MF-04 | PE: igualdade dos objetos de valor | MetaContratualFunctionalTest.metasEquivalentesOcupamUmaEntrada: valor 0.01/500 com escalas distintas em conjunto; PeriodoContratualFunctionalTest conserva igualdade e original após criar novos períodos. |
| MF-05 | VL/TD: edição e preservação | EditarPropostaServiceFunctionalTest: rejeições próximas de zero seguidas de edição válida; todos os status não pendentes cruzados com valor inválido. |
| MF-06 | VL: fronteira da meta na renovação | RenovarContratoDePatrocinioServiceFunctionalTest.avaliaLimiteDaMeta: meta 500, exposição 499.99/500/500.01, ATIVO/EM_RISCO, com parcelas pagas. |
| MF-07 | VL: término do período | RenovarContratoDePatrocinioServiceFunctionalTest.avaliaFronteiraDoTermino: ontem/hoje/amanhã, com quitação e meta válidas. |
| MF-08 | VL: duração | RenovarContratoDePatrocinioServiceFunctionalTest.aceitaDuracaoDeUmMes: mínimo positivo em ATIVO/EM_RISCO; ausência/zero/negativo continuam em TDD #66. |
| MF-09 | TD: status, dívida e meta | Todos os resultados de ATIVO/EM_RISCO cobertos na fronteira da meta e em impedeRenovacaoComDivida; status inválidos continuam no TDD #41. Tabela abaixo. |
| MF-10 | PE/TD: proteção das parcelas | BUG-01: testes RED prontos no documento do Matheus; aplicar depois do registro manual da issue, antes da correção. |
| MF-11 | PE: encerramento salvo | BUG-02: RED pronto no documento do Matheus; aguarda issue/confirmação. Leitura real SQLite será validada na integração. |
| MF-12 | PE: cancelamento salvo | BUG-03: RED pronto no documento do Gabriel; aguarda registro/correção. |
| MF-13 | PE: recusa salva | BUG-04: RED pronto no documento do Vinicius; aguarda registro/correção. |
| MF-14 | PE/VL: exposição salva e soma | BUG-05: RED de soma/gravação pronto no documento do Vinicius; aguarda registro/correção. |
| MF-15 | PE: referências às partes | BUG-06: RED de conservação dos IDs pronto no documento do Gabriel; aguarda registro/correção. |
| MF-16 | VL/TD: pagamento/atraso | ParcelaDePagamentoFunctionalTest cobre ontem/hoje/amanhã, paga/não paga. O serviço e os seis cenários do Vinicius serão revisados após integração; esta linha ainda tem essa dependência. |
| MF-17 | Persistência completa | Pendente da implementação SQLite/JDBC. Não é demonstrada pelos mocks dos unitários; verificar gravação e nova leitura com estado completo na etapa 4. |

Assim, toda a matriz está rastreada, mas MF-10 a MF-15 ainda aguardam o ciclo
de bugs, MF-16 aguarda a integração do pagamento e MF-17 aguarda persistência.
Nenhum teste foi desabilitado para esconder essas dependências; os RED estão
documentados para serem aplicados no momento do trabalho de cada bug.

## Decisões da renovação

Pré-condições: contrato existente, período terminado, duração e nova meta válidas.

| Caso | Status | Parcelas quitadas | Meta atingida | Resultado | Cobertura |
|---|---|---|---|---|---|
| R1 | ATIVO | Sim | Sim | Renova e permanece ATIVO | Funcional MF-06; TDD #38 |
| R2 | ATIVO | Sim | Não | Não renova; passa a EM_RISCO | Funcional MF-06; TDD #40 |
| R3 | ATIVO | Não | Sim | Pendência financeira; preserva contrato | Funcional MF-09; TDD #39 |
| R4 | ATIVO | Não | Não | Pendência financeira tem prioridade | Funcional MF-09; TDD #68 |
| R5 | EM_RISCO | Sim | Sim | Renova e volta a ATIVO | Funcional MF-06; TDD #42 |
| R6 | EM_RISCO | Sim | Não | Não renova; permanece EM_RISCO | Funcional MF-06; TDD #69 |
| R7 | EM_RISCO | Não | Sim | Pendência financeira; preserva contrato | Funcional MF-09 |
| R8 | EM_RISCO | Não | Não | Pendência financeira tem prioridade | Funcional MF-09 |
| R9 | Cada status inválido | Indiferente | Indiferente | Transição inválida | TDD #41 |

## Classes e resultados executados

| Classe funcional | Invocações |
|---|---:|
| SubmeterPropostaServiceFunctionalTest | 3 |
| EditarPropostaServiceFunctionalTest | 7 |
| RenovarContratoDePatrocinioServiceFunctionalTest | 15 |
| PeriodoContratualFunctionalTest | 3 |
| MetaContratualFunctionalTest | 5 |
| ParcelaDePagamentoFunctionalTest | 6 |
| Total | 39 |

Resultado das três suítes na revisão: FunctionalSuite 39, TddSuite 131,
UnitTestSuite 170, todas sem falhas, erros ou testes ignorados. As contagens
se sobrepõem; não somar como testes distintos.

```powershell
.\mvnw.cmd -Dtest=FunctionalSuite test
.\mvnw.cmd -Dtest=TddSuite test
.\mvnw.cmd -Dtest=UnitTestSuite test
```

Essas suítes avaliam domínio/serviços unitários. Inicialização Spring,
SQLite e API continuam sendo trabalho posterior de integração.
