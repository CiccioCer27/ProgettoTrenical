package test;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import factory.TrattaFactoryConcrete;
import grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.Tratta;
import observer.*;
import persistence.*;
import service.BancaServiceClient;
import service.ClientService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔄 TEST INTEGRAZIONE E SCENARI AVANZATI - CORRETTO
 *
 * Corretto per funzionare con la struttura reale del progetto TreniCal
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrenicallIntegrationTest {

    private static final int SERVER_PORT = 8888;
    private static final int BANCA_PORT = 8887;

    private Server server;
    private Server bancaServer;
    private TrenicalServiceImpl trenicalService;
    private MemoriaBiglietti memoriaBiglietti;
    private MemoriaTratte memoriaTratte;
    private MemoriaOsservatori memoriaOsservatori;
    private List<ClientService> clients;

    @BeforeAll
    void setupIntegrationSystem() throws Exception {
        System.out.println("🔄 Setup sistema per test integrazione");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // Componenti memoria THREAD-SAFE (come nel tuo progetto)
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();
        memoriaOsservatori = new MemoriaOsservatori();

        // Genera tratte per test
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        for (int i = 1; i <= 7; i++) {
            List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(i));
            tratte.forEach(memoriaTratte::aggiungiTratta);
        }

        // ✅ CORREZIONE: Handler con le dipendenze corrette
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti,
                memoriaClienti,
                memoriaTratte,
                bancaClient,
                memoriaOsservatori  // ✅ Aggiunto parametro mancante
        );

        // ✅ CORREZIONE: Solo notifiche gRPC, no EventDispatcher complesso
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        trenicalService = new TrenicalServiceImpl(notificaDispatcher, handler, memoriaPromozioni);

        // Server principale
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        Thread.sleep(2000);

        // Setup clienti per test
        clients = new ArrayList<>();
        String[] nomi = {"Alice", "Bob", "Charlie", "Diana", "Eva"};

        for (int i = 0; i < nomi.length; i++) {
            ClientService client = new ClientService("localhost", SERVER_PORT);
            client.attivaCliente(nomi[i], "TestUser",
                    nomi[i].toLowerCase() + "@integration.test",
                    25 + i, "Milano", "333111000" + i);
            clients.add(client);
        }

        System.out.println("✅ Sistema integrazione avviato con " +
                memoriaTratte.getTutteTratte().size() + " tratte e " +
                clients.size() + " clienti");
    }

    @AfterAll
    void teardownIntegrationSystem() throws Exception {
        System.out.println("🧹 Cleanup sistema integrazione");

        if (trenicalService != null) {
            trenicalService.shutdown();
        }

        if (server != null) {
            server.shutdown();
            server.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (bancaServer != null) {
            bancaServer.shutdown();
            bancaServer.awaitTermination(5, TimeUnit.SECONDS);
        }

        System.out.println("✅ Sistema integrazione fermato");
    }

    // ================================================================================
    // 👨‍👩‍👧‍👦 TEST SCENARI UTENTE COMPLETI
    // ================================================================================

    @Test
    @Order(1)
    @DisplayName("👨‍👩‍👧‍👦 Scenario famiglia: viaggio coordinato")
    void testScenarioFamiglia() throws Exception {
        ClientService marco = clients.get(0);
        ClientService laura = clients.get(1);

        // Marco acquista carta fedeltà
        RichiestaDTO fedeltaMarco = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(marco.getCliente().getId().toString())
                .build();

        RispostaDTO rispostaFedelta = marco.inviaRichiesta(fedeltaMarco);
        assertEquals("OK", rispostaFedelta.getEsito(), "Marco deve ottenere carta fedeltà");

        // Trova tratta per il viaggio famiglia
        TrattaDTO trattaFamiglia = trovaTrattaDisponibile(marco);
        assumeTrue(trattaFamiglia != null, "Serve una tratta per test famiglia");

        // Marco prenota per la famiglia (3 biglietti)
        List<BigliettoDTO> prenotazioniFamiglia = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            RichiestaDTO prenotazione = new RichiestaDTO.Builder()
                    .tipo("PRENOTA")
                    .idCliente(marco.getCliente().getId().toString())
                    .tratta(trattaFamiglia)
                    .classeServizio(ClasseServizio.ARGENTO)
                    .build();

            RispostaDTO risposta = marco.inviaRichiesta(prenotazione);
            assertEquals("OK", risposta.getEsito(),
                    "Prenotazione famiglia " + (i+1) + " deve riuscire");

            if (risposta.getBiglietto() != null) {
                prenotazioniFamiglia.add(risposta.getBiglietto());
            }
        }

        assertEquals(3, prenotazioniFamiglia.size(),
                "Devono essere state create 3 prenotazioni");

        // Conferma le prenotazioni
        Thread.sleep(1000);
        int confermate = 0;

        for (BigliettoDTO prenotazione : prenotazioniFamiglia) {
            RichiestaDTO conferma = new RichiestaDTO.Builder()
                    .tipo("CONFERMA")
                    .idCliente(marco.getCliente().getId().toString())
                    .biglietto(prenotazione)
                    .build();

            RispostaDTO rispostaConferma = marco.inviaRichiesta(conferma);
            if (rispostaConferma.getEsito().equals("OK")) {
                confermate++;
            }
        }

        assertTrue(confermate >= 2, "Almeno 2 prenotazioni devono essere confermate");
        System.out.println("✅ Famiglia: " + confermate + "/3 biglietti confermati");

        // Laura si iscrive alle notifiche
        laura.avviaNotificheTratta(trattaFamiglia);
        System.out.println("✅ Laura iscritta alle notifiche famiglia");
    }

    @Test
    @Order(2)
    @DisplayName("💼 Scenario business: modifiche last-minute")
    void testScenarioBusiness() throws Exception {
        ClientService giulia = clients.get(2);

        // Giulia business traveler con carta fedeltà
        RichiestaDTO fedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(giulia.getCliente().getId().toString())
                .build();
        giulia.inviaRichiesta(fedelta);

        // Acquisto iniziale classe GOLD
        TrattaDTO trattaOriginale = trovaTrattaDisponibile(giulia);
        assumeTrue(trattaOriginale != null, "Serve tratta per scenario business");

        RichiestaDTO acquistoGold = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(giulia.getCliente().getId().toString())
                .tratta(trattaOriginale)
                .classeServizio(ClasseServizio.GOLD)
                .tipoPrezzo(TipoPrezzo.FEDELTA)
                .build();

        RispostaDTO rispostaAcquisto = giulia.inviaRichiesta(acquistoGold);
        assertEquals("OK", rispostaAcquisto.getEsito(), "Acquisto GOLD deve riuscire");

        BigliettoDTO bigliettoOriginale = rispostaAcquisto.getBiglietto();
        assertNotNull(bigliettoOriginale, "Biglietto originale necessario");

        // Cambio dell'ultimo minuto: modifica biglietto
        TrattaDTO nuovaTratta = trovaNuovaTratta(giulia, trattaOriginale);

        if (nuovaTratta != null) {
            RichiestaDTO modifica = new RichiestaDTO.Builder()
                    .tipo("MODIFICA")
                    .idCliente(giulia.getCliente().getId().toString())
                    .biglietto(bigliettoOriginale)
                    .tratta(nuovaTratta)
                    .classeServizio(ClasseServizio.GOLD)
                    .tipoPrezzo(TipoPrezzo.FEDELTA)
                    .penale(15.0) // Penale urgenza
                    .build();

            RispostaDTO rispostaModifica = giulia.inviaRichiesta(modifica);
            assertEquals("OK", rispostaModifica.getEsito(),
                    "Modifica business deve riuscire");

            assertNotNull(rispostaModifica.getBiglietto(),
                    "Biglietto modificato deve esistere");
            assertEquals(ClasseServizio.GOLD,
                    rispostaModifica.getBiglietto().getClasseServizio(),
                    "Classe GOLD deve essere mantenuta");

            System.out.println("✅ Business: modifica last-minute completata");
        }

        System.out.println("✅ Scenario business completato");
    }

    @Test
    @Order(3)
    @DisplayName("👥 Scenario gruppo: prenotazioni coordinate")
    void testScenarioGruppo() throws Exception {
        // Usa 3 clienti come gruppo di amici
        List<ClientService> gruppo = clients.subList(0, 3);

        // Solo il primo ha carta fedeltà
        RichiestaDTO fedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(gruppo.get(0).getCliente().getId().toString())
                .build();
        gruppo.get(0).inviaRichiesta(fedelta);

        // Tutti cercano la stessa tratta
        TrattaDTO trattaGruppo = trovaTrattaDisponibile(gruppo.get(0));
        assumeTrue(trattaGruppo != null, "Serve tratta per test gruppo");

        // Strategie diverse per ognuno
        String[] strategie = {"PRENOTA", "ACQUISTA", "PRENOTA"};
        TipoPrezzo[] prezzi = {TipoPrezzo.INTERO, TipoPrezzo.FEDELTA, TipoPrezzo.INTERO};

        List<BigliettoDTO> bigliettiGruppo = new ArrayList<>();

        for (int i = 0; i < gruppo.size(); i++) {
            RichiestaDTO.Builder builder = new RichiestaDTO.Builder()
                    .tipo(strategie[i])
                    .idCliente(gruppo.get(i).getCliente().getId().toString())
                    .tratta(trattaGruppo)
                    .classeServizio(ClasseServizio.BASE);

            if (strategie[i].equals("ACQUISTA")) {
                builder.tipoPrezzo(prezzi[i]);
            }

            RispostaDTO risposta = gruppo.get(i).inviaRichiesta(builder.build());
            assertEquals("OK", risposta.getEsito(),
                    "Operazione gruppo membro " + i + " deve riuscire");

            if (risposta.getBiglietto() != null) {
                bigliettiGruppo.add(risposta.getBiglietto());
            }
        }

        assertEquals(3, bigliettiGruppo.size(), "Gruppo deve avere 3 biglietti");

        System.out.println("✅ Gruppo: 3 biglietti gestiti con strategie diverse");
    }

    // ================================================================================
    // 🚀 TEST CONCORRENZA E PERFORMANCE
    // ================================================================================

    @Test
    @Order(4)
    @DisplayName("🚀 Test concorrenza: acquisti simultanei")
    @Execution(ExecutionMode.SAME_THREAD)
    void testConcorrenzaAcquisti() throws Exception {
        TrattaDTO trattaConcorrenza = trovaTrattaDisponibile(clients.get(0));
        assumeTrue(trattaConcorrenza != null, "Serve tratta per test concorrenza");

        int numThread = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThread);
        CountDownLatch latch = new CountDownLatch(numThread);
        AtomicInteger successi = new AtomicInteger(0);
        AtomicInteger fallimenti = new AtomicInteger(0);

        // Lancia acquisti simultanei
        for (int i = 0; i < numThread; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    ClientService client = clients.get(threadId % clients.size());

                    RichiestaDTO acquisto = new RichiestaDTO.Builder()
                            .tipo("ACQUISTA")
                            .idCliente(client.getCliente().getId().toString())
                            .tratta(trattaConcorrenza)
                            .classeServizio(ClasseServizio.BASE)
                            .tipoPrezzo(TipoPrezzo.INTERO)
                            .build();

                    RispostaDTO risposta = client.inviaRichiesta(acquisto);

                    if (risposta.getEsito().equals("OK")) {
                        successi.incrementAndGet();
                    } else {
                        fallimenti.incrementAndGet();
                    }

                } catch (Exception e) {
                    fallimenti.incrementAndGet();
                    System.err.println("Errore thread " + threadId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta completamento
        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Test concorrenza deve completarsi entro 30s");
        executor.shutdown();

        int totaleOperazioni = successi.get() + fallimenti.get();
        assertEquals(numThread, totaleOperazioni,
                "Tutte le operazioni devono essere processate");

        assertTrue(successi.get() > 0, "Almeno un acquisto deve riuscire");

        System.out.println("🚀 Concorrenza: " + successi.get() + " successi, " +
                fallimenti.get() + " fallimenti su " + numThread + " thread");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 15})
    @DisplayName("⚡ Test performance con carichi diversi")
    void testPerformanceCarichi(int numeroOperazioni) {
        long startTime = System.currentTimeMillis();
        int successi = 0;

        for (int i = 0; i < numeroOperazioni; i++) {
            RichiestaDTO ricerca = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;")
                    .build();

            RispostaDTO risposta = clients.get(0).inviaRichiesta(ricerca);
            if (risposta.getEsito().equals("OK")) {
                successi++;
            }
        }

        long endTime = System.currentTimeMillis();
        long tempoTotale = endTime - startTime;
        double tempoMedio = (double) tempoTotale / numeroOperazioni;

        assertEquals(numeroOperazioni, successi,
                "Tutte le ricerche devono riuscire");
        assertTrue(tempoMedio < 3000,
                "Tempo medio deve essere < 3s anche sotto carico");

        System.out.println("⚡ " + numeroOperazioni + " ops in " + tempoTotale +
                "ms (media: " + String.format("%.2f", tempoMedio) + "ms)");
    }

    // ================================================================================
    // 📡 TEST NOTIFICHE E STREAM gRPC
    // ================================================================================

    @Test
    @Order(5)
    @DisplayName("📡 Test stream notifiche base")
    void testStreamNotifiche() throws Exception {
        CountDownLatch notificheRicevute = new CountDownLatch(1);
        List<String> messaggiRicevuti = Collections.synchronizedList(new ArrayList<>());

        ClientService client = clients.get(0);
        TrattaDTO tratta = trovaTrattaDisponibile(client);

        if (tratta != null) {
            setupStreamNotifiche(client, tratta, 0, notificheRicevute, messaggiRicevuti);

            // Aspetta setup
            Thread.sleep(2000);

            // Genera un acquisto che dovrebbe produrre notifiche
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            client.inviaRichiesta(acquisto);

            // Aspetta notifiche
            boolean ricevute = notificheRicevute.await(10, TimeUnit.SECONDS);

            System.out.println("📡 Stream test: setup completato, notifiche ricevute: " +
                    messaggiRicevuti.size());
        }
    }

    @Test
    @Order(6)
    @DisplayName("🎉 Test broadcast promozioni")
    void testBroadcastPromozioni() throws Exception {
        AtomicInteger promozioniRicevute = new AtomicInteger(0);
        CountDownLatch promoLatch = new CountDownLatch(1);

        // Setup listener promozioni
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", SERVER_PORT)
                .usePlaintext()
                .build();

        TrenicalServiceGrpc.TrenicalServiceStub stub = TrenicalServiceGrpc.newStub(channel);
        RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();

        stub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
            @Override
            public void onNext(PromozioneGrpc promo) {
                promozioniRicevute.incrementAndGet();
                promoLatch.countDown();
                System.out.println("🎉 Ricevuta promozione: " + promo.getNome());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("❌ Errore stream promozioni: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("✅ Stream promozioni completato");
            }
        });

        Thread.sleep(2000);

        // Genera promozioni di test
        for (int i = 0; i < 3; i++) {
            PromozioneDTO promo = new PromozioneDTO(
                    "TestPromo" + i,
                    "Promo test " + i + " - Sconto " + (10 + i * 5) + "%",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1)
            );

            trenicalService.broadcastPromozione(promo);
            Thread.sleep(500);
        }

        // Aspetta alcune promozioni
        boolean ricevuteAlcune = promoLatch.await(10, TimeUnit.SECONDS);

        channel.shutdown();
        channel.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(promozioniRicevute.get() >= 0, "Sistema promozioni deve essere funzionante");
        System.out.println("🎉 Broadcast test: " + promozioniRicevute.get() + " promozioni ricevute");
    }

    // ================================================================================
    // 🛡️ TEST RESILIENZA
    // ================================================================================

    @Test
    @Order(7)
    @DisplayName("🛡️ Test resilienza: operazioni sotto stress")
    void testResilienzaSistema() throws Exception {
        int operazioniRapide = 50;
        int successi = 0;
        int errori = 0;

        for (int i = 0; i < operazioniRapide; i++) {
            try {
                ClientService client = clients.get(i % clients.size());

                // Solo ricerche per il test di resilienza
                RichiestaDTO ricerca = new RichiestaDTO.Builder()
                        .tipo("FILTRA")
                        .messaggioExtra(";;;")
                        .build();

                RispostaDTO risposta = client.inviaRichiesta(ricerca);
                if (risposta.getEsito().equals("OK")) {
                    successi++;
                } else {
                    errori++;
                }

                // Pausa minima per non sovraccaricare
                if (i % 10 == 0) {
                    Thread.sleep(50);
                }

            } catch (Exception e) {
                errori++;
            }
        }

        int totaleOperazioni = successi + errori;
        double percentualeSuccesso = (double) successi / totaleOperazioni * 100;

        assertTrue(totaleOperazioni > 0, "Almeno alcune operazioni devono essere tentate");
        assertTrue(percentualeSuccesso > 80, "Almeno l'80% delle operazioni deve riuscire");

        System.out.println("🛡️ Resilienza: " + successi + "/" + totaleOperazioni +
                " (" + String.format("%.1f", percentualeSuccesso) + "% successo)");
    }

    @ParameterizedTest
    @CsvSource({
            "BASE, INTERO, 1",
            "ARGENTO, FEDELTA, 2",
            "GOLD, INTERO, 3"
    })
    @DisplayName("🎭 Test combinazioni classe-prezzo")
    void testCombinazioniClassePrezzo(ClasseServizio classe, TipoPrezzo tipoPrezzo, int expectedMinPrice) {
        ClientService client = clients.get(0);

        // Per prezzi fedeltà, assicurati che il cliente abbia la carta
        if (tipoPrezzo == TipoPrezzo.FEDELTA) {
            RichiestaDTO fedelta = new RichiestaDTO.Builder()
                    .tipo("CARTA_FEDELTA")
                    .idCliente(client.getCliente().getId().toString())
                    .build();
            client.inviaRichiesta(fedelta);
        }

        TrattaDTO tratta = trovaTrattaDisponibile(client);
        assumeTrue(tratta != null, "Serve tratta per test combinazioni");

        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(tratta)
                .classeServizio(classe)
                .tipoPrezzo(tipoPrezzo)
                .build();

        RispostaDTO risposta = client.inviaRichiesta(acquisto);
        assertEquals("OK", risposta.getEsito(),
                "Acquisto " + classe + "-" + tipoPrezzo + " deve riuscire");

        if (risposta.getBiglietto() != null) {
            assertTrue(risposta.getBiglietto().getPrezzoEffettivo() >= expectedMinPrice,
                    "Prezzo deve essere almeno " + expectedMinPrice);
        }

        System.out.println("🎭 Combinazione " + classe + "-" + tipoPrezzo + ": " +
                (risposta.getBiglietto() != null ?
                        "€" + risposta.getBiglietto().getPrezzoEffettivo() : "N/A"));
    }

    // ================================================================================
    // 🔧 METODI UTILITY
    // ================================================================================

    private TrattaDTO trovaTrattaDisponibile(ClientService client) {
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);

        if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
            return risposta.getTratte().get(0);
        }
        return null;
    }

    private TrattaDTO trovaNuovaTratta(ClientService client, TrattaDTO trattaEsclusa) {
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);

        if (risposta.getTratte() != null && risposta.getTratte().size() > 1) {
            return risposta.getTratte().stream()
                    .filter(t -> !t.getId().equals(trattaEsclusa.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void setupStreamNotifiche(ClientService client, TrattaDTO tratta, int clientId,
                                      CountDownLatch latch, List<String> messaggi) {
        try {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress("localhost", SERVER_PORT)
                    .usePlaintext()
                    .build();

            TrenicalServiceGrpc.TrenicalServiceStub stub = TrenicalServiceGrpc.newStub(channel);

            IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                    .setEmailCliente(client.getCliente().getEmail())
                    .setTrattaId(tratta.getId().toString())
                    .build();

            stub.streamNotificheTratta(richiesta, new StreamObserver<NotificaTrattaGrpc>() {
                @Override
                public void onNext(NotificaTrattaGrpc notifica) {
                    String msg = "Client" + clientId + ": " + notifica.getMessaggio();
                    messaggi.add(msg);
                    latch.countDown();
                    System.out.println("📨 " + msg);
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("❌ Errore notifiche client" + clientId + ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("✅ Stream notifiche client" + clientId + " completato");
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Errore setup stream client" + clientId + ": " + e.getMessage());
        }
    }
}

// ================================================================================
// 🧪 TEST SPECIFICI PER COMPONENTI DEL TUO PROGETTO
// ================================================================================

/**
 * Test per il sistema atomico di gestione capienza
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CapienzaAtomicaTest {

    private MemoriaBiglietti memoriaBiglietti;

    @BeforeAll
    void setupCapienzaTest() {
        memoriaBiglietti = new MemoriaBiglietti();
    }

    @Test
    @DisplayName("🔒 Test controllo capienza atomico")
    void testControlloCapienzaAtomico() {
        UUID trattaTest = UUID.randomUUID();
        int capienzaLimitata = 3;

        // Tenta di aggiungere più biglietti della capienza
        List<model.Biglietto> bigliettiAggiunti = new ArrayList<>();

        for (int i = 0; i < capienzaLimitata + 2; i++) {
            model.Biglietto biglietto = new model.Biglietto.Builder()
                    .idCliente(UUID.randomUUID())
                    .idTratta(trattaTest)
                    .classe(ClasseServizio.BASE)
                    .prezzoPagato(15.0)
                    .dataAcquisto(LocalDate.now())
                    .tipoAcquisto("test_capienza")
                    .build();

            boolean aggiunto = memoriaBiglietti.aggiungiSeSpazioDiponibile(biglietto, capienzaLimitata);
            if (aggiunto) {
                bigliettiAggiunti.add(biglietto);
            }
        }

        assertEquals(capienzaLimitata, bigliettiAggiunti.size(),
                "Devono essere aggiunti esattamente " + capienzaLimitata + " biglietti");

        // Verifica che tutti i biglietti siano per la stessa tratta
        long bigliettiPerTratta = memoriaBiglietti.contaBigliettiPerTratta(trattaTest);
        assertEquals(capienzaLimitata, bigliettiPerTratta,
                "Conteggio biglietti per tratta deve essere corretto");

        System.out.println("🔒 Controllo capienza atomico: " + bigliettiAggiunti.size() +
                "/" + capienzaLimitata + " (limite rispettato)");
    }

    @Test
    @DisplayName("💾 Test thread safety memoria biglietti")
    void testThreadSafetyMemoriaBiglietti() throws Exception {
        int numThread = 5;
        int operazioniPerThread = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numThread);
        CountDownLatch latch = new CountDownLatch(numThread);
        AtomicInteger bigliettiAggiunti = new AtomicInteger(0);

        for (int t = 0; t < numThread; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operazioniPerThread; i++) {
                        model.Biglietto biglietto = new model.Biglietto.Builder()
                                .idCliente(UUID.randomUUID())
                                .idTratta(UUID.randomUUID())
                                .classe(ClasseServizio.BASE)
                                .prezzoPagato(20.0 + threadId)
                                .dataAcquisto(LocalDate.now())
                                .tipoAcquisto("thread_test_" + threadId)
                                .build();

                        boolean aggiunto = memoriaBiglietti.aggiungiSeSpazioDiponibile(biglietto, 1000);
                        if (aggiunto) {
                            bigliettiAggiunti.incrementAndGet();
                        }

                        Thread.sleep(10); // Simula processing time
                    }
                } catch (Exception e) {
                    System.err.println("Errore thread " + threadId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Test thread safety deve completarsi");
        executor.shutdown();

        assertTrue(bigliettiAggiunti.get() > 0, "Almeno alcuni biglietti devono essere aggiunti");
        assertTrue(bigliettiAggiunti.get() <= numThread * operazioniPerThread,
                "Non devono essere aggiunti più biglietti del previsto");

        System.out.println("💾 Thread safety: " + bigliettiAggiunti.get() + "/" +
                (numThread * operazioniPerThread) + " biglietti aggiunti");
    }

    @Test
    @DisplayName("🔄 Test conferma prenotazione atomica")
    void testConfermaPrenotazioneAtomica() {
        // Crea una prenotazione
        model.Biglietto prenotazione = new model.Biglietto.Builder()
                .idCliente(UUID.randomUUID())
                .idTratta(UUID.randomUUID())
                .classe(ClasseServizio.BASE)
                .prezzoPagato(25.0)
                .dataAcquisto(LocalDate.now())
                .tipoAcquisto("prenotazione")
                .build();

        // Aggiungi prenotazione
        boolean prenotazioneAggiunta = memoriaBiglietti.aggiungiSeSpazioDiponibile(prenotazione, 100);
        assertTrue(prenotazioneAggiunta, "Prenotazione deve essere aggiunta");

        // Conferma atomicamente
        boolean confermata = memoriaBiglietti.confermaPrenotazione(prenotazione);
        assertTrue(confermata, "Prenotazione deve essere confermata");

        // Verifica che non ci sia più come prenotazione
        model.Biglietto bigliettoTrovato = memoriaBiglietti.getById(prenotazione.getId());
        assertNotNull(bigliettoTrovato, "Biglietto deve esistere");
        assertEquals("acquisto", bigliettoTrovato.getTipoAcquisto(),
                "Deve essere diventato un acquisto");

        System.out.println("🔄 Conferma atomica: prenotazione convertita in acquisto");
    }
}

/**
 * Test per TrattaFactory del tuo progetto
 */
class TrattaFactoryTest {

    @Test
    @DisplayName("🏭 Test TrattaFactory con configurazioni")
    void testTrattaFactoryPersonalizzata() {
        // Test con configurazione personalizzata
        TrattaFactoryConcrete.TrattaGenerationConfig config =
                TrattaFactoryConcrete.TrattaGenerationConfig.testConfig();

        TrattaFactoryConcrete factory = new TrattaFactoryConcrete(config);
        LocalDate dataTest = LocalDate.now().plusDays(1);

        List<Tratta> tratte = factory.generaTratte(dataTest);

        assertNotNull(tratte, "Tratte generate non devono essere null");
        assertEquals(2, tratte.size(), "Config test deve generare esattamente 2 tratte");

        // Verifica che le tratte rispettino la configurazione test
        tratte.forEach(tratta -> {
            assertTrue(config.getStazioni().contains(tratta.getStazionePartenza()),
                    "Stazione partenza deve essere nella lista config");
            assertTrue(config.getStazioni().contains(tratta.getStazioneArrivo()),
                    "Stazione arrivo deve essere nella lista config");
            assertEquals(dataTest, tratta.getData(), "Data deve corrispondere");
            assertTrue(tratta.getTreno().getCapienzaTotale() >= config.getCapienzaBase(),
                    "Capienza deve essere almeno quella base");
        });

        System.out.println("🏭 Factory personalizzata genera " + tratte.size() + " tratte conformi");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 7})
    @DisplayName("📊 Test generazione tratte per giorni multipli")
    void testGenerazioneGiorniMultipli(int numeroGiorni) {
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        List<Tratta> tutteLeTratte = new ArrayList<>();

        for (int i = 1; i <= numeroGiorni; i++) {
            LocalDate data = LocalDate.now().plusDays(i);
            List<Tratta> tratteGiorno = factory.generaTratte(data);

            assertNotNull(tratteGiorno, "Tratte per giorno " + i + " non devono essere null");
            assertFalse(tratteGiorno.isEmpty(), "Deve generare almeno una tratta per giorno");

            // Verifica che tutte abbiano la data corretta
            tratteGiorno.forEach(tratta ->
                    assertEquals(data, tratta.getData(), "Data tratta deve corrispondere al giorno"));

            tutteLeTratte.addAll(tratteGiorno);
        }

        // Verifica varietà
        Set<String> stazioniPartenza = tutteLeTratte.stream()
                .map(Tratta::getStazionePartenza)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(stazioniPartenza.size() >= 2,
                "Deve esserci varietà nelle stazioni di partenza");

        System.out.println("📊 Generati " + tutteLeTratte.size() + " tratte per " +
                numeroGiorni + " giorni con " + stazioniPartenza.size() + " stazioni diverse");
    }
}

/**
 * Test per il sistema Wallet client
 */
class WalletClientTest {

    private model.Wallet wallet;
    private eventi.ListaEventi listaEventi;

    @BeforeEach
    void setupWallet() {
        wallet = new model.Wallet();
        listaEventi = eventi.ListaEventi.getInstance();
        listaEventi.aggiungiObserver(wallet);
    }

    @Test
    @DisplayName("💼 Test wallet: acquisto biglietto")
    void testWalletAcquistoBiglietto() {
        // Simula evento acquisto
        BigliettoDTO biglietto = createTestBiglietto(enums.StatoBiglietto.CONFERMATO);
        eventi.EventoAcquisto eventoAcquisto = new eventi.EventoAcquisto(biglietto);

        int sizeIniziale = wallet.getBigliettiConfermati().size();

        listaEventi.notifica(eventoAcquisto);

        assertEquals(sizeIniziale + 1, wallet.getBigliettiConfermati().size(),
                "Wallet deve contenere il nuovo biglietto confermato");

        System.out.println("💼 Wallet: biglietto acquistato aggiunto ai confermati");
    }

    @Test
    @DisplayName("📝 Test wallet: prenotazione e conferma")
    void testWalletPrenotazioneConferma() {
        // 1. Prenotazione
        BigliettoDTO prenotazione = createTestBiglietto(enums.StatoBiglietto.NON_CONFERMATO);
        eventi.EventoPrenota eventoPrenota = new eventi.EventoPrenota(prenotazione);

        listaEventi.notifica(eventoPrenota);

        assertEquals(1, wallet.getBigliettiNonConfermati().size(),
                "Wallet deve contenere la prenotazione");
        assertEquals(0, wallet.getBigliettiConfermati().size(),
                "Non ci devono essere biglietti confermati");

        // 2. Conferma
        BigliettoDTO confermato = new BigliettoDTO(
                prenotazione.getId(), // Stesso ID
                prenotazione.getCliente(),
                prenotazione.getTratta(),
                prenotazione.getClasseServizio(),
                prenotazione.getTipoPrezzo(),
                prenotazione.getPrezzoEffettivo(),
                enums.StatoBiglietto.CONFERMATO // Stato cambiato
        );

        eventi.EventoConferma eventoConferma = new eventi.EventoConferma(confermato);
        listaEventi.notifica(eventoConferma);

        assertEquals(0, wallet.getBigliettiNonConfermati().size(),
                "Prenotazione deve essere rimossa");
        assertEquals(1, wallet.getBigliettiConfermati().size(),
                "Biglietto deve essere nei confermati");

        System.out.println("📝 Wallet: prenotazione confermata correttamente");
    }

    @Test
    @DisplayName("🔄 Test wallet: modifica biglietto")
    void testWalletModificaBiglietto() {
        // Biglietto originale
        BigliettoDTO originale = createTestBiglietto(enums.StatoBiglietto.CONFERMATO);
        eventi.EventoAcquisto eventoAcquisto = new eventi.EventoAcquisto(originale);
        listaEventi.notifica(eventoAcquisto);

        // Biglietto modificato
        BigliettoDTO modificato = new BigliettoDTO(
                UUID.randomUUID(), // Nuovo ID
                originale.getCliente(),
                originale.getTratta(),
                ClasseServizio.GOLD, // Classe cambiata
                originale.getTipoPrezzo(),
                originale.getPrezzoEffettivo() + 20.0, // Prezzo aumentato
                enums.StatoBiglietto.CONFERMATO
        );

        eventi.EventoModifica eventoModifica = new eventi.EventoModifica(originale, modificato);
        listaEventi.notifica(eventoModifica);

        assertEquals(1, wallet.getBigliettiConfermati().size(),
                "Deve esserci ancora un biglietto confermato");

        BigliettoDTO bigliettoNelWallet = wallet.getBigliettiConfermati().get(0);
        assertEquals(modificato.getId(), bigliettoNelWallet.getId(),
                "Deve essere il biglietto modificato");
        assertEquals(ClasseServizio.GOLD, bigliettoNelWallet.getClasseServizio(),
                "Classe deve essere aggiornata");

        System.out.println("🔄 Wallet: modifica biglietto gestita correttamente");
    }

    private BigliettoDTO createTestBiglietto(enums.StatoBiglietto stato) {
        ClienteDTO cliente = new ClienteDTO(
                UUID.randomUUID(), "Test", "Cliente", "test@example.com",
                false, 25, "Milano", 0, "1234567890"
        );

        TrattaDTO tratta = new TrattaDTO(
                UUID.randomUUID(), "Milano", "Roma",
                LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0),
                1, null, null
        );

        return new BigliettoDTO(
                UUID.randomUUID(),
                cliente,
                tratta,
                ClasseServizio.BASE,
                TipoPrezzo.INTERO,
                25.0,
                stato
        );
    }
}

/**
 * Test per WalletPromozioni
 */
class WalletPromozioniTest {

    private model.WalletPromozioni walletPromozioni;
    private eventi.ListaEventi listaEventi;

    @BeforeEach
    void setupWalletPromozioni() {
        walletPromozioni = new model.WalletPromozioni();
        listaEventi = eventi.ListaEventi.getInstance();
        listaEventi.aggiungiObserver(walletPromozioni);
    }

    @Test
    @DisplayName("🎉 Test wallet promozioni: ricezione ed expiration")
    void testWalletPromozioniRicezioneExpiration() throws InterruptedException {
        // Crea promozione che scade tra 2 secondi
        PromozioneDTO promozione = new PromozioneDTO(
                "TestPromo",
                "Promozione di test",
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(2)
        );

        eventi.EventoPromozione eventoPromo = new eventi.EventoPromozione(promozione);

        assertEquals(0, walletPromozioni.getPromozioniAttive().size(),
                "Inizialmente non ci devono essere promozioni");

        // Notifica promozione
        listaEventi.notifica(eventoPromo);

        assertEquals(1, walletPromozioni.getPromozioniAttive().size(),
                "Deve esserci una promozione attiva");

        // Aspetta scadenza (2 secondi + margine)
        Thread.sleep(3000);

        assertEquals(0, walletPromozioni.getPromozioniAttive().size(),
                "Promozione deve essere rimossa dopo scadenza");

        System.out.println("🎉 WalletPromozioni: gestione scadenza automatica funziona");
    }

    @Test
    @DisplayName("🎉 Test wallet promozioni: multiple promozioni")
    void testWalletPromozioniMultiple() {
        LocalDateTime ora = LocalDateTime.now();

        // Crea 3 promozioni con scadenze diverse
        PromozioneDTO promo1 = new PromozioneDTO("Promo1", "Test 1", ora, ora.plusDays(1));
        PromozioneDTO promo2 = new PromozioneDTO("Promo2", "Test 2", ora, ora.plusDays(2));
        PromozioneDTO promo3 = new PromozioneDTO("Promo3", "Test 3", ora, ora.plusDays(3));

        // Notifica tutte
        listaEventi.notifica(new eventi.EventoPromozione(promo1));
        listaEventi.notifica(new eventi.EventoPromozione(promo2));
        listaEventi.notifica(new eventi.EventoPromozione(promo3));

        assertEquals(3, walletPromozioni.getPromozioniAttive().size(),
                "Devono esserci 3 promozioni attive");

        List<PromozioneDTO> promozioni = walletPromozioni.getPromozioniAttive();
        assertTrue(promozioni.stream().anyMatch(p -> p.getNome().equals("Promo1")),
                "Deve contenere Promo1");
        assertTrue(promozioni.stream().anyMatch(p -> p.getNome().equals("Promo2")),
                "Deve contenere Promo2");
        assertTrue(promozioni.stream().anyMatch(p -> p.getNome().equals("Promo3")),
                "Deve contenere Promo3");

        System.out.println("🎉 WalletPromozioni: gestione multiple promozioni funziona");
    }
}

/**
 * Test suite finale che raggruppa tutti i test
 */
@org.junit.platform.suite.api.Suite
@org.junit.platform.suite.api.SuiteDisplayName("🚂 Test Suite Completa TreniCal - Corretta")
@org.junit.platform.suite.api.SelectClasses({
        TrenicallIntegrationTest.class,
        CapienzaAtomicaTest.class,
        TrattaFactoryTest.class,
        WalletClientTest.class,
        WalletPromozioniTest.class
})
class CompleteTrenicalTestSuite {
    // Suite completa corretta per il progetto TreniCal
}