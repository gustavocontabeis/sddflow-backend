# Scanner de Repositórios - Guia Rápido

## 📋 O que foi criado

Um serviço completo, pronto para produção, que escaneia repositórios locais e retorna uma lista estruturada de arquivos de código divididos em chunks. Perfeito como base para parser de código e integração com IA.

### Estrutura de Pacotes

```
com.example.springia.scanner/
├── model/
│   ├── CodeFile.java           # Modelo principal com metadados
│   ├── CodeType.java           # Enum: CONTROLLER, SERVICE, REPOSITORY, ENTITY, UNKNOWN
│   └── Language.java           # Enum: JAVA, TYPESCRIPT, JAVASCRIPT, HTML, JSON, XML, YAML
├── service/
│   └── RepositoryScannerService.java  # Orquestrador principal (@Service)
├── util/
│   ├── LanguageDetector.java          # Detecta linguagem por extensão
│   ├── CodeTypeClassifier.java        # Classifica tipo por anotações Spring
│   └── Chunker.java                   # Divide conteúdo em chunks
└── controller/
    └── RepositoryScannerController.java # Endpoints REST
```

## 🚀 Uso Rápido

### 1. Via Serviço (Injeção de Dependência)

```java
@Component
public class MyCodeAnalyzer {
    
    @Autowired
    private RepositoryScannerService scanner;
    
    public void analyzeProject() {
        // Escanear repositório
        List<CodeFile> files = scanner.scan("/path/to/repository");
        
        // Processar arquivos
        files.forEach(file -> {
            System.out.println("Arquivo: " + file.getPath());
            System.out.println("Tipo: " + file.getType());
            System.out.println("Linguagem: " + file.getLanguage());
            System.out.println("Chunk " + file.getChunkNumber() + "/" + file.getTotalChunks());
        });
    }
}
```

### 2. Via REST API

**Endpoint:** `POST /api/scanner/scan`

```bash
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp&chunkSize=2000"
```

**Response:**
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
      "content": "package com.example;\n\nimport org.springframework.web.bind.annotation.*;\n..."
    },
    ...
  ]
}
```

### 3. Configuração

```bash
# Obter configuração do scanner
GET /api/scanner/config

# Response
{
  "supportedExtensions": [".java", ".ts", ".js", ".html", ".json", ".xml", ".yml", ".yaml"],
  "ignoredDirectories": ["node_modules", "target", "dist", ".git", ...],
  "defaultChunkSize": 2000
}
```

## 🔧 Componentes Principais

### RepositoryScannerService

```java
// Scan com tamanho padrão (2000 caracteres)
List<CodeFile> files = scannerService.scan("/path/to/repo");

// Scan com tamanho customizado
List<CodeFile> files = scannerService.scan("/path/to/repo", 5000);

// Obter configurações
Set<String> extensions = RepositoryScannerService.getSupportedExtensions();
Set<String> ignored = RepositoryScannerService.getIgnoredDirectories();
```

### LanguageDetector

```java
Language lang = LanguageDetector.detectLanguage("MyClass.java");
// retorna: Language.JAVA

Language lang = LanguageDetector.detectLanguage("component.ts");
// retorna: Language.TYPESCRIPT
```

### CodeTypeClassifier

```java
String javaCode = "@Service\npublic class UserService { ... }";
CodeType type = CodeTypeClassifier.classify(javaCode, Language.JAVA);
// retorna: CodeType.SERVICE
```

### Chunker

```java
// Dividir em chunks
List<String> chunks = Chunker.chunk(largeContent, 2000);

// Contar chunks
int count = Chunker.getChunkCount(largeContent, 2000);
```

## 📊 Funcionalidades

### Varredura Inteligente
- ✅ Recursiva com `Files.walk()`
- ✅ Ignora diretórios: node_modules, target, dist, .git, etc.
- ✅ Apenas extensões suportadas: .java, .ts, .js, .html, .json, .xml, .yml, .yaml
- ✅ Logging detalhado para debug

### Detecção Precisa
- ✅ Linguagem por extensão (8 linguagens)
- ✅ Tipo por anotações (@Controller, @Service, @Repository, @Entity)
- ✅ Metadados (tamanho, linhas, chunks)

### Chunking Eficiente
- ✅ Respeita quebras de linha
- ✅ Customizável (padrão 2000 chars)
- ✅ Preserva metadados do arquivo original

## 🧪 Testes

Todos os componentes têm testes unitários:

```bash
mvn test -Dtest=LanguageDetectorTest
mvn test -Dtest=ChunkerTest
mvn test -Dtest=CodeTypeClassifierTest
mvn test -Dtest=RepositoryScannerServiceTest
```

## 📈 Casos de Uso

### 1. Geração de Documentação

```java
List<CodeFile> controllers = scanner.scan(path).stream()
    .filter(f -> f.getType() == CodeType.CONTROLLER)
    .collect(Collectors.toList());

controllers.forEach(file -> generateApiDocumentation(file));
```

### 2. Análise de Código com IA

```java
List<CodeFile> files = scanner.scan(path);

files.stream()
    .filter(f -> f.getType() == CodeType.SERVICE)
    .forEach(file -> {
        String analysis = aiService.analyze(file.getContent());
        repository.save(new CodeAnalysis(file, analysis));
    });
```

### 3. Estatísticas de Repositório

```java
List<CodeFile> files = scanner.scan(path);

Map<Language, Long> byLanguage = files.stream()
    .collect(Collectors.groupingBy(CodeFile::getLanguage, 
                                   Collectors.counting()));

Map<CodeType, Long> byType = files.stream()
    .collect(Collectors.groupingBy(CodeFile::getType, 
                                   Collectors.counting()));

System.out.println("Linguagens: " + byLanguage);
System.out.println("Tipos: " + byType);
```

### 4. Pipeline de Processamento

```java
public class CodeProcessingPipeline {
    
    public void process(String repositoryPath) {
        // 1. Scan
        List<CodeFile> files = scanner.scan(repositoryPath);
        
        // 2. Filtrar
        List<CodeFile> javaFiles = files.stream()
            .filter(f -> f.getLanguage() == Language.JAVA)
            .collect(Collectors.toList());
        
        // 3. Processar
        javaFiles.forEach(file -> {
            parse(file);
            analyze(file);
            store(file);
        });
        
        // 4. Gerar relatório
        generateReport(files);
    }
}
```

## 📝 Resposta a Requisitos

| Requisito | Status | Componente |
|-----------|--------|-----------|
| Pacotes estruturados | ✅ | scanner/model, service, util |
| Classe CodeFile com atributos | ✅ | CodeFile.java |
| RepositoryScannerService.scan() | ✅ | RepositoryScannerService.java |
| Caminhada recursiva | ✅ | Files.walk() |
| Ignorar diretórios | ✅ | IGNORED_DIRECTORIES set |
| Extensões suportadas | ✅ | SUPPORTED_EXTENSIONS set |
| Ler conteúdo | ✅ | Files.readString() |
| Detectar linguagem | ✅ | LanguageDetector.java |
| Classificar tipo | ✅ | CodeTypeClassifier.java |
| Chunker com 2000 chars | ✅ | Chunker.java |
| Quebra em chunks | ✅ | scan() método |
| Utilitário LanguageDetector | ✅ | LanguageDetector.java |
| Código limpo e bem organizado | ✅ | Seguindo boas práticas |
| Sem exceções silenciosas | ✅ | Logging e tratamento adequado |
| Pronto para parser + IA | ✅ | Estrutura extensível |

## 🔮 Próximos Passos (Evolução)

1. **Parser de Código**: Extrair AST (Abstract Syntax Tree)
2. **IA Integration**: Gerar documentação automática
3. **Caching**: Implementar cache em memória/banco
4. **Parallelização**: Processar múltiplos arquivos em paralelo
5. **Banco de Dados**: Persistir análises
6. **Webhooks**: Notificações de mudanças
7. **UI Dashboard**: Visualizador de repositoires

## 📚 Documentação Completa

Veja `SCANNER_README.md` para documentação detalhada com exemplos.

---

✨ **Status**: Pronto para uso em produção!
🎯 **Próximo**: Integrar com parser de AST ou IA para análise

