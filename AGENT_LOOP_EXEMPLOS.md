# Exemplos Práticos - Agent Loop com ReAct

Aqui você encontra exemplos prontos para testar a implementação do Agent Loop.

## 1️⃣ Exemplo Simples: Criar um Arquivo

**Requisição:**
```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie um arquivo chamado README.md em teste com o seguinte conteúdo: # Meu Projeto\n\nEste é um teste do agent loop."
  }'
```

**O Agent Irá:**
1. Pensar: "Preciso criar um arquivo README.md no diretório teste"
2. Agir: Usar a ferramenta `create_directory` se não existir
3. Agir: Usar a ferramenta `create_file` com o conteúdo fornecido
4. Observar: Validar que o arquivo foi criado
5. Finalizar: "Arquivo README.md criado com sucesso"

---

## 2️⃣ Exemplo: Criar Estrutura de Pacote Java

**Requisição:**
```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie uma estrutura de pacote Java completo em src/main/java/com/example/myapp com:\n1. Classe Main.java com um método main\n2. Classe Service.java com um método de cálculo\n3. Classe Repository.java com exemplo de persistência\n4. Diretório models com classe User.java"
  }'
```

**Estrutura Criada:**
```
src/main/java/com/example/myapp/
├── Main.java
├── Service.java
├── Repository.java
└── models/
    └── User.java
```

---

## 3️⃣ Exemplo: Executar Comando Maven

**Requisição:**
```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Execute o comando 'mvn clean' para limpar o projeto"
  }'
```

**O Agent Irá:**
1. Reconhecer que é um comando
2. Chamar `execute_command` com "mvn clean"
3. Capturar a saída
4. Reportar sucesso ou erro

---

## 4️⃣ Exemplo: Criar Classe Spring Bean

**Requisição:**
```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie uma classe Spring Service chamada UserService em src/main/java/com/example/service/ com as seguintes características:\n\n- Anotação @Service\n- Método findById(Long id) que retorna Optional<User>\n- Método save(User user) que retorna User\n- Método delete(Long id)\n- Use @Autowired para injetar UserRepository"
  }'
```

---

## 5️⃣ Exemplo: Visualizar Ferramentas Disponíveis

**Requisição:**
```bash
curl -X GET http://localhost:8080/executor-agent/tools
```

**Resposta:**
```
Ferramentas disponíveis:

## create_file
Descrição: Cria um novo arquivo com conteúdo especificado no filesystem
Parâmetros:
  - file_path: Caminho relativo do arquivo a criar (ex: src/main/java/MyClass.java)
  - content: Conteúdo do arquivo

## read_file
Descrição: Lê o conteúdo de um arquivo existente no filesystem
Parâmetros:
  - file_path: Caminho relativo do arquivo a ler (ex: src/main/java/MyClass.java)

## create_directory
Descrição: Cria um novo diretório com todos os diretórios pais necessários
Parâmetros:
  - directory_path: Caminho relativo do diretório a criar (ex: src/main/java/com/example)

## execute_command
Descrição: Executa um comando no shell a partir do diretório base do projeto
Parâmetros:
  - command: Comando a executar (ex: mvn clean compile, gradle build)

## list_files
Descrição: Lista arquivos e diretórios em um caminho específico
Parâmetros:
  - directory_path: Caminho relativo do diretório a listar (ex: src/main/java). Se vazio, lista o diretório base.
```

---

## 6️⃣ Exemplo: Workflow Completo com SDD

**Pré-requisitos:**
- Ter criado um Projeto
- Ter criado uma ConversationSession
- Ter criado uma UserStory
- Ter gerado SpecSdd (especificação)
- Ter gerado PlanSdd (plano)
- Ter gerado TaskSdd (tarefas com conteúdo markdown)

**Requisição:**
```bash
# Buscar o ID do TaskSdd criado (exemplo: ID = 5)

# Visualizar o contexto que será usado
curl -X GET http://localhost:8080/sdd-executor/preview/5

# Executar a tarefa com contexto completo (Spec + Plan + Task)
curl -X POST http://localhost:8080/sdd-executor/execute-task/5
```

---

## 7️⃣ Exemplo: Conteúdo de um Task.md Bem Estruturado

```markdown
# Task 1: Criar estrutura base do projeto

## Objetivo
Criar a estrutura de diretórios e classes base para o microserviço de Autenticação.

## Ações Necessárias

### 1.1 Criar estrutura de pacotes
- `src/main/java/com/example/auth/controller/` - Controllers REST
- `src/main/java/com/example/auth/service/` - Services de negócio
- `src/main/java/com/example/auth/repository/` - Repositories JPA
- `src/main/java/com/example/auth/model/` - Modelos JPA
- `src/main/java/com/example/auth/dto/` - DTOs de requisição/resposta
- `src/main/java/com/example/auth/config/` - Configurações Spring

### 1.2 Criar classes base
- `AuthConfig.java` - Configuração de segurança
- `AuthService.java` - Service de autenticação
- `AuthController.java` - Controller com endpoints de login/logout
- `User.java` - Modelo JPA
- `UserRepository.java` - Repository

### 1.3 Validação
- Listar arquivos criados em src/main/java/com/example/auth
- Compilar com mvn compile
- Verificar se não há erros

## Resultado Esperado
Todas as classes criadas, estrutura pronta para o SDD de Implementação.
```

---

## 🔧 Tratamento de Erros

### Erro: "Arquivo não encontrado"
O agent tenta criar o diretório automaticamente se não existir.

### Erro: "Comando falhou"
O agent captura o erro e pode tentar alternativas.

### Erro: "Máximo de passos atingido"
A execução foi interrompida porque chegou ao limite de 15 passos. Verifique a tarefa é muito complexa.

---

## 📊 Exemplo de Resposta Completa

```json
{
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "input": "Crie um arquivo Hello.java em src/main/java/com/example com um programa Hello World",
  "finalAnswer": "Arquivo Hello.java criado com sucesso em src/main/java/com/example/Hello.java. O programa inclui um método main que imprime 'Hello, World!'",
  "stepCount": 3,
  "status": "SUCCESS",
  "errorMessage": null,
  "totalExecutionTimeMs": 2847,
  "startTime": "2024-05-25T10:30:00",
  "endTime": "2024-05-25T10:30:02.847",
  "steps": [
    {
      "stepNumber": 1,
      "thinking": "Preciso criar um arquivo Hello.java. Primeiro devo verificar se o diretório src/main/java/com/example existe e criá-lo se necessário.",
      "toolName": "create_directory",
      "toolResult": "Diretório criado com sucesso: src/main/java/com/example",
      "observation": "Diretório criado sem problemas",
      "isFinal": false,
      "finalAnswer": null
    },
    {
      "stepNumber": 2,
      "thinking": "Agora vou criar o arquivo Hello.java com um programa Hello World",
      "toolName": "create_file",
      "toolResult": "Arquivo criado com sucesso: src/main/java/com/example/Hello.java",
      "observation": "Arquivo criado com o conteúdo correto",
      "isFinal": false,
      "finalAnswer": null
    },
    {
      "stepNumber": 3,
      "thinking": "Arquivo criado com sucesso. Posso finalizar a tarefa.",
      "toolName": null,
      "toolResult": null,
      "observation": "Tarefa concluída",
      "isFinal": true,
      "finalAnswer": "Arquivo Hello.java criado com sucesso em src/main/java/com/example/Hello.java. O programa inclui um método main que imprime 'Hello, World!'"
    }
  ]
}
```

---

## 🚀 Dicas de Uso

### ✅ Boas Práticas

1. **Seja descritivo** na tarefa
   - ❌ "Crie um arquivo"
   - ✅ "Crie um arquivo User.java em src/main/java/com/example/model com uma classe User anotada com @Entity"

2. **Use contexto SDD** quando possível
   - Isso inclui Spec, Plan e Task automaticamente
   - Melhor do que enviar tudo manualmente

3. **Verifique o preview** antes de executar
   - Use `/preview` para validar o contexto
   - Evita surpresas

4. **Comece simples**
   - Teste com tarefas pequenas primeiro
   - Depois vá aumentando a complexidade

### ⚠️ Cuidados

- Caminhos são relativos ao diretório do projeto
- Comandos são executados realmente (cuidado com `rm`, `delete`, etc)
- O agent é stateless por execução (não mantém estado entre chamadas)
- Máximo de 15 passos por execução

---

## 📝 Template para Nova Requisição

```bash
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "DESCREVA AQUI O QUE QUER CRIAR/EXECUTAR",
    "basePath": "/caminho/opcional/do/projeto"
  }'
```

---

**Pronto para testar! 🎉**

