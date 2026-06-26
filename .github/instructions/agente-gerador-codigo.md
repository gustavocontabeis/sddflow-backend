## Arquitetura Correta (Multi-Stage + Guardrails)

### Fluxo obrigatório
1. DiscoveryTool gera contexto semântico estruturado em JSON.
2. Code Planning Agent valida o plano antes de qualquer alteração.
3. read_file é obrigatório antes de alterar qualquer arquivo Java.
4. Static Validator Agent valida sintaxe, imports, tipos e existência de classes.
7. Feedback Agent devolve erros estruturados para o próximo ciclo.

### Guardrails obrigatórios
- Não criar classes novas sem necessidade explícita.
- Não inventar imports, tipos, métodos ou packages.
- Não alterar package de arquivo Java existente.
- Não sobrescrever arquivos existentes com `create_file`.
- Sempre conferir `package`, `imports`, classes e assinaturas antes de gerar código.
- Sempre ler o arquivo completo com `read_file` antes de `update_file`.
- Se o plano estiver inválido, abortar a geração.
- Antes de finalizar, validar com `validate_java_code` e depois com `docker_build_and_test`.

### Estrutura esperada do plano
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

### Estrutura esperada da validação estática
```json
{
  "errorType": "COMPILATION",
  "file": "UserService.java",
  "line": 42,
  "message": "Cannot find symbol UserDTO"
}
```

### Regras finais
- Planejar → validar → gerar → validar → executar.
- Nunca aceitar `Finalizar:` sem a validação Docker passar.
- O log de compilação deve refletir o build da imagem Docker quando houver Dockerfile.

