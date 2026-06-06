# Parking System

Backend para gestão de um estacionamento: controle de vagas, entrada e saída de veículos e
cálculo de receita. Os eventos de movimentação dos veículos chegam de um simulador externo
de garagem através de um webhook.

## Funcionalidades

- Carga inicial da garagem (setores e vagas) a partir do simulador.
- Recebimento de eventos de **entrada (ENTRY)**, **estacionamento (PARKED)** e
  **saída (EXIT)** via webhook.
- Ocupação e liberação de vagas conforme a movimentação dos veículos.
- Cálculo de tarifa com **preço dinâmico** baseado na lotação.
- Consulta da receita por setor e por dia.

## Stack

- Java 21, Spring Boot 3.4
- MySQL 8 + Flyway (migrações)
- Maven
- JSR-354 (Moneta) para valores monetários, Lombok

## Arquitetura

Organizado em Domain-Driven Design, com separação em camadas
`presentation → application → domain` e `infrastructure` para detalhes técnicos:

```
domain/         agregados, value objects, eventos de domínio, serviços de domínio
application/    serviços de aplicação (casos de uso)
infrastructure/ cliente do simulador, tratamento de erros HTTP, inicialização
presentation/   controllers REST
```

## Como executar

Suba o banco de dados, o simulador e a aplicação de uma vez:

```bash
docker compose up -d
```

O `docker compose` aguarda o MySQL estar saudável antes de iniciar a aplicação. Ao iniciar,
a aplicação busca a configuração da garagem em `GET /garage`, persiste setores e vagas e
registra o webhook. Se o simulador ainda não estiver disponível, a busca é refeita
automaticamente por algumas tentativas.

Para acompanhar os logs:

```bash
docker compose logs -f app
docker compose logs -f garage-sim
```

> **DataGrip / MySQL Workbench:** o banco fica disponível em `localhost:3307`
> (usuário `root`, senha `root`).

## Endpoints

### `POST /webhook`

Recebe os eventos do simulador. Sempre responde `HTTP 200`.

```json
{ "license_plate": "ZUL0001", "entry_time": "2025-01-01T12:00:00.000Z", "event_type": "ENTRY" }
{ "license_plate": "ZUL0001", "lat": -23.561684, "lng": -46.655981, "event_type": "PARKED" }
{ "license_plate": "ZUL0001", "exit_time": "2025-01-01T12:30:00.000Z", "event_type": "EXIT" }
```

### `GET /revenue`

Receita acumulada de um dia, opcionalmente filtrada por setor.

```bash
curl "http://localhost:3003/revenue?date=2025-01-01&sector=A"
```

```json
{ "amount": 0.00, "currency": "BRL", "timestamp": "2025-01-01T12:00:00.000Z" }
```

- `date` é opcional (padrão: dia atual).
- Sem `sector`, a receita de todos os setores é somada.

## Regras de negócio

- **Entrada**: cria uma sessão quando existe ao menos uma vaga livre em um setor aberto no
  horário atual. Se nenhum setor aberto tiver vaga disponível, a entrada é bloqueada.
- **Estacionamento**: associa o veículo à vaga livre mais próxima da localização recebida e
  marca a vaga como ocupada.
- **Saída**: libera a vaga e cobra a permanência usando a ocupação global atual da garagem.
  - Os primeiros 30 minutos são gratuitos.
  - A partir daí, cobra-se uma tarifa fixa por hora iniciada (incluindo a primeira hora),
    usando o `basePrice` do setor, arredondando para cima.
- **Preço dinâmico** conforme a lotação no momento da saída:
  | Lotação | Ajuste |
  |---------|--------|
  | < 25%   | −10%   |
  | < 50%   | base   |
  | < 75%   | +10%   |
  | ≤ 100%  | +25%   |
- Setores são divisões lógicas de um único conjunto de vagas, com uma única entrada.

## Testes

```bash
./mvnw test
```

Cobrem as estratégias de preço, o cálculo de período, o fluxo completo de webhook
(entrada → estacionar → saída) com apuração de receita, o bloqueio de entrada com garagem
cheia, os controllers REST e os value objects.
