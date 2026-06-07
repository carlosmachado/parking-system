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
- Eleição da estratégia de preço configurável: na **saída** (padrão) ou na **entrada**.
- Eventos de webhook **idempotentes**: ENTRY e PARKED repetidos não duplicam sessões.
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

## Configuração

Todos os parâmetros próprios da aplicação ficam sob o namespace `app.*` em `application.yml`
(sobrescrevíveis por variável de ambiente):

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `app.pricing.election` | `AT_EXIT` | Quando eleger a estratégia de preço: `AT_EXIT` ou `AT_ENTRY`. |
| `app.simulator.connect-timeout-ms` | `2000` | Timeout de conexão com o simulador. |
| `app.simulator.read-timeout-ms` | `5000` | Timeout de leitura das respostas do simulador. |
| `app.revenue.update.max-attempts` | `3` | Tentativas de gravação da receita antes de descartar o incremento. |
| `app.revenue.update.retry-delay-ms` | `100` | Espera entre tentativas de gravação da receita. |
| `app.cache.enabled` | `true` | Liga/desliga o cache de setores (`false` usa cache no-op). |
| `app.cache.sector.ttl` | `12h` | TTL do cache de dados de setor. |
| `app.cache.sector.max-size` | `64` | Tamanho máximo do cache de setores. |

No `docker compose`, alterne o modo de eleição via variável de ambiente:

```bash
PRICING_ELECTION=AT_ENTRY docker compose up -d --build app
```

## Endpoints

### `POST /webhook`

Recebe os eventos do simulador. Sempre responde `HTTP 200`.

```json
{ "license_plate": "ZUL0001", "entry_time": "2025-01-01T12:00:00.000Z", "event_type": "ENTRY" }
{ "license_plate": "ZUL0001", "lat": -23.561684, "lng": -46.655981, "event_type": "PARKED" }
{ "license_plate": "ZUL0001", "exit_time": "2025-01-01T12:30:00.000Z", "event_type": "EXIT" }
```

Em caso de erro, a resposta segue um formato padronizado com `code` legível por máquina
(ex.: `EST-001` para garagem cheia, `WEB-001` para campo obrigatório ausente):

```json
{
  "timestamp": "2025-01-01T12:00:00",
  "status": 409,
  "error": "Conflict",
  "code": "EST-001",
  "message": "Garage is full, entry blocked for ZUL0001",
  "path": "/webhook"
}
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
  marca a vaga como ocupada. Um PARKED repetido na mesma vaga é ignorado; em vaga diferente é
  rejeitado.
- **Saída**: libera a vaga e cobra a permanência.
  - Os primeiros 30 minutos são gratuitos.
  - A partir daí, cobra-se uma tarifa fixa por hora iniciada (incluindo a primeira hora),
    usando o `basePrice` do setor, arredondando para cima.
- **Preço dinâmico** conforme a lotação da garagem:
  | Lotação | Ajuste |
  |---------|--------|
  | < 25%   | −10%   |
  | < 50%   | base   |
  | < 75%   | +10%   |
  | ≤ 100%  | +25%   |
- **Momento da eleição** controlado por `app.pricing.election` (ver Configuração):
  - `AT_EXIT` (padrão): a lotação é lida na saída e a estratégia escolhida nesse instante.
  - `AT_ENTRY`: a estratégia é eleita na entrada e gravada na sessão; a saída apenas a aplica,
    sem consultar a lotação. O modo escolhido fica registrado em cada sessão, então alternar a
    configuração não altera o preço de sessões já em andamento.
- Setores são divisões lógicas de um único conjunto de vagas, com uma única entrada.

## Testes

```bash
./mvnw test
```

Cobrem as estratégias de preço e os dois modos de eleição (`AT_ENTRY`/`AT_EXIT`), o cálculo
de período, o fluxo completo de webhook (entrada → estacionar → saída) com apuração de receita,
a idempotência de ENTRY/PARKED repetidos, o bloqueio de entrada com garagem cheia, o retry de
gravação de receita sob falha transitória, o cliente do simulador, os controllers REST e os
value objects.
