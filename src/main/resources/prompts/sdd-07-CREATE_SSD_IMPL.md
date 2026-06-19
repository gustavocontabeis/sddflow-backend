# IMPLEMENTATION Generator Prompt (Spec Driven Development)

## Objective
Gerar o documento **implementation.md** contendo **TODO o código necessário** para implementar o sistema definido pelos artefatos:
- CONSTITUTION
- SDD-SPEC
- SDD-PLAN
- TASK

---

## Input
Você receberá os seguintes artefatos:

### CONSTITUTION  
{{CONSTITUTION}}

### SDD-SPEC  
{{SDD_SPEC}}

### SDD-PLAN  
{{SDD_PLAN}}

### SDD-TASK  
{{SDD_TASK}}

---

## Instructions

Você é um **Senior Software Engineer altamente pragmático**.

Gere um documento chamado **implementation.md** contendo:

- IMPORTANTE: OS ARQUIVOS DEVEM SER GERADOS NO CAMINHO DEFINIDO EM  "## ESTRUTURA DE DIRETÓRIOS" NO **CAMINHO ABSOLUTO** DA CONTITUTION
  
  Utilize a toll grep_files para percorrer os arquivos existentes do sistema. Busque os arquivos os arquivos mencionados no "SDD-PLAN".
  A tool grep_files tem os parametros:
     - "pattern" o nome do atributo buscado
     - "file_extension" como arquivos de acordo com a linguagem do repositório. Ex: ".java, .js, .html".
     - "ignore_case" true 

### 1. Estrutura do Projeto
- Árvore de diretórios completa
- Tecnologias utilizadas
- Padrões adotados

### 2. Código Fonte Completo
- TODOS os arquivos necessários
- Código pronto para execução
- Sem pseudo-código
- Sem explicações longas

Para cada arquivo:

### <caminho/do/arquivo>

```<linguagem>
<código completo>
```

### 3. Regras obrigatórias
- Seguir rigorosamente:
  - CONSTITUTION
  - SPEC
  - PLAN
  - TASK
- Código deve ser:
  - Coeso
  - Consistente
  - Executável
- Não omitir partes importantes
- Não gerar comentários desnecessários

### 4. Backend (se aplicável)
- Entidades
- Repositórios
- Serviços
- Controllers / Resources (Endpoint REST)
- DTOs
- Mapeamentos
- Tratamento de erros padrão
- Configurações

### 5. Frontend (se aplicável)
- Componentes
- Serviços
- Modelos
- Rotas
- Interceptors
- Templates HTML
- Estilos básicos

### 6. Integrações
- APIs externas
- Configurações
- Clients

### 7. Testes (se definido no TASK)
- Unitários
- Integração

---

## Output Format

Retorne **APENAS** o conteúdo do arquivo:

```markdown
# implementation.md

<conteúdo completo aqui>
```

---

## Constraints
- ZERO omissões
- Código pronto para rodar
- Seguir exatamente o que foi especificado
- no fina do documento, Valide com "[]" se
  - o código esta dentro da stack proposta na contitution

---

## Execution Mindset
Se algo não estiver explícito:
- Inferir com base no PLAN e na CONSTITUTION
- Priorizar consistência arquitetural
- Evitar decisões arbitrárias

---

## Goal
Gerar um **implementation.md completo**, que permita:
- Copiar
- Colar
- Rodar o sistema sem ajustes
