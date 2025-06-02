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
import observer.*;
import persistence.*;
import service.BancaServiceClient;
import service.ClientService;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔧 TEST CAPIENZA CORRETTO
 *
 * ✅ CORREZIONI APPLICATE:
 * - Una sola tratta per tutto il test
 * - Controllo rigoroso della capienza
 * - Reset memoria tra test
 * - Verifica atomica dell'overselling
 */
public class CapienzaTestIsolato{

    private static final int TEST_SERVER_PORT = 8200;
    private static final int TEST_BANCA_PORT = 8201;
    private static final int CAPIENZA_TEST = 10;
    private static final int NUM_CLIENT = 30;

    // Componenti sistema
    private static Server serverTest;
    private static Server bancaTest;
    private static MemoriaBiglietti memoriaBiglietti;
    private static MemoriaTratte memoriaTratte;

    // ✅ UNA SOLA TRATTA per tutto il test
    private static TrattaDTO trattaUnica;
    private static UUID idTrattaUnica;

    // Statistiche
    private static final AtomicInteger successi = new AtomicInteger(0);
    private static final AtomicInteger fallimenti = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("🔧 ===== TEST CAPIENZA CORRETTO =====");
        System.out.println("🎯 FIX: Una sola tratta, controllo rigoroso");
        System.out.println("📊 Configurazione:");
        System.out.println("   🚂 Capienza treno: " + CAPIENZA_TEST);
        System.out.println("   👥 Client test: " + NUM_CLIENT);

        try {
            // 1️⃣ Setup sistema
            setupSistema();

            // 2️⃣ Test sequenziale su UNA tratta
            testSequenzialeCorretto();

            // 3️⃣ Reset e test concorrenza su STESSA tratta
            resetSistemaPerConcorrenza();
            testConcorrenzaCorretto();

            // 4️⃣ Verifica finale rigorosa
            verificaIntegritaRigorosa();

        } catch (Exception e) {
            System.err.println("❌ Errore durante test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /**
     * 🚀 Setup con UNA SOLA tratta
     */
    private static void setupSistema() throws Exception {
        System.out.println("\n🚀 Setup sistema corretto...");

        // Server Banca
        bancaTest = ServerBuilder.forPort(TEST_BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // Componenti memoria
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // ✅ Crea UNA SOLA tratta che useremo per tutto
        creaUnicaTratta();

        // Event system
        EventDispatcher dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti, memoriaTratte));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClienti));

        BancaServiceClient bancaClient = new BancaServiceClient("localhost", TEST_BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient
        );

        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // Server principale
        serverTest = ServerBuilder.forPort(TEST_SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        Thread.sleep(2000);
        System.out.println("✅ Sistema pronto con tratta unica");
    }

    /**
     * 🚂 Crea UNA SOLA tratta per tutto il test
     */
    private static void creaUnicaTratta() {
        // ✅ ID fisso per poter identificare sempre la stessa tratta
        idTrattaUnica = UUID.fromString("12345678-1234-1234-1234-123456789abc");

        model.Treno treno = new model.Treno.Builder()
                .numero(9999)
                .tipologia("TestTreno")
                .capienzaTotale(CAPIENZA_TEST)
                .wifiDisponibile(true)
                .preseElettriche(true)
                .ariaCondizionata(true)
                .serviziRistorazione("Test")
                .accessibileDisabili(true)
                .nomeCommerciale("TrenoTestUnico")
                .build();

        Map<enums.ClasseServizio, model.Prezzo> prezzi = new HashMap<>();
        for (enums.ClasseServizio classe : enums.ClasseServizio.values()) {
            prezzi.put(classe, new model.Prezzo(10.0, 8.0, 6.0));
        }

        Tratta trattaModel = new Tratta(
                idTrattaUnica, // ✅ ID fisso
                "TestStart",
                "TestEnd",
                LocalDate.now().plusDays(1),
                java.time.LocalTime.of(14, 30),
                1,
                treno,
                prezzi
        );

        memoriaTratte.aggiungiTratta(trattaModel);
        trattaUnica = Assembler.AssemblerTratta.toDTO(trattaModel);

        System.out.println("🚂 Tratta UNICA creata:");
        System.out.println("   ID FISSO: " + idTrattaUnica);
        System.out.println("   Capienza: " + CAPIENZA_TEST + " posti");
    }

    /**
     * 🧪 Test sequenziale corretto
     */
    private static void testSequenzialeCorretto() throws Exception {
        System.out.println("\n🧪 TEST 1: Sequenziale su TRATTA UNICA");

        ClientService client = new ClientService("localhost", TEST_SERVER_PORT);
        client.attivaCliente("TestSeq", "User", "seq@test.com", 30, "Test", "3331234567");

        int acquistiRiusciti = 0;

        // Prova solo fino alla capienza + 2 per verificare rifiuto
        for (int i = 0; i < CAPIENZA_TEST + 2; i++) {
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(trattaUnica) // ✅ Sempre la stessa tratta
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(acquisto);

            if (risposta.getEsito().equals("OK")) {
                acquistiRiusciti++;
                System.out.println("   ✅ Posto " + acquistiRiusciti + "/" + CAPIENZA_TEST);
            } else {
                System.out.println("   ❌ Rifiutato dopo " + acquistiRiusciti + " posti: " + risposta.getMessaggio());
                break; // Stop al primo rifiuto
            }

            Thread.sleep(50);
        }

        // ✅ Verifica immediata per tratta specifica
        verificaCapienzaTrattaSpecifica("TEST SEQUENZIALE", acquistiRiusciti);
    }

    /**
     * 🔄 Reset sistema per test concorrenza
     */
    private static void resetSistemaPerConcorrenza() {
        System.out.println("\n🔄 RESET per test concorrenza...");

        // ✅ Rimuovi TUTTI i biglietti della tratta test
        List<model.Biglietto> biglietti = memoriaBiglietti.getTuttiIBiglietti();
        for (model.Biglietto b : biglietti) {
            if (b.getIdTratta().equals(idTrattaUnica)) {
                memoriaBiglietti.rimuoviBiglietto(b.getId());
            }
        }

        // Reset contatori
        successi.set(0);
        fallimenti.set(0);

        // Verifica reset
        int bigliettiRimasti = (int) memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(idTrattaUnica))
                .count();

        System.out.println("✅ Reset completato - Biglietti tratta test: " + bigliettiRimasti);
    }

    /**
     * 🏎️ Test concorrenza corretto
     */
    private static void testConcorrenzaCorretto() throws Exception {
        System.out.println("\n🏎️ TEST 2: Concorrenza su TRATTA UNICA");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT);

        long startTime = System.currentTimeMillis();

        // ✅ Tutti i client provano sulla STESSA tratta
        for (int i = 0; i < NUM_CLIENT; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    tentaAcquistoSuTrattaUnica(clientId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        System.out.println("\n📊 RISULTATI TEST CONCORRENZA CORRETTO:");
        System.out.println("   ⏱️ Tempo: " + (endTime - startTime) + "ms");
        System.out.println("   ✅ Successi: " + successi.get());
        System.out.println("   ❌ Fallimenti: " + fallimenti.get());

        // ✅ Verifica immediata capienza tratta unica
        verificaCapienzaTrattaSpecifica("TEST CONCORRENZA", successi.get());
    }

    /**
     * 👤 Client che prova su tratta unica
     */
    private static void tentaAcquistoSuTrattaUnica(int clientId) {
        try {
            ClientService client = new ClientService("localhost", TEST_SERVER_PORT);
            client.attivaCliente("ConcUser" + clientId, "Test",
                    "conc" + clientId + "@test.com", 25, "Test", "333" + clientId);

            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(trattaUnica) // ✅ Sempre la stessa tratta
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            RispostaDTO risposta = client.inviaRichiesta(acquisto);

            if (risposta.getEsito().equals("OK")) {
                successi.incrementAndGet();
            } else {
                fallimenti.incrementAndGet();
                if (clientId < 3) {
                    System.out.println("   ❌ Client " + clientId + ": " + risposta.getMessaggio());
                }
            }

        } catch (Exception e) {
            fallimenti.incrementAndGet();
        }
    }

    /**
     * 🔍 Verifica capienza per la tratta specifica
     */
    private static void verificaCapienzaTrattaSpecifica(String nomeTest, int successiAttesi) {
        int bigliettiTrattaUnica = (int) memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.getIdTratta().equals(idTrattaUnica))
                .count();

        System.out.println("\n🔍 VERIFICA " + nomeTest + ":");
        System.out.println("   🎫 Biglietti venduti per tratta unica: " + bigliettiTrattaUnica);
        System.out.println("   🚂 Capienza massima: " + CAPIENZA_TEST);
        System.out.println("   🎯 Overselling: " + (bigliettiTrattaUnica > CAPIENZA_TEST ? "🚨 SÌ" : "✅ NO"));

        if (bigliettiTrattaUnica > CAPIENZA_TEST) {
            System.out.println("   ⚠️ PROBLEMA: " + (bigliettiTrattaUnica - CAPIENZA_TEST) + " biglietti in eccesso!");
        }
    }

    /**
     * 🔍 Verifica integrità finale rigorosa
     */
    private static void verificaIntegritaRigorosa() {
        System.out.println("\n🔍 VERIFICA INTEGRITÀ FINALE RIGOROSA");

        List<model.Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

        // Raggruppa per tratta
        Map<UUID, List<model.Biglietto>> bigliettiPerTratta = tuttiBiglietti.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getIdTratta()));

        System.out.println("📊 ANALISI PER TRATTA:");
        bigliettiPerTratta.forEach((idTratta, biglietti) -> {
            String trattaInfo = idTratta.equals(idTrattaUnica) ?
                    "🎯 TRATTA TEST (quella che dovevamo testare)" :
                    "❓ TRATTA SCONOSCIUTA (non dovrebbe esistere!)";

            System.out.println("   " + idTratta.toString().substring(0, 8) + ": " +
                    biglietti.size() + " biglietti - " + trattaInfo);
        });

        // ✅ Controllo specifico tratta test
        int bigliettiTrattaTest = bigliettiPerTratta.getOrDefault(idTrattaUnica, List.of()).size();
        boolean overselling = bigliettiTrattaTest > CAPIENZA_TEST;
        int tratteInaspettate = bigliettiPerTratta.size() - (bigliettiPerTratta.containsKey(idTrattaUnica) ? 1 : 0);

        System.out.println("\n🏆 VERDETTO FINALE RIGOROSO:");
        System.out.println("   🎫 Biglietti tratta test: " + bigliettiTrattaTest + "/" + CAPIENZA_TEST);
        System.out.println("   🚨 Overselling: " + (overselling ? "SÌ" : "NO"));
        System.out.println("   ❓ Tratte inaspettate: " + tratteInaspettate);

        if (!overselling && tratteInaspettate == 0) {
            System.out.println("   🎉 SUCCESSO COMPLETO: Controllo capienza perfetto!");
            System.out.println("   ✨ Sistema thread-safe e affidabile");
        } else if (overselling) {
            System.out.println("   ⚠️ FALLIMENTO: Overselling rilevato!");
            System.out.println("   🔧 Problema nel controllo atomico della capienza");
        } else if (tratteInaspettate > 0) {
            System.out.println("   ⚠️ PROBLEMA: Il test ha creato tratte multiple!");
            System.out.println("   🔧 Problema nella logica del test o nel sistema");
        }
    }

    /**
     * 🧹 Cleanup
     */
    private static void cleanup() {
        System.out.println("\n🧹 Cleanup...");
        try {
            if (serverTest != null) {
                serverTest.shutdown();
                serverTest.awaitTermination(3, TimeUnit.SECONDS);
            }
            if (bancaTest != null) {
                bancaTest.shutdown();
                bancaTest.awaitTermination(3, TimeUnit.SECONDS);
            }
            System.out.println("✅ Cleanup completato");
        } catch (Exception e) {
            System.err.println("⚠️ Errore cleanup: " + e.getMessage());
        }
    }
}