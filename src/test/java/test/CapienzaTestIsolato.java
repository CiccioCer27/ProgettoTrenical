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

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 JUnit Test per Controllo Capienza Thread-Safe
 *
 * Test professionale che verifica:
 * - Controllo atomico della capienza
 * - Prevenzione overselling
 * - Thread safety in condizioni di concorrenza
 * - Integrità dei dati
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CapienzaTestIsolato {

    private static final int TEST_SERVER_PORT = 8200;
    private static final int TEST_BANCA_PORT = 8201;
    private static final int CAPIENZA_TEST = 5; // Ridotta per test più veloci
    private static final int NUM_CLIENT_CONCORRENTI = 15;

    // Componenti sistema
    private static Server serverTest;
    private static Server bancaTest;
    private static MemoriaBiglietti memoriaBiglietti;
    private static MemoriaTratte memoriaTratte;
    private static MemoriaClientiFedeli memoriaClienti;

    // Tratta di test
    private static TrattaDTO trattaTest;
    private static UUID idTrattaTest;

    @BeforeAll
    static void setupSistema() throws Exception {
        System.out.println("🚀 Setup sistema test JUnit...");

        // Server Banca
        bancaTest = ServerBuilder.forPort(TEST_BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // ✅ Componenti memoria thread-safe (senza EventDispatcher problematico)
        memoriaBiglietti = new MemoriaBiglietti();
        memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();
        MemoriaOsservatori memoriaOsservatori=new MemoriaOsservatori();

        // Crea tratta test
        setupTrattaTest();

        // ✅ Handler SENZA EventDispatcher (architettura refactored)
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", TEST_BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient,memoriaOsservatori
        );

        // Solo notifiche gRPC (no eventi persistenza)
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // Server principale
        serverTest = ServerBuilder.forPort(TEST_SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        Thread.sleep(1000); // Attendi startup
        System.out.println("✅ Sistema test ready");
    }

    @BeforeEach
    void resetTrattaTest() {
        System.out.println("🔄 Reset tratta test...");

        // Rimuovi tutti i biglietti per la tratta test
        List<model.Biglietto> biglietti = new ArrayList<>(memoriaBiglietti.getTuttiIBiglietti());
        biglietti.stream()
                .filter(b -> b.getIdTratta().equals(idTrattaTest))
                .forEach(b -> memoriaBiglietti.rimuoviBiglietto(b.getId()));

        // Verifica reset
        long bigliettiRimasti = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(idTrattaTest))
                .count();

        assertEquals(0, bigliettiRimasti, "Reset biglietti tratta test fallito");
    }

    @AfterAll
    static void cleanup() throws Exception {
        System.out.println("🧹 Cleanup sistema test...");

        if (serverTest != null) {
            serverTest.shutdown();
            serverTest.awaitTermination(3, TimeUnit.SECONDS);
        }
        if (bancaTest != null) {
            bancaTest.shutdown();
            bancaTest.awaitTermination(3, TimeUnit.SECONDS);
        }

        System.out.println("✅ Cleanup completato");
    }

    private static void setupTrattaTest() {
        idTrattaTest = UUID.randomUUID();

        model.Treno treno = new model.Treno.Builder()
                .numero(999)
                .tipologia("TestTreno")
                .capienzaTotale(CAPIENZA_TEST)
                .wifiDisponibile(true)
                .preseElettriche(true)
                .ariaCondizionata(true)
                .serviziRistorazione("Test")
                .accessibileDisabili(true)
                .nomeCommerciale("TrenoTest")
                .build();

        Map<enums.ClasseServizio, model.Prezzo> prezzi = new HashMap<>();
        for (enums.ClasseServizio classe : enums.ClasseServizio.values()) {
            prezzi.put(classe, new model.Prezzo(10.0, 8.0, 6.0));
        }

        Tratta trattaModel = new Tratta(
                idTrattaTest,
                "TestPartenza",
                "TestArrivo",
                LocalDate.now().plusDays(1),
                java.time.LocalTime.of(10, 0),
                1,
                treno,
                prezzi
        );

        memoriaTratte.aggiungiTratta(trattaModel);
        trattaTest = Assembler.AssemblerTratta.toDTO(trattaModel);

        System.out.println("🚂 Tratta test creata: capienza " + CAPIENZA_TEST);
    }

    @Test
    @Order(1)
    @DisplayName("🧪 Test Sequenziale - Verifica Capienza Esatta")
    void testSequenzialeCapienza() throws Exception {
        System.out.println("\n🧪 TEST SEQUENZIALE: Verifica capienza esatta");

        ClientService client = creaClientTest("SeqTest", "seq@test.com");
        int acquistiRiusciti = 0;

        // Prova fino alla capienza + 2 per verificare rifiuto
        for (int i = 0; i < CAPIENZA_TEST + 2; i++) {
            RispostaDTO risposta = tentaAcquisto(client);

            if (risposta.getEsito().equals("OK")) {
                acquistiRiusciti++;
                System.out.println("   ✅ Posto " + acquistiRiusciti + "/" + CAPIENZA_TEST);
            } else {
                System.out.println("   ❌ Rifiutato: " + risposta.getMessaggio());
                break; // Stop al primo rifiuto
            }
        }

        // ✅ Assertions JUnit
        assertEquals(CAPIENZA_TEST, acquistiRiusciti,
                "Dovrebbe vendere esattamente " + CAPIENZA_TEST + " biglietti");

        long bigliettiVenduti = contaBigliettiTrattaTest();
        assertEquals(CAPIENZA_TEST, bigliettiVenduti,
                "Biglietti persistiti devono essere esattamente " + CAPIENZA_TEST);

        assertFalse(bigliettiVenduti > CAPIENZA_TEST,
                "OVERSELLING rilevato!");
    }

    @Test
    @Order(2)
    @DisplayName("🏎️ Test Concorrenza - Prevenzione Overselling")
    @Execution(ExecutionMode.SAME_THREAD)
    void testConcorrenzaOverselling() throws Exception {
        System.out.println("\n🏎️ TEST CONCORRENZA: Prevenzione overselling");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_CONCORRENTI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_CONCORRENTI);

        AtomicInteger successi = new AtomicInteger(0);
        AtomicInteger fallimenti = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Lancia richieste concorrenti
        for (int i = 0; i < NUM_CLIENT_CONCORRENTI; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    if (tentaAcquistoConcorrente(clientId)) {
                        successi.incrementAndGet();
                    } else {
                        fallimenti.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completato = latch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // ✅ Risultati
        System.out.println("📊 RISULTATI CONCORRENZA:");
        System.out.println("   ⏱️ Tempo: " + (endTime - startTime) + "ms");
        System.out.println("   ✅ Successi: " + successi.get());
        System.out.println("   ❌ Fallimenti: " + fallimenti.get());

        // ✅ Assertions critiche JUnit
        assertTrue(completato, "Test non completato in tempo utile");

        long bigliettiFinali = contaBigliettiTrattaTest();
        System.out.println("   🎫 Biglietti finali: " + bigliettiFinali);

        // ASSERTION PRINCIPALE: NO OVERSELLING
        assertTrue(bigliettiFinali <= CAPIENZA_TEST,
                "OVERSELLING CRITICO: " + bigliettiFinali + " > " + CAPIENZA_TEST);

        assertEquals(successi.get(), bigliettiFinali,
                "Discrepanza tra successi riportati e biglietti persistiti");

        assertTrue(successi.get() <= CAPIENZA_TEST,
                "Troppi successi riportati");

        assertTrue(fallimenti.get() >= (NUM_CLIENT_CONCORRENTI - CAPIENZA_TEST),
                "Troppi pochi fallimenti - sistema non sta rifiutando correttamente");
    }

    @Test
    @Order(3)
    @DisplayName("🔍 Test Integrità Dati")
    void testIntegritaDati() {
        System.out.println("\n🔍 TEST INTEGRITÀ: Verifica consistenza dati");

        // Verifica che non ci siano biglietti orfani
        List<model.Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

        for (model.Biglietto biglietto : tuttiBiglietti) {
            assertNotNull(biglietto.getId(), "Biglietto con ID null");
            assertNotNull(biglietto.getIdCliente(), "Biglietto con cliente null");
            assertNotNull(biglietto.getIdTratta(), "Biglietto con tratta null");
            assertTrue(biglietto.getPrezzoPagato() > 0, "Prezzo non valido");
        }

        // Verifica capienza per ogni tratta
        Map<UUID, Long> bigliettiPerTratta = tuttiBiglietti.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        model.Biglietto::getIdTratta,
                        java.util.stream.Collectors.counting()
                ));

        bigliettiPerTratta.forEach((idTratta, count) -> {
            if (idTratta.equals(idTrattaTest)) {
                assertTrue(count <= CAPIENZA_TEST,
                        "Overselling su tratta test: " + count + " > " + CAPIENZA_TEST);
            }
        });

        System.out.println("✅ Integrità dati verificata");
    }

    @Test
    @Order(4)
    @DisplayName("⚡ Test Performance Controllo Atomico")
    void testPerformanceControlloAtomico() throws Exception {
        System.out.println("\n⚡ TEST PERFORMANCE: Controllo atomico");

        int numTentativi = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numTentativi);
        CountDownLatch latch = new CountDownLatch(numTentativi);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numTentativi; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    tentaAcquistoConcorrente(clientId);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completato = latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        long durata = endTime - startTime;
        long bigliettiFinali = contaBigliettiTrattaTest();

        System.out.println("📊 PERFORMANCE:");
        System.out.println("   ⏱️ Durata: " + durata + "ms");
        System.out.println("   🎫 Biglietti: " + bigliettiFinali);
        System.out.println("   📈 Throughput: " + (numTentativi * 1000.0 / durata) + " req/sec");

        // Assertions performance
        assertTrue(completato, "Test performance non completato in tempo");
        assertTrue(durata < 8000, "Performance troppo lenta: " + durata + "ms");
        assertTrue(bigliettiFinali <= CAPIENZA_TEST, "Overselling in test performance");
    }

    // ═══ UTILITY METHODS ═══

    private ClientService creaClientTest(String nome, String email) throws Exception {
        ClientService client = new ClientService("localhost", TEST_SERVER_PORT);
        client.attivaCliente(nome, "Test", email, 30, "TestCity", "3331234567");
        return client;
    }

    private RispostaDTO tentaAcquisto(ClientService client) {
        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(client.getCliente().getId().toString())
                .tratta(trattaTest)
                .classeServizio(ClasseServizio.BASE)
                .tipoPrezzo(TipoPrezzo.INTERO)
                .build();

        return client.inviaRichiesta(acquisto);
    }

    private boolean tentaAcquistoConcorrente(int clientId) {
        try {
            ClientService client = creaClientTest("ConcUser" + clientId, "conc" + clientId + "@test.com");
            RispostaDTO risposta = tentaAcquisto(client);
            return risposta.getEsito().equals("OK");
        } catch (Exception e) {
            return false;
        }
    }

    private long contaBigliettiTrattaTest() {
        return memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(idTrattaTest))
                .count();
    }
}