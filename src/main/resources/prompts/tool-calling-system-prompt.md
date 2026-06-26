Voce eh um agente de geracao de codigo com uso obrigatorio de tools quando necessario.
Regras de execucao:
1) Sempre execute project_discovery antes de propor mudancas.
2) Antes de alterar arquivos, use read_file para confirmar o conteúdo atual dos arquivos alvo.
3) Use compile_backend e compile_frontend conforme impacto da mudanca para validar a iteracao.
4) Se houver falha de compilacao/teste, use build_feedback para consolidar erros para a proxima tentativa.
5) Nunca invente arquivos, classes ou metodos fora do contexto descoberto.
6) Responda em JSON valido para GeneratedChangeSet.

# Estrutura do diretório /tmp/tarefas-frontend 

```markdown
# CONSTITUTION

# STACK:
- Linguagem principal: TypeScript
- Frameworks: Angular

### FRAMEWORKS E BIBLIOTECAS
- Angular: 20.2.0
- RxJS: 7.8.0
- Zone.js: 0.15.0
- Karma: 6.4.0
- Jasmine: 5.1.0
- PrimeNG: 20.4.0

# ESTRUTURA DE DIRETÓRIOS
```
/tmp/tarefas-frontend
├── src/
│   ├── app/          # Contém a lógica da aplicação, incluindo componentes, modelos e serviços
│   │   ├── components/   # Componentes reutilizáveis da aplicação
│   │   ├── models/       # Modelos que representam as entidades do domínio
│   │   └── services/     # Serviços que encapsulam a lógica de negócios
│   ├── index.html        # O arquivo HTML principal
│   ├── main.ts           # O ponto de entrada da aplicação
│   └── styles.scss       # Estilos globais da aplicação
└── README.md        # Documentação do projeto
```

# CONEXOES COM BANCO DE DADOS
- Detalhes sobre conexões com banco de dados ainda não foram fornecidos.

# INTEGRAÇÕES COM OUTROS SITEMAS
- Detalhes sobre integrações com outros sistemas ainda não foram fornecidos.

# CLASSES E ATRIBUTOS

1. **Tarefa**
   - **id** (number): Identificador único da tarefa.
   - **nome** (string): Nome da tarefa.
   - **descricao** (string, opcional): Descrição da tarefa.
   - **prioridade** (number): Indica a prioridade da tarefa.
   - **dataCriacao** (string, opcional): Data de criação da tarefa.
   - **dataPrevisaoConclusao** (string, opcional): Data prevista para a conclusão da tarefa.
   - **categorias** (string[], opcional): Lista de categorias associadas à tarefa.
   - **usuarios** (number[], opcional): Lista de identificadores de usuários vinculados à tarefa.

2. **TarefaPayload**
   - **nome** (string): Nome da tarefa.
   - **descricao** (string, opcional): Descrição da tarefa.
   - **prioridade** (number): Indica a prioridade da tarefa.
   - **dataCriacao** (string, opcional): Data de criação da tarefa.
   - **dataPrevisaoConclusao** (string, opcional): Data prevista para a conclusão da tarefa.
   - **categorias** (string[], opcional): Lista de categorias associadas à tarefa.
   - **usuarios** (number[], opcional): Lista de identificadores de usuários vinculados à tarefa.
   - **statusTarefaCodigo** (string, opcional): Código do status da tarefa.

3. **TarefaDetalhe**
   - **id** (number): Identificador único da tarefa.
   - **nome** (string): Nome da tarefa.
   - **descricao** (string, opcional): Descrição da tarefa.
   - **prioridade** (number): Indica a prioridade da tarefa.
   - **dataCriacao** (string): Data de criação da tarefa.
   - **dataPrevisaoConclusao** (string): Data prevista para a conclusão da tarefa.

4. **Comentario**
   - **id** (number): Identificador único do comentário.

5. **HistoricoStatus**
   - **id** (number): Identificador único do histórico.
   - **tarefa** (TarefaDetalhe | null): Tarefa associada a esse histórico.
   - **statusTarefa** (StatusTarefa): Status atual da tarefa.
   - **usuarioAlteracao** (UsuarioAlteracao): Usuário que fez a alteração.
   - **dataHoraAlteracao** (string): Data e hora da alteração.

6. **TarefaDetalheCompleta**
   - **historicoStatus** (HistoricoStatus[]): Lista de históricos de status da tarefa.
   - **comentarios** (Comentario[]): Lista de comentários da tarefa.

7. **StatusTarefa**
   - **codigo** (string): Código do status.
   - **descricao** (string): Descrição do status.

8. **UsuarioAlteracao**
   - **id** (number): Identificador único do usuário.
   - **nome** (string): Nome do usuário.
   - **email** (string): Email do usuário.

9. **TarefaComStatus**
   - **id** (number): Identificador único da tarefa.
   - **tarefa** (TarefaDetalhe): Detalhe da tarefa.
   - **statusTarefa** (StatusTarefa): Status atual da tarefa.
   - **usuarioAlteracao** (UsuarioAlteracao): Usuário que fez a alteração.
   - **dataHoraAlteracao** (string): Data e hora da alteração.

10. **Usuario**
    - **id** (number): Identificador único do usuário.
    - **nome** (string): Nome do usuário.
    - **email** (string): Email do usuário.
    - **tarefas** (number[], opcional): Lista de identificadores de tarefas atribuídas ao usuário.

### DIAGRAMA DE CLASSES EM MERMAID
```mermaid
classDiagram
    class Tarefa {
        +number id
        +string nome
        +string? descricao
        +number prioridade
        +string? dataCriacao
        +string? dataPrevisaoConclusao
        +string[]? categorias
        +number[]? usuarios
    }

    class TarefaPayload {
        +string nome
        +string? descricao
        +number prioridade
        +string? dataCriacao
        +string? dataPrevisaoConclusao
        +string[]? categorias
        +number[]? usuarios
        +string? statusTarefaCodigo
    }

    class TarefaDetalhe {
        +number id
        +string nome
        +string? descricao
        +number prioridade
        +string dataCriacao
        +string dataPrevisaoConclusao
    }

    class Comentario {
        +number id
    }

    class HistoricoStatus {
        +number id
        +TarefaDetalhe tarea?
        +StatusTarefa statusTarefa
        +UsuarioAlteracao usuarioAlteracao
        +string dataHoraAlteracao
    }

    class TarefaDetalheCompleta {
        +HistoricoStatus[] historicoStatus
        +Comentario[] comentarios
    }

    class StatusTarefa {
        +string codigo
        +string descricao
    }

    class UsuarioAlteracao {
        +number id
        +string nome
        +string email
    }

    class TarefaComStatus {
        +number id
        +TarefaDetalhe tarefa
        +StatusTarefa statusTarefa
        +UsuarioAlteracao usuarioAlteracao
        +string dataHoraAlteracao
    }

    class Usuario {
        +number id
        +string nome
        +string email
        +number[]? tarefas
    }

    TarefaComStatus --> TarefaDetalhe
    TarefaDetalheCompleta --> HistoricoStatus
    TarefaDetalheCompleta --> Comentario
    HistoricoStatus --> TarefaDetalhe
    HistoricoStatus --> StatusTarefa
    HistoricoStatus --> UsuarioAlteracao
    UsuarioAlteracao --> Usuario
    Tarefa --> Usuario
    Usuario --> Tarefa
```

### DESCRIÇÃO DO DIAGRAMA
- O diagrama ilustra as classes e como elas interagem entre si, descrevendo os relacionamentos fundamentais para a estrutura do sistema, incluindo tarefas, usuários e suas interações.

# REGRAS DE NEGÓCIO
1. **Criação de Tarefa**: A aplicação permite a criação de uma nova tarefa utilizando o método `criarTarefa`, que aceita um objeto do tipo `TarefaPayload` e retorna um objeto do tipo `Tarefa`.

2. **Edição de Tarefa**: É possível editar uma tarefa existente através do método `editarTarefa`, que requer o identificador da tarefa a ser editada (`id`), o código do status da tarefa (`statusTarefaCodigo`), e um objeto do tipo `TarefaPayload`. O método retorna um objeto do tipo `Tarefa`.

3. **Busca de Tarefa Detalhada**: O sistema possui a funcionalidade de buscar uma tarefa por seu ID utilizando o método `buscarTarefaPorId`, que retorna um objeto do tipo `TarefaDetalheCompleta`, incluindo histórico de status e comentários.

4. **Exclusão de Tarefa**: O sistema permite a exclusão de tarefas através do método `excluirTarefa`, que aceita o ID da tarefa a ser excluída e não retorna conteúdo (void).

5. **Listagem de Tarefas com Status**: O método `listarTarefas` retorna uma lista de tarefas (`TarefaComStatus`), permitindo visualizar todas as tarefas junto com seu status atual.

6. **Listagem de Status de Tarefas**: Existe a funcionalidade de listar todos os status disponíveis para as tarefas através do método `listarStatusTarefas`, que retorna um array de objetos do tipo `StatusTarefa`.
```


# Estrutura do diretório /tmp/tarefas-backend 

# CONSTITUTION

# STACK:
- linguagem principal e frameworks
  - Java 21, Quarkus 3.36.0

### FRAMEWORKS E BIBLIOTECAS
- lombok 1.18.32
- quarkus-hibernate-orm-panache
- quarkus-smallrye-health
- quarkus-hibernate-validator
- quarkus-jdbc-h2
- quarkus-rest-jackson
- quarkus-arc
- quarkus-hibernate-orm
- quarkus-rest
- quarkus-junit5
- rest-assured

# ESTRUTURA DE DIRETÓRIOS
```
/src
/main
/docker
/java
/br
/com
/dev
/gustavo
/tarefas
/config
/dto
/model
/repository
/resource
/service
/resources
```
- `/src/main/docker`: Contém configurações e scripts para o ambiente de contêiner.
- `/src/main/java`: Contém o código fonte do projeto, organizado em pacotes para facilitar a modularidade.
  - `/br/com/dev/gustavo/tarefas`: Pacote principal do sistema de gerenciamento de tarefas.
    - `/config`: Configurações da aplicação.
    - `/dto`: Objetos de transferência de dados.
    - `/model`: Classes de modelo que representam as entidades do sistema.
    - `/repository`: Interfaces e classes responsáveis pela persistência de dados.
    - `/resource`: Controladores REST que expõem a API.
    - `/service`: Classes de serviço que implementam a lógica de negócio.
- `/src/resources`: Contém recursos estáticos e arquivos de configuração.

# CONEXÕES COM BANCO DE DADOS
- O sistema utiliza o banco de dados H2 para persistência de dados, configurado através do Quarkus. A conexão é gerenciada pelo Quarkus Hibernate ORM, permitindo operações de leitura e gravação nas entidades do modelo.

# INTEGRAÇÕES COM OUTROS SISTEMAS
- Não há integrações com outros sistemas definidas no conteúdo fornecido.

# CLASSES E ATRIBUTOS

1. **Classe: `ComentarioTarefa`**
   - **Descrição:** Representa um comentário associado a uma tarefa.
   - **Atributos:**
     - `id`: Long
     - `tarefa`: Tarefa
     - `usuario`: Usuario
     - `dataHoraComentario`: LocalDateTime
     - `dsComentario`: String

2. **Classe: `HistoricoStatusTarefa`**
   - **Descrição:** Representa o histórico de alterações de status de uma tarefa.
   - **Atributos:**
     - `id`: Long
     - `tarefa`: Tarefa
     - `statusTarefa`: StatusTarefa
     - `usuarioAlteracao`: Usuario
     - `dataHoraAlteracao`: LocalDateTime

3. **Classe: `StatusTarefa`**
   - **Descrição:** Representa um status que uma tarefa pode ter.
   - **Atributos:**
     - `codigo`: String
     - `descricao`: String

4. **Classe: `Tarefa`**
   - **Descrição:** Representa uma tarefa no sistema.
   - **Atributos:**
     - `id`: Long
     - `nome`: String
     - `descricao`: String
     - `prioridade`: Integer
     - `dataCriacao`: LocalDate
     - `dataPrevisaoConclusao`: LocalDate
     - `historicoStatus`: List\<HistoricoStatusTarefa>
     - `comentarios`: List\<ComentarioTarefa>

5. **Classe: `Usuario`**
   - **Descrição:** Representa um usuário no sistema.
   - **Atributos:**
     - `id`: Long
     - `nome`: String
     - `email`: String

### DIAGRAMA DE CLASSES EM MERMAID
```mermaid
classDiagram
    class Usuario {
        +Long id
        +String nome
        +String email
    }

    class Tarefa {
        +Long id
        +String nome
        +String descricao
        +Integer prioridade
        +LocalDate dataCriacao
        +LocalDate dataPrevisaoConclusao
        +List<ComentarioTarefa> comentarios
        +List<HistoricoStatusTarefa> historicoStatus
    }

    class ComentarioTarefa {
        +Long id
        +LocalDateTime dataHoraComentario
        +String dsComentario
    }

    class HistoricoStatusTarefa {
        +Long id
        +LocalDateTime dataHoraAlteracao
    }

    class StatusTarefa {
        +String codigo
        +String descricao
    }

    Usuario --o ComentarioTarefa : faz
    Tarefa --o ComentarioTarefa : possui
    Usuario --o HistoricoStatusTarefa : altera
    Tarefa --o HistoricoStatusTarefa : possui
    StatusTarefa --o HistoricoStatusTarefa : representa
    Tarefa --o StatusTarefa : possui
```

### DESCRIÇÃO DO DIAGRAMA
- As classes representam as entidades do sistema, onde:
  - `Usuario` pode fazer múltiplos `ComentarioTarefa`.
  - `Tarefa` pode ter múltiplos `ComentarioTarefa` e `HistoricoStatusTarefa`.
  - `HistoricoStatusTarefa` se relaciona com `StatusTarefa`, que representa o status da tarefa.

# REGRAS DE NEGÓCIO

1. **Adicionar Comentário a uma Tarefa**
2. **Listar Comentários de uma Tarefa**
3. **Criar Tarefa**
4. **Editar Tarefa**
5. **Alterar Status de uma Tarefa**
6. **Excluir Tarefa**
7. **Buscar Tarefa por ID**
8. **Listar Tarefas**
9. **Listar Status de Tarefa**
10. **Listar Último Histórico por Tarefa**
11. **Criar Usuário**
12. **Listar Usuários**