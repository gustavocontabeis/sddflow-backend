
# 📜 Constituição de Padrões — JPA & Banco de Dados
---
applyTo: '**/*.java'
description: 'Aplicável a todas as classes Java que contenham a anotação @Entity (JPA).'
condition: 'O arquivo deve conter a anotação @Entity.'
---

# Instruções para classes JPA (@Entity)

Estas instruções se aplicam a todas as classes Java do projeto que possuem a anotação `@Entity`.
- Nao utilize a anotação @Table, apenas @Entity
A anotação `@Entity`. deve possuir o nome da tabela seguinto esse padrão: `@Entity(name = "lictb001_nome_da_tabela")`.
- lic - prefixo do projeto
- tb - significa que é uma tabela, vw - significa que é uma view, sq - significa que é uma sequence
- 001 - número sequencial da tabela, view ou sequence (fazculhe nas classes ecistentes para descobrir o próximo numero sequencial)
- nome da tabela escrito em snake_case seguindo as mesmas regras de abreviação

O Atributo com `@Id` deve ter a sequence declarada com `@GeneratedValue` e `@SequenceGenerator`
 
## Padrões obrigatórios
- Utilize as melhores práticas de modelagem JPA.
- os nomes das tabelas/colunas e sequences devem estar em português/Brasil, seguindo as regras de abreviação e prefixos.
- utilize anotações lombok @Data @AllArgsConstructor @NoArgsConstructor @Builder
- Utilize anotações da API de Validation do Java para garantir a integridade dos dados (ex: `@NotNull`, `@Size`, `@NotBlank` quando String, etc).
- Declare um campo `@Id` obrigatório.
- Forneça construtor padrão (sem argumentos) e, se necessário, construtores adicionais.
- Quando "TEXT" for necessário, utilize `@Lob` para campos de texto longo. Não utilize @Size, @Length e length no @Column.

---

Estas orientações garantem que todas as entidades JPA estejam alinhadas com os padrões e objetivos do projeto.

## 1. Banco de Dados

### 1.1 Nomenclatura Geral
- Máximo 30 caracteres
- snake_case
- Nomes claros e objetivos

### 1.2 Abreviações
- abreviar somente quando exceder o tamanho
- manter a primeira e ultima letra da palavra
- remover vogais intermediarias
- simplificar digrafos ("RR" -> "R", "SS" -> "S")
- preencher com consoantes restantes, na ordem original
- Limite de até 5 caracteres

### 1.2 Prefixos

TODAS as colunas deverão seguir esta regra de prefixo

- de_ → descrição (String)
- nu_ → número (Long, Integer, BigDecimal, etc)
- no_ → nome
- co_ → código (String)
- dt_ → data (LocalDate)
- dh_ → data/hora (LocalDateTime) 
- ts_ → timestamp. data hora , minuto até milésimo de segundo
- aa_ → ano (Integer)
- mm_ → mes (Integer)
- dd_ → dia (Integer)
- ic_ → indicador. para valores pre definidos. ex: ic_sexo  (String, Boolean)
- pc_ → percentual
- qt_ → quantidade
- sg_ → sigla. Ex: SG_UF
- vr_ → valor. Ex: VR_SALARIO  (Double, Float, BigDecimal, etc)

### 1.3 Regras
- Toda tabela tem @Id
- prioridade para PK negocial, evitando chaves sequenciais desnecessárias. mas não é proibido
- Proibido nomes genéricos
- ic_ apenas para domínio simples
- >2 valores → tabela de domínio

## 2. JPA

### 2.1 Atributos
- Todo atributo deve ter @Column
- name obrigatório

### 2.2 Strings
- length obrigatório
- default = 100

### 2.3 Nullable
- default = false exceto quando for regra explicita

### 2.4 Enums
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
