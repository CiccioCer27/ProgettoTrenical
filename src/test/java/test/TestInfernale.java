package test;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.GrpcNotificaDispatcher;
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

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔥 TEST INFERNALE JUNIT - STRESS MASSIMO TRENICAL
 *
 * Test JUnit che simula condizioni ESTREME:
 * - Centinaia di client concorrenti
 * - Capienza MICRO (1-2 posti)
 * - Mix di operazioni concorrenti
 * - Verifica integrità continua
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🔥 Test Infernale - Stress Estremo Sistema TreniCal")
class TestInfernaleJUnit {

    private static final int SERVER_PORT = 8112;
    private static final int BANCA_PORT = 8113;

    // 🔥 CONFIGURAZIONE INFERNALE
    private static final int CAPIENZA_MICRO = 1; // UN SOLO POSTO!
    private static final int NUM_CLIENT_INFERNALI = 100; // Ridotto per JUnit
    private static final int ROUNDS_INFERNALI = 3; // Ridotto per JUnit
    private static final int TENTATIVI_PER_ROUND = 2;
    private static final int TRATTE_MULTIPLE = 5; // Ridotto per JUnit

    private Server server;
    private Server bancaServer;
    private MemoriaBiglietti memoriaBiglietti;
    private MemoriaTratte memoriaTratte;
    private MemoriaOsservatori memoriaOsservatori;
    private List<TrattaDTO> tratteTest = new ArrayList<>();

    // Statistiche infernali
    private final AtomicInteger acquistiTotali = new AtomicInteger(0);
    private final AtomicInteger prenotazioniTotali = new AtomicInteger(0);
    private final AtomicInteger confirmeTotali = new AtomicInteger(0);
    private final AtomicInteger modificheTotali = new AtomicInteger(0);
    private final AtomicInteger rifiutiTotali = new AtomicInteger(0);
    private final AtomicInteger erroriTotali = new AtomicInteger(0);

    private final List<String> problemiRilevati = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong tempoTotaleMs = new AtomicLong(0);

    @BeforeAll
    void setupSistemaInfernale() throws Exception {
        System.out.println("🔥 Setup sistema per TEST INFERNALE");

        // 🧹 PULIZIA TOTALE PRIMA DI INIZIARE
        util.MemoryCleaner.pulisciaRapida();
        System.out.println("🧹 Memoria completamente pulita");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // 🔧 COMPONENTI FRESCHI
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();
        memoriaOsservatori = new MemoriaOsservatori();

        // 🔥 Crea tratte fresche con capienza micro
        creaTratteMicro();

        // Verifica setup
        int tratteNelSistema = memoriaTratte.getTutteTratte().size();
        assertEquals(TRATTE_MULTIPLE, tratteNelSistema,
                "Setup deve creare esattamente " + TRATTE_MULTIPLE + " tratte");

        // Handler thread-safe
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient, memoriaOsservatori
        );

        // Servizio gRPC
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // Server principale
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        Thread.sleep(2000);

        // 🔍 VERIFICA FINALE SETUP
        int bigliettiIniziali = memoriaBiglietti.getTuttiIBiglietti().size();
        assertEquals(0, bigliettiIniziali, "Sistema deve iniziare senza biglietti");
        assertEquals(TRATTE_MULTIPLE, tratteTest.size(), "Deve avere tutte le tratte test");

        System.out.println("✅ Sistema infernale PULITO operativo:");
        System.out.println("   💀 Tratte: " + tratteTest.size());
        System.out.println("   🎯 Posti totali: " + (tratteTest.size() * CAPIENZA_MICRO));
    }

    @AfterAll
    void cleanupSistemaInfernale() throws Exception {
        System.out.println("🧹 Cleanup sistema infernale...");

        if (server != null) {
            server.shutdown();
            server.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (bancaServer != null) {
            bancaServer.shutdown();
            bancaServer.awaitTermination(5, TimeUnit.SECONDS);
        }

        System.out.println("✅ Cleanup completato");
    }

    @Test
    @Order(1)
    @DisplayName("🔥 Test Stress Estremo - Single Round")
    void testStressEstremoSingleRound() throws Exception {
        System.out.println("\n🔥 INIZIO TEST STRESS ESTREMO - SINGLE ROUND");

        resetContatori();

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_INFERNALI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_INFERNALI);

        AtomicInteger acquistiRound = new AtomicInteger(0);
        AtomicInteger rifiutiRound = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Scatena i demoni
        for (int i = 0; i < NUM_CLIENT_INFERNALI; i++) {
            final int demonId = i;
            executor.submit(() -> {
                try {
                    eseguiDemoneConcorrente(demonId, 1, acquistiRound, rifiutiRound);
                } catch (Exception e) {
                    erroriTotali.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta completamento
        assertTrue(latch.await(60, TimeUnit.SECONDS),
                "Test deve completarsi entro 60 secondi");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long roundTime = endTime - startTime;

        // 🔍 VERIFICHE INTEGRITÀ
        verificaIntegritaSistema();

        // 📊 ASSERZIONI
        int bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotali = TRATTE_MULTIPLE * CAPIENZA_MICRO;

        assertTrue(bigliettiTotali <= postiTotali,
                "Non deve esserci overselling: " + bigliettiTotali + "/" + postiTotali);

        assertTrue(acquistiRound.get() > 0, "Almeno alcuni acquisti devono riuscire");

        System.out.println("✅ Round completato:");
        System.out.println("   ⏱️ Tempo: " + roundTime + "ms");
        System.out.println("   ✅ Successi: " + acquistiRound.get());
        System.out.println("   ❌ Rifiuti: " + rifiutiRound.get());
        System.out.println("   💀 Biglietti venduti: " + bigliettiTotali + "/" + postiTotali);
    }

    @Test
    @Order(2)
    @DisplayName("🔥 Test Multi-Round Progressivo")
    void testMultiRoundProgressivo() throws Exception {
        System.out.println("\n🔥 INIZIO TEST MULTI-ROUND PROGRESSIVO");

        resetContatori();

        for (int round = 1; round <= ROUNDS_INFERNALI; round++) {
            System.out.println("\n💀 ===== ROUND " + round + "/" + ROUNDS_INFERNALI + " =====");

            eseguiRoundInfernale(round);
            verificaIntegritaTraRound(round);

            // Pausa tra round
            Thread.sleep(500);
        }

        // 📊 VERIFICA FINALE
        int bigliettiFinali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotali = TRATTE_MULTIPLE * CAPIENZA_MICRO;

        assertTrue(bigliettiFinali <= postiTotali,
                "Overselling finale: " + bigliettiFinali + "/" + postiTotali);

        assertTrue(problemiRilevati.isEmpty(),
                "Non devono esserci problemi di integrità: " + problemiRilevati);

        System.out.println("✅ Multi-round completato con successo");
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 25, 50, 75})
    @DisplayName("🔥 Test Scaling - Diversi Livelli di Stress")
    void testScalingStress(int numClienti) throws Exception {
        System.out.println("\n🔥 TEST SCALING: " + numClienti + " client");

        ExecutorService executor = Executors.newFixedThreadPool(numClienti);
        CountDownLatch latch = new CountDownLatch(numClienti);

        AtomicInteger successi = new AtomicInteger(0);
        AtomicInteger fallimenti = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClienti; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    boolean successo = eseguiSingoloTentativo(clientId);
                    if (successo) {
                        successi.incrementAndGet();
                    } else {
                        fallimenti.incrementAndGet();
                    }
                } catch (Exception e) {
                    fallimenti.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Test scaling deve completarsi entro 30s");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        double throughput = (numClienti * 1000.0) / (endTime - startTime);

        // 📊 VERIFICHE SCALING
        int totaleOperazioni = successi.get() + fallimenti.get();
        assertEquals(numClienti, totaleOperazioni,
                "Tutte le operazioni devono essere processate");

        assertTrue(throughput > 0, "Throughput deve essere positivo");

        System.out.println("📊 Scaling " + numClienti + " client:");
        System.out.println("   ✅ Successi: " + successi.get());
        System.out.println("   ❌ Fallimenti: " + fallimenti.get());
        System.out.println("   ⚡ Throughput: " + String.format("%.1f", throughput) + " op/sec");
    }

    @Test
    @Order(3)
    @DisplayName("🔍 Test Verifica Integrità Finale")
    void testVerificaIntegritaFinale() {
        System.out.println("\n🔍 VERIFICA INTEGRITÀ FINALE");

        // Verifica capienza per ogni tratta
        for (TrattaDTO tratta : tratteTest) {
            long biglietti = memoriaBiglietti.contaBigliettiPerTratta(tratta.getId());
            assertTrue(biglietti <= CAPIENZA_MICRO,
                    "Tratta " + tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() +
                            " ha overselling: " + biglietti + "/" + CAPIENZA_MICRO);
        }

        // Verifica integrità globale
        Map<UUID, Integer> capienzaPerTratta = new HashMap<>();
        for (TrattaDTO tratta : tratteTest) {
            capienzaPerTratta.put(tratta.getId(), CAPIENZA_MICRO);
        }

        boolean integrita = memoriaBiglietti.verificaIntegrita(capienzaPerTratta);
        assertTrue(integrita, "Integrità globale deve essere preservata");

        // Statistiche finali
        int bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotali = TRATTE_MULTIPLE * CAPIENZA_MICRO;

        System.out.println("📊 INTEGRITÀ FINALE:");
        System.out.println("   🎫 Biglietti: " + bigliettiTotali + "/" + postiTotali);
        System.out.println("   ✅ Integrità: " + (integrita ? "PRESERVATA" : "VIOLATA"));

        // Stampa statistiche dettagliate
        memoriaBiglietti.stampaStatisticheDettagliate();
    }

    @ParameterizedTest
    @CsvSource({
            "BASE, INTERO, 1",
            "ARGENTO, INTERO, 1",
            "GOLD, INTERO, 1"
    })
    @DisplayName("🎭 Test Mix Classi Sotto Stress")
    void testMixClassiSottoStress(ClasseServizio classe, TipoPrezzo tipoPrezzo, int capienzaAttesa) throws Exception {
        System.out.println("\n🎭 Test classe " + classe + " - " + tipoPrezzo);

        int numClienti = 20; // Test più contenuto per parametrized
        ExecutorService executor = Executors.newFixedThreadPool(numClienti);
        CountDownLatch latch = new CountDownLatch(numClienti);

        AtomicInteger successi = new AtomicInteger(0);

        for (int i = 0; i < numClienti; i++) {
            executor.submit(() -> {
                try {
                    ClientService client = new ClientService("localhost", SERVER_PORT);
                    client.attivaCliente("TestClass" + Thread.currentThread().getId(), "Test",
                            "test" + Thread.currentThread().getId() + "@test.com", 25, "Test", "123456");

                    TrattaDTO tratta = tratteTest.get(0); // Prima tratta

                    RichiestaDTO acquisto = new RichiestaDTO.Builder()
                            .tipo("ACQUISTA")
                            .idCliente(client.getCliente().getId().toString())
                            .tratta(tratta)
                            .classeServizio(classe)
                            .tipoPrezzo(tipoPrezzo)
                            .build();

                    RispostaDTO risposta = client.inviaRichiesta(acquisto);
                    if (risposta.getEsito().equals("OK")) {
                        successi.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Errore normale sotto stress
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS),
                "Test mix classi deve completarsi");
        executor.shutdown();

        // Con capienza 1, massimo 1 successo per tratta
        assertTrue(successi.get() <= capienzaAttesa,
                "Successi non devono superare capienza: " + successi.get() + "/" + capienzaAttesa);

        System.out.println("🎭 Classe " + classe + ": " + successi.get() + "/" + numClienti + " successi");
    }

    // ================================================================================
    // 🔧 METODI HELPER
    // ================================================================================

    private void creaTratteMicro() {
        System.out.println("🚂 Creando " + TRATTE_MULTIPLE + " tratte micro...");

        tratteTest.clear();

        String[] partenze = {"MilanoInf", "RomaApoc", "NapoliChaos", "TorinoMay", "FirenzeDoom"};
        String[] arrivi = {"VeneziaHell", "BolognaRage", "GenovaWrath", "PalermoFury", "CataniaStorm"};

        for (int i = 0; i < TRATTE_MULTIPLE; i++) {
            // 🔥 Treno con UN SOLO POSTO
            model.Treno trenoMicro = new model.Treno.Builder()
                    .numero(6660 + i)
                    .tipologia("TrenoInf" + i)
                    .capienzaTotale(CAPIENZA_MICRO)
                    .wifiDisponibile(false)
                    .preseElettriche(false)
                    .ariaCondizionata(false)
                    .serviziRistorazione("Lava")
                    .accessibileDisabili(false)
                    .nomeCommerciale("Diablo" + i)
                    .build();

            Map<enums.ClasseServizio, model.Prezzo> prezzi = new HashMap<>();
            for (enums.ClasseServizio classe : enums.ClasseServizio.values()) {
                prezzi.put(classe, new model.Prezzo(666.0, 500.0, 333.0));
            }

            String partenza = partenze[i % partenze.length] + i;
            String arrivo = arrivi[i % arrivi.length] + i;

            Tratta trattaModel = new Tratta(
                    UUID.randomUUID(),
                    partenza,
                    arrivo,
                    LocalDate.now().plusDays(1),
                    java.time.LocalTime.of(6 + i, 0),
                    666 + i,
                    trenoMicro,
                    prezzi
            );

            memoriaTratte.aggiungiTratta(trattaModel);
            tratteTest.add(Assembler.AssemblerTratta.toDTO(trattaModel));
        }

        System.out.println("✅ Create " + TRATTE_MULTIPLE + " tratte micro");
    }

    private void resetContatori() {
        acquistiTotali.set(0);
        prenotazioniTotali.set(0);
        confirmeTotali.set(0);
        modificheTotali.set(0);
        rifiutiTotali.set(0);
        erroriTotali.set(0);
        problemiRilevati.clear();
        tempoTotaleMs.set(0);
    }

    private void eseguiRoundInfernale(int roundNum) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_INFERNALI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_INFERNALI);

        AtomicInteger acquistiRound = new AtomicInteger(0);
        AtomicInteger rifiutiRound = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_CLIENT_INFERNALI; i++) {
            final int demonId = i;
            executor.submit(() -> {
                try {
                    eseguiDemoneConcorrente(demonId, roundNum, acquistiRound, rifiutiRound);
                } catch (Exception e) {
                    erroriTotali.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS),
                "Round " + roundNum + " deve completarsi entro 60s");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long roundTime = endTime - startTime;
        tempoTotaleMs.addAndGet(roundTime);

        System.out.println("   📊 ROUND " + roundNum + " COMPLETATO:");
        System.out.println("      ⏱️ Tempo: " + roundTime + "ms");
        System.out.println("      ✅ Successi: " + acquistiRound.get());
        System.out.println("      ❌ Rifiuti: " + rifiutiRound.get());
    }

    private void eseguiDemoneConcorrente(int demonId, int round,
                                         AtomicInteger successi, AtomicInteger rifiuti) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("Demone" + demonId + "R" + round, "Infernale",
                "demone" + demonId + "r" + round + "@hell.com", 666, "Inferno", "666" + demonId);

        for (int tentativo = 0; tentativo < TENTATIVI_PER_ROUND; tentativo++) {
            try {
                TrattaDTO trattaCasuale = tratteTest.get((int) (Math.random() * tratteTest.size()));
                boolean successo = eseguiOperazioneInfernale(client, trattaCasuale);

                if (successo) {
                    successi.incrementAndGet();
                    acquistiTotali.incrementAndGet();
                } else {
                    rifiuti.incrementAndGet();
                    rifiutiTotali.incrementAndGet();
                }

                Thread.sleep((int) (Math.random() * 10));

            } catch (Exception e) {
                erroriTotali.incrementAndGet();
            }
        }
    }

    private boolean eseguiSingoloTentativo(int clientId) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("Client" + clientId, "Test",
                "client" + clientId + "@test.com", 25, "Test", "123" + clientId);

        TrattaDTO tratta = tratteTest.get(clientId % tratteTest.size());
        return eseguiOperazioneInfernale(client, tratta);
    }

    private boolean eseguiOperazioneInfernale(ClientService client, TrattaDTO tratta) {
        try {
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(acquisto);
            return risposta.getEsito().equals("OK");

        } catch (Exception e) {
            return false;
        }
    }

    private void verificaIntegritaSistema() {
        Map<UUID, Integer> capienzaPerTratta = new HashMap<>();
        for (TrattaDTO tratta : tratteTest) {
            capienzaPerTratta.put(tratta.getId(), CAPIENZA_MICRO);
        }

        boolean integrita = memoriaBiglietti.verificaIntegrita(capienzaPerTratta);
        if (!integrita) {
            String problema = "VIOLAZIONE INTEGRITÀ nel sistema";
            problemiRilevati.add(problema);
        }
    }

    private void verificaIntegritaTraRound(int round) {
        System.out.println("   🔍 VERIFICA INTEGRITÀ POST-ROUND " + round);

        verificaIntegritaSistema();

        int bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotaliSistema = TRATTE_MULTIPLE * CAPIENZA_MICRO;

        if (bigliettiTotali > postiTotaliSistema) {
            String overselling = "OVERSELLING GLOBALE: " + bigliettiTotali + "/" + postiTotaliSistema;
            problemiRilevati.add(overselling);
            System.out.println("   🚨 " + overselling);
        } else {
            System.out.println("   ✅ Integrità preservata");
        }
    }
}