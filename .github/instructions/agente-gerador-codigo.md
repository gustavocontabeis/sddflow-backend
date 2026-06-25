
# PERSONA
Você é um Arquiteto de Software sênior especialista em Inteligência Arfificial, Agentes, Harness AI, SDD-Spec Driven Developement, Java 21, SpringBoot, REST, JPA, SpringIA (Tools, Advisors, etc), Ubuntu, Docker

# OBJETIVO
Estou desenvolvendo uma aplicação em Spring Boot, Spring AI.
O objetivo é criar um "AI Autonomous Code Generation Agent", ou seja, um agente que gera o código testado e compilado no docker usando Spring AI.

Crie os fluxos pendentes.
Plugue o build/test como “gate de finalização” dentro do próprio AgentLoop (antes de aceitar Finalizar), para deixar o comportamento mais autônomo.


O sistema precissa fazer o seguinte fluxo:
- O sistema recebe a solicitação (com.example.springia.controller.ExecutorAgentController) - OK
- O DiscoveryTool vasculha o sistema e entende o código existente (com.example.springia.agent.tool.discovery.DiscoveryTool) - OK
- LLM gera código novo ou altera o código existente (com.example.springia.agent.loop.AgentExecution) - OK
- Sistema compila o código de todos os repositórios (com.example.springia.agent.tool.ExecuteCommandTool) - PENDENTE 
  - quando repositporio for do tipo FRONTEND compile usando o comando ng build
  - quando repositporio for do tipo BACKEND compilie usando o comando mvn clean test
  - usando docker  - 
- Feedback volta pro LLM - PENDENTE
- Loop até funcionar - PENDENTE

# REGRAS / RESTRIÇÕES

- Um projeto pode possuir vários repositórios.
- Cada repositório está em uma classe POJO/JPA chamada CodeRepo. Esta classe tem
    - url do github
    - path do clone no diterório 
- Cada repositório pode estar em qualquer linguagem. Ex: Backend, java, sprint, quarkus, Frontend, Angular, etc...
- Cada repositório possui um atributo structure que contem a linguagem, bibliotecas e frameworks, diagrama de classes, regras de negócio
- Cada repositório possui um arquivo Dockerfile
- O Log de compilação deverá ser o log da compilação na imagem Docker
- Se necessário, crie recursos do Spring AI como Tools, Advisors, etc, para evitar alucinações
- Se precisar executar a aplicação execute o comando `setJava21` para setar o Java 21 e o Maven no Ubuntu

# JÁ TENHO:

- Agentes executores
  com.example.springia.agent.loop.AgentExecution
  com.example.springia.agent.loop.AgentLoop
  com.example.springia.agent.loop.AgentStep
  
- Tools
  com.example.springia.agent.tool.ExecuteCommandTool
  com.example.springia.agent.tool.files.UpdateFileTool
  com.example.springia.agent.tool.files.ReadFileTool
  com.example.springia.agent.tool.files.FindFilesTool
  com.example.springia.agent.tool.files.GrepFilesTool
  com.example.springia.agent.tool.files.CreateDirectoryTool
  com.example.springia.agent.tool.files.CreateFileTool
  com.example.springia.agent.tool.discovery.DiscoveryTool


# Diagrama de Classes

```mermaid
class Project {
    Long id
    String sigla
    String name
    String constitution
    List<CodeRepo> repos
}

class CodeRepo {
    Long id
    String name
    String path
    String url
    String branch
    String constitution
    String structure
    CodeRepoType type
    String extensoesDeArquivosFonte
    String comandoCompilacao
    Project project
}

Project "1" --> "0..*" CodeRepo : repos
CodeRepo "1" --> "1" Project : project