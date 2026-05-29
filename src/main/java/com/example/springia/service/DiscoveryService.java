package com.example.springia.service;

import com.example.springia.dto.DiscoveryDTO;
import com.example.springia.dto.DiscoveryDirsDTO;
import com.example.springia.utils.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
public class DiscoveryService {

    public static final Set<String> IGNORED_DIRECTORIES = Set.of(".git", "target", "node_modules", "dist");
    public static final Set<String> IGNORED_CONFIG_FILES = Set.of("package-lock.json");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public DiscoveryService(
            ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = createObjectMapper();
    }

    static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
    }

    public String dicovery(Path path){

        List<String> strings = FileUtils.listFilesNames(path);
        if(!strings.isEmpty()){
            log.info("");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("LISTA DE NOMES DE ARQUIVOS");
            for (String string : strings) {
                log.info("{}", string);
            }
        }

        DiscoveryDirsDTO discoveryDirs = dadosDeDiretorios(strings);
        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("JSON DE DIRETORIOS");
        log.info("{}", discoveryDirs);

        String[] arquivosConfiguracao = Stream.of(discoveryDirs.getArquivosConfiguracao())
                .filter(arquivo -> arquivo != null && !arquivo.isBlank())
                .filter(arquivo -> !IGNORED_CONFIG_FILES.contains(Path.of(arquivo).getFileName().toString()))
                .toArray(String[]::new);

        String conteudoArquivosConfiguracao = FileUtils.joinFileContents(arquivosConfiguracao);
        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("CONTEUDO DOS ARQUIVOS DE CONFIGURACAO");
        log.info("{}", conteudoArquivosConfiguracao);

        DiscoveryDTO configuracoes = buscarConfiguracoes(conteudoArquivosConfiguracao);
        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("JSON DE CONFIGURAÇÕES");
        log.info("{}", configuracoes);

        String modeloJson = "[Não há arquivos de modelo do sistema]";
        if(discoveryDirs.getPacotesDeDominio().length > 0){

            String conteudoArquivosDeDominio = FileUtils.joinFileContents(discoveryDirs.getPacotesDeDominio());
            log.info("");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("CONTEUDO DOS ARQUIVOS DE MODELO");
            log.info("{}", conteudoArquivosDeDominio);

            if(!conteudoArquivosDeDominio.isEmpty()){
                modeloJson = buscarModelo(conteudoArquivosDeDominio);
                log.info("");
                log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
                log.info("DESCRIÇÃO MODELO DA APLICAÇÃO");
                log.info("{}", modeloJson);
            }
        }

        return createContitution(
                configuracoes,
                discoveryDirs.getDescricaoEstruturaDiretorios(),
                modeloJson
        );
    }

    private String createContitution(DiscoveryDTO configuracoes, String descricaoEstruturaDiretorios, String descricaoModelo) {

        String linguagem = configuracoes.linguagem();
        String frameworksBibliotecas = String.join(", ", configuracoes.frameworksBibliotecas());
        String conexoesComBancoDeDados = String.join(", ", configuracoes.conexoesComBancoDeDados());
        String integracoesComOutrosSistemas = String.join(", ", configuracoes.integracoesComOutrosSistemas());

        String prompt = String.format("""
                Voce e um arquiteto de software senior especializado em SDD Spec Driven Developement..
                A linguagem do sistema é 
                [%s].
                Essas são os frameworks e bibliotecas utilizadas no sistema.
                [%s]
                Esse é o conteúdo refetente a descrição da estrutura de diretórios.
                [%s]
                Esse é o conteúdo refetente a conexoes com banco de dados.
                [%s]
                Esse é o conteúdo refetente a integrações com outros sistemas.
                [%s]
                Esse é o conteúdo refetente descrição dos modelos e relacionamentos do sistema.
                [%s]
                Crie o conteudo de um documento contituition.md contendo os seguintes tópicos:
                # CONSTITUTION
                # STACK: 
                 - linguagem principal e frameworks
                ### FRAMEWORKS E BIBLIOTECAS
                  - listar os frameworks e bibliotecas e versões
                # ESTRUTURA DE DIRETÓRIOS
                  - mostrar estrutura de diretórios e detalhamento
                # CONEXOES COM BANDO DE DADOS
                  - detahlar conexões com banco de dados
                # INTEGRAÇÕES COM OUTRUS SITEMAS
                # CLASSES E ATRIBUTOS
                ### DIAGRAMA DE CLASSES EM MERMAID
                ### DESCRIÇÃO DO DIAGRAMA
                IMPORTANTE: retorne somente o conteúdo do documento sem outros comentários.
                """, linguagem,
                frameworksBibliotecas,
                descricaoEstruturaDiretorios,
                conexoesComBancoDeDados,
                integracoesComOutrosSistemas,
                descricaoModelo);

        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("PROMPT CONSTITUTION");
        log.info("{}", prompt);
        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("CONSTITUTION");
        log.info("{}", content);
        return content;
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

    private DiscoveryDTO buscarConfiguracoes(String conteudoArquivosConfiguracao) {
        String prompt = String.format("""
                Voce e um arquiteto de software senior.
                Esse é o conteúdo dos arquivos de configuração do sistema.
                identifique:
                - Linguagem - versão
                - frameworks e bibliotecas com as versões. Ex: ["java 21", "quarkus 3.14.234", "JPA 2.1", "lombok 2.6", ...]
                - conexoes com banco de dados em array de strings informando o tipo de banco (oracle, postgres, etc... ) e IP/DNS de destino. Ex: ["oracle IP 123.456.654.321", "mysql IP 123.456.654.321"]
                - integracoes com outros sistemas em array de strings descritivas. Ex: ["REST - receita federal - http://receitafederal"]
                
                retornar nesta estrutura em JSON conforme exemplo:
                {
                  "linguagem":"java",
                  "frameworksBibliotecas":[""]
                  "conexoesComBancoDeDados":[""]
                  "integracoesComOutrosSistemas":[""]
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

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Resposta vazia do modelo para ConfiguracoesDiscobertasDTO");
        }

        return parseDiscoveryDTO(objectMapper, content);
    }

    static DiscoveryDTO parseDiscoveryDTO(ObjectMapper objectMapper, String content) {
        String json = extractJsonObject(content);

        try {
            return objectMapper.readValue(json, DiscoveryDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao converter resposta em ConfiguracoesDiscobertasDTO. Resposta: " + content, e);
        }
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
                - arquivos de configuracao: arquivos de configuração que não seja de teste. Ex pom.xml, application.properties, application.yalm, package.json, build.gradle, Dockerfile, requirements.txt, .csproj 
                - descrição da estrutura de diretorios do projeto em formato markdown exibindo uma árvore de diretórios sem arquivos, destacando a finalidade de cada diretório.

                retornar nesta estrutura em JSON conforme exemplo:
                {
                  "linguagem":"java",
                  "pacotesDeDominio:["/tmp/projeto1/domani", "/tmp/projeto1/entity"]
                  "pacotesRegrasNegocio":["/tmp/projeto1/service"]
                  "pacotesEndpointsRest":["/tmp/projeto1/resource"]
                  "arquivosConfiguracao":["/tmp/projeto1/application.properties"]
                  "descricaoEstruturaDiretorios: ""
                }
                IMPORTANTE: retornar apenas o JSON puro. nada antes nem depois
                %s
                """, String.join("\n", files));

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("{}", content);

        return parseDiscoveryDirsDTO(objectMapper, content);
    }

    static DiscoveryDirsDTO parseDiscoveryDirsDTO(ObjectMapper objectMapper, String content) {
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

    static String extractJsonObject(String content) {
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
