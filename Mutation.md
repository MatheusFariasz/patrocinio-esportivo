# Testes de mutação

Execução realizada em **06/11/2026**, com Java 21 e Pitest 1.18.1, usando os mutadores padrão em todas as classes do projeto, sem exclusão de pacotes.

| Resultado | Quantidade |
|---|---:|
| Mutantes gerados | 233 |
| Mutantes mortos | 233 |
| Mutantes sobreviventes | 0 |
| Mutantes sem cobertura | 0 |
| Mutation coverage | 100% |
| Test strength | 100% |

## Mutantes equivalentes

Não restaram mutantes considerados equivalentes nesta execução: todos os 233 mutantes foram mortos pelos testes. Portanto, não há classes, linhas ou IDs de equivalentes para registrar nas tabelas solicitadas pela atividade.

Os testes de mutação estão identificados com `@Tag("UnitTest")` e `@Tag("Mutation")` nas classes `MatheusMutationTest`, `GabrielMutationTest` e `ViniciusMutationTest`.

## Como reproduzir

Na raiz do projeto, com Java 21:

```powershell
.\mvnw.cmd clean test org.pitest:pitest-maven:mutationCoverage "-DoutputFormats=HTML,XML" "-DreportsDirectory=target/pit-reports-atual" "-Dthreads=2"
```

O relatório HTML fica em `target/pit-reports-atual/index.html`, e os dados dos mutantes em `target/pit-reports-atual/mutations.xml`.

Esses resultados correspondem à configuração e ao código analisados nessa execução. Alterações no projeto ou nos mutadores exigem uma nova análise.
