package com.example.springia.service;

import com.example.springia.dto.DiscoveryDTO;
import com.example.springia.dto.DiscoveryDirsDTO;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;

    public DiscoveryService(
            ChatClient.Builder chatClientBuilder,
            ProjectRepository projectRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = createObjectMapper();
        this.projectRepository = projectRepository;
    }

    public String answerProjectQuestion(Long projectId, String question) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId e obrigatorio");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question e obrigatoria");
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return null;
        }

        StringBuilder reposContext = new StringBuilder();
        if (project.getRepos() != null && !project.getRepos().isEmpty()) {
            for (CodeRepo repo : project.getRepos()) {
                reposContext.append("\n--- REPOSITORIO ---\n")
                        .append("Nome: ").append(repo.getName()).append("\n")
                        .append("Tipo: ").append(repo.getType()).append("\n")
                        .append("Diretório: ").append(repo.getPath().replaceAll("\\d+$", "")).append("\n")
                        .append("URL: ").append(repo.getUrl()).append("\n")
                        .append("Branch: ").append(repo.getBranch()).append("\n")
                        .append("Constitution: ").append(repo.getConstitution() != null ? repo.getConstitution() : "[vazio]").append("\n")
                        .append("Structure: ").append(repo.getStructure() != null ? repo.getStructure() : "[vazio]").append("\n");
            }
        } else {
            reposContext.append("[Projeto sem repositorios cadastrados]");
        }

        String prompt = """
                Voce e um arquiteto de software senior e analista de requisitos nivel sênior.
                Responda a pergunta do usuario usando o contexto do projeto.
                Voce tem condições de:
                  - responder a perguntas do contexto do projeto.
                  - criar soluções consultando o modelo ou incrementando o modelo se necessário.
                Analise os diagramas de classes e NA RESPOSTA liste as classes e os atributos envolvidos na pergunta.
                Utilize as tools e liste os arquivos fonte relacionados a esta funcionalidade.
       
                DADOS DO PROJETO:
                - ID: %d
                - Sigla: %s
                - Nome: %s
                - Constitution:
                %s

                DADOS DOS REPOSITORIOS:
                %s

                PERGUNTA:
                %s
                
                """.formatted(
                project.getId(),
                project.getSigla() != null ? project.getSigla() : "[vazio]",
                project.getName() != null ? project.getName() : "[vazio]",
                project.getConstitution() != null ? project.getConstitution() : "[vazio]",
                reposContext,
                question
        );

        log.info("[Perguntas]: {}\n", prompt);


        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return (content == null || content.isBlank())
                ? "Nao foi possivel gerar uma resposta no momento."
                : content;
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

        String conteudoPacotesRegrasNegocio = "";
        if(discoveryDirs.getPacotesRegrasNegocio().length > 0){

            conteudoPacotesRegrasNegocio = FileUtils.joinFileContents(discoveryDirs.getPacotesRegrasNegocio());
            log.info("");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("CONTEUDO DOS ARQUIVOS DE REGRA DE NECÓCIO");
            log.info("{}", conteudoPacotesRegrasNegocio);

        }

        String conteudoPacotesEndpointsRest = "";
        if(discoveryDirs.getPacotesEndpointsRest().length > 0){

            conteudoPacotesEndpointsRest = FileUtils.joinFileContents(discoveryDirs.getPacotesEndpointsRest());
            log.info("");
            log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
            log.info("CONTEUDO DOS ARQUIVOS DE ENDPOINTS REST");
            log.info("{}", conteudoPacotesEndpointsRest);

        }

        String regrasNegocio = buscarRegrasNegocio(conteudoPacotesRegrasNegocio, conteudoPacotesEndpointsRest, modeloJson);
        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("DESCRIÇÃO DAS REGRAS DE NEGÓCIO APLICAÇÃO");
        log.info("{}", regrasNegocio);

        return createContitution(
                configuracoes,
                discoveryDirs.getDescricaoEstruturaDiretorios(),
                modeloJson,
                regrasNegocio
        );
    }

    private String createContitution(DiscoveryDTO configuracoes, String descricaoEstruturaDiretorios, String descricaoModelo, String regrasNegocio) {

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
                Esse é o conteúdo refetente regras de negócio do sistema.
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
                # REGRAS DE NEGÓCIO
                IMPORTANTE: retorne somente o conteúdo do documento sem outros comentários.
                """, linguagem,
                frameworksBibliotecas,
                descricaoEstruturaDiretorios,
                conexoesComBancoDeDados,
                integracoesComOutrosSistemas,
                descricaoModelo,
                regrasNegocio);

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

    private String buscarRegrasNegocio(String conteudoArquivosRegraNegocio, String conteudoArquivosEndpointRest, String modelo) {
        String prompt = String.format("""
                Voce e um arquiteto de software senior.
                Este é o diagrama do modelo do sistema:
                %s
                Esse é o conteúdo dos arquivos de regra de negócio do sistema.
                %s
                Esse é o conteúdo dos arquivos de endpoint REST do sistema.
                %s
                Liste as regras de negócio existentes no sistema.
                Não alucine.
                """, modelo, String.join("\n", conteudoArquivosRegraNegocio), conteudoArquivosEndpointRest);

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
                - regras de negócio. Liste todas as regras de negócio encontradas nas classes de Endpoints REST e classes de regras de negócio.
                retornar nesta estrutura em JSON conforme exemplo:
                {
                  "linguagem":"java",
                  "frameworksBibliotecas":[""]
                  "conexoesComBancoDeDados":[""]
                  "integracoesComOutrosSistemas":[""]
                  "arquivosConfiguracao":[""]
                  "regrasDeNegocio":[""]
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
