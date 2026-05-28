# 🚀 Comece Agora

## Instalação e Teste

### 1️⃣ Compilar o Projeto

```bash
cd /home/gustavo/dev/teste-spring-ia/springia
mvn clean compile
```

### 2️⃣ Executar os Testes

```bash
# Todos os testes
mvn test

# Teste específico
mvn test -Dtest=RepositoryScannerServiceTest
mvn test -Dtest=LanguageDetectorTest
```

### 3️⃣ Usar em Seu Código

#### Opção A: Injeção de Dependência (Recomendado)

```java
@Component
public class MyAnalyzer {
    
    @Autowired
    private RepositoryScannerService scanner;
    
    public void analyze() {
        List<CodeFile> files = scanner.scan("/path/to/repo");
        // Seu código aqui
    }
}
```

#### Opção B: Via REST API

```bash
# Terminal 1: Iniciar aplicação
mvn spring-boot:run

# Terminal 2: Fazer requisição
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp"
```

## 📚 Documentação

| Documento | Conteúdo |
|-----------|----------|
| **SCANNER_README.md** | 📖 Documentação completa (300+ linhas) |
| **SCANNER_QUICK_START.md** | 🚀 Guia rápido com exemplos |
| **SCANNER_SUMMARY.md** | 📊 Resumo visual e status |

## 🎯 Próximos Passos

### 1. Explorar Exemplos
Veja `RepositoryScannerExample.java` com 9 exemplos práticos

### 2. Integrar com IA
```java
// Processar com IA
List<CodeFile> files = scanner.scan(path);
files.forEach(file -> {
    String analysis = aiService.analyze(file.getContent());
    // Sua lógica aqui
});
```

### 3. Criar Parser
Estenda `RepositoryScannerService` para extrair AST

### 4. Gerar Documentação
Use os `CodeFile` para gerar Constitution, Spec, etc.

## 📋 Checklist de Implementação

- ✅ Estrutura de pacotes criada
- ✅ Classe CodeFile com todos os atributos
- ✅ RepositoryScannerService.scan() implementado
- ✅ Caminhada recursiva com Files.walk()
- ✅ Ignorar 13 diretórios
- ✅ Processar 8 extensões
- ✅ LanguageDetector criado
- ✅ CodeTypeClassifier criado
- ✅ Chunker implementado
- ✅ Quebra em chunks funcionando
- ✅ REST API criada
- ✅ 35 testes passando
- ✅ Documentação completa
- ✅ Exemplos práticos
- ✅ Pronto para evolução

## 🔗 Links Úteis

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Files.walk() JavaDoc](https://docs.oracle.com/javase/9/docs/api/java/nio/file/Files.html#walk-java.nio.file.Path-java.nio.file.FileVisitOption...-)
- [Lombok Documentation](https://projectlombok.org/)

## 💬 Perguntas Frequentes

### P: Como mudar o tamanho do chunk?
```java
// Padrão: 2000
List<CodeFile> files = scanner.scan(path, 5000);
```

### P: Como ignorar mais diretórios?
Edite `RepositoryScannerService.IGNORED_DIRECTORIES`

### P: Como adicionar mais extensões?
Edite `RepositoryScannerService.SUPPORTED_EXTENSIONS`

### P: Como add mais linguagens?
1. Adicione enum em `Language.java`
2. Adicione case em `LanguageDetector.detectLanguage()`

### P: Como add mais tipos?
1. Adicione enum em `CodeType.java`
2. Adicione detecção em `CodeTypeClassifier.classify()`

## 🎓 Estrutura de Aprendizado

1. **Iniciar**: Leia `SCANNER_QUICK_START.md`
2. **Entender**: Explore `RepositoryScannerExample.java`
3. **Usar**: Injete `RepositoryScannerService` no seu código
4. **Estender**: Customize conforme necessário
5. **Integrar**: Conecte com IA/Parser/etc

## 📞 Suporte

Se tiver dúvidas:
1. Verifique a documentação em `SCANNER_README.md`
2. Veja exemplos em `RepositoryScannerExample.java`
3. Execute os testes para referência
4. Veja logs com `logger.info()`

---

🎉 **Pronto para começar!**

Qualquer dúvida, veja a documentação ou os exemplos.

