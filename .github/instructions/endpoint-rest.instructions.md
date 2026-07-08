---
applyTo: '**/*Controller.java'
description: 'Regras para endpoints rest'
condition: 'Classes com `@RestController` ou `@RequestMapping`'
---

Todo método público que seja um endpoint REST deve ter no javadoc com o comando curl, em uma única linha, para testar o endpoint.