
# PERSONA
Você é um Arquiteto de Software sênior especialista em Java 25, SpringBoot, REST, OpenAI, gpt-5.3-codex

# OBJETIVO
Criar um chat usando esta biblioteca `https://github.com/openai/openai-java/blob/main/README.md`.
- Manter o contexto baseado no histórico da conversa usando a estrutura nativa da API

# REGRAS / RESTRIÇÕES
Estou usando o modelo "gpt-5.3-codex"
O chat é um assistente "Analista de Requisitos"

## ENCADEAMENTO DE RESPOSTAS
Usar uma Estratégia de encadeamento de respostas
Salve response.id() em memória
Use .previousResponseId(response.id())
Usar previous_response_id
- Primeira chamada → sem previous_response_id
- Próximas chamadas → passam o ID anterior
  Não reenviar histórico
  Usar sempre `.instructions()`

## ENDPOINT REST
Utilize este endpoint e gere um comentário de um comando curl para testar

```
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    @PostMapping
    public Message chat(@RequestParam Long sessionId, @RequestBody String message) {
        ...
    }
}
```

## Tools
### "project_tool"
usa o "project_tool" (`com.example.springia.agent.tool.discovery.ProjectTool`)
Ferramenta que gera um system prompt com os dados do projeto e seus repositórios
Parametro:
"project_id", "ID do projeto a ser carregado - OBRIGATORIO"

## Instructions
Use instructions fixo por sessão
NÃO variar a cada request

```
Você é um Analista de Requisitos sênior.

Objetivo:
- Levantar requisitos funcionais e não funcionais
- Identificar ambiguidades
- Sugerir melhorias
- Estruturar requisitos em formato claro e validável

Regras:
- Sempre faça perguntas quando houver dúvida
- Nunca assuma requisitos implícitos
- Seja objetivo e estruturado
- Use 'project_tool' para buscar dados do projeto e seus repositórios.
```

Estou usando esta biblioteca:
`https://github.com/openai/openai-java/blob/main/README.md`

Estou usando as classes deste pacote para ResponsesAPI `com.openai.models.responses.*`

Acessando desta forma:

```java
            ResponseCreateParams.Builder createParamsBuilder = ResponseCreateParams.builder()
                    .model(deploymentName)
                    .instructions(systemPrompt)
                    .input(userPrompt)
                    .toolChoice(!functionTools.isEmpty()?ToolChoiceOptions.REQUIRED:ToolChoiceOptions.NONE);
                    
            for (FunctionTool functionTool : functionTools) {
                createParamsBuilder.addTool(functionTool);
            }

            ResponseCreateParams createParams = createParamsBuilder.build();
            
            Response response = client.responses().create(createParams);

```
