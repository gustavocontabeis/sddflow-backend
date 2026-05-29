# Instruções para o GitHub Copilot

Este projeto utiliza um serviço de scanner de repositório para identificar arquivos de código, linguagens, tipos de componentes (controller, service, repository, entity) e realizar chunking do conteúdo para uso posterior por IA.

## Objetivo
- Gerar uma lista estruturada de chunks de código para documentação automatizada e análise de stack.
- Descobrir a stack da aplicação (linguagem, frameworks, bibliotecas, bancos de dados, etc).

## Boas práticas para sugestões do Copilot
- Siga a estrutura de pacotes: `com.example.springia.scanner` (service, model, util).
- Use Java records para DTOs.
- Sempre utilize caminhos absolutos para arquivos.
- Implemente validações robustas e tratamento explícito de erros.
- Utilize utilitários para detecção de linguagem, classificação de tipo e chunking.
- Documente endpoints REST com exemplos de uso (curl) em Javadoc e markdown.
- Ignore diretórios técnicos: `node_modules`, `target`, `dist`, `.git`.
- Considere extensibilidade futura para integração com IA.

## Exemplos de arquivos relevantes
- `src/main/java/com/example/springia/scanner/model/CodeFile.java`
- `src/main/java/com/example/springia/scanner/service/RepositoryScannerService.java`
- `src/main/java/com/example/springia/scanner/util/Chunker.java`
- `src/main/java/com/example/springia/scanner/util/LanguageDetector.java`
- `src/main/java/com/example/springia/scanner/util/CodeTypeClassifier.java`
- `src/main/java/com/example/springia/scanner/model/StackDiscoveryReport.java`
- `src/main/java/com/example/springia/scanner/service/StackDiscoveryService.java`
- `src/main/java/com/example/springia/scanner/controller/RepositoryScannerController.java`

## Restrições
- Não sugerir código que utilize caminhos relativos para arquivos processados.
- Não sugerir código que ignore validação de entrada.
- Não sugerir código que silencie exceções.
- Não sugerir código fora dos padrões de organização do projeto.

## Exemplo de prompt para Copilot
> "Implemente um serviço que escaneie recursivamente um diretório, ignore pastas técnicas, detecte linguagem e tipo de arquivo, quebre o conteúdo em chunks de até 2000 caracteres e retorne uma lista de records CodeFile com caminho absoluto, linguagem, conteúdo e tipo."

---

Estas instruções visam garantir que o Copilot gere sugestões alinhadas com os padrões, objetivos e arquitetura deste projeto.
