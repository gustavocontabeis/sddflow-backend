# 🤖 AGENTS - Documentação Completa

## 📋 Visão Geral

O sistema de agentes do **sddflow-backend** implementa um **Agent Loop com padrão ReAct** (Reasoning + Acting) para gerar e modificar código automaticamente usando Spring AI e Azure OpenAI.

### Arquitetura Geral

```
Usuario (API) 
    ↓
ExecutorAgentController
    ↓
AgentLoop (Reasoning + Acting)
    ↓
ChatClient + ToolRegistry
    ↓
Tools (Discovery, Files, GitHub, Docker)
    ↓
Resultado (AgentExecution)
```

---

## 🎯 Agentes Principais

### 1. **AgentLoop**
**Localização:** `com.example.springia.agent.loop.AgentLoop`

**Responsabilidade:** Orquestrador principal que implementa o padrão ReAct

**Fluxo:**
1. Recebe input do usuário
2. LLM pensa (Reasoning) - analisa a tarefa
3. LLM decide ação (Acting) - qual tool usar
4. Executa a tool e observa resultado
5. Repete até atingir "Finalizar" ou max_steps

**Métodos principais:**
- `execute(String input)` - Executa sem validação final
- `execute(String input, Project project)` - Executa com gate de validação Docker

**Limite de passos:** Configurável, evita loops infinitos

**Gate de finalização:** Se `project != null`, valida build/test com Docker antes de aceitar resultado

**Log debug:**
```bash
curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentLoop" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

---

### 2. **CodeGeneratorResponseAPIAgent**
**Localização:** `com.example.springia.agent.client.CodeGeneratorResponseAPIAgent`

**Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API

**Características:**
- ✅ Usa RestClient para chamar Responses API (suporta Tools)
- ✅ Configurado para modelo customizado gpt-5.3-codex
- ✅ Endpoint: `https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1/responses`
- ✅ Com Tools/Function Calling
- ✅ Com Advisors

#### Request

As classes de DTO devem ficar em `com.example.springia.agent.responseapi.request`

#### Response

As classes de DTO devem ficar em `com.example.springia.agent.responseapi.response`

**Métodos:**
- `generateCode(String userPrompt)` - Gera código Java a partir de especificação

**Configuração:**
```properties
spring.ai.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.openai.base-url=https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1
```

---

### 3. **CodeGeneratorOpenApiAgent**
**Localização:** `com.example.springia.agent.client.CodeGeneratorOpenApiAgent`

**Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API usando `com.openai.client.OpenAIClient` 

**Características:**
- ✅ Usa `com.openai.client.OpenAIClient` para chamar Responses API (suporta Tools)
- ✅ Configurado para modelo customizado gpt-5.3-codex
- ✅ Endpoint: `https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1/responses`
- ✅ Com Tools/Function Calling
- ✅ Carrega o System Prompt de `src/main/resources/prompts/system-prompt.md`

**Métodos:**
- `generateCode(String userPrompt)` - Gera código Java a partir de User Prompt

**Configuração:**
```properties
spring.ai.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.openai.base-url=https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1
```

---

### 4. **SddPlanServiceAgent**
**Localização:** `com.example.springia.serviceagent.SddPlanServiceAgent`

**Responsabilidade:** Valida e ajusta prompts de acordo com estrutura do repositório

**Métodos:**
- `validarRepositorio(Project project, String input)` - Garante que paths estejam corretos dentro dos diretórios existentes

**Usa:** ChatClient com prompt contextualizado

---

## 🛠️ Sistema de Ferramentas (Tools)

### Interface Base

```java
public interface Tool {
    String getName();              // Nome para LLM selecionar
    String getDescription();       // Descrição da ferramenta
    Map<String, String> getParameters(); // Parâmetros esperados
    String execute(Map<String, String> params) throws Exception;
}
```

### Ferramentas Disponíveis

#### 📂 **File Tools**

| Tool | Descrição | Parâmetros |
|------|-----------|-----------|
| `read_file` | Lê conteúdo de arquivo | `path`, `startLine`, `endLine` |
| `create_file` | Cria novo arquivo | `path`, `content` |
| `update_file` | Atualiza arquivo existente | `path`, `content` |
| `find_files` | Busca arquivos por padrão | `directory`, `pattern` |
| `grep_files` | Busca texto em arquivos | `directory`, `pattern`, `includePattern` |
| `create_directory` | Cria diretório | `path` |

#### 🔍 **Discovery Tools**

| Tool | Descrição | Parâmetros |
|------|-----------|-----------|
| `discovery_tool` | Busca informações no código-fonte | `project_id`, `question` |
| `github_discovery_tool` | Descobre estrutura de repo GitHub | `repository_url`, `branch` |

#### 🐙 **GitHub Tools**

| Tool | Descrição | Parâmetros |
|------|-----------|-----------|
| `github_list_repos` | Lista repositórios | `owner` |
| `github_clone_repo` | Clona repositório | `repository_url`, `branch`, `destination_path` |
| `github_create_commit` | Cria commit | `repository_url`, `branch`, `message`, `files` |
| `github_create_pull_request` | Cria pull request | `repository_url`, `title`, `description`, `branch` |

#### 🐳 **Docker & Build Tools**

| Tool | Descrição | Parâmetros |
|------|-----------|-----------|
| `execute_command` | Executa comando no shell | `command`, `timeout` |
| `docker_build_and_test` | Compila e testa em Docker | `project_id`, `timeout` |

---

## 📊 Dados de Resultado

### AgentExecution

```java
{
  executionId: "unique-uuid",
  input: "Descrição da tarefa",
  finalAnswer: "Resultado final",
  steps: [ /* Lista de AgentStep */ ],
  totalExecutionTimeMs: 5432,
  stepCount: 7,
  startTime: "2026-06-30T10:30:00",
  endTime: "2026-06-30T10:30:05.432",
  status: "SUCCESS", // SUCCESS, ERROR, TIMEOUT
  errorMessage: null
}
```

### AgentStep

```java
{
  stepNumber: 1,
  thinking: "Preciso analisar o projeto...",
  toolName: "discovery_tool",
  toolParams: { "project_id": "1", "question": "..." },
  toolResult: "Resultado da execução",
  timestamp: "2026-06-30T10:30:01"
}
```

---

## 🔄 Fluxo de Execução Completo

### Exemplo: Gerar nova classe Service

```
1. POST /executor-agent/execute
   Input: "Crie um UserService com CRUD básico"
   ProjectId: 1

2. AgentLoop.execute()
   └─ Step 1: discovery_tool → Análise projeto
   └─ Step 2: find_files → Localiza padrão existente
   └─ Step 3: read_file → Lê UserRepository
   └─ Step 4: create_file → Gera UserService.java
   └─ Step 5: docker_build_and_test → Valida compilação
   └─ Step 6: GitHub → Commit automático
   └─ FINAL: Retorna sucesso

3. Response:
   {
     status: "SUCCESS",
     stepCount: 6,
     finalAnswer: "UserService criado em src/main/java/...",
     executionId: "xyz-123"
   }
```

---

## ⚙️ Configuração

### Environment Variables

```bash
SPRING_AI_OPENAI_API_KEY=sk-...
SPRING_AI_OPENAI_BASE_URL=https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=gpt-5.3-codex  # ou gpt-4o para Chat API
GITHUB_TOKEN=ghp_...
DOCKER_HOST=http://localhost:2375  # Opcional
```

### application.properties

```properties
spring.ai.openai.api-key=${SPRING_AI_OPENAI_API_KEY}
spring.ai.openai.base-url=${SPRING_AI_OPENAI_BASE_URL}
spring.ai.openai.chat.options.model=${SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL}
spring.ai.openai.chat.timeout=300s
agent.max-steps=20
agent.enable-docker-gate=true
```

---

## 🚀 Como Usar via API

### 1. Executar Tarefa de Código

```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Implemente validações de email na classe User",
    "projectId": 1
  }'
```

### 2. Sem Projeto (sem validação Docker)

```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie uma classe simples de teste",
    "basePath": "tmp-test"
  }'
```

### 3. Com basePath Customizado

```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Modifique UserService para usar PgVector",
    "projectId": 2,
    "basePath": "/custom/path"
  }'
```

---

## 🛡️ Guardrails (Proteções)

### 1. **Validação de Sintaxe Java**
- ✅ JavaParser valida estrutura antes do build
- ❌ Rejeita classes sem package declaration
- ❌ Rejeita imports inválidos

### 2. **Gate de Finalização Docker**
- ✅ Compila código com `mvn clean compile`
- ✅ Executa testes com `mvn test`
- ❌ Bloqueia se houver erro de compilação

### 3. **Tool Registry Whitelist**
- ✅ Apenas ferramentas registradas podem ser usadas
- ❌ LLM não pode invocar tools arbitrárias

### 4. **Max Steps**
- ✅ Limite de iterações (padrão: 20)
- ❌ Evita loops infinitos

---

## 🔌 Clientes Disponíveis

| Cliente | Usa Tools | Usa Advisors | Uso |
|---------|-----------|--------------|-----|
| **ChatClient** | ✅ Sim | ✅ Sim | Agent loops com ferramentas |
| **ChatModel** | ❌ Não | ❌ Não | LLM básico portável |
| **RestClient** | ❌ Não | ❌ Não | HTTP direto (gpt-5.3-codex) |
| **OpenAiChatModel** | ❌ Não | ❌ Não | OpenAI específico |

**Recomendação para seu caso:**
- AgentLoop + Code Generation + Tools = **ChatClient**
- gpt-5.3-codex via Responses API = **RestClient**

---

## 📝 Logging

### Ativar DEBUG para AgentLoop

```bash
curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentLoop" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

### Logs Importantes

```
[AGENT] Iniciando execução id=xyz-123 input_length=245
[AGENT] Passo 1 de 20
[TOOL REGISTRY] Tool registrada: discovery_tool
[TOOL CALL] Executando discovery_tool com params={...}
[AGENT] Gate de finalização acionado: validando build/test com Docker
[AGENT] Execução concluída: SUCCESS - 6 passos
```

