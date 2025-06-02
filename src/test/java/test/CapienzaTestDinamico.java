package test;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import service.ClientService;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔄 TEST DINAMICO - Compatibile con Scheduler
 *
 * ✅ VANTAGGI:
 * - Funziona con server di produzione attivo
 * - Si adatta dinamicamente alle tratte esistenti
 * - NON interferisce con il sistema
 * - Testa thread safety in condizioni reali
 *
 * 🚀 COME USARE:
 * 1. Assicurati che il server di produzione (porta 9090) SIA ATTIVO
 * 2. Esegui: java test.CapienzaTestDinamico
 * 3. Il test si adatta alle tratte presenti nel sistema
 */
public class CapienzaTestDinamico {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090; // Server di produzione

    // Statistiche
    private static final AtomicInteger successi = new AtomicInteger(0);
    private static final AtomicInteger fallimenti = new AtomicInteger(0);
    private static final AtomicInteger erroriConnessione = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("🔄 ===== TEST DINAMICO SCHEDULER-COMPATIBLE =====");
        System.out.println("🎯 Target: Server produzione (porta " + SERVER_PORT + ")");

        try {
            // 1️⃣ Rileva stato sistema
            StatoSistema stato = rilevaSistema();
            if (stato == null) {
                System.out.println("❌ Impossibile connettersi al server di produzione");
                System.out.println("💡 Assicurati che il server sia avviato su porta " + SERVER_PORT);
                return;
            }

            // 2️⃣ Mostra configurazione rilevata
            mostraConfigurazioneRilevata(stato);

            // 3️⃣ Esegui test adattivo
            eseguiTestAdattivo(stato);

        } catch (Exception e) {
            System.err.println("❌ Errore durante test dinamico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔍 Rileva lo stato attuale del sistema
     */
    private static StatoSistema rilevaSistema() {
        try {
            System.out.println("🔍 Rilevamento stato sistema...");

            ClientService client = new ClientService(SERVER_HOST, SERVER_PORT);

            // Richiedi tutte le tratte disponibili
            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;") // Filtro vuoto = tutte le tratte
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(richiesta);

            if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
                System.out.println("⚠️ Nessuna tratta presente, aspetto che lo scheduler le generi...");
                Thread.sleep(10000); // Aspetta 10 secondi

                // Riprova
                risposta = client.inviaRichiesta(richiesta);
            }

            List<TrattaDTO> tratte = risposta.getTratte();
            if (tratte == null || tratte.isEmpty()) {
                System.out.println("❌ Ancora nessuna tratta dopo attesa");
                return null;
            }

            return new StatoSistema(tratte);

        } catch (Exception e) {
            System.err.println("❌ Errore rilevamento sistema: " + e.getMessage());
            return null;
        }
    }

    /**
     * 📊 Mostra configurazione rilevata
     */
    private static void mostraConfigurazioneRilevata(StatoSistema stato) {
        System.out.println("\n📊 SISTEMA RILEVATO:");
        System.out.println("   🚂 Tratte disponibili: " + stato.tratte.size());
        System.out.println("   🎫 Posti totali stimati: " + stato.postiTotaliStimati);
        System.out.println("   📅 Date tratte: " + stato.getDateRange());

        System.out.println("\n🎯 CONFIGURAZIONE TEST:");
        System.out.println("   👥 Client concorrenti: " + stato.clientiDaUsare);
        System.out.println("   🎲 Strategia: Distribuzione casuale su tutte le tratte");
        System.out.println("   ⏱️ Timeout: 60 secondi");
    }

    /**
     * 🚀 Esegui test adattivo basato sullo stato rilevato
     */
    private static void eseguiTestAdattivo(StatoSistema stato) throws Exception {
        System.out.println("\n🚀 AVVIO TEST ADATTIVO");

        // Reset contatori
        successi.set(0);
        fallimenti.set(0);
        erroriConnessione.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(stato.clientiDaUsare, 100) // Max 100 thread concorrenti
        );
        CountDownLatch latch = new CountDownLatch(stato.clientiDaUsare);

        long startTime = System.currentTimeMillis();

        // Lancia client concorrenti
        for (int i = 0; i < stato.clientiDaUsare; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    eseguiClientAdattivo(clientId, stato);
                } catch (Exception e) {
                    erroriConnessione.incrementAndGet();
                    if (clientId < 5) { // Log solo primi errori
                        System.err.println("❌ Errore client " + clientId + ": " + e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta completamento
        boolean completato = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        // 📊 Analisi risultati
        analizzaRisultati(stato, startTime, endTime, completato);
    }

    /**
     * 👤 Client adattivo singolo
     */
    private static void eseguiClientAdattivo(int clientId, StatoSistema stato) throws Exception {
        ClientService client = new ClientService(SERVER_HOST, SERVER_PORT);
        client.attivaCliente("DynUser" + clientId, "Dynamic",
                "dyn" + clientId + "@test.com", 25, "Test", "333" + clientId);

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

        RispostaDTO risposta = client.inviaRichiesta(acquisto);

        if (risposta.getEsito().equals("OK")) {
            successi.incrementAndGet();
            if (clientId < 10) { // Log solo primi successi
                System.out.println("   ✅ Client " + clientId + " acquisto riuscito su " +
                        trattaCasuale.getStazionePartenza() + "→" + trattaCasuale.getStazioneArrivo());
            }
        } else {
            fallimenti.incrementAndGet();
            if (clientId < 5) { // Log solo primi fallimenti
                System.out.println("   ❌ Client " + clientId + " rifiutato: " + risposta.getMessaggio());
            }
        }
    }

    /**
     * 📊 Analizza risultati del test
     */
    private static void analizzaRisultati(StatoSistema stato, long startTime, long endTime, boolean completato) {
        int totalSuccessi = successi.get();
        int totalFallimenti = fallimenti.get();
        int totalErrori = erroriConnessione.get();
        int totalTentativi = totalSuccessi + totalFallimenti + totalErrori;

        System.out.println("\n📊 RISULTATI TEST DINAMICO:");
        System.out.println("   ⏱️ Tempo esecuzione: " + (endTime - startTime) + "ms");
        System.out.println("   ✅ Completato: " + (completato ? "SÌ" : "TIMEOUT"));
        System.out.println("   👥 Client lanciati: " + stato.clientiDaUsare);
        System.out.println("   📤 Tentativi totali: " + totalTentativi);
        System.out.println("   ✅ Acquisti riusciti: " + totalSuccessi);
        System.out.println("   ❌ Acquisti rifiutati: " + totalFallimenti);
        System.out.println("   🔌 Errori connessione: " + totalErrori);

        if (totalTentativi > 0) {
            double successRate = (totalSuccessi * 100.0) / totalTentativi;
            double rejectionRate = (totalFallimenti * 100.0) / totalTentativi;

            System.out.println("   📈 Tasso successo: " + String.format("%.1f%%", successRate));
            System.out.println("   📉 Tasso rifiuto: " + String.format("%.1f%%", rejectionRate));
        }

        System.out.println("\n🎯 ANALISI COMPORTAMENTO:");

        if (totalSuccessi > 0 && totalFallimenti > totalSuccessi) {
            System.out.println("   ✅ BUONO: Sistema respinge appropriatamente l'eccesso");
            System.out.println("   🛡️ Controllo capienza sembra funzionare");
        } else if (totalSuccessi == 0) {
            System.out.println("   ⚠️ PROBLEMA: Nessun acquisto riuscito");
            System.out.println("   💡 Possibili cause: sistema sovraccarico o tutte le tratte piene");
        } else if (totalFallimenti == 0) {
            System.out.println("   🤔 AMBIGUO: Tutti gli acquisti riusciti");
            System.out.println("   💡 Possibile se c'era molto spazio disponibile");
        }

        System.out.println("\n🏆 VERDETTO DINAMICO:");
        if (totalSuccessi > 0 && (totalFallimenti > 0 || totalSuccessi < stato.postiTotaliStimati)) {
            System.out.println("   🎉 Sistema sembra gestire bene la concorrenza");
            System.out.println("   ✨ Thread safety probabilmente funzionante");
        } else {
            System.out.println("   🤷 Risultati inconcludenti - test più specifico necessario");
        }

        System.out.println("\n💡 NOTA: Per test più precisi, usa CapienzaTestIsolato");
    }

    /**
     * 📋 Classe per mantenere lo stato del sistema rilevato
     */
    private static class StatoSistema {
        final List<TrattaDTO> tratte;
        final int postiTotaliStimati;
        final int clientiDaUsare;

        StatoSistema(List<TrattaDTO> tratte) {
            this.tratte = tratte;
            // Stima conservativa: 100 posti per treno
            this.postiTotaliStimati = tratte.size() * 100;
            // Usa 5x overcapacity ma max 200 client
            this.clientiDaUsare = Math.min(postiTotaliStimati * 5, 200);
        }

        String getDateRange() {
            if (tratte.isEmpty()) return "Nessuna";

            java.time.LocalDate prima = tratte.get(0).getData();
            java.time.LocalDate ultima = prima;

            for (TrattaDTO tratta : tratte) {
                if (tratta.getData().isBefore(prima)) prima = tratta.getData();
                if (tratta.getData().isAfter(ultima)) ultima = tratta.getData();
            }

            if (prima.equals(ultima)) {
                return prima.toString();
            } else {
                return prima + " → " + ultima;
            }
        }
    }
}