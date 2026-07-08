---
applyTo: '**/*.java'
description: 'Regras de logging para classes com Lombok Slf4j.'
condition: 'A classe deve conter a anotação @Slf4j.'
---

Deve usar a anotação @Slf4j
A classe deve ter no javadoc com 
- o comando curl, numa única linha, para alterar o nível do log desta classe via endpoint /actuator/loggers
  ex: `curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentLoop" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'`
- a propertie para alterar o nível de log no arquivo application.properties. Ex: `logging.level.com.example.springia.agent.client.CodeGeneratorOpenApiAgent=TRACE`
Todo o log deve ter um prefixo '[NOME_METODO]' entre chaves, uppercase, snake_case e resumido em no máximo 20 caracteres.
Devem ser colocados logs no início de métodos públicos em nível INFO e privados, nível debug, dentro de iterações nível trace
- Antes de throw new Exception, deve ter um log no nível warn.
- Dentro do catch, deve ter um log no nível error.
Todo catch deve ter um log no nível error.