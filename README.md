# WattMeter

Monitor in tempo reale della potenza di ricarica per Android, scritto in Kotlin con
Jetpack Compose e Material 3 Expressive.

## Cosa mostra

- **Watt in ingresso** istantanei, calcolati come tensione di cella × corrente reale
- **Tempo stimato** al 100% e all'80%, con orario previsto di completamento
- Corrente (mA), tensione (V), temperatura (°C), carica residua e capacità stimata (mAh)
- Salute della batteria, cicli di carica (Android 14+), capacità nominale residua (Android 15+)
- Grafico dell'andamento della potenza sugli ultimi 180 secondi
- Statistiche di sessione: durata, percentuale guadagnata, energia accumulata in Wh,
  potenza media e di picco, temperatura massima
- Record storici di potenza e corrente
- Notifica live persistente con avvio automatico opzionale al collegamento del caricatore

## Note tecniche

Il framework Android dichiara la corrente in microampere, ma diversi produttori
restituiscono milliampere. L'app riconosce l'ordine di grandezza in automatico e
permette comunque di forzare l'unità dalle impostazioni.

Il valore misurato è la potenza che entra nella cella: il caricatore ne eroga
tipicamente il 10-20% in più, perso fra cavo e conversione interna.

Nessun permesso speciale richiesto, a parte le notifiche per il servizio in foreground.

## Build

Ogni push su `main` avvia GitHub Actions, che compila un APK firmato e pubblica una
release scaricabile direttamente dal telefono. Il keystore è incluso nel repo
(`app/wattmeter.jks`), quindi tutte le build si installano come aggiornamento
della precedente.

Requisiti: minSdk 26, target/compile SDK 36, AGP 8.13.0, Kotlin 2.2.20, Gradle 8.13.
