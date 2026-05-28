# 🔌 Exemplos de CURL - Scanner API

Exemplos práticos de como usar a API REST do Scanner.

## Prerequisites

```bash
# Iniciar o servidor Spring Boot
cd /home/gustavo/dev/teste-spring-ia/springia
mvn spring-boot:run
```

## Base URL

```
http://localhost:8080/api/scanner
```

## Exemplos

### 1️⃣ Scan Básico

Escaneia um repositório com tamanho de chunk padrão (2000 caracteres).

```bash
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp"
```

**Resposta:**
```json
{
  "success": true,
  "totalChunks": 150,
  "totalFiles": 25,
  "codeFiles": [
    {
      "path": "src/main/java/com/example/UserController.java",
      "language": "JAVA",
      "type": "CONTROLLER",
      "chunkNumber": 1,
      "totalChunks": 2,
      "fileSize": 3500,
      "lineCount": 95,
      "content": "package com.example;..."
    },
    ...
  ]
}
```

### 2️⃣ Scan com Chunk Customizado (Maior)

Aumentar tamanho dos chunks para 5000 caracteres.

```bash
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp&chunkSize=5000"
```

### 3️⃣ Scan com Chunk Pequeno

Reduzir tamanho dos chunks para 1000 caracteres.

```bash
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp&chunkSize=1000"
```

### 4️⃣ Scan com JSON Formatado

Usar `jq` para formatar e embelezar a resposta JSON.

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq .
```

### 5️⃣ Obter Configuração

Verificar extensões suportadas, diretórios ignorados e tamanho padrão.

```bash
curl -X GET "http://localhost:8080/api/scanner/config"
```

**Resposta:**
```json
{
  "supportedExtensions": [".java", ".ts", ".js", ".html", ".json", ".xml", ".yml", ".yaml"],
  "ignoredDirectories": ["node_modules", "target", "dist", ".git", "build", "__pycache__", ".venv", "venv", ".env", ".gradle", ".mvn", ".idea", ".vscode"],
  "defaultChunkSize": 2000
}
```

### 6️⃣ Obter Configuração Formatada

```bash
curl -s -X GET "http://localhost:8080/api/scanner/config" | jq .
```

### 7️⃣ Descobrir Stack da Aplicação

Retorna linguagem principal, frameworks, bibliotecas, bancos, cloud e manifestos detectados.

```bash
curl -s -X GET "http://localhost:8080/api/scanner/stack?repositoryPath=/home/user/myapp" | jq .
```

**Resposta (exemplo):**
```json
{
  "rootPath": "/home/user/myapp",
  "primaryLanguage": "java",
  "javaVersion": "21",
  "springBootVersion": "4.0.6",
  "buildTools": ["Maven"],
  "frameworks": ["Spring Boot", "Spring AI", "Spring MVC"],
  "libraries": ["Spring Data JPA", "Lombok", "Spring Boot Actuator"],
  "databases": ["H2 Database"],
  "cloudServices": ["Azure Spring Cloud"]
}
```

## Análise de Resultados

### 7️⃣ Contar Total de Chunks

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.totalChunks'
```

**Saída:**
```
150
```

### 8️⃣ Contar Total de Arquivos Únicos

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.totalFiles'
```

### 9️⃣ Listar Caminhos de Todos os Arquivos

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[].path'
```

**Saída:**
```
"src/main/java/com/example/UserController.java"
"src/main/java/com/example/UserService.java"
"src/main/java/com/example/UserRepository.java"
...
```

### 🔟 Listar Linguagens Detectadas

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[].language' | sort | uniq
```

**Saída:**
```
"HTML"
"JAVA"
"JSON"
"XML"
"YAML"
```

### 1️⃣1️⃣ Estatística de Tipos

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[].type' | sort | uniq -c
```

**Saída:**
```
      5 "CONTROLLER"
     12 "SERVICE"
      8 "REPOSITORY"
      3 "ENTITY"
     50 "UNKNOWN"
```

### 1️⃣2️⃣ Filtrar Apenas Arquivos Java

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.language=="JAVA")'
```

### 1️⃣3️⃣ Filtrar Apenas Controllers

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.type=="CONTROLLER")'
```

### 1️⃣4️⃣ Filtrar Apenas Services

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.type=="SERVICE")'
```

### 1️⃣5️⃣ Listar Arquivos Java Services

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.language=="JAVA" and .type=="SERVICE")'
```

### 1️⃣6️⃣ Contar Arquivos por Tipo

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '[.codeFiles[].type] | group_by(.) | map({type: .[0], count: length})'
```

**Saída:**
```json
[
  {"type": "CONTROLLER", "count": 5},
  {"type": "SERVICE", "count": 12},
  {"type": "REPOSITORY", "count": 8},
  {"type": "ENTITY", "count": 3},
  {"type": "UNKNOWN", "count": 50}
]
```

## Salvando Resultados

### 1️⃣7️⃣ Salvar Resposta em Arquivo

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" > scan_result.json
```

### 1️⃣8️⃣ Salvar Resposta Formatada

```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq . > scan_result_formatted.json
```

## Tratamento de Erros

### 1️⃣9️⃣ Caminho Inválido

```bash
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/path/que/nao/existe"
```

**Resposta (400):**
```json
{
  "success": false,
  "error": "Caminho não existe: /path/que/nao/existe"
}
```

### 2️⃣0️⃣ Parâmetro Obrigatório Ausente

```bash
curl -X POST "http://localhost:8080/api/scanner/scan"
```

**Resposta (400):**
```json
{
  "success": false,
  "error": "Required request parameter 'repositoryPath' for method parameter type String is not present"
}
```

## Dicas Úteis

### Instalar jq (se não tiver)

```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq

# CentOS/RHEL
sudo yum install jq
```

### Usando com Pretty Print

```bash
# Compacto
curl -s -X POST "..." | jq -c '.'

# Pretty print (padrão)
curl -s -X POST "..." | jq '.'

# Colorido
curl -s -X POST "..." | jq -C '.'
```

### Modo Silencioso (-s)

```bash
# Com progress
curl -X POST "http://..."

# Silencioso (recomendado para scripts)
curl -s -X POST "http://..."
```

### Variáveis de Ambiente

```bash
# Definir base URL
BASE_URL="http://localhost:8080/api/scanner"
REPO_PATH="/home/user/myapp"

# Usar variáveis
curl -X POST "${BASE_URL}/scan?repositoryPath=${REPO_PATH}"
```

## Script Automático

Para executar todos os exemplos:

```bash
bash CURL_EXAMPLES.sh
```

Veja o arquivo `CURL_EXAMPLES.sh` para mais exemplos com contexto completo.

---

💡 **Dica:** Copie e cole estes comandos no seu terminal para testar a API!

