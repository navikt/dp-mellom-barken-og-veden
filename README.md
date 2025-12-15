# dp-mellom-barken-og-veden


## Ansvarsområder

- Reagere på behandlingsresultat hendelser med utbetalinger fra [dp-behandling](https://github.com/navikt/dp-behandling) 
- Sørge for å effekturere utbetalinger til Hel-ved (https://helved-docs.ansatt.dev.nav.no) 
- Lytter på status på utbetalinger og oppdaterer behandling med resultat

### Flyttdiagram

```mermaid
flowchart TD
    A[dp-behandling] -->|behandlingsresultat<br/>teamdagpenger.rapid.v1| B[MeldingOmUtbetalingVedtakMottak]
    B -->|Lagrer UtbetalingVedtak<br/>Status: Mottatt| DB[(Database)]
    B -->|Publiserer utbetaling_mottatt<br/>teamdagpenger.rapid.v1| RAPID[Rapids]
    
    JOB[BehandleMottatteUtbetalinger<br/>Scheduled Job - 1 min] -->|Henter alle mottatte<br/>som ikke er sendt| DB
    JOB -->|HelvedUtsender| KAFKA_OUT[teamdagpenger.utbetaling.v1]
    KAFKA_OUT --> HELVED[Hel-ved]
    JOB -->|Oppdaterer Status:<br/>TilUtbetaling/SENDT| DB
    
    HELVED -->|Status kvitteringer| KAFKA_IN[helved.status.v1]
    KAFKA_IN -->|OK/MOTTATT/HOS_OPPDRAG/FEILET| STATUS[HelvedStatusMottak]
    STATUS -->|Oppdaterer Status:<br/>Ferdig eller TilUtbetaling| DB
    STATUS -->|Publiserer utbetaling_sendt/<br/>utbetaling_utført/utbetaling_feilet<br/>teamdagpenger.rapid.v1| RAPID
```



## Utvikling

### Komme i gang

Gradle brukes som byggverktøy og er bundlet inn.

```
./gradlew build
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* Eller en annen måte for omverden å kontakte teamet på

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #dagpenger-dev.
