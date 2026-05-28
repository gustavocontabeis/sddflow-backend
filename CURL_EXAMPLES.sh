#!/bin/bash

# ╔═══════════════════════════════════════════════════════════════════════════════╗
# ║          EXEMPLOS DE CURL - REPOSITORY SCANNER API                           ║
# ╚═══════════════════════════════════════════════════════════════════════════════╝

# Base URL
BASE_URL="http://localhost:8080/api/scanner"

echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
echo "║          EXEMPLOS DE CURL - REPOSITORY SCANNER API                           ║"
echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 1. SCAN BÁSICO - Usar com repositório local
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣  SCAN BÁSICO (tamanho de chunk padrão: 2000)"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\""
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 2. SCAN COM CHUNK CUSTOMIZADO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "2️⃣  SCAN COM CHUNK CUSTOMIZADO (5000 caracteres)"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp&chunkSize=5000\""
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 3. SCAN COM CHUNK PEQUENO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "3️⃣  SCAN COM CHUNK PEQUENO (1000 caracteres)"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp&chunkSize=1000\""
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 4. SCAN COM PRETTY JSON
# ═══════════════════════════════════════════════════════════════════════════════════
echo "4️⃣  SCAN COM JSON FORMATADO (usando jq)"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq ."
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 5. OBTER CONFIGURAÇÃO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "5️⃣  OBTER CONFIGURAÇÃO DO SCANNER"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X GET \"${BASE_URL}/config\""
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 6. OBTER CONFIGURAÇÃO COM JSON FORMATADO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "6️⃣  OBTER CONFIGURAÇÃO FORMATADA"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X GET \"${BASE_URL}/config\" | jq ."
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 7. ESCANEAR REPOSITÓRIO ATUAL
# ═══════════════════════════════════════════════════════════════════════════════════
echo "7️⃣  ESCANEAR REPOSITÓRIO ATUAL (padrão: 2000)"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
CURRENT_DIR=$(pwd)
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=${CURRENT_DIR}\""
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 8. SALVAR RESPOSTA EM ARQUIVO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "8️⃣  SALVAR RESPOSTA EM ARQUIVO JSON"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" > scan_result.json"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 9. CONTAR TOTAL DE CHUNKS
# ═══════════════════════════════════════════════════════════════════════════════════
echo "9️⃣  CONTAR TOTAL DE CHUNKS"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.totalChunks'"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 10. LISTAR APENAS CAMINHO DOS ARQUIVOS
# ═══════════════════════════════════════════════════════════════════════════════════
echo "🔟 LISTAR APENAS CAMINHO DOS ARQUIVOS"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.codeFiles[].path'"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 11. LISTAR APENAS LINGUAGENS
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣1️⃣ LISTAR APENAS LINGUAGENS"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.codeFiles[].language' | sort | uniq"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 12. LISTAR APENAS TIPOS
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣2️⃣ LISTAR APENAS TIPOS"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.codeFiles[].type' | sort | uniq -c"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 13. FILTRAR APENAS JAVA
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣3️⃣ FILTRAR APENAS ARQUIVOS JAVA"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.codeFiles[] | select(.language==\"JAVA\")'"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 14. FILTRAR APENAS CONTROLLERS
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣4️⃣ FILTRAR APENAS CONTROLLERS"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '.codeFiles[] | select(.type==\"CONTROLLER\")'"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════════
# 15. CONTAR ARQUIVOS POR TIPO
# ═══════════════════════════════════════════════════════════════════════════════════
echo "1️⃣5️⃣ CONTAR ARQUIVOS POR TIPO"
echo "─────────────────────────────────────────────────────────────────────────────"
echo ""
echo "curl -s -X POST \"${BASE_URL}/scan?repositoryPath=/home/user/myapp\" | jq '[.codeFiles[].type] | group_by(.) | map({type: .[0], count: length})'"
echo ""

echo "═══════════════════════════════════════════════════════════════════════════════════"
echo ""
echo "💡 DICAS:"
echo "   • Use 'jq' para formatar JSON: pip install jq (ou apt-get install jq)"
echo "   • Use '-s' nos curls para modo silencioso (não mostra progresso)"
echo "   • Substitua '/home/user/myapp' pelo caminho real do seu repositório"
echo "   • O servidor deve estar rodando antes de executar os curls"
echo ""
echo "🚀 INICIAR SERVIDOR:"
echo "   cd /home/gustavo/dev/teste-spring-ia/springia"
echo "   mvn spring-boot:run"
echo ""

