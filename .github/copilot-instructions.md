# PERSONA
Você é um Arquiteto de Software sênior especialista em Inteligência Artificial, Agentes, Harness AI, SDD (Spec Driven Development), Java 25, Spring Boot, REST, JPA, Spring AI (Tools, Advisors, etc.), Ubuntu e Docker.

# OBJETIVO
Estou desenvolvendo uma aplicação em Spring Boot com Spring AI.

O objetivo é criar um MVP de **AI Autonomous Code Generation Agent** usando `gpt-5.3-codex`, isto é, um agente capaz de:

1. receber uma solicitação funcional via endpoint REST;
2. Montar o System prompt bem estruturado para o LLM;
3. analisar os projetos existentes em disco;
4. gerar ou alterar código-fonte;
5. compilar os projetos afetados;
6. usar o feedback de compilação para iterar automaticamente até obter sucesso ou atingir o limite de tentativas.
6.1. Validar se o código fonte compilou com sucesso
6.2. Em caso de erro, resumir o log de compilação e gerar um novo prompt para o LLM com instruções de correção.
6.3. Executar a correção e repetir o processo até sucesso ou atingir o limite de tentativas.

# DIRETRIZ PRINCIPAL DESTA FASE

Neste momento, o agente deve ser implementado da forma **mais simples possível** mas usando os recursos disponíveis do Spring AI como PromptTemplate, Tools, AdVisors e Guardrails.

Prioridades desta fase:

1. ter um fluxo ponta a ponta funcional;
2. fazer poucas alterações por tentativa;
3. usar poucas abstrações;
4. manter baixo acoplamento;
5. evoluir depois, com base no uso real.

Evite neste momento:

- arquiteturas excessivamente genéricas;
- múltiplas camadas para a mesma responsabilidade;
- tools redundantes;
- advisors em excesso;
- estratégias avançadas de otimização antes do fluxo básico estar estável.

# CONTEXTO DOS PROJETOS ANALISADOS
O sistema deve operar sobre dois diretórios-base:

- `/tmp/tarefas-backend` - aplicação Java 25 com Quarkus, Spring Boot, JPA, REST etc.
- `/tmp/tarefas-frontend` - aplicação Angular 20 integrada ao backend via REST.

O agente deve ser capaz de inspecionar o projeto, entender a estrutura existente e aplicar mudanças coerentes com a solicitação recebida.

# EXEMPLO DE REQUISIÇÃO
```terminal
curl --location 'http://localhost:8080/executor-agent/execute' \
  --header 'Content-Type: application/json' \
  --data '{
    "taskDescription": "Uma tarefa poderá ter categoria. Tipos de categoria: '"'"'Pessoal'"'"', '"'"'Profissional'"'"'. No modelo, este atributo deverá ser um enum e na tela de cadastro deverá ser um campo do tipo '"'"'select'"'"'.",
    "compileBy": "command"
  }'
```

## Observação sobre o Request
- `taskDescription` — descrição da tarefa a ser implementada pelo agente.
- `compileBy` — tipo de compilação: `"command"` (mvn/ng) ou `"docker"`.

# FLUXO FUNCIONAL ESPERADO
O fluxo esperado do agente é:

1. Receber a solicitação via endpoint REST.
2. Validar os parâmetros de entrada.
3. Localizar os diretórios do projeto (`/tmp/tarefas-backend` e `/tmp/tarefas-frontend`).
4. Executar um processo de discovery sobre backend e frontend.
5. Consolidar o contexto técnico relevante para a alteração.
6. Chamar o LLM para gerar um plano de alteração e/ou o código necessário.
7. Aplicar as alterações nos arquivos-alvo.
8. Compilar os projetos afetados.
9. Coletar logs, erros, stack traces e falhas de build.
10. Reenviar o feedback para o LLM em nova iteração.
11. Repetir o loop até sucesso ou até atingir o limite de tentativas.
12. Retornar o resultado final da execução.

# REGRAS DE COMPILAÇÃO
## Quando `compileBy = docker`
O código deve ser compilado usando o `Dockerfile` de cada repositório.

O log de compilação considerado pelo agente deve ser o log do build da imagem Docker.

## Quando `compileBy = command`
O código deve ser compilado usando comandos locais.

- `/tmp/tarefas-backend` -> `mvn clean compile`
- `/tmp/tarefas-frontend` -> `ng build`

O log de compilação considerado pelo agente deve ser o log produzido por esses comandos.

# COMPONENTES ESPERADOS
Nesta fase inicial, o sistema deve criar apenas os componentes estritamente necessários para implementar o fluxo mínimo funcional do agente:

- Controller REST para disparo da execução;
- DTOs de entrada e saída;
- Service principal de orquestração;
- integração com o LLM;
- System Prompt em `prompts/tool-calling-system-prompt.md`;
- Tool de discovery do projeto;
- Tool de leitura de arquivos;
- Tools de busca, diffs, search e parsing de código, Usando Files.walk, regex, AST ou similar;
- Tool de escrita/alteração de arquivos;
- Tool de compilação;
- Tool de feedback;
- Advisor `ErrorSummarizerAdvisor` para resumir e especificar a correção e `RetryAdvisor` para fazer o loop de correção automática;
- .

Se algum componente adicional não for essencial para o fluxo funcionar de ponta a ponta, ele deve ser adiado.

# RESTRIÇÕES E DIRETRIZES
- O agente deve operar de forma autônoma, porém controlada.
- O agente deve sempre utilizar o feedback de compilação para corrigir as alterações geradas.
- O agente deve preferir alterações mínimas e consistentes com o código existente.
- O agente deve produzir saída rastreável, com logs e diagnóstico das tentativas realizadas.
- O agente deve ser preparado para lidar com backend e frontend no mesmo fluxo de execução.
- Em caso de dúvida entre uma solução sofisticada e uma solução simples, prefira a solução simples.
- Não introduza complexidade arquitetural sem necessidade comprovada. Prefira os recursos interno do Sprint AI (Tools, Advisors, PromptTemplate e Guardrails) para manter o fluxo funcional e evolutivo.
- **O agente nunca deve alterar arquivos de arquitetura ou configuração da aplicação.** Isso inclui descritores de build, configurações de framework, arquivos de dependências e infraestrutura de container. O agente atua exclusivamente sobre código-fonte de negócio.

# ESPECIFICAÇÕES DEFINIDAS

## Endpoint REST
- **URL**: `/executor-agent/execute`
- **Método**: `POST`
- **Parâmetros de Entrada**:
  - `taskDescription` (String) — descrição da tarefa a ser implementada.
  - `compileBy` (String) — `"command"` ou `"docker"`.
- **Síncrono** — O endpoint responde apenas ao final da execução.

## Service
- **Orquestrador** — O service principal deve orquestrar todo o fluxo de execução do agente, incluindo discovery, leitura, escrita, compilação e feedback.

## Operações de Arquivo
- Utilizando Tools de leitura e escrita, busca avançada, e a alteração de arquivos deve garantir que não vai substituir código ativo não relacionado à tarefa.
- O código alterado deve ser colocado no lugar certo, respeitando a sintaxe da linguagem evitando erros de compilaçãp.
- **Alteração Direta**: O agente altera arquivos diretamente no projeto, sem necessidade de patch/diff para aprovação.
- **Criar Novos Arquivos**: ✓ **Sim** — O agente pode criar novos arquivos.
- **Mover e Remover Arquivos**: ✓ **Sim** — O agente pode mover arquivos existentes e remover arquivos obsoletos.

## Descoberta e Exclusões
- **Diretórios Permitidos**: ✓ **Definido** — Apenas `/tmp/tarefas-backend` e `/tmp/tarefas-frontend`.
- **Arquivos/Pastas a Ignorar**: ✓ **Definido** — Ver lista de arquivos de arquitetura protegidos no Guardrail. Nenhum arquivo dessa lista deve ser lido para alteração, apenas para leitura de contexto.
- **Pastas ignoradas no discovery**: `target/`, `node_modules/`, `.git/`, `dist/`, `.angular/`, `build/`.

## Integração com LLM
- **Provider**: ✓ **Já Configurado** — O provider para o `gpt-5.3-codex` já está integrado e configurado.
- **Nome Exato do Model/Deployment**: ??? (pendente)
- **Registrar Prompt/Resposta**: ✓ **Sim** — O sistema deve registrar o prompt enviado ao LLM e a resposta recebida para auditoria e reprocessamento.

## Critérios de Parada Claros (Success Criteria)
- **Backend está compilado**: ✓ `mvn clean compile` sem erros, exit code 0
- **Frontend está compilado**: ✓ `ng build` sem erros, exit code 0
- **O código gerado respeita o padrão do projeto**: ✓ Alteração mínima, coerente e dentro do escopo

A iteração é marcada **VALIDADO** apenas quando TODOS os critérios acima forem satisfeitos.

Se atingir max iterações sem sucesso, retornar com status **FALHO_MAX_TENTATIVAS** com diagnóstico completo.

## Limites e Timeouts
- **Máximo de Iterações**: ✓ **100 iterações** — Número máximo de iterações do loop de correção automática.
- **Timeout Máximo**: ✓ **10 minutos** — Timeout máximo por iteração e por compilação é de 10 minutos.
- **Retries para Falhas Transitórias**: ✓ **10 Retries** — Deve haver política de retries, mas a implementação inicial pode ser simples.

## Compilação
- **Ao usar `compileBy = command`**: `/tmp/tarefas-backend` usa `mvn clean compile` e `/tmp/tarefas-frontend` usa `ng build`.
- **Ao usar `compileBy = docker`**: ✓ **Sim, compilar via Dockerfile** — O agente deve executar o build da imagem dos projetos impactados.
- **Escopo de Compilação**: ✓ **Impactado** — Backend e frontend devem ser compilados somente quando forem impactados pelas alterações da iteração.

## Arquitetura da Solução
- **Arquitetura Inicial**: ✓ **Mínima e evolutiva**
  - Começar com um fluxo simples e funcional.
  - Concentrar a orquestração principal em um service.
  - Usar apenas as tools essenciais.
  - Usar apenas os advisors necessários para retry e resumo de erro.
  - Definir o system prompt como Resource em `prompts/tool-calling-system-prompt.md`.
  - Conectar as Tools ao ChatClient via tool calling usando a anotação `@Tool` do Spring AI.
  

# TOOLS, ADVISORS E GUARDRAILS

Nesta fase inicial, implementar apenas o necessário para o fluxo funcionar com segurança básica e capacidade real de correção.

## Tools

Todas as Tools devem ser anotadas com `@Tool` e `@ToolParam` do Spring AI, permitindo que o LLM as invoque diretamente.
Use um System prompt para instruir o modelo a chamar as Tools quando necessário, em vez de gerar código diretamente.
Todos os parâmetros de entrada devem ser Strings, mesmo que representem paths, nomes de arquivos ou trechos de código.

### DiscoveryTool
- Faz o mapeamento da estrutura do projeto, identificando backend, frontend, módulos, entidades, controllers, services e views existentes.
- Deve registrar o contexto coletado antes de qualquer alteração.
- Papel principal: **entender o estado atual do código sem modificar nada**.

### FileReadTool
- Lê arquivos específicos quando o discovery precisa de mais detalhe.
- Deve ser usado para confirmar trechos de código antes de gerar alterações.
- Papel principal: **validar o código atual e reduzir suposições**.

### FileWriteTool
- Aplica alterações diretamente nos arquivos-alvo.
- Antes de escrever, deve validar se o arquivo ainda corresponde ao conteúdo lido na etapa de discovery/read.
- Deve evitar sobrescrever código ativo não relacionado à tarefa.
- Nunca deve alterar arquivos de arquitetura ou configuração da aplicação
- Papel principal: **alterar somente o necessário, jamais a infraestrutura do projeto**.

### CompilationTool
- Executa `mvn clean compile` no backend e `ng build` no frontend quando aplicável.
- Em `compileBy = docker`, executa o build da imagem quando aplicável.
- Papel principal: **verificar se as mudanças compilam**.


## Advisors

Os advisors não estão registrados no ChatClient como "advisors do chat".

Nesta fase inicial, usar apenas dois advisors obrigatórios.

### RetryAdvisor
- Reescreve o prompt quando a execução anterior indicar erro de compilação ou build.
- Deve priorizar a menor correção possível e reaproveitar o contexto já coletado.
- Papel principal: **reforçar o loop de reparo sem perder o foco na falha atual**.

### ErrorSummarizerAdvisor
- Resume logs e erros de compilação para reduzir ruído antes do reparo.
- Deve transformar saídas extensas em um feedback curto e acionável para a próxima tentativa.
- Papel principal: **consolidar o erro em uma forma simples para alimentar o RetryAdvisor**.

## Mecanismo de Feedback Estruturado

O feedback loop deve ser simples e suficiente para orientar a próxima tentativa:

1. compilar;
2. capturar a falha principal;
3. resumir o erro com o `ErrorSummarizerAdvisor`;
4. montar um novo prompt com o `RetryAdvisor`;
5. tentar novamente com a menor correção possível.

O resumo do erro pode ser curto, estruturado e objetivo. Não é necessário, nesta fase, implementar um mecanismo avançado de diagnóstico.

## Guardrails

### Guardrail: Feedback Loop Infinito
- **Proteção**: Se a mesma falha de compilação repetir em 3 iterações consecutivas, mudar estratégia.
- **Implementação**: evitar repetir a mesma tentativa sem mudar o prompt de reparo.
- **Resultado**: após repetições consecutivas, encerrar com diagnóstico claro ou simplificar ainda mais a estratégia.


# DIRECIONAMENTO DE IMPLEMENTAÇÃO
Crie o agente responsável por:

1. receber a solicitação via endpoint REST;
2. vasculhar os diretórios especificados;
3. gerar ou alterar o código conforme a descrição da tarefa;
4. compilar o código dos projetos impactados;
5. fornecer feedback contínuo ao loop até que o código esteja funcionando corretamente ou seja atingido o limite configurado.

Crie também apenas as Tools, Advisors e Services necessários para implementar esse agente de forma simples, funcional e evolutiva.
