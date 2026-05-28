# 📦 Serviço Scanner - Resumo de Curl Examples

## ✅ O que foi adicionado

### 📝 Arquivos Criados

1. **CURL_EXAMPLES.md** (6.4 KB)
   - 20 exemplos de curl prático
   - Respostas esperadas em JSON
   - Dicas de filtros com jq
   - Tratamento de erros

2. **CURL_EXAMPLES.sh** (16 KB)
   - Script bash executável
   - 15 exemplos formatados
   - Instruções de uso
   - Pronto para rodar: `bash CURL_EXAMPLES.sh`

3. **RepositoryScannerController.java** (ATUALIZADO)
   - Javadoc com exemplos de curl nos métodos
   - Respostas JSON esperadas
   - Documentação integrada

## 🎯 20 Exemplos de Curl Inclusos

### Básicos
1. ✅ Scan básico
2. ✅ Scan com chunk customizado (5000)
3. ✅ Scan com chunk pequeno (1000)
4. ✅ Scan com JSON formatado (jq)

### Configuração
5. ✅ Obter configuração
6. ✅ Obter configuração formatada

### Variações
7. ✅ Escanear repositório atual
8. ✅ Salvar resposta em arquivo

### Análises
9. ✅ Contar total de chunks
10. ✅ Listar apenas caminhos de arquivos
11. ✅ Listar apenas linguagens
12. ✅ Listar apenas tipos
13. ✅ Contar arquivos por tipo
14. ✅ Contar arquivos por linguagem

### Filtros
15. ✅ Filtrar apenas Java
16. ✅ Filtrar apenas Controllers
17. ✅ Listar arquivos Java Services
18. ✅ Salvar resposta formatada

### Erros
19. ✅ Tratamento - Caminho inválido
20. ✅ Tratamento - Parâmetro ausente

## 🚀 Como Usar

### Iniciar Servidor
```bash
mvn spring-boot:run
```

### Executar Exemplos (Opção 1 - Script)
```bash
bash CURL_EXAMPLES.sh
```

### Executar Exemplos (Opção 2 - Manual)
```bash
# Básico
curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp"

# Com jq (formatado)
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq .

# Configuração
curl -X GET "http://localhost:8080/api/scanner/config"
```

## 📚 Exemplos Mais Usados

### Contar Chunks
```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.totalChunks'
```

### Listar Linguagens
```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[].language' | sort | uniq
```

### Filtrar Controllers
```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.type=="CONTROLLER")'
```

### Filtrar Services
```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" | jq '.codeFiles[] | select(.type=="SERVICE")'
```

### Salvar em Arquivo
```bash
curl -s -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp" > resultado.json
```

## 📖 Documentação Relacionada

- **CURL_EXAMPLES.md** - Exemplos com explicações
- **CURL_EXAMPLES.sh** - Script executável
- **SCANNER_README.md** - Documentação completa
- **SCANNER_QUICK_START.md** - Guia rápido
- **RepositoryScannerController.java** - Javadoc com exemplos

## 💡 Requisitos

### Essencial
- curl (geralmente pré-instalado)
- Servidor rodando: `mvn spring-boot:run`

### Opcional (Para JSON formatado)
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq

# CentOS/RHEL
sudo yum install jq
```

## 🎓 Próximos Passos

1. ✅ Leia CURL_EXAMPLES.md
2. ✅ Execute bash CURL_EXAMPLES.sh
3. ✅ Adapte os exemplos para seus repositórios
4. ✅ Integre com scripts/automação

## ✨ Status Final

```
✅ Exemplos de curl adicionados
✅ Script bash criado
✅ Javadoc atualizado
✅ Compilação bem-sucedida
✅ Pronto para uso
```

**Tudo pronto para testar a API! 🎉**

