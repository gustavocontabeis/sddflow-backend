# PGVector Quick Start

Este projeto esta configurado para usar PostgreSQL com PGVector via `docker-compose`.

## 1) Subir banco com PGVector

```bash
docker compose up -d
```

## 2) Rodar aplicacao

```bash
./mvnw spring-boot:run
```

## 3) Endpoints de exemplo

### Inserir documento vetorial

`POST /api/vector-documents`

Exemplo de payload (vetor com 1536 dimensoes):

```json
{
  "content": "Exemplo de documento",
  "embedding": [0.1, 0.2, 0.3]
}
```

> Observacao: para passar na validacao, o campo `embedding` deve ter exatamente 1536 valores.

### Buscar vizinhos mais proximos

`POST /api/vector-documents/search`

```json
{
  "embedding": [0.1, 0.2, 0.3],
  "limit": 5
}
```

## 4) Comandos cURL (com vetor ficticio)

```bash
curl -X POST http://localhost:8080/api/vector-documents \
  -H 'Content-Type: application/json' \
  -d '{"content":"doc 1","embedding":['"$(python3 - <<'PY'
print(','.join(['0.001']*1536))
PY
)"']}'
```

```bash
curl -X POST http://localhost:8080/api/vector-documents/search \
  -H 'Content-Type: application/json' \
  -d '{"embedding":['"$(python3 - <<'PY'
print(','.join(['0.001']*1536))
PY
)'"],"limit":3}'
```

