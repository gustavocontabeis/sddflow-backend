# Restrições e padrões para Copilot

- Sempre utilize caminhos absolutos para arquivos processados.
- Não sugerir código que ignore validação de entrada.
- Não sugerir código que silencie exceções.
- Siga a estrutura de pacotes: `com.example.springia.scanner`.
- Use Java records para DTOs.
- Implemente utilitários para detecção de linguagem, classificação de tipo e chunking.
- Documente endpoints REST com exemplos de uso (curl) em Javadoc e markdown.
- Ignore diretórios técnicos: `node_modules`, `target`, `dist`, `.git`.
- Considere extensibilidade futura para integração com IA.

Estes padrões garantem qualidade, segurança e alinhamento com os objetivos do projeto.
