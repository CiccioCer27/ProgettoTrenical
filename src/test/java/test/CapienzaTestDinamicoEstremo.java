package test;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import service.ClientService;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔥 TEST DINAMICO ESTREMO - STRESS MASSIMO
 *
 * 🎯 OBIETTIVO: Trovare il punto di rottura del sistema
 * - Client configurabili da menu
 * - Stress progressivo
 * - Analisi dettagliata delle prestazioni
 */
public class CapienzaTestDinamicoEstremo {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;

    // Statistiche dettagliate
    private static final AtomicInteger successi = new AtomicInteger(0);
    private static final AtomicInteger fallimenti = new AtomicInteger(0);
    private static final AtomicInteger erroriConnessione = new AtomicInteger(0);
    private static final AtomicInteger timeout = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("🔥 ===== TEST DINAMICO ESTREMO =====");
        System.out.println("🎯 Obiettivo: Stress test progressivo del sistema reale");

        try {
            // 1️⃣ Verifica sistema
            StatoSistema stato = rilevaSistema();
            if (stato == null) {
                System.out.println("❌ Server non raggiungibile su porta " + SERVER_PORT);
                System.out.println("💡 Assicurati che ServerConsoleMain sia attivo");
                return;
            }

            // 2️⃣ Mostra configurazione
            mostraConfigurazione(stato);

            // 3️⃣ Menu scelta intensità
            int numClient = scegliIntensitaTest(stato);

            // 4️⃣ Esegui test estremo
            eseguiTestEstremo(stato, numClient);

        } catch (Exception e) {
            System.err.println("❌ Errore durante test estremo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔍 Rileva sistema (versione ottimizzata)
     */
    private static StatoSistema rilevaSistema() {
        try {
            System.out.println("🔍 Connessione al sistema...");

            ClientService client = new ClientService(SERVER_HOST, SERVER_PORT);

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;")
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(richiesta);
            List<TrattaDTO> tratte = risposta.getTratte();

            if (tratte == null || tratte.isEmpty()) {
                System.out.println("⚠️ Aspetto che il sistema generi tratte...");
                Thread.sleep(5000);
                risposta = client.inviaRichiesta(richiesta);
                tratte = risposta.getTratte();
            }

            if (tratte == null || tratte.isEmpty()) {
                return null;
            }

            return new StatoSistema(tratte);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 📊 Mostra configurazione rilevata
     */
    private static void mostraConfigurazione(StatoSistema stato) {
        System.out.println("\n📊 SISTEMA RILEVATO:");
        System.out.println("   🚂 Tratte disponibili: " + stato.tratte.size());
        System.out.println("   🎫 Posti stimati totali: " + stato.postiTotaliStimati);
        System.out.println("   📅 Periodo: " + stato.getDateRange());

        // Mostra dettagli per alcune tratte
        System.out.println("\n🔍 Dettaglio prime tratte:");
        for (int i = 0; i < Math.min(3, stato.tratte.size()); i++) {
            TrattaDTO t = stato.tratte.get(i);
            System.out.println("   " + (i + 1) + ") " + t.getStazionePartenza() +
                    " → " + t.getStazioneArrivo() + " | " + t.getData() + " " + t.getOra());
        }
        if (stato.tratte.size() > 3) {
            System.out.println("   ... e altre " + (stato.tratte.size() - 3) + " tratte");
        }
    }

    /**
     * 🎯 Menu scelta intensità test
     */
    private static int scegliIntensitaTest(StatoSistema stato) {
        System.out.println("\n🎯 SCELTA INTENSITÀ STRESS TEST:");
        System.out.println("================================");

        int base = stato.postiTotaliStimati;

        System.out.println("1️⃣  LEGGERO:    " + (base * 2) + " client (2x capacità)");
        System.out.println("2️⃣  MODERATO:   " + (base * 5) + " client (5x capacità)");
        System.out.println("3️⃣  INTENSO:    " + (base * 10) + " client (10x capacità)");
        System.out.println("4️⃣  ESTREMO:    " + (base * 20) + " client (20x capacità)");
        System.out.println("5️⃣  APOCALISSE: " + (base * 50) + " client (50x capacità)");
        System.out.println("6️⃣  PERSONALIZZATO: Inserisci numero manualmente");

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.print("\n👉 Scegli intensità (1-6): ");

        try {
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            return switch (scelta) {
                case 1 -> base * 2;
                case 2 -> base * 5;
                case 3 -> base * 10;
                case 4 -> base * 20;
                case 5 -> base * 50;
                case 6 -> {
                    System.out.print("Inserisci numero client: ");
                    yield Integer.parseInt(scanner.nextLine().trim());
                }
                default -> {
                    System.out.println("Scelta non valida, uso MODERATO");
                    yield base * 5;
                }
            };

        } catch (NumberFormatException e) {
            System.out.println("Input non valido, uso MODERATO");
            return base * 5;
        }
    }

    /**
     * 🔥 Esegui test estremo
     */
    private static void eseguiTestEstremo(StatoSistema stato, int numClient) throws Exception {
        System.out.println("\n🔥 AVVIO TEST ESTREMO");
        System.out.println("🎯 Client da lanciare: " + numClient);
        System.out.println("📊 Posti disponibili stimati: " + stato.postiTotaliStimati);
        System.out.println("⚡ Intensità: " + String.format("%.1fx", (double)numClient / stato.postiTotaliStimati));

        // Conferma per test molto intensi
        if (numClient > 1000) {
            System.out.print("\n⚠️ Test molto intenso (" + numClient + " client). Confermi? (s/N): ");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            String conferma = scanner.nextLine().trim().toLowerCase();
            if (!"s".equals(conferma) && !"si".equals(conferma)) {
                System.out.println("❌ Test annullato");
                return;
            }
        }

        // Reset contatori
        successi.set(0);
        fallimenti.set(0);
        erroriConnessione.set(0);
        timeout.set(0);

        // Configura thread pool dinamico
        int threadPool = Math.min(numClient, 500); // Max 500 thread concorrenti
        ExecutorService executor = Executors.newFixedThreadPool(threadPool);
        CountDownLatch latch = new CountDownLatch(numClient);

        System.out.println("🚀 Lancio " + numClient + " client con pool di " + threadPool + " thread...");

        long startTime = System.currentTimeMillis();

        // Lancia tutti i client
        for (int i = 0; i < numClient; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    eseguiClientEstremo(clientId, stato);
                } catch (Exception e) {
                    erroriConnessione.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // Piccola pausa ogni 100 client per non sovraccaricare la creazione
            if (i > 0 && i % 100 == 0) {
                Thread.sleep(10);

                // Progress indicator
                if (i % 500 == 0) {
                    System.out.println("   📤 Lanciati " + i + "/" + numClient + " client...");
                }
            }
        }

        // Aspetta completamento con timeout esteso
        int timeoutMinuti = Math.max(2, numClient / 500); // Timeout dinamico
        boolean completato = latch.await(timeoutMinuti, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        // 📊 Analisi risultati estremi
        analizzaRisultatiEstremi(stato, numClient, startTime, endTime, completato);
    }

    /**
     * 👤 Client estremo ottimizzato
     */
    private static void eseguiClientEstremo(int clientId, StatoSistema stato) throws Exception {
        ClientService client = new ClientService(SERVER_HOST, SERVER_PORT);
        client.attivaCliente("ExtremeUser" + clientId, "Stress",
                "extreme" + clientId + "@test.com", 25, "Test", "333" + (clientId % 10000));

        // Sceglie tratta casuale
        TrattaDTO trattaCasuale = stato.tratte.get(
                (int)(Math.random() * stato.tratte.size())
        );

        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(trattaCasuale)
                .classeServizio(ClasseServizio.BASE)
                .tipoPrezzo(TipoPrezzo.INTERO)
                .build();

        try {
            RispostaDTO risposta = client.inviaRichiesta(acquisto);

            if (risposta.getEsito().equals("OK")) {
                int currentSuccessi = successi.incrementAndGet();
                // Log milestone
                if (currentSuccessi % 100 == 0) {
                    System.out.println("   ✅ Milestone: " + currentSuccessi + " acquisti completati");
                }
            } else {
                fallimenti.incrementAndGet();
            }

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                timeout.incrementAndGet();
            } else {
                erroriConnessione.incrementAndGet();
            }
        }
    }

    /**
     * 📊 Analisi risultati estremi
     */
    private static void analizzaRisultatiEstremi(StatoSistema stato, int numClient,
                                                 long startTime, long endTime, boolean completato) {

        int totalSuccessi = successi.get();
        int totalFallimenti = fallimenti.get();
        int totalErrori = erroriConnessione.get();
        int totalTimeout = timeout.get();
        int totalTentativi = totalSuccessi + totalFallimenti + totalErrori + totalTimeout;

        long tempoTotale = endTime - startTime;
        double throughput = totalTentativi > 0 ? (totalTentativi * 1000.0) / tempoTotale : 0;

        System.out.println("\n🔥 RISULTATI TEST ESTREMO:");
        System.out.println("=" .repeat(50));
        System.out.println("   ⏱️ Tempo totale: " + tempoTotale + "ms (" + String.format("%.1f", tempoTotale/1000.0) + "s)");
        System.out.println("   ✅ Completato: " + (completato ? "SÌ" : "⚠️ TIMEOUT"));
        System.out.println("   👥 Client lanciati: " + numClient);
        System.out.println("   📤 Richieste completate: " + totalTentativi + "/" + numClient);
        System.out.println("   🚀 Throughput: " + String.format("%.1f", throughput) + " richieste/sec");

        System.out.println("\n📊 DETTAGLIO RISULTATI:");
        System.out.println("   ✅ Acquisti riusciti: " + totalSuccessi);
        System.out.println("   ❌ Acquisti rifiutati: " + totalFallimenti);
        System.out.println("   🔌 Errori connessione: " + totalErrori);
        System.out.println("   ⏰ Timeout: " + totalTimeout);

        if (totalTentativi > 0) {
            double successRate = (totalSuccessi * 100.0) / totalTentativi;
            double rejectionRate = (totalFallimenti * 100.0) / totalTentativi;
            double errorRate = ((totalErrori + totalTimeout) * 100.0) / totalTentativi;

            System.out.println("\n📈 PERCENTUALI:");
            System.out.println("   ✅ Tasso successo: " + String.format("%.1f%%", successRate));
            System.out.println("   ❌ Tasso rifiuto: " + String.format("%.1f%%", rejectionRate));
            System.out.println("   🔧 Tasso errore: " + String.format("%.1f%%", errorRate));
        }

        // Analisi prestazioni
        System.out.println("\n⚡ ANALISI PRESTAZIONI:");
        if (tempoTotale > 0) {
            double clientPerSecondo = (numClient * 1000.0) / tempoTotale;
            System.out.println("   🏃 Velocità lancio client: " + String.format("%.1f", clientPerSecondo) + " client/sec");
        }

        if (throughput > 100) {
            System.out.println("   🚀 ECCELLENTE: Throughput molto alto");
        } else if (throughput > 50) {
            System.out.println("   👍 BUONO: Throughput accettabile");
        } else if (throughput > 10) {
            System.out.println("   ⚠️ LENTO: Throughput basso");
        } else {
            System.out.println("   🐌 CRITICO: Sistema sovraccarico");
        }

        // Analisi comportamento sistema
        System.out.println("\n🎯 ANALISI COMPORTAMENTO SISTEMA:");

        double intensita = (double)numClient / stato.postiTotaliStimati;

        if (totalSuccessi > stato.postiTotaliStimati * 1.1) {
            System.out.println("   🚨 POSSIBILE OVERSELLING: Troppi acquisti riusciti!");
            System.out.println("   🔍 Venduti: " + totalSuccessi + " vs Capacità stimata: " + stato.postiTotaliStimati);
        } else if (totalFallimenti > totalSuccessi && intensita > 2) {
            System.out.println("   ✅ CONTROLLO CAPIENZA: Funziona bene sotto stress");
            System.out.println("   🛡️ Sistema respinge appropriatamente l'eccesso");
        } else if (totalErrori + totalTimeout > totalTentativi * 0.1) {
            System.out.println("   ⚠️ PROBLEMI CONNETTIVITÀ: Molti errori di rete");
            System.out.println("   💡 Sistema potrebbe essere sovraccarico");
        } else if (intensita > 10 && totalSuccessi > 0) {
            System.out.println("   🏆 RESISTENZA ESTREMA: Sistema funziona sotto stress " + String.format("%.0fx", intensita));
        }

        // Raccomandazioni
        System.out.println("\n💡 RACCOMANDAZIONI:");
        if (totalErrori + totalTimeout > numClient * 0.2) {
            System.out.println("   🔧 Considera ottimizzazione server o aumento risorse");
        }
        if (totalSuccessi == 0 && intensita > 5) {
            System.out.println("   🎯 Sistema potrebbe essere già saturo - prova con meno client");
        }
        if (totalSuccessi > 0 && totalFallimenti == 0 && intensita > 1.5) {
            System.out.println("   📈 Sistema ha molto spazio - prova stress ancora maggiore");
        }

        System.out.println("\n🏆 VERDETTO STRESS TEST ESTREMO:");
        if (totalSuccessi <= stato.postiTotaliStimati && totalFallimenti > 0 && (totalErrori + totalTimeout) < numClient * 0.3) {
            System.out.println("   🎉 SISTEMA ROBUSTO: Gestisce bene carichi estremi!");
        } else if (totalErrori + totalTimeout > numClient * 0.5) {
            System.out.println("   ⚠️ SISTEMA SOVRACCARICO: Troppe connessioni fallite");
        } else {
            System.out.println("   🤔 RISULTATI MISTI: Sistema funziona ma con limitazioni");
        }
    }

    /**
     * 📋 Classe stato sistema (ottimizzata)
     */
    private static class StatoSistema {
        final List<TrattaDTO> tratte;
        final int postiTotaliStimati;

        StatoSistema(List<TrattaDTO> tratte) {
            this.tratte = tratte;
            // Stima più conservativa
            this.postiTotaliStimati = tratte.size() * 80; // 80 posti per treno medio
        }

        String getDateRange() {
            if (tratte.isEmpty()) return "Nessuna";

            java.time.LocalDate prima = tratte.get(0).getData();
            java.time.LocalDate ultima = prima;

            for (TrattaDTO tratta : tratte) {
                if (tratta.getData().isBefore(prima)) prima = tratta.getData();
                if (tratta.getData().isAfter(ultima)) ultima = tratta.getData();
            }

            return prima.equals(ultima) ? prima.toString() : prima + " → " + ultima;
        }
    }
}