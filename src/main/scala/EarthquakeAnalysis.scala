import org.apache.spark.sql.SparkSession

object EarthquakeAnalysis {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("Earthquake Analysis")
      // .master("local[*]") // Scommenta SOLO per test in locale
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val filename = if (args.length > 0) args(0) else "dataset-earthquakes-trimmed.csv"
    println(s"Leggendo il dataset da: $filename")

    val data = spark.read.option("header", value = true).csv(filename).rdd

    // 1. Parsing, normalizzazione e deduplicazione
    val cleanedData = data.flatMap(row => {
      try {
        val lon      = row.getString(0).toDouble
        val lat      = row.getString(1).toDouble
        val datetime = row.getString(2)

        // Arrotondamento alla prima cifra decimale
        val roundedLat = math.round(lat * 10.0) / 10.0
        val roundedLon = math.round(lon * 10.0) / 10.0

        // Finestra temporale giornaliera
        val date = datetime.substring(0, 10)

        Some((date, (roundedLat, roundedLon)))
      } catch {
        case _: Exception => None // Salta righe malformate
      }
    }).distinct() // Rimuove duplicati (stessa cella, stesso giorno)


    // 2. Generazione delle co-occorrenze
    val coOccurrences = cleanedData
      .groupByKey()
      .flatMap { case (date, locations) =>
        val sorted = locations.toSeq.sorted
        for {
          i <- sorted.indices
          j <- (i + 1) until sorted.size
        } yield ((sorted(i), sorted(j)), date)
      }

    // 3. Raggruppamento per coppia e ordinamento delle date
    val groupedOccurrences = coOccurrences
      .groupByKey()
      .mapValues(dates => dates.toList.distinct.sorted)

    // 4. Ricerca del massimo con reduce (evita di ordinare l'intero RDD)
    val maxCoOccurrence = groupedOccurrences.reduce { (a, b) =>
      if (a._2.size >= b._2.size) a else b
    }

    // 5. Output 
    val ((loc1, loc2), dates) = maxCoOccurrence
    println("--- RISULTATO ---")
    println(s"((${loc1._1}, ${loc1._2}), (${loc2._1}, ${loc2._2}))")
    dates.foreach(println)
    println("-----------------")

    spark.stop()
  }
}