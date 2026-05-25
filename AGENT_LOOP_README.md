# Agent Loop com ReAct Pattern - Guia de Uso

Implementação completa de um **Agent Loop** que segue o padrão **ReAct** (Reasoning + Acting) para executar tarefas do SDD (Spec Driven Development) com geração de código real no filesystem.

## 📋 Arquivos Implementados

### 1. **Ferramentas (Tools)**
- `src/main/java/com/example/springia/agent/tool/Tool.java` - Interface base
- `src/main/java/com/example/springia/agent/tool/CreateFileTool.java` - Cria arquivos
- `src/main/java/com/example/springia/agent/tool/ReadFileTool.java` - Lê arquivos
- `src/main/java/com/example/springia/agent/tool/CreateDirectoryTool.java` - Cria diretórios
- `src/main/java/com/example/springia/agent/tool/ExecuteCommandTool.java` - Executa comandos shell
- `src/main/java/com/example/springia/agent/tool/ListFilesTool.java` - Lista arquivos/direrórios
- `src/main/java/com/example/springia/agent/tool/ToolRegistry.java` - Registro de ferramentas

### 2. **Agent Loop (ReAct)**
- `src/main/java/com/example/springia/agent/loop/AgentStep.java` - Representa um passo do agent
- `src/main/java/com/example/springia/agent/loop/AgentExecution.java` - Resultado completo da execução
- `src/main/java/com/example/springia/agent/loop/AgentLoop.java` - **Núcleo do ReAct Pattern**

### 3. **Serviços**
- `src/main/java/com/example/springia/service/ExecutorAgentService.java` - Orquestra o agent
- `src/main/java/com/example/springia/service/SddTaskExecutorService.java` - Integra com SDD (Spec+Plan+Task)

### 4. **Controllers**
- `src/main/java/com/example/springia/controller/ExecutorAgentController.java` - Endpoints genéricos
- `src/main/java/com/example/springia/controller/SddTaskExecutorController.java` - Endpoints SDD

### 5. **DTOs**
- `src/main/java/com/example/springia/dto/ExecutorAgentRequest.java` - Request de execução
- `src/main/java/com/example/springia/dto/ExecutorAgentResponse.java` - Response com detalhes

---

## 🚀 Como Usar

### **Opção 1: Executar Tarefa Simples**

```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie um arquivo Hello.java em src/main/java/com/example com um programa Hello World"
  }'
```

**Resposta:**
```json
{
  "executionId": "uuid-xxx",
  "finalAnswer": "Arquivo Hello.java criado com sucesso em src/main/java/com/example/Hello.java",
  "stepCount": 3,
  "status": "SUCCESS",
  "totalExecutionTimeMs": 2450,
  "steps": [
    {
      "stepNumber": 1,
      "thinking": "Preciso criar um arquivo Hello.java...",
      "toolName": "create_directory",
      "toolResult": "Diretório criado com sucesso",
      "isFinal": false
    },
    {
      "stepNumber": 2,
      "toolName": "create_file",
      "toolResult": "Arquivo criado com sucesso"
    },
    {
      "stepNumber": 3,
      "isFinal": true,
      "finalAnswer": "Tarefa concluída com sucesso"
    }
  ]
}
```

---

### **Opção 2: Executar TaskSdd com Contexto Completo (Spec + Plan + Task)**

Este é o fluxo principal do SDD. Ele carrega toda a documentação e executa a tarefa com contexto total.

```bash
# Usando TaskSdd ID
curl -X POST http://localhost:8080/sdd-executor/execute-task/1

# Usando UserStory (busca TaskSdd associada)
curl -X POST http://localhost:8080/sdd-executor/execute-userstory/1
```

---

### **Opção 3: Visualizar Contexto (Preview)**

Veja exatamente o que será enviado ao agent sem executar:

```bash
curl -X GET http://localhost:8080/sdd-executor/preview/1
```

Exemplo de saída:
```
# CONTEXTO DE EXECUÇÃO DO SDD

## UserStory
Conteúdo: Sistema de gerenciamento de tarefas...

## ESPECIFICAÇÃO (Spec.md)
# Especificação Funcional
...conteúdo da spec...

## PLANO DE IMPLEMENTAÇÃO (Plan.md)
# Plano de Implementação
...conteúdo do plan...

## TAREFA A EXECUTAR (Task.md)
# Tarefa 1: Criar estrutura de arquivos
- Crie pacote com/example/task
- Crie interface Task.java
- Crie classe TaskImpl.java
...
```

---

### **Opção 4: Ver Ferramentas Disponíveis**

```bash
curl -X GET http://localhost:8080/executor-agent/tools
```

---

## 🔄 Fluxo do ReAct Pattern

```
┌──────────────────┐
│   INPUT (Task)   │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────┐
│ LLM THINKING             │
│ "Preciso executar..."    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ LLM DECISION (ACTION)    │
│ Tool: create_file        │
│ Params: {...}            │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ TOOL EXECUTION           │
│ Resultado: "OK"          │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ OBSERVATION              │
│ Analisa resultado        │
└────────┬─────────────────┘
         │
         ▼
    Mais ações?
    ┌────────────┐
    │ SIM ► próximo step (volta ao THINKING)
    │ NÃO ► FINALIZAR
    └────────────┘
         │
         ▼
┌──────────────────────────┐
│ FINAL ANSWER             │
│ "Tarefa completada!"     │
└──────────────────────────┘
```

---

## 💻 Exemplo Completo de Fluxo

### 1. Criar um Banco de Dados com:
   - Projeto
   - ConversationSession
   - UserStory
   - SpecSdd (com especificação funcional)
   - PlanSdd (com plano de implementação)
   - TaskSdd (com tarefas a executar)

### 2. Chamar o Endpoint SDD:
```bash
curl -X POST http://localhost:8080/sdd-executor/execute-task/{taskId}
```

### 3. O Agent Irá:
   1. **Ler** o contexto completo (Spec + Plan + Task)
   2. **Pensar** sobre o que fazer
   3. **Decidir** qual ferramenta usar
   4. **Executar** a ferramenta (criar arquivo, rodar comando, etc)
   5. **Observar** o resultado
   6. **Repetir** até finalizar (máx 15 passos)

### 4. Resultado:
   - Código real criado no filesystem
   - Estruturas de diretórios criadas
   - Comandos executados (mvn, gradle, etc)
   - Arquivos validados
   - Tudo registrado nos steps da execução

---

## 🛠️ Personalizações

### Adicionar Nova Ferramenta

1. Crie uma classe que implemente `Tool`:
```java
public class MyCustomTool implements Tool {
    @Override
    public String getName() { return "my_tool"; }
    
    @Override
    public String getDescription() { return "Descrição da ferramenta"; }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("param1", "Descrição do param1");
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) throws Exception {
        // Implementação
        return "Resultado";
    }
}
```

2. Registre em `ExecutorAgentService`:
```java
toolRegistry.registerTool(new MyCustomTool(basePath));
```

---

### Aumentar Limite de Passos

Em `ExecutorAgentService`, altere:
```java
this.agentLoop = new AgentLoop(this.chatClient, this.toolRegistry, 15); // Aumentar para 20, 25, etc
```

---

## 📊 Estrutura de Resposta

```json
{
  "executionId": "uuid-123",
  "input": "Conteúdo da tarefa...",
  "finalAnswer": "Resposta final do agent",
  "stepCount": 5,
  "status": "SUCCESS",           // SUCCESS, ERROR, TIMEOUT
  "errorMessage": null,
  "totalExecutionTimeMs": 3500,
  "startTime": "2024-05-25T10:30:00",
  "endTime": "2024-05-25T10:30:03.500",
  "steps": [
    {
      "stepNumber": 1,
      "thinking": "Análise do problema",
      "toolName": "create_directory",
      "toolResult": "Diretório criado",
      "isFinal": false
    },
    // ... mais steps
  ]
}
```

---

## ⚙️ Configuração

### .properties ou .yml

Você pode adicionar configurações como:

```properties
# application.properties
executor.agent.max-steps=15
executor.agent.base-path=/seu/caminho/base
executor.agent.timeout-ms=60000
```

E injetar em `ExecutorAgentService`.

---

## 🔍 Logs

Todos os passos são registrados com logs detalhados:

```
[AGENT] Iniciando execução id=uuid step_count=1
[AGENT] LLM Response (passo 1): Pensamento: ...
[AGENT] Executando tool: create_directory...
[TOOL] Diretório criado: /caminho/xxx
[AGENT] Tool result (passo 1): Sucesso
[AGENT] Execução finalizada id=uuid status=SUCCESS passos=5 tempo=3500ms
```

---

## ✅ Próximos Passos

- [ ] Armazenar execuções em banco (AgentExecutionHistory)
- [ ] Visualizar histórico de execuções
- [ ] Reexecutar com diferentes parâmetros
- [ ] Adicionar mais ferramentas (git, npm, pytest, etc)
- [ ] Validação e teste de código gerado
- [ ] Integração com CI/CD
- [ ] Webhooks para eventos de execução

---

## 📝 Notas Importantes

1. **Máximo de Passos**: Há limite de 15 passos para evitar loops infinitos
2. **Filesystem**: As operações são reais, cuidado com paths
3. **Contexto LLM**: O contexto completo é enviado ao LLM (Spec+Plan+Task), isso pode ser grande
4. **Erros**: Se ferramenta falhar, agent tenta alternativas ou finaliza com erro
5. **ReAct**: O pattern ReAct permite reflexão e correção de erros durante execução

---

## 🎯 Caso de Uso Principal

**Fluxo Completo SDD:**

```
1. Usuário descreve o que quer
   ↓
2. Conversa refina requisitos
   ↓
3. LLM gera Especificação (Spec.md)
   ↓
4. LLM gera Plano (Plan.md)
   ↓
5. LLM gera Tarefas (Task.md)
   ↓
6. ⭐ [NOVO] Agent Executor rodaas tarefas com ReAct
   ↓
7. Código é gerado no filesystem
   ↓
8. Tudo pronto para compilar, testar e commitar
```

---

## 🚀 Começar Agora

```bash
# 1. Compilar
./mvnw clean compile

# 2. Rodar a aplicação
./mvnw spring-boot:run

# 3. Fazer chamada simples
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{"taskDescription":"Crie um arquivo test.txt em ./test"}'

# 4. Ver resultado
# Arquivo test.txt será criado em ./test/test.txt
```

---

**Autor:** Copilot  
**Data:** 25/05/2026  
**Versão:** 1.0

