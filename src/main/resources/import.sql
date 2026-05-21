INSERT INTO project (id, name, sigla, constitution) VALUES (1, 'Projeto Spring IA', 'ASDFG', '# SDD Constitution - Sistema de Controle de Estacionamento\n\n## 1. Objetivo\n\nEste documento define a constituição técnica (SDD - Software Design Document) para o desenvolvimento de um Sistema de Controle de Estacionamento utilizando:\n\n- Backend: Java 21\n- Frontend: Angular (última versão estável)\n- Persistência: JPA (Jakarta Persistence)\n- UI Frontend: PrimeNG\n- Validação: Jakarta Validation\n');
INSERT INTO project (id, name, sigla) VALUES (2, 'Sistema de Atendimento', 'SDFGH');
INSERT INTO project (id, name, sigla) VALUES (3, 'Portal do Cliente', 'QWERT');
INSERT INTO project (id, name, sigla) VALUES (4, 'Gestao de Tarefas', 'QAZXS');
INSERT INTO code_repo (id_code_repo, id_project, name, path, branch) VALUES (1, 1, 'springia-backend', 'https://github.com/exemplo/springia-backend.git', 'main');
INSERT INTO code_repo (id_code_repo, id_project, name, path, branch) VALUES (2, 1, 'springia-frontend', 'https://github.com/exemplo/springia-frontend.git', 'main');
INSERT INTO code_repo (id_code_repo, id_project, name, path, branch) VALUES (3, 2, 'atendimento-api', 'https://github.com/exemplo/atendimento-api.git', 'develop');
INSERT INTO code_repo (id_code_repo, id_project, name, path, branch) VALUES (4, 3, 'atendimento-web', 'https://github.com/exemplo/atendimento-web.git', 'main');
INSERT INTO code_repo (id_code_repo, id_project, name, path, branch) VALUES (5, 4, 'portal-cliente-api', 'https://github.com/exemplo/portal-cliente-api.git', 'release');