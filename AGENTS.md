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

### 2. **CodeGeneratorAgent**
**Localização:** `com.example.springia.agent.CodeGeneratorAgent`

**Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API

**Características:**
- ✅ Usa RestClient para chamar Responses API (não suporta Tools)
- ✅ Configurado para modelo customizado gpt-5.3-codex
- ✅ Endpoint: `https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1/responses`
- ❌ Sem Tools/Function Calling
- ❌ Sem Advisors

#### Request

As classes de DTO devem ficar em `com.example.springia.agent.responseapi.request`

```json
{
  "model": "gpt-5.3-codex",
  "input": [
    {
      "role": "system",
      "content": [
        {
          "type": "input_text",
          "text": "Você é um agente gerador de código especializado em Java + Spring Boot. Gere código limpo, com imports corretos, estrutura de projeto e boas práticas. o pacote da aplicacao é br.com.gustavodasilva.minhaapp"
        }
      ]
    },
    {
      "role": "user",
      "content": [
        {
          "type": "input_text",
          "text": "Crie um CRUD completo de Pessoa com entidade, repository, service e controller. Salve os arquivos no sistema usando as tools disponíveis."
        }
      ]
    }
  ],
  "tools": [
    {
      "type": "function",
      "name": "read_file",
      "description": "Lê o conteúdo de um arquivo no sistema",
      "parameters": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Caminho completo do arquivo"
          }
        },
        "required": ["path"]
      }
    },
    {
      "type": "function",
      "name": "write_file",
      "description": "Cria ou sobrescreve um arquivo com conteúdo",
      "parameters": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Caminho completo do arquivo"
          },
          "content": {
            "type": "string",
            "description": "Conteúdo do arquivo"
          }
        },
        "required": ["path", "content"]
      }
    },
    {
      "type": "function",
      "name": "list_files",
      "description": "Lista arquivos de um diretório",
      "parameters": {
        "type": "object",
        "properties": {
          "path": {
            "type": "string",
            "description": "Diretório a ser listado"
          }
        },
        "required": ["path"]
      }
    }
  ]
}
```

#### Response

As classes de DTO devem ficar em `com.example.springia.agent.responseapi.response`

```json
{
    "id": "resp_0bb77337c460de53006a443d17cb7c81979dad0b1ef4739a34",
    "object": "response",
    "created_at": 1782856983,
    "status": "completed",
    "background": false,
    "completed_at": 1782856992,
    "content_filters": [
        {
            "blocked": false,
            "source_type": "prompt",
            "content_filter_raw": [],
            "content_filter_results": {
                "hate": {
                    "filtered": false,
                    "severity": "safe"
                },
                "sexual": {
                    "filtered": false,
                    "severity": "safe"
                },
                "violence": {
                    "filtered": false,
                    "severity": "safe"
                },
                "self_harm": {
                    "filtered": false,
                    "severity": "safe"
                },
                "jailbreak": {
                    "detected": false,
                    "filtered": false
                }
            },
            "content_filter_offsets": {
                "start_offset": 0,
                "end_offset": 170,
                "check_offset": 0
            }
        },
        {
            "blocked": false,
            "source_type": "completion",
            "content_filter_raw": [],
            "content_filter_results": {
                "protected_material_text": {
                    "detected": false,
                    "filtered": false
                },
                "protected_material_code": {
                    "detected": true,
                    "filtered": false,
                    "citation": {
                        "URL": "https://github.com/mdarif-k/way2p-notes/tree/26c63a3cc9f76ef9237945b7d8d5c02bf7afcb40/src%2Fassets%2Fbackend%2Fblogs.js",
                        "license": "NOASSERTION"
                    }
                },
                "hate": {
                    "filtered": false,
                    "severity": "safe"
                },
                "sexual": {
                    "filtered": false,
                    "severity": "safe"
                },
                "violence": {
                    "filtered": false,
                    "severity": "safe"
                },
                "self_harm": {
                    "filtered": false,
                    "severity": "safe"
                }
            },
            "content_filter_offsets": {
                "start_offset": 0,
                "end_offset": 5368,
                "check_offset": 0
            }
        }
    ],
    "error": null,
    "frequency_penalty": 0.0,
    "incomplete_details": null,
    "instructions": null,
    "max_output_tokens": null,
    "max_tool_calls": null,
    "model": "gpt-5.3-codex",
    "moderation": null,
    "output": [
        {
            "id": "fc_0bb77337c460de53006a443d20944081978e89fac3876fdc19",
            "type": "function_call",
            "status": "completed",
            "arguments": "{\"path\":\"/src/main/java/br/com/gustavodasilva/menhaapp/model/Pessoa.java\",\"content\":\"package br.com.gustavodasilva.menhaapp.model;\\n\\nimport jakarta.persistence.Column;\\nimport jakarta.persistence.Entity;\\nimport jakarta.persistence.GeneratedValue;\\nimport jakarta.persistence.GenerationType;\\nimport jakarta.persistence.Id;\\nimport jakarta.persistence.Table;\\n\\n@Entity\\n@Table(name = \\\"pessoas\\\")\\npublic class Pessoa {\\n\\n    @Id\\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\\n    private Long id;\\n\\n    @Column(nullable = false)\\n    private String nome;\\n\\n    @Column(nullable = false, unique = true)\\n    private String email;\\n\\n    public Pessoa() {\\n    }\\n\\n    public Pessoa(Long id, String nome, String email) {\\n        this.id = id;\\n        this.nome = nome;\\n        this.email = email;\\n    }\\n\\n    public Long getId() {\\n        return id;\\n    }\\n\\n    public void setId(Long id) {\\n        this.id = id;\\n    }\\n\\n    public String getNome() {\\n        return nome;\\n    }\\n\\n    public void setNome(String nome) {\\n        this.nome = nome;\\n    }\\n\\n    public String getEmail() {\\n        return email;\\n    }\\n\\n    public void setEmail(String email) {\\n        this.email = email;\\n    }\\n}\\n\"}",
            "call_id": "call_KkRr58KqUzrckDb45NBUOqGw",
            "name": "write_file"
        },
        {
            "id": "fc_0bb77337c460de53006a443d2094588197bb2e4cc2de608302",
            "type": "function_call",
            "status": "completed",
            "arguments": "{\"path\":\"/src/main/java/br/com/gustavodasilva/menhaapp/repository/PessoaRepository.java\",\"content\":\"package br.com.gustavodasilva.menhaapp.repository;\\n\\nimport br.com.gustavodasilva.menhaapp.model.Pessoa;\\nimport org.springframework.data.jpa.repository.JpaRepository;\\nimport org.springframework.stereotype.Repository;\\n\\nimport java.util.Optional;\\n\\n@Repository\\npublic interface PessoaRepository extends JpaRepository<Pessoa, Long> {\\n    Optional<Pessoa> findByEmail(String email);\\n}\\n\"}",
            "call_id": "call_6dRxgFReQjMi3cUQutOVH5zb",
            "name": "write_file"
        },
        {
            "id": "fc_0bb77337c460de53006a443d2094688197a3c6482dfdd348cf",
            "type": "function_call",
            "status": "completed",
            "arguments": "{\"path\":\"/src/main/java/br/com/gustavodasilva/menhaapp/service/PessoaService.java\",\"content\":\"package br.com.gustavodasilva.menhaapp.service;\\n\\nimport br.com.gustavodasilva.menhaapp.model.Pessoa;\\nimport br.com.gustavodasilva.menhaapp.repository.PessoaRepository;\\nimport org.springframework.stereotype.Service;\\n\\nimport java.util.List;\\nimport java.util.NoSuchElementException;\\n\\n@Service\\npublic class PessoaService {\\n\\n    private final PessoaRepository pessoaRepository;\\n\\n    public PessoaService(PessoaRepository pessoaRepository) {\\n        this.pessoaRepository = pessoaRepository;\\n    }\\n\\n    public List<Pessoa> listarTodas() {\\n        return pessoaRepository.findAll();\\n    }\\n\\n    public Pessoa buscarPorId(Long id) {\\n        return pessoaRepository.findById(id)\\n                .orElseThrow(() -> new NoSuchElementException(\\\"Pessoa não encontrada com id: \\\" + id));\\n    }\\n\\n    public Pessoa criar(Pessoa pessoa) {\\n        pessoa.setId(null);\\n        return pessoaRepository.save(pessoa);\\n    }\\n\\n    public Pessoa atualizar(Long id, Pessoa pessoaAtualizada) {\\n        Pessoa pessoaExistente = buscarPorId(id);\\n        pessoaExistente.setNome(pessoaAtualizada.getNome());\\n        pessoaExistente.setEmail(pessoaAtualizada.getEmail());\\n        return pessoaRepository.save(pessoaExistente);\\n    }\\n\\n    public void deletar(Long id) {\\n        Pessoa pessoaExistente = buscarPorId(id);\\n        pessoaRepository.delete(pessoaExistente);\\n    }\\n}\\n\"}",
            "call_id": "call_UrrE9rZz0JJJMr5vHUdt0Jht",
            "name": "write_file"
        },
        {
            "id": "fc_0bb77337c460de53006a443d20947481979519a2ce9289fe88",
            "type": "function_call",
            "status": "completed",
            "arguments": "{\"path\":\"/src/main/java/br/com/gustavodasilva/menhaapp/controller/PessoaController.java\",\"content\":\"package br.com.gustavodasilva.menhaapp.controller;\\n\\nimport br.com.gustavodasilva.menhaapp.model.Pessoa;\\nimport br.com.gustavodasilva.menhaapp.service.PessoaService;\\nimport org.springframework.http.HttpStatus;\\nimport org.springframework.web.bind.annotation.DeleteMapping;\\nimport org.springframework.web.bind.annotation.GetMapping;\\nimport org.springframework.web.bind.annotation.PathVariable;\\nimport org.springframework.web.bind.annotation.PostMapping;\\nimport org.springframework.web.bind.annotation.PutMapping;\\nimport org.springframework.web.bind.annotation.RequestBody;\\nimport org.springframework.web.bind.annotation.RequestMapping;\\nimport org.springframework.web.bind.annotation.ResponseStatus;\\nimport org.springframework.web.bind.annotation.RestController;\\n\\nimport java.util.List;\\n\\n@RestController\\n@RequestMapping(\\\"/api/pessoas\\\")\\npublic class PessoaController {\\n\\n    private final PessoaService pessoaService;\\n\\n    public PessoaController(PessoaService pessoaService) {\\n        this.pessoaService = pessoaService;\\n    }\\n\\n    @GetMapping\\n    public List<Pessoa> listarTodas() {\\n        return pessoaService.listarTodas();\\n    }\\n\\n    @GetMapping(\\\"/{id}\\\")\\n    public Pessoa buscarPorId(@PathVariable Long id) {\\n        return pessoaService.buscarPorId(id);\\n    }\\n\\n    @PostMapping\\n    @ResponseStatus(HttpStatus.CREATED)\\n    public Pessoa criar(@RequestBody Pessoa pessoa) {\\n        return pessoaService.criar(pessoa);\\n    }\\n\\n    @PutMapping(\\\"/{id}\\\")\\n    public Pessoa atualizar(@PathVariable Long id, @RequestBody Pessoa pessoa) {\\n        return pessoaService.atualizar(id, pessoa);\\n    }\\n\\n    @DeleteMapping(\\\"/{id}\\\")\\n    @ResponseStatus(HttpStatus.NO_CONTENT)\\n    public void deletar(@PathVariable Long id) {\\n        pessoaService.deletar(id);\\n    }\\n}\\n\"}",
            "call_id": "call_NJHYd5SPLzlNthBGMSKhsvTW",
            "name": "write_file"
        }
    ],
    "parallel_tool_calls": true,
    "presence_penalty": 0.0,
    "previous_response_id": null,
    "prompt_cache_key": null,
    "prompt_cache_retention": "in_memory",
    "reasoning": {
        "context": null,
        "effort": "none",
        "summary": null
    },
    "safety_identifier": null,
    "service_tier": "default",
    "store": true,
    "temperature": 1.0,
    "text": {
        "format": {
            "type": "text"
        },
        "verbosity": "medium"
    },
    "tool_choice": "auto",
    "tools": [
        {
            "type": "function",
            "description": "Lê o conteúdo de um arquivo no sistema",
            "name": "read_file",
            "parameters": {
                "type": "object",
                "properties": {
                    "path": {
                        "type": "string",
                        "description": "Caminho completo do arquivo"
                    }
                },
                "required": [
                    "path"
                ],
                "additionalProperties": false
            },
            "strict": true
        },
        {
            "type": "function",
            "description": "Cria ou sobrescreve um arquivo com conteúdo",
            "name": "write_file",
            "parameters": {
                "type": "object",
                "properties": {
                    "path": {
                        "type": "string",
                        "description": "Caminho completo do arquivo"
                    },
                    "content": {
                        "type": "string",
                        "description": "Conteúdo do arquivo"
                    }
                },
                "required": [
                    "path",
                    "content"
                ],
                "additionalProperties": false
            },
            "strict": true
        },
        {
            "type": "function",
            "description": "Lista arquivos de um diretório",
            "name": "list_files",
            "parameters": {
                "type": "object",
                "properties": {
                    "path": {
                        "type": "string",
                        "description": "Diretório a ser listado"
                    }
                },
                "required": [
                    "path"
                ],
                "additionalProperties": false
            },
            "strict": true
        }
    ],
    "top_logprobs": 0,
    "top_p": 0.98,
    "truncation": "disabled",
    "usage": {
        "input_tokens": 202,
        "input_tokens_details": {
            "cached_tokens": 0
        },
        "output_tokens": 1295,
        "output_tokens_details": {
            "reasoning_tokens": 0
        },
        "total_tokens": 1497
    },
    "user": null,
    "metadata": {}
}
```




**Métodos:**
- `generateJavaCode(String specification)` - Gera código Java a partir de especificação

**Configuração:**
```properties
spring.ai.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.openai.base-url=https://gustavocontabeis-9085-resource.services.ai.azure.com/openai/v1
```

---

### 3. **SddPlanServiceAgent**
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

---

## 🐛 Troubleshooting

### Problema: "Tool não encontrada"

**Causa:** Tool não foi registrada em `ToolRegistry`

**Solução:**
```java
ToolRegistry registry = new ToolRegistry();
registry.registerTool(new DiscoveryTool(...));
```

### Problema: "Resposta vazia do LLM"

**Causa:** Prompt muito vago ou modelo saturado

**Solução:** Adicione mais contexto ao prompt, reduza tamanho do input

### Problema: "Docker build falha no Windows"

**Causa:** Paths com `\` em vez de `/`

**Solução:** Use `Path.normalize()` ou `FileUtils.normalizePath()`

### Problema: "ChatClient não suporta Tools"

**Causa:** Usando modelo que não suporta function calling

**Solução:** Trocar para gpt-4o ou usar RestClient para Responses API

---

## 📚 Referências

- **Pattern ReAct:** Reasoning + Action loops para agentes de IA
- **Spring AI Docs:** https://docs.spring.io/spring-ai/reference/
- **Azure OpenAI:** https://learn.microsoft.com/en-us/azure/ai-services/openai/
- **Tool Design:** Tool Registry pattern para permitir LLM usar ferramentas selecionadas

---

## 🎓 Próximos Passos

- [ ] Implementar validação estática com JavaParser
- [ ] Adicionar suporte a multi-language (Go, Python, JS)
- [ ] Criar advisor específico para Code Review
- [ ] Integrar com plataforma de CI/CD (GitHub Actions)
- [ ] Cachear resultados de Discovery para otimizar
- [ ] Implementar Planning Agent para validação antes de gerar código

---

**Última atualização:** 30/06/2026  
**Versão:** 1.0  
**Status:** Produção

