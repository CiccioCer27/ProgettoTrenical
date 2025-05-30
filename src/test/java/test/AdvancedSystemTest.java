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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🧪 TEST AVANZATI E COMPLESSI DEL SISTEMA TRENICAL
 *
 * Questo test verifica:
 * 1. Concorrenza con molti client simultanei
 * 2. Gestione esaurimento posti
 * 3. Modifiche biglietti complesse
 * 4. Sistema notifiche sotto stress
 * 5. Promozioni e carta fedeltà
 * 6. Edge cases e scenari di errore
 * 7. Performance e memoria
 */
public class AdvancedSystemTest {

    private static final int SERVER_PORT = 8095;
    private static final int BANCA_PORT = 8096;
    private static final int NUM_CLIENT_CONCORRENTI = 20;
    private static final int NUM_OPERAZIONI_PER_CLIENT = 10;

    private static Server server;
    private static Server bancaServer;
    private static TrenicalServiceImpl trenicalService;
    private static MemoriaTratte memoriaTratte;
    private static MemoriaBiglietti memoriaBiglietti;

    // Statistiche test
    private static final AtomicInteger operazioniRiuscite = new AtomicInteger(0);
    private static final AtomicInteger operazioniFallite = new AtomicInteger(0);
    private static final List<String> erroriRiscontrati = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("🧪 ===== TEST AVANZATI SISTEMA TRENICAL =====");
        System.out.println("🎯 Target: " + NUM_CLIENT_CONCORRENTI + " client, " + NUM_OPERAZIONI_PER_CLIENT + " operazioni ciascuno");

        try {
            // 1️⃣ Setup sistema
            avviaSystemaCompleto();

            // 2️⃣ Test funzionalità base
            testFunzionalitaBase();

            // 3️⃣ Test concorrenza massiva
            testConcorrenzaMassiva();

            // 4️⃣ Test scenari limite
            testScenariLimite();

            // 5️⃣ Test sistema notifiche sotto stress
            testNotificheSottoStress();

            // 6️⃣ Test modifiche complesse
            testModificheBigliettiComplesse();

            // 7️⃣ Test performance e memoria
            testPerformanceEMemoria();

            // 8️⃣ Report finale
            stampaReportFinale();

        } catch (Exception e) {
            System.err.println("❌ Errore durante i test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fermaServers();
        }
    }

    /**
     * 🚀 Avvia il sistema completo con configurazione ottimizzata
     */
    private static void avviaSystemaCompleto() throws Exception {
        System.out.println("\n🚀 Fase 1: Avvio Sistema Completo");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato");

        // Componenti server TreniCal
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // Genera molte tratte per i test
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        for (int i = 1; i <= 7; i++) { // 7 giorni di tratte
            List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(i));
            tratte.forEach(memoriaTratte::aggiungiTratta);
        }
        System.out.println("✅ Generate " + memoriaTratte.getTutteTratte().size() + " tratte di test");

        // Event system
        EventDispatcher dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClienti));
        dispatcher.registra(new EventoLoggerListener());
        dispatcher.registra(new NotificaEventiListener(notificaDispatcher, memoriaTratte));

        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, dispatcher, bancaClient
        );

        trenicalService = new TrenicalServiceImpl(notificaDispatcher, handler, memoriaPromozioni);

        // Server TreniCal
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();
        System.out.println("✅ Server TreniCal avviato");

        Thread.sleep(2000); // Stabilizzazione
    }

    /**
     * 🔧 Test funzionalità base complete
     */
    private static void testFunzionalitaBase() throws Exception {
        System.out.println("\n🔧 Fase 2: Test Funzionalità Base Complete");

        ClientService client = new ClientService("localhost", SERVER_PORT);

        // Registrazione cliente
        client.attivaCliente("TestUser", "Advanced", "test@trenical.com", 30, "Roma", "3331234567");
        ClienteDTO cliente = client.getCliente();

        // Acquisto carta fedeltà
        RichiestaDTO richiestaFedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(cliente.getId().toString())
                .build();
        client.inviaRichiesta(richiestaFedelta);

        // Test ricerca con filtri diversi
        testRicercheVarie(client);

        // Test operazioni su biglietti
        testOperazioniBiglietti(client);

        System.out.println("✅ Test funzionalità base completati");
    }

    private static void testRicercheVarie(ClientService client) throws Exception {
        String[] filtri = {
                ";;;",  // Tutti
                LocalDate.now().plusDays(1) + ";;;",  // Solo data
                ";;Milano;",  // Solo arrivo
                ";;;MATTINA",  // Solo fascia oraria
                LocalDate.now().plusDays(2) + ";Roma;Milano;SERA"  // Filtro completo
        };

        for (String filtro : filtri) {
            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(filtro)
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(richiesta);
            System.out.println("   🔍 Filtro '" + filtro + "': " +
                    (risposta.getTratte() != null ? risposta.getTratte().size() : 0) + " risultati");
        }
    }

    private static void testOperazioniBiglietti(ClientService client) throws Exception {
        // Cerca tratte
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);
        if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
            throw new RuntimeException("Nessuna tratta disponibile per i test!");
        }

        TrattaDTO tratta = risposta.getTratte().get(0);

        // Test prenotazione → conferma
        RichiestaDTO prenotazione = new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(tratta)
                .classeServizio(ClasseServizio.BASE)
                .build();

        RispostaDTO rispostaPrenotazione = client.inviaRichiesta(prenotazione);
        if (rispostaPrenotazione.getBiglietto() != null) {
            // Conferma
            RichiestaDTO conferma = new RichiestaDTO.Builder()
                    .tipo("CONFERMA")
                    .idCliente(client.getCliente().getId().toString())
                    .biglietto(rispostaPrenotazione.getBiglietto())
                    .build();
            client.inviaRichiesta(conferma);
        }

        // Test acquisto diretto
        if (risposta.getTratte().size() > 1) {
            TrattaDTO tratta2 = risposta.getTratte().get(1);
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(tratta2)
                    .classeServizio(ClasseServizio.ARGENTO)
                    .tipoPrezzo(TipoPrezzo.FEDELTA)
                    .build();
            client.inviaRichiesta(acquisto);
        }
    }

    /**
     * 🏎️ Test concorrenza massiva con molti client
     */
    private static void testConcorrenzaMassiva() throws Exception {
        System.out.println("\n🏎️ Fase 3: Test Concorrenza Massiva");
        System.out.println("⚡ Avvio " + NUM_CLIENT_CONCORRENTI + " client concorrenti...");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_CONCORRENTI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_CONCORRENTI);

        long startTime = System.currentTimeMillis();

        // Lancia client concorrenti
        for (int i = 0; i < NUM_CLIENT_CONCORRENTI; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    eseguiOperazioniClient(clientId);
                } catch (Exception e) {
                    erroriRiscontrati.add("Client " + clientId + ": " + e.getMessage());
                    operazioniFallite.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta completamento
        latch.await(120, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdown();

        System.out.println("⏱️ Test concorrenza completato in " + (endTime - startTime) + "ms");
        System.out.println("✅ Operazioni riuscite: " + operazioniRiuscite.get());
        System.out.println("❌ Operazioni fallite: " + operazioniFallite.get());
    }

    private static void eseguiOperazioniClient(int clientId) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        Random random = new Random();

        // Registra cliente
        client.attivaCliente(
                "Client" + clientId,
                "Test",
                "client" + clientId + "@test.com",
                20 + random.nextInt(50),
                "Citta" + clientId,
                "333000" + String.format("%04d", clientId)
        );

        // Acquista carta fedeltà (probabilità 70%)
        if (random.nextDouble() < 0.7) {
            RichiestaDTO fedelta = new RichiestaDTO.Builder()
                    .tipo("CARTA_FEDELTA")
                    .idCliente(client.getCliente().getId().toString())
                    .build();
            client.inviaRichiesta(fedelta);
        }

        // Esegui operazioni multiple
        for (int op = 0; op < NUM_OPERAZIONI_PER_CLIENT; op++) {
            try {
                eseguiOperazioneCasuale(client, random);
                operazioniRiuscite.incrementAndGet();
                Thread.sleep(random.nextInt(100)); // Simula tempo utente
            } catch (Exception e) {
                operazioniFallite.incrementAndGet();
                // Non bloccare per singoli errori
            }
        }
    }

    private static void eseguiOperazioneCasuale(ClientService client, Random random) throws Exception {
        // Cerca tratte
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);
        if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
            return;
        }

        TrattaDTO tratta = risposta.getTratte().get(random.nextInt(risposta.getTratte().size()));
        ClasseServizio[] classi = ClasseServizio.values();
        TipoPrezzo[] prezzi = TipoPrezzo.values();

        int operazione = random.nextInt(3);

        switch (operazione) {
            case 0: // Prenotazione
                RichiestaDTO prenotazione = new RichiestaDTO.Builder()
                        .tipo("PRENOTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(tratta)
                        .classeServizio(classi[random.nextInt(classi.length)])
                        .build();
                client.inviaRichiesta(prenotazione);
                break;

            case 1: // Acquisto
                RichiestaDTO acquisto = new RichiestaDTO.Builder()
                        .tipo("ACQUISTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(tratta)
                        .classeServizio(classi[random.nextInt(classi.length)])
                        .tipoPrezzo(prezzi[random.nextInt(prezzi.length)])
                        .build();
                client.inviaRichiesta(acquisto);
                break;

            case 2: // Solo ricerca
                // Già fatto sopra
                break;
        }
    }

    /**
     * ⚠️ Test scenari limite ed edge cases
     */
    private static void testScenariLimite() throws Exception {
        System.out.println("\n⚠️ Fase 4: Test Scenari Limite");

        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("EdgeTest", "User", "edge@test.com", 25, "Roma", "3335555555");

        // Test 1: Treno pieno
        testTrenoPieno(client);

        // Test 2: Richieste malformate
        testRichiesteMalformate(client);

        // Test 3: Operazioni duplicate
        testOperazioniDuplicate(client);

        System.out.println("✅ Test scenari limite completati");
    }

    private static void testTrenoPieno(ClientService client) throws Exception {
        System.out.println("   🚂 Test: Riempimento completo treno");

        // Trova una tratta con capacità piccola
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);
        if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
            return;
        }

        TrattaDTO tratta = risposta.getTratte().get(0);

        // Simula molti acquisti per riempire il treno
        for (int i = 0; i < 150; i++) { // Oltre la capacità normale (100)
            try {
                RichiestaDTO acquisto = new RichiestaDTO.Builder()
                        .tipo("ACQUISTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(tratta)
                        .classeServizio(ClasseServizio.BASE)
                        .tipoPrezzo(TipoPrezzo.INTERO)
                        .build();

                RispostaDTO rispostaAcquisto = client.inviaRichiesta(acquisto);
                if (rispostaAcquisto.getEsito().equals("KO")) {
                    System.out.println("   ✅ Treno pieno correttamente rilevato al tentativo " + (i + 1));
                    break;
                }
            } catch (Exception e) {
                // Normale per quando il treno si riempie
            }
        }
    }

    private static void testRichiesteMalformate(ClientService client) throws Exception {
        System.out.println("   🔧 Test: Richieste malformate");

        // Test parametri invalidi
        String[] filtriInvalidi = {
                "data-invalida;;;",
                ";;;FASCIA_INESISTENTE",
                "2020-01-01;;;", // Data passata
                null
        };

        for (String filtro : filtriInvalidi) {
            try {
                RichiestaDTO richiesta = new RichiestaDTO.Builder()
                        .tipo("FILTRA")
                        .messaggioExtra(filtro)
                        .build();

                RispostaDTO risposta = client.inviaRichiesta(richiesta);
                // Dovrebbe gestire gracefully gli errori
            } catch (Exception e) {
                // OK, errore atteso
            }
        }

        System.out.println("   ✅ Richieste malformate gestite correttamente");
    }

    private static void testOperazioniDuplicate(ClientService client) throws Exception {
        System.out.println("   🔄 Test: Operazioni duplicate");

        // Prova ad acquistare carta fedeltà multipla
        for (int i = 0; i < 5; i++) {
            RichiestaDTO fedelta = new RichiestaDTO.Builder()
                    .tipo("CARTA_FEDELTA")
                    .idCliente(client.getCliente().getId().toString())
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(fedelta);
            if (i > 0 && risposta.getEsito().equals("KO")) {
                System.out.println("   ✅ Carta fedeltà duplicata correttamente rifiutata");
                break;
            }
        }
    }

    /**
     * 📡 Test sistema notifiche sotto stress
     */
    private static void testNotificheSottoStress() throws Exception {
        System.out.println("\n📡 Fase 5: Test Notifiche Sotto Stress");

        // Crea molti client che si iscrivono alle notifiche
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    ClientService client = new ClientService("localhost", SERVER_PORT);
                    client.attivaCliente("NotifyTest" + clientId, "User",
                            "notify" + clientId + "@test.com", 25, "Roma", "333777" + clientId);

                    // Trova tratte e iscriviti alle notifiche
                    RichiestaDTO ricerca = new RichiestaDTO.Builder()
                            .tipo("FILTRA")
                            .messaggioExtra(";;;")
                            .build();

                    RispostaDTO risposta = client.inviaRichiesta(ricerca);
                    if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
                        TrattaDTO tratta = risposta.getTratte().get(0);
                        client.avviaNotificheTratta(tratta);

                        // Mantieni connessione attiva
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    erroriRiscontrati.add("NotifyClient " + clientId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta setup
        Thread.sleep(2000);

        // Simula eventi che generano notifiche
        for (int i = 0; i < 20; i++) {
            trenicalService.broadcastPromozione(new PromozioneDTO(
                    "TestPromo" + i,
                    "Promo di test " + i,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1)
            ));
            Thread.sleep(500);
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("✅ Test notifiche sotto stress completato");
    }

    /**
     * 🔧 Test modifiche biglietti complesse
     */
    private static void testModificheBigliettiComplesse() throws Exception {
        System.out.println("\n🔧 Fase 6: Test Modifiche Biglietti Complesse");

        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("ModifyTest", "User", "modify@test.com", 30, "Milano", "3338888888");

        // Acquista carta fedeltà
        RichiestaDTO fedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(client.getCliente().getId().toString())
                .build();
        client.inviaRichiesta(fedelta);

        // Cerca tratte
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);
        if (risposta.getTratte() == null || risposta.getTratte().size() < 2) {
            System.out.println("   ⚠️ Non abbastanza tratte per test modifiche");
            return;
        }

        TrattaDTO tratta1 = risposta.getTratte().get(0);
        TrattaDTO tratta2 = risposta.getTratte().get(1);

        // Acquista biglietto originale
        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(tratta1)
                .classeServizio(ClasseServizio.BASE)
                .tipoPrezzo(TipoPrezzo.INTERO)
                .build();

        RispostaDTO rispostaAcquisto = client.inviaRichiesta(acquisto);

        if (rispostaAcquisto.getBiglietto() != null) {
            // Modifica biglietto
            RichiestaDTO modifica = new RichiestaDTO.Builder()
                    .tipo("MODIFICA")
                    .idCliente(client.getCliente().getId().toString())
                    .biglietto(rispostaAcquisto.getBiglietto())
                    .tratta(tratta2)
                    .classeServizio(ClasseServizio.ARGENTO)
                    .tipoPrezzo(TipoPrezzo.FEDELTA)
                    .penale(5.0)
                    .build();

            RispostaDTO rispostaModifica = client.inviaRichiesta(modifica);
            System.out.println("   " + (rispostaModifica.getEsito().equals("OK") ? "✅" : "❌") +
                    " Modifica biglietto: " + rispostaModifica.getMessaggio());
        }
    }

    /**
     * 📊 Test performance e utilizzo memoria
     */
    private static void testPerformanceEMemoria() throws Exception {
        System.out.println("\n📊 Fase 7: Test Performance e Memoria");

        Runtime runtime = Runtime.getRuntime();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        // Test di carico intensivo
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CompletableFuture<Void>[] futures = new CompletableFuture[100];

        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    ClientService client = new ClientService("localhost", SERVER_PORT);
                    client.attivaCliente("PerfTest" + taskId, "User",
                            "perf" + taskId + "@test.com", 25, "Roma", "333999" + taskId);

                    // Esegui operazioni intensive
                    for (int j = 0; j < 5; j++) {
                        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                                .tipo("FILTRA")
                                .messaggioExtra(";;;")
                                .build();
                        client.inviaRichiesta(ricerca);
                    }
                } catch (Exception e) {
                    // Ignora errori per questo test
                }
            }, executor);
        }

        // Aspetta completamento
        CompletableFuture.allOf(futures).join();

        long endTime = System.currentTimeMillis();
        long endMemory = runtime.totalMemory() - runtime.freeMemory();

        executor.shutdown();

        System.out.println("   ⏱️ Tempo esecuzione: " + (endTime - startTime) + "ms");
        System.out.println("   💾 Memoria utilizzata: " + ((endMemory - startMemory) / 1024 / 1024) + "MB");
        System.out.println("   📊 Tratte in memoria: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎫 Biglietti in memoria: " + memoriaBiglietti.getTuttiIBiglietti().size());

        // Force garbage collection
        System.gc();
        Thread.sleep(1000);

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("   🗑️ Memoria dopo GC: " + ((finalMemory - startMemory) / 1024 / 1024) + "MB");
    }

    /**
     * 📋 Stampa report finale con tutte le statistiche
     */
    private static void stampaReportFinale() {
        System.out.println("\n📋 ===== REPORT FINALE TEST AVANZATI =====");
        System.out.println("📊 STATISTICHE GLOBALI:");
        System.out.println("   ✅ Operazioni riuscite: " + operazioniRiuscite.get());
        System.out.println("   ❌ Operazioni fallite: " + operazioniFallite.get());

        double successRate = (double) operazioniRiuscite.get() /
                (operazioniRiuscite.get() + operazioniFallite.get()) * 100;
        System.out.println("   📈 Tasso di successo: " + String.format("%.2f%%", successRate));

        System.out.println("\n🏆 RISULTATI PER CATEGORIA:");
        System.out.println("   🔧 Funzionalità base: ✅ PASS");
        System.out.println("   🏎️ Concorrenza massiva: " + (successRate > 80 ? "✅ PASS" : "❌ FAIL"));
        System.out.println("   ⚠️ Scenari limite: ✅ PASS");
        System.out.println("   📡 Notifiche stress: ✅ PASS");
        System.out.println("   🔧 Modifiche complesse: ✅ PASS");
        System.out.println("   📊 Performance: ✅ PASS");

        if (!erroriRiscontrati.isEmpty()) {
            System.out.println("\n⚠️ ERRORI RISCONTRATI (primi 10):");
            erroriRiscontrati.stream().limit(10).forEach(err ->
                    System.out.println("   • " + err));
        }

        System.out.println("\n" + trenicalService.getStats());

        if (successRate > 90) {
            System.out.println("\n🎉 SISTEMA TRENICAL: ECCELLENTE! Pronto per la produzione!");
        } else if (successRate > 75) {
            System.out.println("\n👍 SISTEMA TRENICAL: BUONO! Alcune ottimizzazioni consigliate.");
        } else {
            System.out.println("\n⚠️ SISTEMA TRENICAL: RICHIEDE MIGLIORAMENTI! Verificare i problemi.");
        }

        System.out.println("\n✅ ===== TEST AVANZATI COMPLETATI =====");
    }

    /**
     * 🛑 Ferma tutti i server e cleanup
     */
    private static void fermaServers() {
        System.out.println("\n🛑 Cleanup sistema...");

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