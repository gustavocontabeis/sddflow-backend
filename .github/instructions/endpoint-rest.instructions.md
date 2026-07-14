---
applyTo: '**/*Controller.java'
description: 'Regras para endpoints rest'
condition: 'Classes com `@RestController` ou `@RequestMapping`'
---

Todo método público que seja um endpoint REST deve ter no javadoc com o comando curl, em uma única linha, para testar o endpoint.

Usar anotações do pacote ```io.swagger.v3.oas.annotations.*``` paragerar documentação automática.

```
    @Operation(summary = "Alteração de repositório")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repositório atualizado."),
            @ApiResponse(responseCode = "400", description = "Erros de validação.", content = @Content(schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repositório não encontrado.", content = @Content(schema = @Schema(implementation = ResponseDTO.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
```

Outro exemplo

```
    @Operation(summary = "Consulta paginada de repositórios com filtros",
            description = "Retorna uma lista paginada de repositórios de um projeto. Permite filtrar por nome ou link.")
    @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso.")
    @GetMapping
    public ResponseEntity<RepositorioListResponse> listRepositorios(
            @Parameter(description = "ID do projeto proprietário") @PathVariable("projetoId") UUID projetoId,
            @Parameter(description = "Número da página (1-indexed)") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "Quantidade de itens por página") @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(description = "Busca textual no nome do repositório") @RequestParam(name = "nome", required = false) String nome,
            @Parameter(description = "Busca textual no link do repositório") @RequestParam(name = "link", required = false) String link) {
```

Usar anotação `@Valid` para validar os dados

Precisa ter versionamnento de API, usando o padrão de versionamento na URL, ex: `/api/v1/repositorios` (incluir no `application.properties`)