---
applyTo: '**/*.java'
description: 'Regras de uso do Lombok em classes Java.'
condition: 'A classe deve conter pelo menos uma das anotações: @Data, @AllArgsConstructor, @NoArgsConstructor ou @Builder.'
---

# Regras de uso do Lombok

## Anotações obrigatórias na classe

Toda classe de modelo, entidade ou DTO deve ter:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
```

---

## Atributos que referenciam outras classes ou listas

Todo atributo cujo tipo seja uma **outra classe** (relacionamento) ou uma **coleção** (`List`, `Set`, `Map`, etc.) deve ser anotado com:

```java
@EqualsAndHashCode.Exclude
@ToString.Exclude
```

### Por quê?
Sem essas anotações, o `@Data` gera `equals`, `hashCode` e `toString` que percorrem os objetos relacionados, causando:
- **StackOverflowError** em relacionamentos bidirecionais
- **Consultas desnecessárias** ao banco (lazy loading)
- **Logs poluídos** com grafos inteiros de objetos

### Exemplo correto

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Project {

    private Long id;
    private String name;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<CodeRepo> repos;
}
```

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CodeRepo {

    private Long id;
    private String name;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Project project;
}
```
