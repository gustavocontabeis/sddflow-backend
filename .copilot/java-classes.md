# Instruções específicas para classes Java

Estas instruções se aplicam a todas as classes Java deste projeto.

## Padrões obrigatórios
- Utilize records para DTOs sempre que possível.
- Siga a estrutura de pacotes: `com.example.springia.scanner` (service, model, util).
- Implemente validação de entrada em métodos públicos.
- Não silencie exceções: trate-as explicitamente ou propague-as.
- Documente classes e métodos públicos com Javadoc, incluindo exemplos de uso (curl para endpoints REST).
- Utilize caminhos absolutos para arquivos processados.
- Ignore diretórios técnicos ao manipular arquivos: `node_modules`, `target`, `dist`, `.git`.
- Use utilitários para detecção de linguagem, classificação de tipo e chunking de conteúdo.
- Mantenha o código limpo, organizado e pronto para evolução (ex: integração futura com IA).

## Restrições
- Não utilize caminhos relativos para arquivos processados.
- Não ignore validação de entrada.
- Não silencie exceções.
- Não fuja dos padrões de organização do projeto.

## Exemplo de anotação para Copilot
```java
// copilot:aplicar-instrucoes
```

Inclua este comentário no início de cada classe Java para indicar que as instruções do Copilot devem ser aplicadas.

---

Estas orientações garantem que todas as classes Java estejam alinhadas com os padrões e objetivos do projeto.
