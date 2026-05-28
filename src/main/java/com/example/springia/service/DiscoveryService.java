package com.example.springia.service;

import com.example.springia.dto.DiscoveryDirsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
public class DiscoveryService {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(".git", "target", "node_modules", "dist");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public DiscoveryService(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    public String dicovery(Path path){

        List<String> strings = listarArquivos(path);
        for (String string : strings) {
            log.info("{}", string);
        }

        DiscoveryDirsDTO discoveryDirs = dadosDeDiretorios(strings);
        log.info("{}", discoveryDirs);

        String conteudoArquivosConfiguracao = lerConteudoArquivos(discoveryDirs.getArquivosConfiguracao());
        log.info("{}", conteudoArquivosConfiguracao);

        String configuracoesJson = buscarConfiguracoes(conteudoArquivosConfiguracao);
        log.info("{}", configuracoesJson);


        StringBuilder sb = new StringBuilder();
        for (String pathFile : discoveryDirs.getPacotesDeDominio()) {
            List<String> stringss = listarArquivos(Paths.get(pathFile));
            sb.append(lerConteudoArquivos(stringss.toArray(new String[0])));
        }


        String conteudoArquivosDeDominio = lerConteudoArquivos(discoveryDirs.getPacotesDeDominio());
        log.info("{}", conteudoArquivosDeDominio);

        String modeloJson = buscarModelo(conteudoArquivosDeDominio);
        log.info("{}", modeloJson);

        return modeloJson;
    }

    private String buscarModelo(String conteudoArquivos) {
        String prompt = String.format("""
                Voce e um arquiteto de software senior.
                Esse é o conteúdo dos arquivos modelo do sistema.
                identifique:
                - Classes e atributos
                - Inclua a descrição das classes e dos atributos
                - Mostre o relacionamento das classes em um diagrama Mermaid
                %s
                """, String.join("\n", conteudoArquivos));

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("{}", content);
        return content;
    }

    private String buscarConfiguracoes(String conteudoArquivosConfiguracao) {
        String prompt = String.format("""
                Voce e um arquiteto de software senior.
                Esse é o conteúdo dos arquivos de configuração do sistema.
                identifique:
                - Linguagem
                - frameworks e bibliotecas. Ex: ["java 21", "quarkus", "lombok", ...]
                - conexoes com banco de dados. Ex: [{"tipo":"oracle"}]
                - integracoes com outros sistemas Ex: [{"tipo":"rest", "nome":"receita federal", "url":"http://xxx"}]
                
                retornar nesta estrutura em JSON conforme exemplo:
                {
                  "linguagem":"java",
                  "frameworksBibliotecas:[""]
                  "conexoesComBancoDeDados":[{}]
                  "integracoesComOutrosSistemas":[{}]
                  "arquivosConfiguracao":[""]
                }
                IMPORTANTE: retornar apenas o JSON puro. nada antes nem depois
                %s
                """, String.join("\n", conteudoArquivosConfiguracao));

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("{}", content);
        return content;
    }

    // Consolida o conteudo dos arquivos em uma unica string para o proximo processamento.
    private String lerConteudoArquivos(String[] arquivos) {
        if (arquivos == null || arquivos.length == 0) {
            return "";
        }

        StringBuilder conteudo = new StringBuilder();

        for (String arquivo : arquivos) {
            if (arquivo == null || arquivo.isBlank()) {
                continue;
            }

            Path filePath = Path.of(arquivo).toAbsolutePath().normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("Arquivo ignorado (nao existe ou nao e regular): {}", filePath);
                continue;
            }

            try {
                if (!conteudo.isEmpty()) {
                    conteudo.append("\n\n");
                }

                conteudo.append("Arquivo: ")
                        .append(filePath)
                        .append("\n")
                        .append(Files.readString(filePath));
            } catch (IOException e) {
                log.warn("Falha ao ler arquivo de configuracao: {}", filePath, e);
            }
        }

        return conteudo.toString();
    }

    private DiscoveryDirsDTO dadosDeDiretorios(List<String> files) {
        String prompt = String.format("""
                Voce e um arquiteto de software senior.
                a partir da lista de arquivos recebida, retorne:
                - Linguagem
                - pacotes de dominio: pacote **que contem** as classes de dominio. Classes de dominio sao objetos que representam entidades e conceitos do negocio, contendo estado (atributos) e, idealmente, comportamento (regras de negocio). Nao sao classes de acesso a banco, endpoint, DTOs, etc
                - pacotes de regras de negocio
                - pacotes de endpoints rest
                - arquivos de configuracao

                retornar nesta estrutura em JSON conforme exemplo:
                {
                  "linguagem":"java",
                  "pacotesDeDominio:["/tmp/projeto1/domani", "/tmp/projeto1/entity"]
                  "pacotesRegrasNegocio":["/tmp/projeto1/service"]
                  "pacotesEndpointsRest":["/tmp/projeto1/resource"]
                  "arquivosConfiguracao":["/tmp/projeto1/application.properties"]
                }
                IMPORTANTE: retornar apenas o JSON puro. nada antes nem depois
                %s
                """, String.join("\n", files));

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("{}", content);

        return parseDiscoveryDirsDTO(content);
    }

    private DiscoveryDirsDTO parseDiscoveryDirsDTO(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Resposta vazia do modelo para DiscoveryDirsDTO");
        }

        String json = extractJsonObject(content);

        try {
            return objectMapper.readValue(json, DiscoveryDirsDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao converter resposta em DiscoveryDirsDTO. Resposta: " + content, e);
        }
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new IllegalStateException("Nao foi encontrado JSON valido na resposta: " + content);
        }

        return content.substring(start, end + 1).trim();
    }

    /**
     * Lista TODOS os arquivos recursivamente
     *
     * @param path caminho raiz para varredura
     * @return lista de caminhos absolutos dos arquivos encontrados
     */
    public List<String> listarArquivos(Path path){
        if (path == null) {
            throw new IllegalArgumentException("Caminho nao pode ser nulo");
        }

        Path root = path.toAbsolutePath().normalize();

        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Caminho nao existe: " + root);
        }

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Caminho nao e um diretorio: " + root);
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(this::notInIgnoredDirectory)
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(Path::toString))
                .map(candidate -> candidate.toAbsolutePath().normalize().toString())
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao listar arquivos em: " + root, e);
        }
    }

    private boolean notInIgnoredDirectory(Path candidate) {
        for (Path segment : candidate) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return false;
            }
        }
        return true;
    }


}
