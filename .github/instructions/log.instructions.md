---
applyTo: '**/*.java'
description: 'Regras de logging para classes com Lombok Slf4j.'
condition: 'O arquivo deve conter a anotação @Slf4j.'
---

Deve usar a anotação @Slf4j
A classe deve ter no javadoc com o comando curl, em uma única linha, para alterar o nível do log desta classe via endpoint /actuator/loggers
  ex:  curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentLoop" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
Todo log deve ter um prefixo '[NOME_METODO]' entre chaves, uppercade, snake_case e resumido em no maximo 20 caracteres.
Devem ser colocados logs no início e no retorno de métodos públicos em nível INFO e privados nível debug, dentro de iterações nível trace
Os parâmetros de métodos públicos devem ser exibidos no log.
Todo catch deve ter um log no nível error.