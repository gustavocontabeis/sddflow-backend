#!/bin/bash

# ========================================================
# EXEMPLO: Como usar o Agent com Gate de Finalização
# ========================================================

# 1. Crie um projeto com repositórios no banco
# INSERT INTO project (sigla, name) VALUES ('TEST', 'Test Project');
# INSERT INTO code_repo (name, path, type, project_id) VALUES ('backend', '/tmp/backend', 'B', 1);

# 2. Execute o agent com validação automática
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie uma classe JPA chamada User com campos email e passwordHash. A classe deve ter anotações @Entity, @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor e validações @NotNull, @NotBlank, @Size",
    "projectId": 1,
    "basePath": "springia-workspace"
  }' | jq .

# ========================================================
# O que acontece:
# ========================================================
# 1. Agent recebe a tarefa
# 2. Agent executa discovery_tool para entender código existente
# 3. Agent cria os arquivos conforme solicitado (create_file, update_file)
# 4. Agent tenta "Finalizar"
# 5. [NOVO] Gate automático: Agent executa docker_build_and_test
#    - Compila o código com Maven
#    - Se sucesso: Finaliza com status SUCCESS
#    - Se falha: LLM recebe erro e corrige automaticamente
# 6. Loop continua até sucesso ou atingir máximo de iterações
#
# ========================================================
# Resposta esperada:
# ========================================================
# {
#   "executionId": "uuid-xxxxx",
#   "status": "SUCCESS",
#   "finalAnswer": "Classe User criada com sucesso em User.java",
#   "stepCount": 10,
#   "totalExecutionTimeMs": 45000,
#   "steps": [
#     {
#       "stepNumber": 1,
#       "toolName": "discovery_tool",
#       "toolResult": "Estrutura descoberta: existem 2 classes..."
#     },
#     {
#       "stepNumber": 2,
#       "toolName": "create_file",
#       "toolResult": "Arquivo User.java criado"
#     },
#     ...
#     {
#       "stepNumber": 8,
#       "toolName": "docker_build_and_test",
#       "toolResult": "✅ VALIDAÇÃO COMPLETA: Todos repositórios compilados com sucesso!"
#     },
#     {
#       "stepNumber": 9,
#       "isFinal": true,
#       "finalAnswer": "..."
#     }
#   ]
# }

# ========================================================
# Para ver logs do Agent em tempo real:
# ========================================================
# curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent" \
#   -H "Content-Type: application/json" \
#   -d '{"configuredLevel":"DEBUG"}'

# ========================================================
# Variações de uso:
# ========================================================

# Caso 1: Com projeto específico
echo "=== Caso 1: Com Project ID ==="
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Implemente validações de email na classe User",
    "projectId": 1
  }' | jq .

# Caso 2: Sem projeto (sem gate de validação)
echo "=== Caso 2: Sem Project ID (sem validação Docker) ==="
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie uma classe simples de teste",
    "basePath": "tmp-test"
  }' | jq .

# Caso 3: Com basePath customizado
echo "=== Caso 3: Com basePath customizado ==="
curl -X POST http://localhost:8080/executor-agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "taskDescription": "Crie estrutura de diretórios para novo módulo",
    "projectId": 2,
    "basePath": "modulo-novo"
  }' | jq .

