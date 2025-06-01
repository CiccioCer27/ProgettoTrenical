package test;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import factory.TrattaFactoryConcrete;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.*;
import persistence.*;
import service.BancaServiceClient;
import service.ClientService;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🧪 TEST CAPIENZA E CONCORRENZA
 *
 * Verifica che il sistema:
 * 1. Rispetti rigorosamente la capienza dei treni
 * 2. Gestisca correttamente la concorrenza (molti client simultanei)
 * 3. Non overselli mai i biglietti
 * 4. Gestisca gracefully i rifiuti quando il treno è pieno
 */
public class CapienzaConcurrencyTest {

    private static final int SERVER_PORT = 8105;
    private static final int BANCA_PORT = 8106;

    // Test configuration
    private static final int CAPIENZA_TRENO_TEST = 10; // Capienza piccola per test rapidi
    private static final int NUM_CLIENT_CONCORRENTI = 25; // Più client della capienza
    private static final int TENTATIVI_PER_CLIENT = 3; // Ogni client prova più volte

    private static Server server;
    private static Server bancaServer;
    private static TrenicalServiceImpl trenicalService;
    private static MemoriaTratte memoriaTratte;
    private static MemoriaBiglietti memoriaBiglietti;

    // Statistiche del test
    private static final AtomicInteger richiesteInviate = new AtomicInteger(0);
    private static final AtomicInteger acquistiRiusciti = new AtomicInteger(0);
    private static final AtomicInteger prenotazioniRiuscite = new AtomicInteger(0);
    private static final AtomicInteger richiesteRifiutate = new AtomicInteger(0);
    private static final AtomicInteger erroriConnessione = new AtomicInteger(0);

    private static TrattaDTO trattaTest;

    public static void main(String[] args) {
        System.out.println("🧪 ===== TEST CAPIENZA E CONCORRENZA TRENICAL =====");
        System.out.println("🎯 Configurazione test:");
        System.out.println("   🚂 Capienza treno: " + CAPIENZA_TRENO_TEST + " posti");
        System.out.println("   👥 Client concorrenti: " + NUM_CLIENT_CONCORRENTI);
        System.out.println("   🔄 Tentativi per client: " + TENTATIVI_PER_CLIENT);
        System.out.println("   📊 Richieste totali attese: " + (NUM_CLIENT_CONCORRENTI * TENTATIVI_PER_CLIENT));

        try {
            // 1️⃣ Setup sistema
            setupSistema();

            // 2️⃣ Test capienza base
            testCapienzaBase();

            // 3️⃣ Test concorrenza estrema
            testConcorrenzaEstrema();

            // 4️⃣ Test misto acquisti + prenotazioni
            testMistoAcquistiPrenotazioni();

            // 5️⃣ Verifica integrità finale
            verificaIntegritaFinale();

            // 6️⃣ Report dettagliato
            stampaReportDettagliato();

        } catch (Exception e) {
            System.err.println("❌ Errore durante i test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /**
     * 🚀 Setup sistema con treno a capienza ridotta per test
     */
    private static void setupSistema() throws Exception {
        System.out.println("\n🚀 Setup sistema per test capienza");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // Componenti memoria
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // 🔧 Crea tratta speciale con capienza ridotta per il test
        creaTrattaTestCapienza();

        // Event system
        EventDispatcher dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti, memoriaTratte));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClienti));

        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, dispatcher, bancaClient
        );

        trenicalService = new TrenicalServiceImpl(notificaDispatcher, handler, memoriaPromozioni);

        // Server principale
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        Thread.sleep(2000);
        System.out.println("✅ Sistema avviato con tratta test (capienza " + CAPIENZA_TRENO_TEST + ")");
    }

    /**
     * 🚂 Crea una tratta speciale con capienza ridotta per il test
     */
    private static void creaTrattaTestCapienza() {
        // Crea treno con capienza molto ridotta
        model.Treno trenoTest = new model.Treno.Builder()
                .numero(9999)
                .tipologia("TestTreno")
                .capienzaTotale(CAPIENZA_TRENO_TEST) // ⚠️ Capienza ridotta per test
                .wifiDisponibile(true)
                .preseElettriche(true)
                .ariaCondizionata(true)
                .serviziRistorazione("Test")
                .accessibileDisabili(true)
                .nomeCommerciale("TrenoTest")
                .build();

        // Crea prezzi per il test
        Map<enums.ClasseServizio, model.Prezzo> prezzi = new HashMap<>();
        for (enums.ClasseServizio classe : enums.ClasseServizio.values()) {
            prezzi.put(classe, new model.Prezzo(20.0, 15.0, 10.0));
        }

        // Crea tratta test
        Tratta trattaTestModel = new Tratta(
                UUID.randomUUID(),
                "TestPartenza",
                "TestArrivo",
                LocalDate.now().plusDays(1),
                java.time.LocalTime.of(10, 30),
                99,
                trenoTest,
                prezzi
        );

        memoriaTratte.aggiungiTratta(trattaTestModel);

        // Converte a DTO per i test
        trattaTest = Assembler.AssemblerTratta.toDTO(trattaTestModel);

        System.out.println("🚂 Tratta test creata:");
        System.out.println("   ID: " + trattaTest.getId());
        System.out.println("   Tratta: " + trattaTest.getStazionePartenza() + " → " + trattaTest.getStazioneArrivo());
        System.out.println("   Capienza: " + CAPIENZA_TRENO_TEST + " posti");
    }

    /**
     * 🧪 Test 1: Verifica capienza base (sequenziale)
     */
    private static void testCapienzaBase() throws Exception {
        System.out.println("\n🧪 TEST 1: Verifica Capienza Base (Sequenziale)");
        System.out.println("Target: Riempire esattamente la capienza, poi verificare rifiuti");

        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("TestCapienza", "User", "capienza@test.com", 30, "Test", "3334444444");

        int bigliettiAcquistati = 0;
        int tentativiFalliti = 0;

        // Tenta di acquistare più biglietti della capienza
        for (int i = 0; i < CAPIENZA_TRENO_TEST + 5; i++) {
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(trattaTest)
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(acquisto);

            if (risposta.getEsito().equals("OK")) {
                bigliettiAcquistati++;
                System.out.println("   ✅ Biglietto " + (i + 1) + " acquistato");
            } else {
                tentativiFalliti++;
                System.out.println("   ❌ Biglietto " + (i + 1) + " rifiutato: " + risposta.getMessaggio());
            }

            Thread.sleep(100); // Piccola pausa per evitare spam
        }

        System.out.println("\n📊 RISULTATI TEST CAPIENZA BASE:");
        System.out.println("   ✅ Biglietti acquistati: " + bigliettiAcquistati);
        System.out.println("   ❌ Tentativi falliti: " + tentativiFalliti);
        System.out.println("   🎯 Capienza rispettata: " + (bigliettiAcquistati <= CAPIENZA_TRENO_TEST ? "SÌ" : "NO"));

        if (bigliettiAcquistati > CAPIENZA_TRENO_TEST) {
            System.out.println("   ⚠️ PROBLEMA: Venduti più biglietti della capienza!");
        } else if (bigliettiAcquistati == CAPIENZA_TRENO_TEST) {
            System.out.println("   🎉 PERFETTO: Venduti esattamente i posti disponibili!");
        }
    }

    /**
     * 🏎️ Test 2: Concorrenza estrema (molti client simultanei)
     */
    private static void testConcorrenzaEstrema() throws Exception {
        System.out.println("\n🏎️ TEST 2: Concorrenza Estrema");
        System.out.println("Target: " + NUM_CLIENT_CONCORRENTI + " client tentano simultaneamente di acquistare");

        // Reset contatori
        richiesteInviate.set(0);
        acquistiRiusciti.set(0);
        richiesteRifiutate.set(0);
        erroriConnessione.set(0);

        // Crea nuova tratta per questo test
        creaTrattaTestCapienza();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_CONCORRENTI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_CONCORRENTI);

        long startTime = System.currentTimeMillis();

        // Lancia client concorrenti
        for (int i = 0; i < NUM_CLIENT_CONCORRENTI; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    eseguiClientConcorrente(clientId);
                } catch (Exception e) {
                    erroriConnessione.incrementAndGet();
                    System.err.println("❌ Errore client " + clientId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta completamento
        latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        System.out.println("\n📊 RISULTATI TEST CONCORRENZA:");
        System.out.println("   ⏱️ Tempo esecuzione: " + (endTime - startTime) + "ms");
        System.out.println("   📤 Richieste inviate: " + richiesteInviate.get());
        System.out.println("   ✅ Acquisti riusciti: " + acquistiRiusciti.get());
        System.out.println("   ❌ Richieste rifiutate: " + richiesteRifiutate.get());
        System.out.println("   🔌 Errori connessione: " + erroriConnessione.get());

        // Verifica integrità
        int bigliettiVenduti = memoriaBiglietti.getTuttiIBiglietti().size();
        System.out.println("   🎫 Biglietti effettivamente salvati: " + bigliettiVenduti);
        System.out.println("   🎯 Capienza rispettata: " + (bigliettiVenduti <= CAPIENZA_TRENO_TEST ? "SÌ" : "NO"));

        if (bigliettiVenduti > CAPIENZA_TRENO_TEST) {
            System.out.println("   🚨 GRAVE: OVERSELLING RILEVATO!");
        } else {
            System.out.println("   ✅ Controllo capienza funziona correttamente sotto stress");
        }
    }

    /**
     * 👤 Simula un client concorrente che tenta più acquisti
     */
    private static void eseguiClientConcorrente(int clientId) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("ConcurrentUser" + clientId, "Test",
                "concurrent" + clientId + "@test.com", 25, "Test", "333" + clientId);

        for (int tentativo = 0; tentativo < TENTATIVI_PER_CLIENT; tentativo++) {
            try {
                RichiestaDTO acquisto = new RichiestaDTO.Builder()
                        .tipo("ACQUISTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(trattaTest)
                        .classeServizio(ClasseServizio.BASE)
                        .tipoPrezzo(TipoPrezzo.INTERO)
                        .build();

                richiesteInviate.incrementAndGet();
                RispostaDTO risposta = client.inviaRichiesta(acquisto);

                if (risposta.getEsito().equals("OK")) {
                    acquistiRiusciti.incrementAndGet();
                    System.out.println("   ✅ Client " + clientId + " acquisto " + (tentativo + 1) + " riuscito");
                } else {
                    richiesteRifiutate.incrementAndGet();
                    if (tentativo == 0) { // Log solo il primo rifiuto per non intasare
                        System.out.println("   ❌ Client " + clientId + " primo rifiuto: " + risposta.getMessaggio());
                    }
                }

                // Pausa casuale tra tentativi
                Thread.sleep(50 + (int) (Math.random() * 100));

            } catch (Exception e) {
                erroriConnessione.incrementAndGet();
            }
        }
    }

    /**
     * 🔀 Test 3: Mix di acquisti e prenotazioni
     */
    private static void testMistoAcquistiPrenotazioni() throws Exception {
        System.out.println("\n🔀 TEST 3: Mix Acquisti + Prenotazioni");

        // Reset
        prenotazioniRiuscite.set(0);
        acquistiRiusciti.set(0);

        // Crea nuova tratta
        creaTrattaTestCapienza();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(20);

        for (int i = 0; i < 20; i++) {
            final int clientId = i;
            final boolean faAcquisto = (i % 2 == 0); // Alterna acquisti e prenotazioni

            executor.submit(() -> {
                try {
                    ClientService client = new ClientService("localhost", SERVER_PORT);
                    client.attivaCliente("MixUser" + clientId, "Test",
                            "mix" + clientId + "@test.com", 25, "Test", "333" + clientId);

                    RichiestaDTO.Builder builder = new RichiestaDTO.Builder()
                            .idCliente(client.getCliente().getId().toString())
                            .tratta(trattaTest)
                            .classeServizio(ClasseServizio.BASE);

                    if (faAcquisto) {
                        builder.tipo("ACQUISTA").tipoPrezzo(TipoPrezzo.INTERO);
                    } else {
                        builder.tipo("PRENOTA");
                    }

                    RispostaDTO risposta = client.inviaRichiesta(builder.build());

                    if (risposta.getEsito().equals("OK")) {
                        if (faAcquisto) {
                            acquistiRiusciti.incrementAndGet();
                        } else {
                            prenotazioniRiuscite.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    erroriConnessione.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("   💳 Acquisti riusciti: " + acquistiRiusciti.get());
        System.out.println("   📝 Prenotazioni riuscite: " + prenotazioniRiuscite.get());
        System.out.println("   📊 Totale biglietti: " + (acquistiRiusciti.get() + prenotazioniRiuscite.get()));
    }

    /**
     * 🔍 Verifica integrità finale del sistema
     */
    private static void verificaIntegritaFinale() {
        System.out.println("\n🔍 VERIFICA INTEGRITÀ FINALE");

        List<model.Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

        // Raggruppa per tratta
        Map<UUID, List<model.Biglietto>> bigliettiPerTratta = tuttiBiglietti.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getIdTratta()));

        System.out.println("📊 ANALISI BIGLIETTI PER TRATTA:");

        bigliettiPerTratta.forEach((idTratta, biglietti) -> {
            Tratta tratta = memoriaTratte.getTrattaById(idTratta);
            if (tratta != null) {
                int capienza = tratta.getTreno().getCapienzaTotale();
                int venduti = biglietti.size();

                System.out.println("   🚂 Tratta: " + tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo());
                System.out.println("      Capienza: " + capienza + " | Venduti: " + venduti +
                        " | " + (venduti <= capienza ? "✅ OK" : "❌ OVERSELLING"));

                if (venduti > capienza) {
                    System.out.println("      🚨 ATTENZIONE: Venduti " + (venduti - capienza) + " biglietti in eccesso!");
                }
            }
        });
    }

    /**
     * 📋 Report dettagliato finale
     */
    private static void stampaReportDettagliato() {
        System.out.println("\n📋 ===== REPORT DETTAGLIATO TEST CAPIENZA =====");

        int bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();
        boolean overselling = bigliettiTotali > CAPIENZA_TRENO_TEST;

        System.out.println("🎯 OBIETTIVO TEST:");
        System.out.println("   Verificare che il sistema non venda mai più biglietti della capienza disponibile");

        System.out.println("\n📊 STATISTICHE GLOBALI:");
        System.out.println("   🚂 Capienza treno test: " + CAPIENZA_TRENO_TEST);
        System.out.println("   🎫 Biglietti venduti totali: " + bigliettiTotali);
        System.out.println("   📤 Richieste inviate: " + richiesteInviate.get());
        System.out.println("   ✅ Operazioni riuscite: " + (acquistiRiusciti.get() + prenotazioniRiuscite.get()));
        System.out.println("   ❌ Operazioni rifiutate: " + richiesteRifiutate.get());

        System.out.println("\n🧪 RISULTATI PER CATEGORIA:");

        // Test capienza base
        System.out.println("   📝 Test Capienza Base: " +
                (!overselling ? "✅ PASS" : "❌ FAIL"));

        // Test concorrenza
        double successRate = richiesteInviate.get() > 0 ?
                (acquistiRiusciti.get() * 100.0 / richiesteInviate.get()) : 0;
        System.out.println("   🏎️ Test Concorrenza: " +
                (successRate > 10 && !overselling ? "✅ PASS" : "❌ FAIL") +
                " (success rate: " + String.format("%.1f%%", successRate) + ")");

        // Test misto
        System.out.println("   🔀 Test Misto: " +
                (!overselling ? "✅ PASS" : "❌ FAIL"));

        System.out.println("\n🎯 ANALISI COMPORTAMENTO SISTEMA:");
        if (overselling) {
            System.out.println("   🚨 CRITICO: Sistema ha venduto più biglietti della capienza!");
            System.out.println("   💡 Possibili cause:");
            System.out.println("      - Race condition nel controllo capienza");
            System.out.println("      - Problemi di sincronizzazione nella MemoriaBiglietti");
            System.out.println("      - Event system che non preserva l'atomicità");
        } else {
            System.out.println("   ✅ ECCELLENTE: Controllo capienza funziona perfettamente");
            System.out.println("   🛡️ Sistema resiste a condizioni di stress elevato");
            System.out.println("   ⚡ Gestione concorrenza stabile e affidabile");
        }

        double rejectionRate = richiesteInviate.get() > 0 ?
                (richiesteRifiutate.get() * 100.0 / richiesteInviate.get()) : 0;

        System.out.println("\n💡 METRICHE PERFORMANCE:");
        System.out.println("   📈 Tasso di rifiuto: " + String.format("%.1f%%", rejectionRate));
        System.out.println("   🔌 Errori connessione: " + erroriConnessione.get());
        System.out.println("   🎯 Efficienza controllo capienza: " +
                (rejectionRate > 50 && !overselling ? "ALTA" : "MEDIA"));

        // Verdetto finale
        System.out.println("\n🏆 VERDETTO FINALE:");
        if (!overselling && successRate > 5) {
            System.out.println("   🎉 SISTEMA CAPIENZA: ECCELLENTE!");
            System.out.println("   ✨ Gestione concorrenza robusta e affidabile");
            System.out.println("   🚀 Pronto per carichi di produzione elevati");
        } else if (!overselling) {
            System.out.println("   👍 SISTEMA CAPIENZA: BUONO!");
            System.out.println("   ✅ Controlli funzionano ma potrebbero essere ottimizzati");
        } else {
            System.out.println("   ⚠️ SISTEMA CAPIENZA: RICHIEDE CORREZIONI!");
            System.out.println("   🔧 Problemi critici di overselling da risolvere");
        }

        System.out.println("\n✅ ===== TEST CAPIENZA COMPLETATO =====");
    }

    /**
     * 🧹 Cleanup finale
     */
    private static void cleanup() {
        System.out.println("\n🧹 Cleanup test...");

        if (trenicalService != null) {
            trenicalService.shutdown();
        }

        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
            }
        }

        if (bancaServer != null) {
            bancaServer.shutdown();
            try {
                if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    bancaServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                bancaServer.shutdownNow();
            }
        }

        System.out.println("✅ Cleanup completato");
    }
}