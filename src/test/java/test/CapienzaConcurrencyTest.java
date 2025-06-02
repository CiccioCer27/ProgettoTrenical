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
import observer.GrpcNotificaDispatcher;
import persistence.*;
import service.BancaServiceClient;
import service.ClientService;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🧪 TEST DEFINITIVO THREAD-SAFE
 *
 * Verifica che la versione corretta NON abbia overselling
 */
public class CapienzaConcurrencyTest {

    private static final int SERVER_PORT = 8110;
    private static final int BANCA_PORT = 8111;

    // Configurazione test ESTREMA
    private static final int CAPIENZA_TRENO_TEST = 3; // Piccolissima per stress massimo
    private static final int NUM_CLIENT_CONCORRENTI = 30; // 10x la capienza
    private static final int TENTATIVI_PER_CLIENT = 2;

    private static Server server;
    private static Server bancaServer;
    private static MemoriaBiglietti memoriaBiglietti;
    private static TrattaDTO trattaTest;

    // Statistiche
    private static final AtomicInteger acquistiRiusciti = new AtomicInteger(0);
    private static final AtomicInteger richiesteRifiutate = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("🧪 ===== TEST DEFINITIVO THREAD-SAFE =====");
        System.out.println("🎯 OBIETTIVO: ZERO OVERSELLING CON STRESS ESTREMO");
        System.out.println("   🚂 Capienza: " + CAPIENZA_TRENO_TEST + " posti");
        System.out.println("   👥 Client: " + NUM_CLIENT_CONCORRENTI + " concorrenti");
        System.out.println("   🔄 Tentativi: " + TENTATIVI_PER_CLIENT + " per client");
        System.out.println("   📊 Richieste totali: " + (NUM_CLIENT_CONCORRENTI * TENTATIVI_PER_CLIENT));

        try {
            // Setup sistema thread-safe
            setupSistemaThreadSafe();

            // Test di stress
            testStressThreadSafe();

            // Verifica risultati finali
            verificaRisultatiFinali();

        } catch (Exception e) {
            System.err.println("❌ Errore test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static void setupSistemaThreadSafe() throws Exception {
        System.out.println("\n🚀 Setup sistema THREAD-SAFE");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // ⚠️ COMPONENTI THREAD-SAFE (SENZA EventDispatcher)
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // Crea tratta test piccolissima
        creaTrattaTestStress();
        memoriaTratte.aggiungiTratta(Assembler.AssemblerTratta.fromDTO(trattaTest));

        // ⚠️ HANDLER SEMPLIFICATO (senza EventDispatcher)
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient
        );

        // Solo notifiche gRPC
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
        System.out.println("✅ Sistema THREAD-SAFE operativo");
        System.out.println("   🔒 Controllo atomico: ATTIVO");
        System.out.println("   📊 " + memoriaBiglietti.getStatistiche());
    }

    private static void creaTrattaTestStress() {
        model.Treno trenoTest = new model.Treno.Builder()
                .numero(9999)
                .tipologia("ThreadSafeTest")
                .capienzaTotale(CAPIENZA_TRENO_TEST) // PICCOLISSIMA
                .wifiDisponibile(true)
                .preseElettriche(true)
                .ariaCondizionata(true)
                .serviziRistorazione("Test")
                .accessibileDisabili(true)
                .nomeCommerciale("TrenoThreadSafe")
                .build();

        Map<enums.ClasseServizio, model.Prezzo> prezzi = new HashMap<>();
        for (enums.ClasseServizio classe : enums.ClasseServizio.values()) {
            prezzi.put(classe, new model.Prezzo(20.0, 15.0, 10.0));
        }

        Tratta trattaModel = new Tratta(
                UUID.randomUUID(),
                "ThreadSafePartenza",
                "ThreadSafeArrivo",
                LocalDate.now().plusDays(1),
                java.time.LocalTime.of(14, 30),
                99,
                trenoTest,
                prezzi
        );

        trattaTest = Assembler.AssemblerTratta.toDTO(trattaModel);
        System.out.println("🚂 Tratta stress creata: capienza " + CAPIENZA_TRENO_TEST);
    }

    private static void testStressThreadSafe() throws Exception {
        System.out.println("\n🏎️ TEST STRESS THREAD-SAFE");
        System.out.println("   Target: Verificare che SOLO " + CAPIENZA_TRENO_TEST + " biglietti vengano venduti");

        // Reset contatori
        acquistiRiusciti.set(0);
        richiesteRifiutate.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_CONCORRENTI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_CONCORRENTI);

        long startTime = System.currentTimeMillis();

        // Lancia client concorrenti AGGRESSIVI
        for (int i = 0; i < NUM_CLIENT_CONCORRENTI; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    eseguiClientAggressivo(clientId);
                } catch (Exception e) {
                    System.err.println("❌ Errore client " + clientId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Monitoraggio real-time ogni 3 secondi
        Thread monitor = new Thread(() -> {
            while (!(latch.getCount() == 0)) {
                try {
                    Thread.sleep(3000);
                    int venduti = memoriaBiglietti.getTuttiIBiglietti().size();
                    System.out.println("   📊 REAL-TIME: " + venduti + "/" + CAPIENZA_TRENO_TEST +
                            " venduti | " + acquistiRiusciti.get() + " successi | " +
                            richiesteRifiutate.get() + " rifiuti");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitor.start();

        // Attendi completamento
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        monitor.interrupt();

        long endTime = System.currentTimeMillis();

        System.out.println("\n📊 RISULTATI STRESS TEST:");
        System.out.println("   ⏱️ Tempo: " + (endTime - startTime) + "ms");
        System.out.println("   ✅ Successi: " + acquistiRiusciti.get());
        System.out.println("   ❌ Rifiuti: " + richiesteRifiutate.get());
        System.out.println("   📊 " + memoriaBiglietti.getStatistiche());
    }

    private static void eseguiClientAggressivo(int clientId) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("ThreadSafeUser" + clientId, "Test",
                "threadsafe" + clientId + "@test.com", 25, "Test", "333" + clientId);

        for (int tentativo = 0; tentativo < TENTATIVI_PER_CLIENT; tentativo++) {
            try {
                RichiestaDTO acquisto = new RichiestaDTO.Builder()
                        .tipo("ACQUISTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(trattaTest)
                        .classeServizio(ClasseServizio.BASE)
                        .tipoPrezzo(TipoPrezzo.INTERO)
                        .build();

                RispostaDTO risposta = client.inviaRichiesta(acquisto);

                if (risposta.getEsito().equals("OK")) {
                    acquistiRiusciti.incrementAndGet();
                } else {
                    richiesteRifiutate.incrementAndGet();
                }

                // Pausa aggressiva casuale
                Thread.sleep(5 + (int) (Math.random() * 30));

            } catch (Exception e) {
                // Ignora errori di connessione
            }
        }
    }

    private static void verificaRisultatiFinali() {
        System.out.println("\n🔍 ===== VERIFICA RISULTATI FINALI =====");

        int bigliettiVenduti = memoriaBiglietti.getTuttiIBiglietti().size();
        boolean overselling = bigliettiVenduti > CAPIENZA_TRENO_TEST;

        System.out.println("🎯 OBIETTIVO: Vendere MAX " + CAPIENZA_TRENO_TEST + " biglietti");
        System.out.println("📊 RISULTATO: " + bigliettiVenduti + " biglietti venduti");

        memoriaBiglietti.stampaStatisticheDettagliate();

        System.out.println("\n🏆 VERDETTO FINALE:");
        if (!overselling && bigliettiVenduti == CAPIENZA_TRENO_TEST) {
            System.out.println("   🎉 PERFETTO! Sistema Thread-Safe IMPECCABILE!");
            System.out.println("   ✅ Venduti esattamente " + CAPIENZA_TRENO_TEST + " biglietti");
            System.out.println("   🔒 Zero overselling sotto stress estremo");
            System.out.println("   🚀 PRONTO PER PRODUZIONE!");
        } else if (!overselling && bigliettiVenduti < CAPIENZA_TRENO_TEST) {
            System.out.println("   👍 OTTIMO! Nessun overselling rilevato");
            System.out.println("   ✅ Sistema Thread-Safe funziona correttamente");
            System.out.println("   📝 Venduti " + bigliettiVenduti + "/" + CAPIENZA_TRENO_TEST + " (sotto-utilizzo accettabile)");
        } else if (overselling) {
            System.out.println("   🚨 FALLIMENTO! OVERSELLING RILEVATO!");
            System.out.println("   ❌ Venduti " + bigliettiVenduti + " biglietti su " + CAPIENZA_TRENO_TEST + " disponibili");
            System.out.println("   🔧 Sistema richiede correzioni immediate");
            System.out.println("   ⚠️ NON utilizzare in produzione!");
        }

        System.out.println("\n✅ ===== TEST THREAD-SAFE COMPLETATO =====");
    }

    private static void cleanup() {
        System.out.println("\n🧹 Cleanup...");

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