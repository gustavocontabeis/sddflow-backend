# SpringIA

API Spring Boot com:
- health check via Actuator
- endpoint POST que recebe nome e idade e gera mensagem de aniversario com LLM

## Pre-requisitos

- Java 21
- Maven Wrapper (`./mvnw`)

## Executar

```bash
cd /home/gustavo/dev/teste-spring-ia/springia
./mvnw spring-boot:run
```

## Endpoints

### Health check

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:

```json
{"status":"UP"}
```

### Gerar mensagem de aniversario

```bash
curl -X POST 'http://localhost:8080/api/birthday-message' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Maria",
    "age": 30
  }'
```

### Chat - Enviar mensagem

```bash
curl -X POST 'http://localhost:8080/chat?sessionId=sessao-1' \
  -H 'Content-Type: text/plain' \
  -d 'Quero um sistema de agendamento para clinica'
```

### Chat - Gerar e gravar especificacao

```bash
curl -X POST 'http://localhost:8080/chat/specification?sessionId=sessao-1'
```

### Chat - Buscar ultima especificacao gravada

```bash
curl -X GET 'http://localhost:8080/chat/specification?sessionId=sessao-1'
```

### GitHub - Listar repositórios

```bash
curl -X GET 'http://localhost:8080/api/github/repos/octocat'
```

### GitHub - Fazer commit

```bash
curl -X POST 'http://localhost:8080/api/github/commit' \
  -H 'Content-Type: application/json' \
  -d '{
    "owner": "seu-usuario",
    "repo": "seu-repo",
    "branch": "main",
    "message": "Atualizar README",
    "filePath": "README.md",
    "fileContent": "# Meu projeto"
  }'
```

### GitHub - Criar Pull Request

```bash
curl -X POST 'http://localhost:8080/api/github/pull-request' \
  -H 'Content-Type: application/json' \
  -d '{
    "owner": "seu-usuario",
    "repo": "seu-repo",
    "title": "Feature: Novo endpoint",
    "description": "Adicionando novo endpoint",
    "headBranch": "feature/search",
    "baseBranch": "main"
  }'
```

## Variáveis de ambiente

```bash
export GITHUB_TOKEN='seu_token_github'
```

## Rodar testes

```bash
./mvnw test
```

Se ocorrer erro de versao do Java, confirme que `java -version` esta em Java 21.





```terminal
curl -X POST http://localhost:8080/api/projects \
  -H 'Content-Type: application/json' \
  -d '{
    "sigla":"ABCDE",
    "name":"Spring IA",
    "constitution":"
      # Estrutura do projeto
      ## backend
      ### Stack
        - Java 21
        - Spring Boot
        - Spring Data JPA
      ### Estrutura de pastas
        - src/main/java/br/com/codersistemas/condominiosadm
          - conections - conexoes com sistemas externos
          - constantes - constantes do sistema
          - consumer - consumidores de eventos de RabbitMQ
          - controller - Endipoints REST
          - domain - entidades do sistema
          - dto - objetos de transferencia de dados
          - enums - enumeradores do sistema
          - repository - repositórios de acesso a dados
          - service - regras de negocio
          - specification - especificações para consultas dinâmicas
      ## frontend
      ### Stack
        - Angular 16
        - Spring Boot
        - Spring Data JPA
      ### Estrutura de pastas
        - src/app
          - features - funcionalidades do sistema
          - service - serviços de comunicação com backend
          - model - modelos de dados
    ",
    "repos":[
      {"path":"https://github.com/gustavocontabeis/catalogo-musical-api","type":"BACKEND","branch":"master","name":"microservico-unico"},
      {"path":"https://github.com/gustavocontabeis/catalogo-musical","type":"FRONTEND","branch":"master","name":"frontend-unico"},
    ]
  }'
```