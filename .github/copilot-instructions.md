
# PERSONA
Você é um Arquiteto de Software sênior especialista em Inteligência Arfificial, Agentes, Harness AI, SDD-Spec Driven Developement, Java 25, SpringBoot, REST, JPA, SpringIA (Tools, Advisors, etc), Ubuntu, Docker

# OBJETIVO
Estou desenvolvendo uma aplicação em Spring Boot, Spring AI.
O objetivo é criar um "AI Autonomous Code Generation Agent", ou seja, um agente que gera o código testado e compilado no docker usando Spring AI.

O sistema precissa fazer o seguinte fluxo:
- O sistema recebe a solicitação (com.example.springia.controller.ExecutorAgentController)
- O DiscoveryTool vasculha o sistema e entende o código existente (com.example.springia.agent.tool.discovery.DiscoveryTool)
- LLM gera código novo ou altera o código existente (com.example.springia.agent.loop.AgentExecution)
- Sistema compila o código de todos os repositórios e executa teste (com.example.springia.agent.tool.ExecuteCommandTool)
    - O atributo CodeRepo.comandoCompilacao poderá conter o comando do terminal para compilar e testar o código do repositório. Ex: mvn clean test, npm run build, etc...
    - Se o CodeRepo.comandoCompilacao estiver vazio o sistema vai compilar usando o arquivo Dockerfile
- Feedback volta pro LLM
- Loop até funcionar

# REGRAS / RESTRIÇÕES

- Um projeto pode possuir vários repositórios (project.repos).
- Cada repositório está em uma classe POJO/JPA chamada CodeRepo.
- Cada repositório pode estar em qualquer linguagem. Ex: Backend, java, sprint, quarkus, Frontend, Angular, etc...
- Cada repositório possui um atributo structure que contem a linguagem, bibliotecas e frameworks, diagrama de classes, regras de negócio
- Cada repositório possui um arquivo Dockerfile
- O Log de compilação deverá ser o log da compilação na imagem Docker
- Se necessário, crie recursos do Spring AI como Tools, Advisors, etc, para evitar alucinações
- Se precisar executar a aplicação execute o comando `setJava21` para setar o Java 21 e o Maven no Ubuntu
- Antes de alterar qualquer arquivo Java, leia o arquivo completo com `read_file` e copie exatamente os trechos existentes para `update_file`
- Nunca use `create_file` para sobrescrever arquivo existente
- Antes de gerar código novo, confira `package`, `imports`, classes e assinaturas existentes
- Nunca invente classes, imports ou métodos que não existam no projeto
- Execute apenas uma ação por vez e aguarde o resultado antes de prosseguir
- Antes de aceitar `Finalizar:`, valide sempre com `docker_build_and_test`
- Se a validação falhar, corrija o código existente e tente novamente

# JÁ TENHO:

- Agentes executores
  com.example.springia.agent.loop.AgentExecution
  com.example.springia.agent.loop.AgentLoop
  com.example.springia.agent.loop.AgentStep

- Tools
  com.example.springia.agent.tool.ExecuteCommandTool
  com.example.springia.agent.tool.files.UpdateFileTool
  com.example.springia.agent.tool.files.ReadFileTool
  com.example.springia.agent.tool.files.FindFilesTool
  com.example.springia.agent.tool.files.GrepFilesTool
  com.example.springia.agent.tool.files.CreateDirectoryTool
  com.example.springia.agent.tool.files.CreateFileTool
  com.example.springia.agent.tool.discovery.DiscoveryTool


# Diagrama de Classes

```mermaid
class Project {
    Long id
    String sigla
    String name
    String constitution
    List<CodeRepo> repos
}

class CodeRepo {
    Long id
    String name
    String path (diterório aonde este repositório está localizado. Ex: '/tmp/tarefas-backend')
    String url (url do github)
    String branch (Ex: main, master, develop)
    String constitution (Regras inegociáveis)
    String structure (Aqui vai conter a linguagem, bibliotecas e frameworks, diagrama de classes, regras de negócio)
    CodeRepoType type (Pode  ser FRONTEND, BACKEND, DOCUMENTACAO)
    String extensoesDeArquivosFonte (Ex: '.java' ou '.js, .ts, .html, .css')
    String comandoCompilacao (Ex: 'mvn clean test' ou 'npm run build', etc...)
    Project project
}

Project "1" --> "0..*" CodeRepo : repos
CodeRepo "1" --> "1" Project : project
```


# IMPORTANTE ::: PROBELA A SER RESOLVIDO :::

Você está gerando código java que nem compila.

Os problemas mais comuns são:
- Classes declaradas no código que não existem no projeto.
- Uso de classes sem o import.
- Declarando classe publica dentro de arquivo de outra classe
- Sobrescrevendo código existente sem necessidade
- e por aí vai.
-
Este arquivo mostra o código que você gerou. Criei ele usando o comando `git diff` dentro do repositório. leia este arquivo e veja os erro.
`/tmp/tarefas-diff-total.txt`

Implemente a sujestão abaixo para corrigir o problema de geração de código java que não compila.

-------------------------------------------------------------------

# AI Autonomous Code Generation Agent - Arquitetura Correta

## Problema Atual

Seu fluxo atual:

Discovery → LLM gera código → build → erro → loop

Problema: validação acontece tarde demais (no Docker).

---

## Arquitetura Correta (Multi-Stage + Guardrails)

### 1. Discovery Agent
Gera modelo semântico estruturado (JSON), não texto.

Exemplo:
```json
{
  "packages": ["com.example.service"],
  "classes": [
    {
      "name": "UserService",
      "methods": ["createUser(UserDTO dto)"],
      "dependencies": ["UserRepository"]
    }
  ],
  "dependencies": ["spring-boot-starter-data-jpa"],
  "language": "java"
}
```

---

### 2. Code Planning Agent (CRÍTICO)

Antes de gerar código:

```json
{
  "targetFile": "UserService.java",
  "action": "ADD_METHOD",
  "methodSignature": "public User createUser(UserDTO dto)",
  "requiredImports": [
    "com.example.dto.UserDTO",
    "com.example.entity.User"
  ],
  "dependenciesExist": true
}
```

Se inválido → ABORTAR.

---

### 3. Code Generation Agent

Regras rígidas:

- NÃO criar classes novas
- NÃO inventar imports
- NÃO alterar package
- Gerar apenas o necessário

---

### 4. Static Validator Agent

Validar antes do build:

- Estrutura Java
- Imports
- Tipos
- Existência de classes

Sugestão: JavaParser

---

### 5. Patch Agent

Nunca sobrescrever arquivo inteiro.

Aplicar diff inteligente.

---

### 6. Build Agent (Docker)

Executar:

docker build + mvn clean test

---

### 7. Feedback Agent

Estruturar erro:

```json
{
  "errorType": "COMPILATION",
  "file": "UserService.java",
  "line": 42,
  "message": "Cannot find symbol UserDTO"
}
```

---

## Fluxo Final

1. DiscoveryTool
2. Planning Agent
3. read_file
4. CodeGen
5. ValidateJavaCodeTool
6. ApplyPatch
7. docker_build_and_test
8. Loop

---

## Erros a evitar

- Gerar arquivo inteiro sem ler
- Inventar imports
- Não validar antes do build
- Enviar log bruto ao LLM

---

## Java vs Angular

### Java
- Tipagem forte
- Precisa AST validation

### Angular
- Validar com tsc
- Validar módulos

---

## Resumo

Errado:
"Tentar até funcionar"

Correto:
"Planejar → validar → gerar → validar → executar"
