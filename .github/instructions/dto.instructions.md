---
applyTo: '**/*[Dd][Tt][Oo].java'
description: 'Aplicavel a classes DTO em Java com sufixo DTO.java ou Dto.java.'
condition: 'O arquivo deve ter nome terminado em DTO.java ou Dto.java.'
---

# Instrucoes para DTOs Java

Estas instruções se aplicam a todos arquivos `.java` com sufixo `DTO.java` ou `Dto.java`.

## Regras obrigatórias

Todo DTO deve possuir as seguintes anotações do Lombok:

- `@Data`
- `@AllArgsConstructor`
- `@NoArgsConstructor`
- `@Builder`

