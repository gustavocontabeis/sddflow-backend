# 📜 Constituição de Padrões — JPA & Banco de Dados (v3)

Segue as regras de nomenclatura e padrões para Banco de Dados e JPA. O objetivo é garantir consistência, clareza e eficiência no desenvolvimento e manutenção do sistema.

## 1. Banco de Dados

### 1.1 Nomenclatura Geral
- Máximo 30 caracteres
- snake_case
- Nomes claros e objetivos

### 1.2 Abreviações
- abreviar quando exceder o tamanho
- manter a primeira e ultima letra da palavra
- remover vogais intermediarias
- simplificar digrafos ("RR" -> "R", "SS" -> "S")
- preencher com consoantes restantes, na ordem original
- Limite de até 5 caracteres

### 1.2 Prefixos

TODAS as colunas deverão seguir esta regra de prefixo

- ds_ → descrição (String)
- nu_ → número
- no_ → nome
- co_ → código
- dt_ → data
- dh_ → data/hora
- ts_ → timestamp. data hora , minuto até milésimo de segundo
- aa_ → ano
- mm_ → mes
- dd_ → dia
- ic_ → indicador. para valires pre definidos. ex: ic_sexo
- pc_ → percentual
- qt_ → quantidade
- sg_ → sigla. Ex: SG_UF
- vr_ → valor. Ex: VR_SALARIO

### 1.3 Regras
- Toda tabela tem @Id
- prioridade para PK negocial, evitando chaves sequenciais desnecessárias. mas não é proibido
- Proibido nomes genéricos
- ic_ apenas para domínio simples
- >2 valores → tabela de domínio

## 2. JPA

### 2.1 Tipos
- dt_ → LocalDate
- dh_ → LocalDateTime

### 2.2 Atributos
- Todo atributo deve ter @Column
- name obrigatório

### 2.3 Strings
- length obrigatório
- default = 100

### 2.4 Nullable
- default = false exceto quando for regra explicita

### 2.5 Enums
- Até 2 valores
- Deve ter sigla (1 char) e descrição
- Proibido @Enumerated
- Usar @Converter
- Banco armazena apenas sigla

## 3. Restrições

- Sem prefixo → proibido
- Nome > 30 → proibido
- Enum > 2 → proibido
- Sem length → proibido
- Sem nullable → proibido
