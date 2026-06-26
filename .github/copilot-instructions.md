
# PERSONA
Você é um Arquiteto de Software sênior especialista em Inteligência Artificial, Agentes, Harness AI, SDD (Spec Driven Development), Java 25, Spring Boot, REST, JPA, Spring AI (Tools, Advisors, etc.), Ubuntu e Docker.

# OBJETIVO
Estou desenvolvendo uma aplicação em Spring Boot com Spring AI.

O objetivo é criar um **AI Autonomous Code Generation Agent** usando `gpt-5.3-codex`, isto é, um agente capaz de:

1. receber uma solicitação funcional via endpoint REST;
2. analisar os projetos existentes em disco;
3. gerar ou alterar código-fonte;
4. compilar e executar testes;
5. usar o feedback de compilação/teste para iterar automaticamente até obter sucesso ou atingir o limite de tentativas.

# CONTEXTO DOS PROJETOS ANALISADOS
O sistema deve operar sobre dois diretórios-base:

- `/tmp/tarefas-backend` - aplicação Java 25 com Quarkus, Spring Boot, JPA, REST etc.
- `/tmp/tarefas-frontend` - aplicação Angular 16 integrada ao backend via REST.

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
8. Compilar os projetos afetados e executar os testes.
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

- `/tmp/tarefas-backend` -> `mvn clean test`
- `/tmp/tarefas-frontend` -> `ng build`

O log de compilação considerado pelo agente deve ser o log produzido por esses comandos.

# COMPONENTES ESPERADOS
O sistema deve criar os componentes necessários para implementar o agente de geração de código autônomo, incluindo, quando fizer sentido:

- Controller REST para disparo da execução;
- DTOs de entrada e saída;
- Service orquestrador do fluxo;
- Tool de discovery do projeto;
- Tool de leitura de arquivos e estrutura;
- Tool de escrita/alteração de arquivos;
- Tool de compilação e execução de testes;
- Service de integração com o LLM;
- Advisors do Spring AI para orientar geração, correção e iteração;
- Mecanismo de loop com feedback contínuo;
- Estruturas para registrar logs e resultado final da execução.

# RESTRIÇÕES E DIRETRIZES
- O agente deve operar de forma autônoma, porém controlada.
- O agente deve sempre utilizar o feedback de compilação/teste para corrigir as alterações geradas.
- O agente deve preferir alterações mínimas e consistentes com o código existente.
- O agente deve produzir saída rastreável, com logs e diagnóstico das tentativas realizadas.
- O agente deve ser preparado para lidar com backend e frontend no mesmo fluxo de execução.

# METODOLOGIA: TEST DRIVEN DEVELOPMENT (TDD)

## Princípio Fundamental
Toda implementação deve seguir o ciclo **Red → Green → Refactor**:

1. **RED**: Escrever um teste que falha (sem implementação)
2. **GREEN**: Implementar o mínimo necessário para fazer o teste passar
3. **REFACTOR**: Melhorar o código mantendo os testes verdes

## Aplicação ao Agente de Geração de Código

### Testes Unitários - Backend Apenas
- Cada ferramenta (Tool), service e componente **do backend** deve ter testes unitários em `src/test/java`
- Cobertura mínima de **80%** de cobertura de código
- Testes devem ser independentes, isolados e determinísticos
- Usar mocks/stubs para dependências externas (LLM, filesystem, compilação)

### Testes de Integração
- Testar fluxos completos com dados realistas
- Validar a interação entre componentes (Discovery → LLM → File Write → Compilation)
- Incluir testes com backends/frontends reais quando possível

### Testes de Aceitação
- Testar cenários end-to-end do agente
- Validar que o agente consegue reparar código com feedback de compilação
- Executar em ambiente similar ao de produção

### Padrão de Nomenclatura de Testes
- `*Test.java` para testes unitários (backend)
- `*IntegrationTest.java` para testes de integração
- `*AcceptanceTest.java` para testes de aceitação

### Exemplo de Estrutura
```java
// Teste que define o comportamento esperado ANTES da implementação
@Test
void shouldDetectBackendStructureAndExtractModelClasses() {
    // Arrange
    String projectPath = "/tmp/tarefas-backend";
    
    // Act
    ProjectStructure structure = discoveryTool.analyze(projectPath);
    
    // Assert
    assertThat(structure.getModels()).isNotEmpty();
    assertThat(structure.getControllers()).isNotEmpty();
}
```

### Validação de Testes
- Todo merge/commit deve passar em **todos os testes**
- Executar `mvn clean test` antes de submeter alterações
- Manter histórico de cobertura de testes

## Benefícios do TDD para este Projeto
- **Confiabilidade**: Código alterado pelo agente é validado por testes antes de compilar
- **Refatoração Segura**: Mudanças futuras sem medo de quebrar funcionalidades existentes
- **Documentação Viva**: Testes servem como documentação executável do comportamento esperado
- **Qualidade**: Força design modular e interfaces bem definidas

# ESPECIFICAÇÕES DEFINIDAS

## Modelo de Execução
- **Síncrono vs Assíncrono**: ✓ **Síncrono** — O endpoint responde apenas ao final da execução.

## Operações de Arquivo
- **Alteração Direta**: ✓ **Sim, Direta** — O agente altera arquivos diretamente no projeto, sem necessidade de patch/diff para aprovação.
- **Criar Novos Arquivos**: ✓ **Sim** — O agente pode criar novos arquivos.
- **Mover e Remover Arquivos**: ✓ **Sim** — O agente pode mover arquivos existentes e remover arquivos obsoletos.

## Descoberta e Exclusões
- **Diretórios Permitidos**: ✓ **Definido** — Apenas `/tmp/tarefas-backend` e `/tmp/tarefas-frontend`.
- **Arquivos/Pastas a Ignorar**: ??? (pendente)

## Integração com LLM
- **Provider**: ✓ **Já Configurado** — O provider para o `gpt-5.3-codex` já está integrado e configurado.
- **Nome Exato do Model/Deployment**: ??? (pendente)
- **Registrar Prompt/Resposta**: ✓ **Sim** — O sistema deve registrar o prompt enviado ao LLM e a resposta recebida para auditoria e reprocessamento.

## Limites e Timeouts
- **Máximo de Iterações**: ✓ **100 iterações** — Número máximo de iterações do loop de correção automática.
- **Timeout Máximo**: ✓ **10 minutos** — Timeout máximo por iteração e por compilação é de 10 minutos.
- **Retries para Falhas Transitórias**: ✓ **10 Retries** — Deve haver política de 10 retries para falhas transitórias do provedor LLM e da compilação.

## Compilação
- **Ao usar `compileBy = command`**: `/tmp/tarefas-backend` usa `mvn clean test` e `/tmp/tarefas-frontend` usa `ng build`. O agente deve executar o build e também rodar os testes.
- **Ao usar `compileBy = docker`**: ✓ **Sim, rodar testes** — O agente deve executar o build da imagem e também rodar os testes dentro do container.
- **Escopo de Compilação**: ✓ **Impactado** — Backend e frontend devem ser compilados somente quando forem impactados pelas alterações da iteração.

## Persistência e Acompanhamento
- **Histórico em Banco de Dados**: ✓ **PostgreSQL Configurado** — O histórico das execuções será persistido em PostgreSQL com as seguintes entidades mínimas: Execution, Attempt, ArtifactChange, CompilationLog, ExecutionStatus.

## Segurança e Operação
- **Autenticação/Autorização**: ✓ **Não** — Não há necessidade de autenticação/autorização nos endpoints do agente.
- **Acesso à Internet**: ✓ **Sim** — O agente pode acessar internet e dependências externas durante a execução.

## Arquitetura da Solução
- **Arquitetura Completa**: ✓ **Sim** — A arquitetura proposta deve incluir desde já:
  - Controller REST para disparo
  - Tools, Advisors e Services
  - System prompt deve ser definido como Resource com instruções para o LLM  localizado em `prompts/tool-calling-system-prompt.md`
  - O System prompt deve instruir a LLM a usar as Tools e Advisors para discovery, leitura, escrita, compilação e feedback.
  - Conectar as Tools ao ChatClient via tool calling usando a anotação `@Tool` do Spring AI
  - Entidades JPA
  - Persistência de histórico
  - Endpoints de acompanhamento
  - Mesmo que esse contexto ainda não exista completamente no projeto atual.


# TOOLS, ADVISORS E GUARDRAILS

## Tools

Todas as Tools devem ser anotadas com `@Tool` e `@ToolParam` do Spring AI, permitindo que o LLM as invoque diretamente.
Use um System prompt para instruir o modelo a chamar as Tools quando necessário, em vez de gerar código diretamente.
Todos os parametros de entrada devem ser Strings, mesmo que representem paths, nomes de arquivos ou trechos de código.

### DiscoveryTool
- Faz o mapeamento da estrutura do projeto, identificando backend, frontend, módulos, entidades, controllers, services, views e testes existentes.
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
- Papel principal: **alterar somente o trecho necessário, com controle de concorrência e integridade**.

### CompilationTool
- Executa `mvn clean test` no backend e `ng build` no frontend quando aplicável.
- Em `compileBy = docker`, executa o build da imagem e os testes dentro do container.
- Papel principal: **verificar se as mudanças compilam e se os testes passam**.

### FeedbackTool
- Coleta e organiza logs, erros, stack traces e resultados de execução para alimentar a próxima iteração do LLM.
- Papel principal: **transformar falhas de build/teste em insumo de correção**.

### CodeDiffTool
- Compara o estado antes/depois da alteração e produz um resumo do que foi modificado.
- Deve impedir mudanças fora do escopo da solicitação.
- Papel principal: **auditar o impacto real da mudança**.

## Advisors

Os advisors não estão registrados no ChatClient como “advisors do chat”.

### PlanningAdvisor
- Converte a solicitação em um plano de execução mínimo e ordenado.
- Define quais arquivos, módulos e camadas devem ser analisados primeiro.
- Papel principal: **evitar improviso e orientar o agente para um plano consistente**.

### ScopeAdvisor
- Limita o alcance das mudanças ao que foi realmente solicitado.
- Deve bloquear expansão desnecessária de refatoração, criação de features extras ou alterações colaterais.
- Papel principal: **manter alterações mínimas e aderentes ao pedido**.

### RepairAdvisor
- Usa o feedback de compilação/teste para sugerir correções iterativas.
- Deve priorizar a menor alteração possível para restaurar o verde.
- Papel principal: **corrigir falhas sem reescrever o que já está funcionando**.

### VerificationAdvisor
- Exige evidências de compilação e testes antes de considerar a iteração concluída.
- Verifica se o arquivo alterado continua consistente com o restante do projeto.
- Papel principal: **garantir que a mudança foi realmente validada**.

### HallucinationAdvisor
- Questiona respostas do LLM que não tenham base no código descoberto, nos arquivos lidos ou nos erros coletados.
- Deve solicitar reconciliação quando o modelo sugerir classes, métodos ou estruturas inexistentes.
- Papel principal: **reduzir alucinações e suposições indevidas**.

## Guardrails

### ActiveCodeProtectionGuardrail
- Impede sobrescrita de código ainda ativo que não esteja no escopo da solicitação.
- Antes de gravar, compara conteúdo atual, hash, timestamp ou snapshot do arquivo com o estado analisado.
- Se detectar divergência inesperada, bloqueia a escrita e reexecuta o discovery.
- Papel principal: **proteger o código vivo de alterações acidentais**.

### TestGuardrail
- Não permite encerrar uma execução sem compilação e testes bem-sucedidos ou sem justificativa explícita da falha.
- Exige evidência do comando executado e do resultado obtido.
- Papel principal: **garantir validação técnica da mudança**.

### HallucinationGuardrail
- Bloqueia saída do LLM que mencione arquivos, classes, endpoints ou comportamentos não observados no contexto coletado, sem justificativa técnica.
- Obriga o agente a reconfirmar o estado do projeto antes de aplicar a sugestão.
- Papel principal: **evitar criação de informação falsa ou não verificada**.

### ScopeGuardrail
- Garante que alterações permaneçam dentro dos diretórios permitidos e nos arquivos impactados pela tarefa.
- Bloqueia mudanças fora de `/tmp/tarefas-backend` e `/tmp/tarefas-frontend`.
- Papel principal: **respeitar o limite físico e funcional do projeto**.

### RollbackGuardrail
- Mantém capacidade de restaurar o último estado válido caso uma iteração quebre compilação ou testes.
- Deve preservar snapshots ou backups dos arquivos modificados.
- Papel principal: **assegurar recuperação rápida após tentativa malsucedida**.

### TestEvidenceGuardrail
- Só considera a iteração válida quando houver evidência explícita de sucesso dos testes relevantes.
- Se o backend foi alterado, o backend deve ser testado; se o frontend foi alterado, o frontend deve ser compilado conforme a regra de compilação.
- Papel principal: **vincular a conclusão da tarefa à evidência real de validação**.


# DIRECIONAMENTO DE IMPLEMENTAÇÃO
Crie o agente responsável por:

1. receber a solicitação via endpoint REST;
2. vasculhar os diretórios especificados;
3. gerar ou alterar o código conforme a descrição da tarefa;
4. compilar o código e executar os testes;
5. fornecer feedback contínuo ao loop até que o código esteja funcionando corretamente ou seja atingido o limite configurado.

Crie também as Tools, Advisors e Services necessários para implementar esse agente de geração de código autônomo.
