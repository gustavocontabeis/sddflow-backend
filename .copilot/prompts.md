# Exemplos de prompts para Copilot

Estes prompts podem ser usados para orientar o Copilot a gerar código alinhado com as necessidades do projeto.

## Prompt para scanner de repositório
Implemente um serviço Java chamado `RepositoryScannerService` que:
- Escaneie recursivamente um diretório informado.
- Ignore os diretórios: `node_modules`, `target`, `dist`, `.git`.
- Processe apenas arquivos com extensões: `.java`, `.ts`, `.js`, `.html`, `.json`, `.xml`, `.yml`, `.yaml`.
- Leia o conteúdo de cada arquivo.
- Detecte a linguagem pelo sufixo do arquivo.
- Classifique o tipo do arquivo pelo conteúdo (`@RestController`, `@Service`, `@Repository`, `@Entity`).
- Quebre o conteúdo em chunks de até 2000 caracteres.
- Retorne uma lista de records `CodeFile` com caminho absoluto, linguagem, conteúdo e tipo.

## Prompt para descoberta de stack
Implemente um serviço que, a partir da lista de `CodeFile`, descubra a stack da aplicação (linguagem, frameworks, bibliotecas, bancos de dados) e retorne um record `StackDiscoveryReport`.

## Prompt para documentação de endpoint
Inclua exemplos de uso com curl no Javadoc dos endpoints REST implementados.

---

Adapte os prompts conforme a evolução do projeto e das necessidades de automação.
