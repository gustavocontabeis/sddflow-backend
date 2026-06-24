
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
- Sistema compila o código usando docker (com.example.springia.agent.tool.ExecuteCommandTool) - PENDENTE
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

# JÁ TENHO:

- Agentes executores
  AgentExecution
  AgentLoop
  AgentStep

- Tools
  ExecuteCommandTool
  UpdateFileTool
  ReadFileTool
  ListFilesTool
  GrepFilesTool
  CreateDirectoryTool
  CreateFileTool
  DiscoveryTool

- Um método que builda a imagem Docker
  Linha 356 SddTaskExecutorService: public ProcessBuilderReturnDTO executeDockerBuildImage(String imageName)



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