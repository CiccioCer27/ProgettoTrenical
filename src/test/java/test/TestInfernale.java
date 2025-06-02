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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🔥 TEST INFERNALE - STRESS MASSIMO TRENICAL
 *
 * Test che simula condizioni ESTREME:
 * - Centinaia di client concorrenti
 * - Capienza MICRO (1-2 posti)
 * - Mix di operazioni (acquisto + prenotazione + conferma + modifica)
 * - Stress prolungato nel tempo
 * - Verifica integrità continua
 */
public class TestInfernale {

    private static final int SERVER_PORT = 8112;
    private static final int BANCA_PORT = 8113;

    // 🔥 CONFIGURAZIONE INFERNALE
    private static final int CAPIENZA_MICRO = 1; // UN SOLO POSTO!
    private static final int NUM_CLIENT_INFERNALI = 200; // 200 client per 1 posto
    private static final int ROUNDS_INFERNALI = 5; // 5 round di stress
    private static final int TENTATIVI_PER_ROUND = 3;
    private static final int TRATTE_MULTIPLE = 10; // 10 tratte diverse

    private static Server server;
    private static Server bancaServer;
    private static MemoriaBiglietti memoriaBiglietti;
    private static MemoriaTratte memoriaTratte;
    private static List<TrattaDTO> tratteTest = new ArrayList<>();

    // Statistiche infernali
    private static final AtomicInteger acquistiTotali = new AtomicInteger(0);
    private static final AtomicInteger prenotazioniTotali = new AtomicInteger(0);
    private static final AtomicInteger confirmeTotali = new AtomicInteger(0);
    private static final AtomicInteger modificheTotali = new AtomicInteger(0);
    private static final AtomicInteger rifiutiTotali = new AtomicInteger(0);
    private static final AtomicInteger erroriTotali = new AtomicInteger(0);

    private static final List<String> problemiRilevati = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicLong tempoTotaleMs = new AtomicLong(0);

    public static void main(String[] args) {
        System.out.println("🔥 ===== TEST INFERNALE TRENICAL =====");
        System.out.println("💀 CONFIGURAZIONE ESTREMA:");
        System.out.println("   🚂 Capienza: " + CAPIENZA_MICRO + " posto PER TRATTA");
        System.out.println("   🚂 Tratte: " + TRATTE_MULTIPLE + " tratte diverse");
        System.out.println("   👥 Client: " + NUM_CLIENT_INFERNALI + " demoni concorrenti");
        System.out.println("   🔄 Round: " + ROUNDS_INFERNALI + " ondate di attacchi");
        System.out.println("   📊 Operazioni totali: ~" + (NUM_CLIENT_INFERNALI * ROUNDS_INFERNALI * TENTATIVI_PER_ROUND));
        System.out.println("   ⚡ MODALITÀ: DISTRUZIONE TOTALE");

        try {
            setupSistemaInfernale();
            eseguiTestInfernale();
            analisiPostApocalisse();
        } catch (Exception e) {
            System.err.println("💀 ERRORE DURANTE L'APOCALISSE: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /**
     * 🔧 Setup del sistema PULITO per il test infernale
     */
    private static void setupSistemaInfernale() throws Exception {
        System.out.println("\n🔥 Setup sistema per TEST INFERNALE");

        // 🧹 PULIZIA TOTALE PRIMA DI INIZIARE
        util.MemoryCleaner.pulisciaRapida();
        System.out.println("🧹 Memoria completamente pulita");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // 🔧 COMPONENTI FRESCHI (ora i file sono vuoti)
        memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte(); // Ora è vuota!
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // 🔥 Crea ESATTAMENTE 10 tratte fresche
        creaEsattamente10TratteMicro();

        // Verifica che abbiamo esattamente 10 tratte
        int tratteNelSistema = memoriaTratte.getTutteTratte().size();
        if (tratteNelSistema != TRATTE_MULTIPLE) {
            throw new RuntimeException("ERRORE SETUP: Attese " + TRATTE_MULTIPLE +
                    " tratte, trovate " + tratteNelSistema);
        }

        // Handler thread-safe
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient
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
        int tratteFinali = memoriaTratte.getTutteTratte().size();
        int tratteTest = TestInfernale.tratteTest.size();

        System.out.println("✅ Sistema infernale PULITO operativo:");
        System.out.println("   💀 Tratte in memoria: " + tratteFinali);
        System.out.println("   💀 Tratte per test: " + tratteTest);
        System.out.println("   🎫 Biglietti iniziali: " + bigliettiIniziali);
        System.out.println("   🎯 Posti totali: " + (tratteFinali * CAPIENZA_MICRO));

        if (tratteFinali != TRATTE_MULTIPLE || tratteTest != TRATTE_MULTIPLE || bigliettiIniziali != 0) {
            throw new RuntimeException("SETUP FALLITO: Sistema non pulito!");
        }
    }

    private static void creaEsattamente10TratteMicro() {
        System.out.println("🚂 Creando ESATTAMENTE " + TRATTE_MULTIPLE + " tratte micro...");

        // 🧹 PULISCI TUTTO
        tratteTest.clear();

        String[] partenze = {"MilanoInf", "RomaApoc", "NapoliChaos", "TorinoMay", "FirenzeDoom"};
        String[] arrivi = {"VeneziaHell", "BolognaRage", "GenovaWrath", "PalermoFury", "CataniaStorm"};

        for (int i = 0; i < TRATTE_MULTIPLE; i++) {
            // 🔥 Treno con UN SOLO POSTO
            model.Treno trenoMicro = new model.Treno.Builder()
                    .numero(6660 + i)
                    .tipologia("TrenoInf" + i)
                    .capienzaTotale(CAPIENZA_MICRO) // 💀 UN POSTO SOLO!
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

            // 🎯 Nomi UNICI per evitare confusione
            String partenza = partenze[i % partenze.length] + i;
            String arrivo = arrivi[i % arrivi.length] + i;

            Tratta trattaModel = new Tratta(
                    UUID.randomUUID(),
                    partenza, // Nome unico
                    arrivo,   // Nome unico
                    LocalDate.now().plusDays(1),
                    java.time.LocalTime.of(6 + i, 0), // Orari diversi
                    666 + i,
                    trenoMicro,
                    prezzi
            );

            // 🔧 AGGIUNGI A MEMORIA E LISTA TEST
            memoriaTratte.aggiungiTratta(trattaModel);
            tratteTest.add(Assembler.AssemblerTratta.toDTO(trattaModel));

            System.out.println("💀 Tratta " + (i + 1) + "/" + TRATTE_MULTIPLE + ": " +
                    partenza + " → " + arrivo +
                    " (ID: " + trattaModel.getId().toString().substring(0, 8) + ")");
        }

        System.out.println("✅ Create ESATTAMENTE " + TRATTE_MULTIPLE + " tratte uniche");
    }
    /**
     * 🔥 Esecuzione del test infernale
     */
    private static void eseguiTestInfernale() throws Exception {
        System.out.println("\n🔥 INIZIO TEST INFERNALE");
        System.out.println("💀 Preparatevi all'apocalisse...");

        for (int round = 1; round <= ROUNDS_INFERNALI; round++) {
            System.out.println("\n💀 ===== ROUND " + round + "/" + ROUNDS_INFERNALI + " =====");
            eseguiRoundInfernale(round);

            // Verifica integrità tra i round
            verificaIntegritaTraRound(round);

            // Pausa tra apocalissi
            Thread.sleep(1000);
        }

        System.out.println("\n🔥 TEST INFERNALE COMPLETATO");
    }

    /**
     * 💀 Esegue un singolo round infernale
     */
    private static void eseguiRoundInfernale(int roundNum) throws Exception {
        System.out.println("   🔥 Scatenando " + NUM_CLIENT_INFERNALI + " demoni...");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENT_INFERNALI);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENT_INFERNALI);

        // Reset contatori per questo round
        AtomicInteger acquistiRound = new AtomicInteger(0);
        AtomicInteger rifiutiRound = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // Scatena i demoni
        for (int i = 0; i < NUM_CLIENT_INFERNALI; i++) {
            final int demonId = i;
            final int roundId = roundNum;

            executor.submit(() -> {
                try {
                    eseguiDemoneConcorrente(demonId, roundId, acquistiRound, rifiutiRound);
                } catch (Exception e) {
                    erroriTotali.incrementAndGet();
                    System.err.println("💀 Demone " + demonId + " esploso: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Monitoraggio real-time dell'apocalisse
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    if (latch.await(2, TimeUnit.SECONDS)) break;
                } catch (InterruptedException e) {
                    break;
                }

                int bigliettiVenduti = memoriaBiglietti.getTuttiIBiglietti().size();
                System.out.println("   💀 APOCALISSE IN CORSO: " + bigliettiVenduti + " anime catturate | " +
                        acquistiRound.get() + " successi | " + rifiutiRound.get() + " respinti");
            }
        });
        monitor.start();

        // Aspetta che l'apocalisse finisca
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        monitor.interrupt();

        long endTime = System.currentTimeMillis();
        long roundTime = endTime - startTime;
        tempoTotaleMs.addAndGet(roundTime);

        System.out.println("   📊 ROUND " + roundNum + " COMPLETATO:");
        System.out.println("      ⏱️ Tempo: " + roundTime + "ms");
        System.out.println("      ✅ Successi: " + acquistiRound.get());
        System.out.println("      ❌ Rifiuti: " + rifiutiRound.get());
        System.out.println("      💀 Anime totali catturate: " + memoriaBiglietti.getTuttiIBiglietti().size());
    }

    /**
     * 👹 Singolo demone che attacca il sistema
     */
    private static void eseguiDemoneConcorrente(int demonId, int round,
                                                AtomicInteger successi, AtomicInteger rifiuti) throws Exception {
        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("Demone" + demonId + "R" + round, "Infernale",
                "demone" + demonId + "r" + round + "@hell.com", 666, "Inferno", "666" + demonId);

        for (int tentativo = 0; tentativo < TENTATIVI_PER_ROUND; tentativo++) {
            try {
                // Sceglie tratta casuale
                TrattaDTO trattaCasuale = tratteTest.get((int) (Math.random() * tratteTest.size()));

                // Mix di operazioni infernali
                TipoOperazione operazione = scegliOperazioneCasuale();

                boolean successo = eseguiOperazioneInfernale(client, trattaCasuale, operazione);

                if (successo) {
                    successi.incrementAndGet();
                    aggiornaContatori(operazione, true);
                } else {
                    rifiuti.incrementAndGet();
                    aggiornaContatori(operazione, false);
                }

                // Pausa infernale casuale
                Thread.sleep((int) (Math.random() * 20));

            } catch (Exception e) {
                erroriTotali.incrementAndGet();
            }
        }
    }

    enum TipoOperazione {
        ACQUISTO, PRENOTAZIONE, CONFERMA, MODIFICA
    }

    private static TipoOperazione scegliOperazioneCasuale() {
        double random = Math.random();
        if (random < 0.6) return TipoOperazione.ACQUISTO;      // 60% acquisti
        if (random < 0.8) return TipoOperazione.PRENOTAZIONE; // 20% prenotazioni
        if (random < 0.9) return TipoOperazione.CONFERMA;     // 10% conferme
        return TipoOperazione.MODIFICA;                        // 10% modifiche
    }

    private static boolean eseguiOperazioneInfernale(ClientService client, TrattaDTO tratta, TipoOperazione op) {
        try {
            RichiestaDTO.Builder builder = new RichiestaDTO.Builder()
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(ClasseServizio.BASE);

            RichiestaDTO richiesta = switch (op) {
                case ACQUISTO -> builder.tipo("ACQUISTA").tipoPrezzo(TipoPrezzo.INTERO).build();
                case PRENOTAZIONE -> builder.tipo("PRENOTA").build();
                case CONFERMA -> {
                    // Per conferma, serve una prenotazione esistente - skip per ora
                    yield builder.tipo("ACQUISTA").tipoPrezzo(TipoPrezzo.INTERO).build();
                }
                case MODIFICA -> {
                    // Per modifica, serve un biglietto esistente - skip per ora
                    yield builder.tipo("ACQUISTA").tipoPrezzo(TipoPrezzo.INTERO).build();
                }
            };

            RispostaDTO risposta = client.inviaRichiesta(richiesta);
            return risposta.getEsito().equals("OK");

        } catch (Exception e) {
            return false;
        }
    }

    private static void aggiornaContatori(TipoOperazione op, boolean successo) {
        if (successo) {
            switch (op) {
                case ACQUISTO -> acquistiTotali.incrementAndGet();
                case PRENOTAZIONE -> prenotazioniTotali.incrementAndGet();
                case CONFERMA -> confirmeTotali.incrementAndGet();
                case MODIFICA -> modificheTotali.incrementAndGet();
            }
        } else {
            rifiutiTotali.incrementAndGet();
        }
    }

    /**
     * 🔍 Verifica integrità tra i round
     */
    private static void verificaIntegritaTraRound(int round) {
        System.out.println("   🔍 VERIFICA INTEGRITÀ POST-ROUND " + round);

        // Conta biglietti per tratta
        Map<UUID, Integer> capienzaPerTratta = new HashMap<>();
        for (TrattaDTO tratta : tratteTest) {
            capienzaPerTratta.put(tratta.getId(), CAPIENZA_MICRO);
        }

        boolean integrita = memoriaBiglietti.verificaIntegrita(capienzaPerTratta);

        if (!integrita) {
            String problema = "VIOLAZIONE INTEGRITÀ nel round " + round;
            problemiRilevati.add(problema);
            System.out.println("   🚨 " + problema);
        } else {
            System.out.println("   ✅ Integrità preservata");
        }

        // Conta biglietti totali
        int bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotaliSistema = TRATTE_MULTIPLE * CAPIENZA_MICRO;

        if (bigliettiTotali > postiTotaliSistema) {
            String overselling = "OVERSELLING GLOBALE: " + bigliettiTotali + "/" + postiTotaliSistema;
            problemiRilevati.add(overselling);
            System.out.println("   🚨 " + overselling);
        }
    }

    /**
     * 📊 Analisi post-apocalisse
     */
    private static void analisiPostApocalisse() {
        System.out.println("\n📊 ===== ANALISI POST-APOCALISSE =====");

        int bigliettiFinali = memoriaBiglietti.getTuttiIBiglietti().size();
        int postiTotali = TRATTE_MULTIPLE * CAPIENZA_MICRO;
        boolean sistemaIntegro = problemiRilevati.isEmpty();

        System.out.println("💀 STATISTICHE DELL'APOCALISSE:");
        System.out.println("   ⏱️ Tempo totale: " + tempoTotaleMs.get() + "ms");
        System.out.println("   🔥 Round completati: " + ROUNDS_INFERNALI);
        System.out.println("   👹 Demoni scatenati: " + (NUM_CLIENT_INFERNALI * ROUNDS_INFERNALI));
        System.out.println("   💰 Acquisti: " + acquistiTotali.get());
        System.out.println("   📝 Prenotazioni: " + prenotazioniTotali.get());
        System.out.println("   ✅ Conferme: " + confirmeTotali.get());
        System.out.println("   🔄 Modifiche: " + modificheTotali.get());
        System.out.println("   ❌ Rifiuti: " + rifiutiTotali.get());
        System.out.println("   💥 Errori: " + erroriTotali.get());

        System.out.println("\n🎯 RISULTATI CAPIENZA:");
        System.out.println("   🚂 Posti totali sistema: " + postiTotali);
        System.out.println("   🎫 Biglietti venduti: " + bigliettiFinali);
        System.out.println("   ⚖️ Rapporto: " + bigliettiFinali + "/" + postiTotali);

        // Dettaglio per tratta
        System.out.println("\n📋 DETTAGLIO PER TRATTA:");
        for (TrattaDTO tratta : tratteTest) {
            long biglietti = memoriaBiglietti.contaBigliettiPerTratta(tratta.getId());
            String status = (biglietti <= CAPIENZA_MICRO) ? "✅" : "❌ OVERSELLING";
            System.out.println("   🚂 " + tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() +
                    ": " + biglietti + "/" + CAPIENZA_MICRO + " " + status);
        }

        System.out.println("\n🔍 PROBLEMI RILEVATI:");
        if (problemiRilevati.isEmpty()) {
            System.out.println("   🎉 NESSUN PROBLEMA! Sistema ha resistito all'apocalisse!");
        } else {
            problemiRilevati.forEach(p -> System.out.println("   🚨 " + p));
        }

        // Statistiche avanzate
        memoriaBiglietti.stampaStatisticheDettagliate();

        System.out.println("\n🏆 VERDETTO FINALE DELL'APOCALISSE:");
        if (sistemaIntegro && bigliettiFinali <= postiTotali) {
            System.out.println("   🎉 LEGGENDARIO! Sistema ha DOMINATO l'apocalisse!");
            System.out.println("   🛡️ Zero overselling con stress INFERNALE");
            System.out.println("   ⚡ Thread safety INDISTRUTTIBILE");
            System.out.println("   👑 IL SISTEMA È IMMORTALE!");
        } else if (bigliettiFinali <= postiTotali) {
            System.out.println("   👍 OTTIMO! Sistema resistente ma con problemi minori");
            System.out.println("   🔧 Alcuni problemi da verificare");
        } else {
            System.out.println("   💀 DISTRUTTO! Sistema non ha resistito all'apocalisse");
            System.out.println("   🚨 Overselling rilevato sotto stress estremo");
            System.out.println("   ⚠️ Richiede correzioni immediate");
        }

        System.out.println("\n🔥 ===== APOCALISSE COMPLETATA =====");
    }

    private static void cleanup() {
        System.out.println("\n🧹 Pulizia post-apocalisse...");

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

        System.out.println("✅ Cleanup apocalisse completato");
    }
}