# 🔄 Fluxo Detalhado do Gate de Finalização

## Diagrama de Sequência: Sucesso no Primeiro Build

```
USER
  │
  ├─ POST /executor-agent/execute {projectId: 1, taskDescription: "..."}
  │
  ▼
ExecutorAgentController
  │
  ├─ Resolve projectId → Project com 2 CodeRepos [backend, frontend]
  │
  ├─ Calls: executorAgentService.executeTask(task, project)
  │
  ▼
ExecutorAgentService
  │
  ├─ registerTools(project):
  │   ├─ CreateFileTool
  │   ├─ UpdateFileTool
  │   ├─ ReadFileTool
  │   ├─ ... (outras tools)
  │   │
  │   └─ ✨ NEW: DockerBuildAndTestTool(project) ✨
  │       └─ Tem referência aos 2 repos do project
  │
  ├─ Calls: agentLoop.execute(task, project)
  │
  ▼
AgentLoop (Iteration 1-5: Criação de código)
  │
  ├─ BUILD_CONTEXT: Menciona docker_build_and_test como ferramenta disponível
  │
  ├─ ITERATION 1:
  │   ├─ callLLM(context)
  │   │   ├─ LLM: "Vou usar discovery_tool para entender código existente"
  │   │   └─ LLM Response: "Ação: discovery_tool, Parâmetros: {...}"
  │   │
  │   ├─ parseAgentResponse() → AgentStep(toolName: discovery_tool)
  │   │
  │   └─ executeSingleTool(discovery_tool) → "Estrutura descoberta..."
  │
  ├─ ITERATION 2-4: [Similarmente...]
  │   ├─ create_file: CompanyController.java
  │   ├─ create_file: CompanyService.java
  │   └─ create_file: CompanyRepository.java
  │
  ├─ ITERATION 5:
  │   ├─ callLLM(context + feedback das iterações anteriores)
  │   │   ├─ LLM thinks: "Código criado, vou tentar finalizar"
  │   │   └─ LLM Response: "Finalizar: Implementação concluída com sucesso"
  │   │
  │   ├─ parseAgentResponse() → AgentStep(isFinal: true)
  │   │
  │   └─ ⚠️ GATE OF FINALIZATION DETECTED ⚠️
  │
  ▼
AgentLoop - Gate de Finalização
  │
  ├─ Check: alreadyValidatedBuild = false? → YES
  ├─ Check: project != null? → YES
  ├─ Check: project.getRepos().isEmpty()? → NO (tem 2 repos)
  │
  ├─ 🎯 EXECUTA VALIDAÇÃO AUTOMÁTICA:
  │
  │   ├─ Create AgentStep(stepNumber: 6, toolName: docker_build_and_test)
  │   │
  │   ├─ executeSingleTool(docker_build_and_test):
  │   │
  │   │   ├─ FOR EACH repo in project.repos:
  │   │   │   │
  │   │   │   ├─ Repo 1: backend (type: BACKEND)
  │   │   │   │   ├─ buildCommand = "mvn clean test -DskipTests=false"
  │   │   │   │   ├─ ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
  │   │   │   │   ├─ pb.directory(/tmp/sddflow-backend)
  │   │   │   │   ├─ Execute...
  │   │   │   │   ├─ exitCode = 0
  │   │   │   │   └─ Output: "✓ Build + Test SUCESSO"
  │   │   │   │
  │   │   │   └─ Repo 2: frontend (type: FRONTEND)
  │   │   │       ├─ buildCommand = "ng build --configuration=development"
  │   │   │       ├─ ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
  │   │   │       ├─ pb.directory(/tmp/sddflow-frontend)
  │   │   │       ├─ Execute...
  │   │   │       ├─ exitCode = 0
  │   │   │       └─ Output: "✓ Build + Test SUCESSO"
  │   │   │
  │   │   └─ Aggregate Result:
  │   │       ├─ "Total: 2 | ✅ Sucesso: 2 | ❌ Falha: 0"
  │   │       └─ "✅ VALIDAÇÃO COMPLETA: Todos os repositórios foram compilados e testados com sucesso!"
  │   │
  │   └─ validationResult = "✅ VALIDAÇÃO COMPLETA..."
  │
  ├─ Check: validationResult.contains("✅ VALIDAÇÃO COMPLETA")? → YES
  │   │
  │   ├─ alreadyValidatedBuild = true
  │   ├─ step.setObservation("Validação passou")
  │   ├─ execution.setFinalAnswer(step.getFinalAnswer())
  │   ├─ execution.setStatus("SUCCESS")
  │   └─ BREAK loop
  │
  ▼
Return AgentExecution
  │
  ├─ executionId: "uuid-12345"
  ├─ status: "SUCCESS"
  ├─ stepCount: 6
  ├─ steps: [discovery_tool, create_file, create_file, create_file, finalizar, docker_build_and_test]
  ├─ finalAnswer: "Implementação concluída com sucesso"
  │
  ▼
ExecutorAgentController
  │
  └─ ResponseEntity.ok(ExecutorAgentResponse)
      │
      ▼
    USER
```

---

## Diagrama de Sequência: Falha no Build (Com Auto-Correção)

```
[Mesmo início até ITERATION 5...]

ITERATION 5: Agent tenta finalizar
  │
  ├─ LLM Response: "Finalizar: Código pronto"
  ├─ ⚠️ GATE TRIGGERED
  │
  ▼
AgentLoop - Gate de Finalização (Primeira tentativa)
  │
  ├─ executeSingleTool(docker_build_and_test):
  │   │
  │   ├─ Repo 1: backend (BACKEND)
  │   │   ├─ Execute: mvn clean test
  │   │   ├─ exitCode = 1  ← ❌ ERRO!
  │   │   └─ Output: 
  │   │       ```
  │   │       [ERROR] CompileErrorCompanyController.java:42:
  │   │       class Company has no field 'email'
  │   │       ```
  │   │
  │   └─ validationResult = 
  │       ```
  │       ❌ VALIDAÇÃO FALHOU: Um ou mais repositórios...
  │       Erro: class Company has no field 'email'
  │       ```
  │
  ├─ Check: validationResult.contains("✅")? → NO
  ├─ Check: validationResult.contains("❌")? → YES
  │   │
  │   ├─ 🔄 NÃO FINALIZA - REALIMENTA AO LLM
  │   │
  │   ├─ context = context + 
  │   │   ```
  │   │   [GATE DE FINALIZAÇÃO] Erro detectado:
  │   │   ❌ VALIDAÇÃO FALHOU: Um ou mais repositórios apresentaram problemas.
  │   │   
  │   │   [ERROR] CompileErrorCompanyController.java:42:
  │   │   class Company has no field 'email'
  │   │   
  │   │   CORRIJA OS ERROS ACIMA E TENTE NOVAMENTE.
  │   │   ```
  │   │
  │   ├─ stepCount++ (now 7)
  │   └─ CONTINUE loop (não break)
  │
  ▼
ITERATION 6: Agent corrige o erro
  │
  ├─ callLLM(context + erro da validação):
  │   ├─ LLM reads: "[GATE] Erro: class Company has no field 'email'"
  │   ├─ LLM thinks: "Preciso adicionar o campo email na classe Company"
  │   └─ LLM Response: 
  │       ```
  │       Pensamento: O erro indica que a classe Company não tem o campo email que o Controller espera.
  │       Vou usar update_file para adicionar este campo.
  │       
  │       Ação: read_file
  │       Parâmetros: {"file_path": "/tmp/backend/src/.../Company.java"}
  │       ```
  │
  ├─ executeSingleTool(read_file) → Content da classe Company
  │
  ├─ context = context + "Resultado: Company.java contém: [...class code...]"
  │
  └─ Continue loop...
  │
  ▼
ITERATION 7: Atualizar código
  │
  ├─ callLLM(context com código lido):
  │   └─ LLM Response:
  │       ```
  │       Ação: update_file
  │       Parâmetros: {
  │         "file_path": "/tmp/backend/src/.../Company.java",
  │         "old_text": "    private String name;",
  │         "new_text": "    private String name;\n    private String email;"
  │       }
  │       ```
  │
  ├─ executeSingleTool(update_file)
  │   └─ File updated! Email field added.
  │
  └─ context = context + "Arquivo atualizado com sucesso"
  │
  ▼
ITERATION 8: Tentar finalizar novamente
  │
  ├─ callLLM(context):
  │   └─ LLM Response: "Finalizar: Campo email adicionado com sucesso"
  │
  ├─ ⚠️ GATE TRIGGERED NOVAMENTE
  │
  ├─ executeSingleTool(docker_build_and_test):
  │   │
  │   ├─ Repo 1: backend
  │   │   ├─ Execute: mvn clean test
  │   │   ├─ exitCode = 0  ← ✅ SUCCESS!
  │   │   └─ Output: "✓ Build + Test SUCESSO"
  │   │
  │   └─ validationResult = "✅ VALIDAÇÃO COMPLETA..."
  │
  ├─ Check: validationResult.contains("✅")? → YES
  │   ├─ alreadyValidatedBuild = true
  │   ├─ execution.setStatus("SUCCESS")
  │   └─ BREAK loop
  │
  ▼
Return AgentExecution (STATUS: SUCCESS)
  │
  ├─ stepCount: 8
  ├─ steps: [discovery, read_file, update_file, create_file, finalizar_FAIL, 
  │          docker_build_and_test_FAIL, read_file, update_file, 
  │          finalizar_SUCCESS, docker_build_and_test_SUCCESS]
  │
  ▼
USER recebe resposta com SUCCESS e detalhes de todos os 8 passos
```

---

## Estados e Transições

```
┌─────────────────────────────────────────────────────┐
│                   AGENT LOOP START                   │
└────────────────────────────┬────────────────────────┘
                             │
                    Create/Update Code
                    (Iterações 1-N)
                             │
                             ▼
┌─────────────────────────────────────────────────────┐
│  Agent Attempts to Finalize                          │
│  (Sends "Finalizar: ...")                            │
└────────────────┬────────────────────────────────────┘
                 │
         Is Final = true?
         /               \
       YES               NO
       │                  │
       ▼                  ▼
   Has Project?    Continue Loop
   /         \
 YES        NO
  │          │
  ▼          ▼
Run      Return SUCCESS
docker_build_and_test (sem gate)
  │
  │
  ▼
VALIDAÇÃO 
RESULTADO?
│
├─ ✅ SUCESSO ────┐
├─ ❌ FALHA  ─────┤
│                 │
▼                 ▼
finalize      Realimenta erro
SUCCESS       ao LLM
              │
              ▼
         Continue Loop
         (ITERATION N+1)
```

---

## Métrica-chave: Feedback Loop

O sistema implementa um **feedback loop automático**:

1. **Compilação falha** → Log capturado
2. **Erro no log** → Incluído no contexto do LLM
3. **LLM lê erro** → Compreende o problema
4. **LLM corrige** → Executa `update_file` ou `create_file`
5. **Volta para step 1** → Valida novamente

Este loop continua até:
- ✅ Build passar
- ❌ Atingir limite de iterações (30)

---

## Configuração de Limites

### Dentro do AgentLoop

```java
public AgentLoop(ChatClient chatClient, ToolRegistry toolRegistry, int maxSteps) {
    this.maxSteps = maxSteps;  // Padrão: 30
}
```

**Para alterar limite:**
```java
// Em ExecutorAgentService
this.agentLoop = new AgentLoop(this.chatClient, this.toolRegistry, 50);  // 50 iterações max
```

---

## Tratamento de Erros

### Se Docker não está disponível
```java
// DockerBuildAndTestTool.buildAndTestRepository() já trata
try {
    Process process = pb.start();
    // ...
} catch (Exception e) {
    output.append("ERRO ao executar build: ").append(e.getMessage());
    return output.toString();  // Retorna erro como string
}
```

### Se repositório não existe
```java
if (project == null || project.getRepos() == null || project.getRepos().isEmpty()) {
    return "ERRO: Nenhum repositório configurado no projeto para validação.";
}
```

---

## Otimizações Futuras

1. **Cache de compilação**: Reutilizar output anterior se arquivos não mudarm
2. **Timeout por repo**: Limitar tempo de build por repositório
3. **Compilação paralela**: Validar múltiplos repos em thread pool
4. **Snapshots**: Salvar snapshot antes/depois de cada build
5. **AI-powered error messages**: LLM analisa erro e sugere correção específica


