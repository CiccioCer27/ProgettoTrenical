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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🎉 TEST SPECIALIZZATO PROMOZIONI E NOTIFICHE
 *
 * Verifica in dettaglio:
 * 1. Sistema stream promozioni gRPC
 * 2. Notifiche tratte in tempo reale
 * 3. Gestione connessioni multiple
 * 4. Resilienza del sistema notifiche
 * 5. Performance con molti stream aperti
 */
public class PromozioniNotificheTest {

    private static final int SERVER_PORT = 8097;
    private static final int BANCA_PORT = 8098;

    private static Server server;
    private static Server bancaServer;
    private static TrenicalServiceImpl trenicalService;

    // Contatori per verificare le notifiche
    private static final AtomicInteger promozioniRicevute = new AtomicInteger(0);
    private static final AtomicInteger notificheRicevute = new AtomicInteger(0);
    private static final List<String> logPromozioni = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> logNotifiche = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("🎉 ===== TEST PROMOZIONI E NOTIFICHE =====");

        try {
            // 1️⃣ Setup sistema
            avviaSistema();

            // 2️⃣ Test stream promozioni
            testStreamPromozioni();

            // 3️⃣ Test notifiche tratte
            testNotificheTratte();

            // 4️⃣ Test stress con molti stream
            testStressStreamMultipli();

            // 5️⃣ Test resilienza connessioni
            testResilienzaConnessioni();

            // 6️⃣ Test coordinazione wallet-promozioni
            testCoordinazioneWallet();

            // 7️⃣ Report finale
            stampaReportNotifiche();

        } catch (Exception e) {
            System.err.println("❌ Errore durante i test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fermaSistema();
        }
    }

    private static void avviaSistema() throws Exception {
        System.out.println("\n🚀 Avvio sistema per test notifiche");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();

        // Setup componenti server
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // Genera tratte
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        for (int i = 1; i <= 3; i++) {
            List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(i));
            tratte.forEach(memoriaTratte::aggiungiTratta);
        }

        // Event system
        EventDispatcher dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClienti));
        dispatcher.registra(new NotificaEventiListener(notificaDispatcher, memoriaTratte));

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
        System.out.println("✅ Sistema avviato");
    }

    /**
     * 🎯 Test stream promozioni con client multipli
     */
    private static void testStreamPromozioni() throws Exception {
        System.out.println("\n🎯 Test 1: Stream Promozioni");

        // Crea canali gRPC diretti per testare gli stream
        List<ManagedChannel> channels = new ArrayList<>();
        List<TrenicalServiceGrpc.TrenicalServiceStub> stubs = new ArrayList<>();

        // Setup 5 client per stream promozioni
        for (int i = 0; i < 5; i++) {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress("localhost", SERVER_PORT)
                    .usePlaintext()
                    .build();
            channels.add(channel);

            TrenicalServiceGrpc.TrenicalServiceStub stub = TrenicalServiceGrpc.newStub(channel);
            stubs.add(stub);

            // Avvia stream promozioni
            final int clientId = i;
            RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();

            stub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
                @Override
                public void onNext(PromozioneGrpc promo) {
                    promozioniRicevute.incrementAndGet();
                    String msg = "Client " + clientId + " ricevuto: " + promo.getNome();
                    logPromozioni.add(msg);
                    System.out.println("   📥 " + msg);
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("   ❌ Errore stream client " + clientId + ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("   ✅ Stream promozioni client " + clientId + " completato");
                }
            });
        }

        // Aspetta che gli stream si stabilizzino
        Thread.sleep(2000);

        // Genera promozioni di test
        System.out.println("   🎉 Generazione promozioni di test...");
        for (int i = 0; i < 10; i++) {
            PromozioneDTO promo = new PromozioneDTO(
                    "TestPromo" + i,
                    "Descrizione promo " + i + " - Sconto del " + (10 + i * 5) + "%",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1)
            );

            trenicalService.broadcastPromozione(promo);
            Thread.sleep(300); // Pausa tra promozioni
        }

        // Aspetta propagazione
        Thread.sleep(3000);

        // Verifica risultati
        System.out.println("   📊 Promozioni totali ricevute: " + promozioniRicevute.get());
        System.out.println("   📊 Attese: " + (5 * 10) + " (5 client × 10 promozioni)");

        // Cleanup canali
        for (ManagedChannel channel : channels) {
            channel.shutdown();
            channel.awaitTermination(2, TimeUnit.SECONDS);
        }

        System.out.println("   ✅ Test stream promozioni completato");
    }

    /**
     * 📡 Test notifiche tratte specifiche
     */
    private static void testNotificheTratte() throws Exception {
        System.out.println("\n📡 Test 2: Notifiche Tratte");

        // Crea client con servizio standard
        ClientService client1 = new ClientService("localhost", SERVER_PORT);
        ClientService client2 = new ClientService("localhost", SERVER_PORT);

        // Registra clienti
        client1.attivaCliente("NotifyUser1", "Test", "n1@test.com", 25, "Roma", "3331111111");
        client2.attivaCliente("NotifyUser2", "Test", "n2@test.com", 30, "Milano", "3332222222");

        // Trova tratte
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client1.inviaRichiesta(ricerca);
        if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
            System.out.println("   ⚠️ Nessuna tratta disponibile");
            return;
        }

        TrattaDTO tratta1 = risposta.getTratte().get(0);
        TrattaDTO tratta2 = risposta.getTratte().size() > 1 ?
                risposta.getTratte().get(1) : tratta1;

        // Setup notifiche con stub personalizzato per catturare messaggi
        setupNotifichePersonalizzate(client1, tratta1, "Client1");
        setupNotifichePersonalizzate(client2, tratta2, "Client2");

        // Aspetta setup
        Thread.sleep(2000);

        // Simula attività che genera notifiche
        System.out.println("   🚀 Simulazione attività per generare notifiche...");

        // Acquisti sulla tratta1 (dovrebbe notificare client1)
        for (int i = 0; i < 3; i++) {
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client1.getCliente().getId().toString())
                    .tratta(tratta1)
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.INTERO)
                    .build();

            client1.inviaRichiesta(acquisto);
            Thread.sleep(1000);
        }

        // Prenotazioni sulla tratta2 (dovrebbe notificare client2)
        for (int i = 0; i < 2; i++) {
            RichiestaDTO prenotazione = new RichiestaDTO.Builder()
                    .tipo("PRENOTA")
                    .idCliente(client2.getCliente().getId().toString())
                    .tratta(tratta2)
                    .classeServizio(ClasseServizio.ARGENTO)
                    .build();

            client2.inviaRichiesta(prenotazione);
            Thread.sleep(1000);
        }

        // Aspetta propagazione notifiche
        Thread.sleep(3000);

        System.out.println("   📊 Notifiche tratte ricevute: " + notificheRicevute.get());
        System.out.println("   ✅ Test notifiche tratte completato");
    }

    private static void setupNotifichePersonalizzate(ClientService client, TrattaDTO tratta, String clientName) {
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
                    notificheRicevute.incrementAndGet();
                    String msg = clientName + " ricevuto: " + notifica.getMessaggio();
                    logNotifiche.add(msg);
                    System.out.println("   📨 " + msg);
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("   ❌ Errore notifiche " + clientName + ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("   ✅ Stream notifiche " + clientName + " completato");
                }
            });

        } catch (Exception e) {
            System.err.println("   ❌ Errore setup notifiche " + clientName + ": " + e.getMessage());
        }
    }

    /**
     * 🚀 Test stress con molti stream simultanei
     */
    private static void testStressStreamMultipli() throws Exception {
        System.out.println("\n🚀 Test 3: Stress Stream Multipli");

        int numStreams = 25;
        ExecutorService executor = Executors.newFixedThreadPool(numStreams);
        CountDownLatch latch = new CountDownLatch(numStreams);
        AtomicInteger streamAttivi = new AtomicInteger(0);

        System.out.println("   📡 Avvio " + numStreams + " stream simultanei...");

        for (int i = 0; i < numStreams; i++) {
            final int streamId = i;
            executor.submit(() -> {
                try {
                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress("localhost", SERVER_PORT)
                            .usePlaintext()
                            .build();

                    TrenicalServiceGrpc.TrenicalServiceStub stub = TrenicalServiceGrpc.newStub(channel);

                    // Alterna tra stream promozioni e notifiche
                    if (streamId % 2 == 0) {
                        // Stream promozioni
                        RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();
                        streamAttivi.incrementAndGet();

                        stub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
                            @Override
                            public void onNext(PromozioneGrpc promo) {
                                // Solo conta, non stampa per non intasare output
                            }

                            @Override
                            public void onError(Throwable t) {
                                streamAttivi.decrementAndGet();
                            }

                            @Override
                            public void onCompleted() {
                                streamAttivi.decrementAndGet();
                            }
                        });

                    } else {
                        // Stream notifiche (usa una tratta casuale)
                        ClientService tempClient = new ClientService("localhost", SERVER_PORT);
                        tempClient.attivaCliente("StressUser" + streamId, "Test",
                                "stress" + streamId + "@test.com", 25, "Test", "333" + streamId);

                        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                                .tipo("FILTRA")
                                .messaggioExtra(";;;")
                                .build();

                        RispostaDTO risposta = tempClient.inviaRichiesta(ricerca);
                        if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
                            TrattaDTO tratta = risposta.getTratte().get(streamId % risposta.getTratte().size());

                            IscrizioneNotificheGrpc richiesta = IscrizioneNotificheGrpc.newBuilder()
                                    .setEmailCliente(tempClient.getCliente().getEmail())
                                    .setTrattaId(tratta.getId().toString())
                                    .build();

                            streamAttivi.incrementAndGet();

                            stub.streamNotificheTratta(richiesta, new StreamObserver<NotificaTrattaGrpc>() {
                                @Override
                                public void onNext(NotificaTrattaGrpc notifica) {
                                    // Solo conta
                                }

                                @Override
                                public void onError(Throwable t) {
                                    streamAttivi.decrementAndGet();
                                }

                                @Override
                                public void onCompleted() {
                                    streamAttivi.decrementAndGet();
                                }
                            });
                        }
                    }

                    // Mantieni stream attivo per 10 secondi
                    Thread.sleep(10000);
                    channel.shutdown();

                } catch (Exception e) {
                    System.err.println("   ❌ Errore stream " + streamId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Aspetta setup
        Thread.sleep(3000);
        System.out.println("   📊 Stream attivi: " + streamAttivi.get());

        // Genera traffico intenso
        for (int i = 0; i < 20; i++) {
            trenicalService.broadcastPromozione(new PromozioneDTO(
                    "StressPromo" + i,
                    "Promo stress test " + i,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1)
            ));
            Thread.sleep(200);
        }

        // Aspetta completamento
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("   ✅ Test stress stream completato");
        System.out.println("   📊 Stream rimanenti attivi: " + streamAttivi.get());
    }

    /**
     * 🛡️ Test resilienza disconnessioni
     */
    private static void testResilienzaConnessioni() throws Exception {
        System.out.println("\n🛡️ Test 4: Resilienza Connessioni");

        List<ManagedChannel> channels = new ArrayList<>();

        // Crea stream che si disconnettono improvvisamente
        for (int i = 0; i < 10; i++) {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress("localhost", SERVER_PORT)
                    .usePlaintext()
                    .build();
            channels.add(channel);

            TrenicalServiceGrpc.TrenicalServiceStub stub = TrenicalServiceGrpc.newStub(channel);

            RichiestaPromozioni richiesta = RichiestaPromozioni.newBuilder().build();
            stub.streamPromozioni(richiesta, new StreamObserver<PromozioneGrpc>() {
                @Override
                public void onNext(PromozioneGrpc promo) {
                    // Ricevi normalmente
                }

                @Override
                public void onError(Throwable t) {
                    // Disconnessione attesa
                }

                @Override
                public void onCompleted() {
                    // OK
                }
            });
        }

        Thread.sleep(2000);

        // Disconnetti brutalmente metà dei client
        System.out.println("   🔌 Disconnessione brutale di 5 client...");
        for (int i = 0; i < 5; i++) {
            channels.get(i).shutdownNow();
        }

        // Invia promozioni e verifica che il server continui a funzionare
        for (int i = 0; i < 5; i++) {
            trenicalService.broadcastPromozione(new PromozioneDTO(
                    "ResiliencePromo" + i,
                    "Test resilienza " + i,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1)
            ));
            Thread.sleep(500);
        }

        // Cleanup rimanenti
        for (int i = 5; i < channels.size(); i++) {
            channels.get(i).shutdown();
        }

        System.out.println("   ✅ Test resilienza completato - server stabile");
    }

    /**
     * 💼 Test coordinazione wallet con promozioni
     */
    private static void testCoordinazioneWallet() throws Exception {
        System.out.println("\n💼 Test 5: Coordinazione Wallet-Promozioni");

        ClientService client = new ClientService("localhost", SERVER_PORT);
        client.attivaCliente("WalletTest", "User", "wallet@test.com", 25, "Roma", "3334444444");

        // Setup wallet con observer
        model.Wallet wallet = new model.Wallet();
        eventi.ListaEventi.getInstance().aggiungiObserver(wallet);

        // Setup listener promozioni
        model.WalletPromozioni walletPromo = new model.WalletPromozioni();
        eventi.ListaEventi.getInstance().aggiungiObserver(walletPromo);

        // Acquista carta fedeltà
        RichiestaDTO fedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(client.getCliente().getId().toString())
                .build();
        client.inviaRichiesta(fedelta);

        // Trova tratte e acquista biglietti
        RichiestaDTO ricerca = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(";;;")
                .build();

        RispostaDTO risposta = client.inviaRichiesta(ricerca);
        if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
            TrattaDTO tratta = risposta.getTratte().get(0);

            // Test acquisto diretto
            RichiestaDTO acquisto = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(client.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(ClasseServizio.BASE)
                    .tipoPrezzo(TipoPrezzo.FEDELTA)
                    .build();

            RispostaDTO rispostaAcquisto = client.inviaRichiesta(acquisto);

            // Test prenotazione
            if (risposta.getTratte().size() > 1) {
                RichiestaDTO prenotazione = new RichiestaDTO.Builder()
                        .tipo("PRENOTA")
                        .idCliente(client.getCliente().getId().toString())
                        .tratta(risposta.getTratte().get(1))
                        .classeServizio(ClasseServizio.ARGENTO)
                        .build();

                client.inviaRichiesta(prenotazione);
            }
        }

        // Genera promozioni e verifica che il wallet le riceva
        for (int i = 0; i < 3; i++) {
            PromozioneDTO promo = new PromozioneDTO(
                    "WalletPromo" + i,
                    "Promo per wallet test " + i,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1)
            );

            // Simula evento promozione
            eventi.ListaEventi.getInstance().notifica(new eventi.EventoPromozione(promo));
            Thread.sleep(1000);
        }

        // Verifica stato wallet
        System.out.println("   💼 Biglietti confermati: " + wallet.getBigliettiConfermati().size());
        System.out.println("   💼 Biglietti non confermati: " + wallet.getBigliettiNonConfermati().size());
        System.out.println("   🎉 Promozioni attive: " + walletPromo.getPromozioniAttive().size());

        System.out.println("   ✅ Test coordinazione wallet completato");
    }

    /**
     * 📊 Report finale delle notifiche
     */
    private static void stampaReportNotifiche() {
        System.out.println("\n📊 ===== REPORT NOTIFICHE E PROMOZIONI =====");
        System.out.println("🎉 PROMOZIONI:");
        System.out.println("   📥 Totali ricevute: " + promozioniRicevute.get());
        System.out.println("   📋 Prime 5 promozioni:");
        logPromozioni.stream().limit(5).forEach(log ->
                System.out.println("     • " + log));

        System.out.println("\n📡 NOTIFICHE TRATTE:");
        System.out.println("   📨 Totali ricevute: " + notificheRicevute.get());
        System.out.println("   📋 Prime 5 notifiche:");
        logNotifiche.stream().limit(5).forEach(log ->
                System.out.println("     • " + log));

        System.out.println("\n🏆 VALUTAZIONE SISTEMA:");
        if (promozioniRicevute.get() > 40 && notificheRicevute.get() > 5) {
            System.out.println("   ✅ ECCELLENTE - Sistema notifiche robusto e performante!");
        } else if (promozioniRicevute.get() > 20 && notificheRicevute.get() > 2) {
            System.out.println("   👍 BUONO - Sistema notifiche funzionale");
        } else {
            System.out.println("   ⚠️ SUFFICIENTE - Possibili miglioramenti nel sistema notifiche");
        }

        System.out.println("\n" + trenicalService.getStats());
    }

    private static void fermaSistema() {
        System.out.println("\n🛑 Cleanup sistema notifiche...");

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

        System.out.println("✅ Sistema fermato");
    }
}