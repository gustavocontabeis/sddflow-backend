package com.example.springia.controller;

import com.example.springia.dto.ChatMessageRequest;
import com.example.springia.dto.CreateSessionRequest;
import com.example.springia.dto.SpecificationResponse;
import com.example.springia.model.Message;
import com.example.springia.model.UserStory;
import com.example.springia.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint REST para chat por sessão.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.controller.ChatController" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 * <p>{@code logging.level.com.example.springia.controller.ChatController=DEBUG}</p>
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Endpoints para chat por sessão, mensagens e geração de especificação.")
public class ChatController {

    private final ChatService chatService;

    /**
     * {@code curl -X POST "http://localhost:8080/chat/chat-message" -H "Content-Type: application/json" -d '{"sessionId":null,"projectId":1,"messageId":null,"message":"Existe alguma listagem de tarefas?","sessionName":"Ordenação de Lista de Tarefas","role":"USER","timestamp":"2026-07-08T10:30:00"}'}
     */
    @PostMapping("/chat-message")
    @Operation(summary = "Cria ou continua uma conversa de chat", description = "Recebe uma mensagem da sessão e devolve o histórico atualizado da conversa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem processada com sucesso.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageRequest.class)))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public List<ChatMessageRequest> chatMessage(@Valid @RequestBody ChatMessageRequest request) {
        log.info("[CHAT_MESSAGE] POST /chat/chat-message request={}", request);
        return chatService.chatMessage(request);
    }

    /**
     * {@code curl -X GET "http://localhost:8080/chat/chat-message/1"}
     */
    @GetMapping("/chat-message/{sessionId}")
    @Operation(summary = "Lista as mensagens de uma sessão", description = "Retorna o histórico de mensagens de uma sessão de chat.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens encontradas.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageRequest.class)))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public List<ChatMessageRequest> getChatMessage(@Parameter(description = "Identificador da sessão") @PathVariable Long sessionId) {
        log.info("[GET_CHAT_MESSAGE] GET /chat/chat-message/{} sessionId={}", sessionId, sessionId);
        return chatService.listBySession(sessionId);
    }

    /**
     * {@code curl -X POST "http://localhost:8080/chat/create-session" -H "Content-Type: application/json" -d '{"projectId":1,"name":"sessao requisitos","message":"Quero um sistema de tarefas com prioridade e prazo"}'}
     */
    @PostMapping("/create-session")
    @Operation(summary = "Cria uma nova sessão de chat", description = "Cria a sessão e registra a primeira mensagem do contexto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão criada com sucesso.", content = @Content(schema = @Schema(implementation = Message.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public Message createSession(@Valid @RequestBody CreateSessionRequest request) {
        log.info("[CREATE_SESSION] POST /chat/create-session request={}", request);
        return chatService.createSession(request);
    }

    /**
     * {@code curl -X POST "http://localhost:8080/chat?sessionId=1" -H "Content-Type: text/plain" --data "Quais requisitos nao funcionais estao faltando?"}
     */
    @PostMapping
    @Operation(summary = "Envia uma mensagem para a sessão", description = "Processa uma nova mensagem de texto puro em uma sessão existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem processada com sucesso.", content = @Content(schema = @Schema(implementation = Message.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos."),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public Message chat(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId,
                        @RequestBody String message) {
        log.info("[CHAT] POST /chat sessionId={} tamanhoMensagem={}", sessionId, message != null ? message.length() : 0);
        return chatService.chat(sessionId, null, message);
    }

    /**
     * {@code curl -X POST "http://localhost:8080/chat/aprove?sessionId=1"}
     */
    @PostMapping("/aprove")
    @Operation(summary = "Aprova a sessão", description = "Marca a sessão como aprovada para seguir no fluxo de geração.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão aprovada.", content = @Content(schema = @Schema(implementation = Message.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public Message aprove(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[APPROVE_CHAT] POST /chat/aprove sessionId={}", sessionId);
        return chatService.aprove(sessionId);
    }

    /**
     * {@code curl -X POST "http://localhost:8080/chat/gerar-historia-de-usuario?sessionId=1"}
     */
    @PostMapping("/gerar-historia-de-usuario")
    @Operation(summary = "Gera uma história de usuário", description = "Consolida o conteúdo da sessão em uma história de usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "História de usuário gerada.", content = @Content(schema = @Schema(implementation = UserStory.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public UserStory gerarHistoriaDeUsuario(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[GERAR_HISTORIA] POST /chat/gerar-historia-de-usuario sessionId={}", sessionId);
        return chatService.gerarHistoriaDeUsuario(sessionId);
    }

    /**
     * {@code curl -X GET "http://localhost:8080/chat?sessionId=1"}
     */
    @GetMapping
    @Operation(summary = "Lista as mensagens da sessão", description = "Retorna as mensagens armazenadas para uma sessão específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens listadas com sucesso.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Message.class)))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public List<Message> getChat(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[GET_CHAT] GET /chat sessionId={}", sessionId);
        return chatService.list(sessionId);
    }


    /**
     * {@code curl -X DELETE "http://localhost:8080/chat?sessionId=1"}
     */
    @DeleteMapping
    @Operation(summary = "Remove uma sessão de chat", description = "Exclui a sessão e todas as mensagens associadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão removida com sucesso."),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public void delete(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[DELETE_CHAT] DELETE /chat sessionId={}", sessionId);
        chatService.delete(sessionId);
    }

    /**
     * {@code curl -X POST "http://localhost:8080/chat/specification?sessionId=1"}
     */
    @PostMapping("/specification")
    @Operation(summary = "Gera a especificação da sessão", description = "Cria uma especificação estruturada a partir da sessão aprovada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Especificação gerada com sucesso.", content = @Content(schema = @Schema(implementation = SpecificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public SpecificationResponse generateSpecification(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[GEN_SPEC] POST /chat/specification sessionId={}", sessionId);
        UserStory saved = chatService.createUserStory(sessionId);
        return new SpecificationResponse(saved.getId(), saved.getConversationSession().getId(), saved.getContent(), saved.getGeneratedAt());
    }

    /**
     * {@code curl -X GET "http://localhost:8080/chat/specification?sessionId=1"}
     */
    @GetMapping("/specification")
    @Operation(summary = "Busca a última especificação da sessão", description = "Retorna a última especificação gerada para uma sessão, se existir.")
    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Especificação encontrada.", content = @Content(schema = @Schema(implementation = SpecificationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Especificação não encontrada."),
            @ApiResponse(responseCode = "500", description = "Erro interno.")
    })
    public ResponseEntity<SpecificationResponse> getLatestSpecification(@Parameter(description = "Identificador da sessão") @RequestParam Long sessionId) {
        log.info("[GET_SPEC] GET /chat/specification sessionId={}", sessionId);
        return chatService.getLatestSpecification(sessionId)
                .map(s -> ResponseEntity.ok(new SpecificationResponse(s.getId(), s.getConversationSession().getId(), s.getContent(), s.getGeneratedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}