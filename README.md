# Earthquake Co-occurrence Analysis with Apache Spark

Progetto per il corso di **Scalable and Cloud Programming** (A.A. 2025-26).  
Implementazione in Scala + Spark di un'analisi di co-occorrenza di terremoti su dataset globale.

## Descrizione

L'obiettivo è trovare la coppia di località geografiche con il massimo numero di co-occorrenze sismiche,
e restituire le date in cui tali co-occorrenze avvengono in ordine crescente.

Le coordinate vengono approssimate alla prima cifra decimale e la finestra temporale è di un giorno.

## Risultato

La coppia con più co-occorrenze nel dataset completo è: 

((38.8, -122.8), (38.8, -122.7))

Due zone sismicamente attive nella California del Nord (area di San Francisco/Berkeley),
con oltre 10.000 giorni di co-occorrenza nel periodo 1990-2023.

## Struttura del progetto

progetto_esame/
├── build.sbt
├── project/
│   ├── build.properties
│   └── plugins.sbt
├── src/
│   └── main/
│       └── scala/
│           └── EarthquakeAnalysis.scala
├── README.md          
└── .gitignore         


## Requisiti

- Java 11+
- sbt 1.12+
- Account Google Cloud con progetto attivo e fatturazione abilitata
- Google Cloud CLI (`gcloud`) installato e autenticato

## Compilazione

```bash
sbt assembly
```

Il JAR viene generato in:

target/scala-2.12/earthquake-assembly.jar

## Esecuzione su Google Cloud DataProc

### 1. Carica i file sul bucket

```bash
gsutil cp target/scala-2.12/earthquake-assembly.jar gs://NOME-BUCKET/
gsutil cp dataset-earthquakes-full.csv gs://NOME-BUCKET/
```

### 2. Crea il cluster DataProc

```bash
gcloud dataproc clusters create earthquake-cluster --region=REGIONE --num-workers=2 --master-boot-disk-size=240 --worker-boot-disk-size=240 --master-machine-type=n2-standard-4 --worker-machine-type=n2-standard-4
```

### 3. Lancia il job

```bash
gcloud dataproc jobs submit spark --cluster=earthquake-cluster --region=REGIONE --class=EarthquakeAnalysis --jars=gs://NOME-BUCKET/earthquake-assembly.jar -- gs://NOME-BUCKET/dataset-earthquakes-full.csv
```

### 4. Elimina il cluster al termine

```bash
gcloud dataproc clusters delete earthquake-cluster --region=REGIONE
```

Sostituisci `NOME-BUCKET` e `REGIONE` con i tuoi valori (es. `europe-west1`).

## Esecuzione in locale (test)

Decommentare la riga `.master("local[*]")` in `EarthquakeAnalysis.scala`, poi:

```bash
spark-submit --master "local[*]" \
  target/scala-2.12/earthquake-assembly.jar \
  dataset-earthquakes-trimmed.csv
```

## Analisi di scalabilità

Test eseguiti su Google Cloud DataProc con macchine `n2-standard-4`:

| Numero di Worker | Tempo di esecuzione |
|------------------|---------------------|
| 2                | ~33 minuti          |
| 3                | ~36 minuti          |
| 4                | ~37 minuti          |

I risultati mostrano che l'aggiunta di worker non migliora le prestazioni in modo lineare.
Questo è dovuto all'overhead di shuffle generato dall'operazione `groupByKey`,
che richiede intensa comunicazione tra i nodi e scala male all'aumentare dei worker.
