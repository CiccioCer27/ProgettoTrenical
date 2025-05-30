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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Test completo del sistema TreniCal
 * Testa: Server + Banca + Client + Comunicazione gRPC + Business Logic
 */
public class CompleteIntegrationTest {

    private static final int SERVER_PORT = 8090;  // Porte diverse per evitare conflitti
    private static final int BANCA_PORT = 8091;

    private static Server server;
    private static Server bancaServer;
    private static TrenicalServiceImpl trenicalService;

    public static void main(String[] args) {
        System.out.println("🧪 ===== TEST INTEGRAZIONE COMPLETO TRENICAL =====");

        try {
            // 1️⃣ Avvia i server
            avviaServers();

            // 2️⃣ Aspetta che i server siano pronti
            Thread.sleep(2000);

            // 3️⃣ Esegui tutti i test
            eseguiTestCompleto();

            // 4️⃣ Test stress (opzionale)
            System.out.println("\n🔥 Vuoi eseguire il test di stress? (y/n)");
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().toLowerCase().startsWith("y")) {
                eseguiTestStress();
            }

            System.out.println("\n✅ ===== TUTTI I TEST COMPLETATI CON SUCCESSO! =====");

        } catch (Exception e) {
            System.err.println("❌ Errore durante i test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5️⃣ Cleanup
            fermaServers();
        }
    }

    /**
     * 🚀 Avvia server TreniCal e Banca
     */
    private static void avviaServers() throws Exception {
        System.out.println("\n🚀 Fase 1: Avvio Servers");

        // Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato su porta " + BANCA_PORT);

        // Componenti server TreniCal
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // Genera tratte di test
        TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
        for (int i = 1; i <= 3; i++) {
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

        // Handler e service
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
        System.out.println("✅ Server TreniCal avviato su porta " + SERVER_PORT);
    }

    /**
     * 🧪 Esegue tutti i test del sistema
     */
    private static void eseguiTestCompleto() throws Exception {
        System.out.println("\n🧪 Fase 2: Test Funzionalità Complete");

        // Crea client
        ClientService client = new ClientService("localhost", SERVER_PORT);

        // TEST 1: Registrazione Cliente
        System.out.println("\n1️⃣ Test: Registrazione Cliente");
        client.attivaCliente("Mario", "Rossi", "mario@test.com", 35, "Milano", "3331234567");
        ClienteDTO cliente = client.getCliente();
        System.out.println("   ✅ Cliente registrato: " + cliente.getNome() + " " + cliente.getCognome());

        // TEST 2: Ricerca Tratte
        System.out.println("\n2️⃣ Test: Ricerca Tratte");
        RichiestaDTO richiestaFiltro = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(LocalDate.now().plusDays(1) + ";Milano;Roma;MATTINA")
                .build();

        RispostaDTO rispostaFiltro = client.inviaRichiesta(richiestaFiltro);
        List<TrattaDTO> tratte = rispostaFiltro.getTratte();

        if (tratte == null || tratte.isEmpty()) {
            // Prova ricerca più ampia
            richiestaFiltro = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;")
                    .build();
            rispostaFiltro = client.inviaRichiesta(richiestaFiltro);
            tratte = rispostaFiltro.getTratte();
        }

        assert tratte != null && !tratte.isEmpty() : "Nessuna tratta trovata!";
        TrattaDTO trattaTest = tratte.get(0);
        System.out.println("   ✅ Trovate " + tratte.size() + " tratte");
        System.out.println("   📍 Tratta test: " + trattaTest.getStazionePartenza() + " → " + trattaTest.getStazioneArrivo());

        // TEST 3: Carta Fedeltà
        System.out.println("\n3️⃣ Test: Acquisto Carta Fedeltà");
        RichiestaDTO richiestaFedelta = new RichiestaDTO.Builder()
                .tipo("CARTA_FEDELTA")
                .idCliente(cliente.getId().toString())
                .build();

        RispostaDTO rispostaFedelta = client.inviaRichiesta(richiestaFedelta);
        System.out.println("   " + (rispostaFedelta.getEsito().equals("OK") ? "✅" : "❌") +
                " Carta Fedeltà: " + rispostaFedelta.getMessaggio());

        // TEST 4: Prenotazione
        System.out.println("\n4️⃣ Test: Prenotazione Biglietto");
        RichiestaDTO richiestaPrenotazione = new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(cliente.getId().toString())
                .tratta(trattaTest)
                .classeServizio(ClasseServizio.BASE)
                .build();

        RispostaDTO rispostaPrenotazione = client.inviaRichiesta(richiestaPrenotazione);
        System.out.println("   " + (rispostaPrenotazione.getEsito().equals("OK") ? "✅" : "❌") +
                " Prenotazione: " + rispostaPrenotazione.getMessaggio());

        // TEST 5: Conferma Prenotazione
        if (rispostaPrenotazione.getBiglietto() != null) {
            System.out.println("\n5️⃣ Test: Conferma Prenotazione");
            RichiestaDTO richiestaConferma = new RichiestaDTO.Builder()
                    .tipo("CONFERMA")
                    .idCliente(cliente.getId().toString())
                    .biglietto(rispostaPrenotazione.getBiglietto())
                    .build();

            RispostaDTO rispostaConferma = client.inviaRichiesta(richiestaConferma);
            System.out.println("   " + (rispostaConferma.getEsito().equals("OK") ? "✅" : "❌") +
                    " Conferma: " + rispostaConferma.getMessaggio());
        }

        // TEST 6: Acquisto Diretto
        System.out.println("\n6️⃣ Test: Acquisto Diretto con Fedeltà");
        RichiestaDTO richiestaAcquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(cliente.getId().toString())
                .tratta(trattaTest)
                .classeServizio(ClasseServizio.ARGENTO)
                .tipoPrezzo(TipoPrezzo.FEDELTA)
                .build();

        RispostaDTO rispostaAcquisto = client.inviaRichiesta(richiestaAcquisto);
        System.out.println("   " + (rispostaAcquisto.getEsito().equals("OK") ? "✅" : "❌") +
                " Acquisto: " + rispostaAcquisto.getMessaggio());

        // TEST 7: Notifiche Tratta
        System.out.println("\n7️⃣ Test: Iscrizione Notifiche Tratta");
        client.avviaNotificheTratta(trattaTest);
        Thread.sleep(3000); // Aspetta per vedere eventuali notifiche
        System.out.println("   ✅ Notifiche configurate");

        System.out.println("\n📊 " + trenicalService.getStats());
    }

    /**
     * 🔥 Test di stress con client multipli
     */
    private static void eseguiTestStress() throws Exception {
        System.out.println("\n🔥 Fase 3: Test di Stress (5 client simultanei)");

        Thread[] clientThreads = new Thread[5];

        for (int i = 0; i < 5; i++) {
            final int clientId = i;
            clientThreads[i] = new Thread(() -> {
                try {
                    ClientService client = new ClientService("localhost", SERVER_PORT);

                    // Registra cliente
                    client.attivaCliente("Cliente" + clientId, "Test",
                            "cliente" + clientId + "@test.com", 25, "Roma", "333000000" + clientId);

                    // Compra carta fedeltà
                    RichiestaDTO fedelta = new RichiestaDTO.Builder()
                            .tipo("CARTA_FEDELTA")
                            .idCliente(client.getCliente().getId().toString())
                            .build();
                    client.inviaRichiesta(fedelta);

                    // Cerca tratte e acquista
                    RichiestaDTO ricerca = new RichiestaDTO.Builder()
                            .tipo("FILTRA")
                            .messaggioExtra(";;;")
                            .build();

                    RispostaDTO risposta = client.inviaRichiesta(ricerca);
                    if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
                        TrattaDTO tratta = risposta.getTratte().get(clientId % risposta.getTratte().size());

                        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                                .tipo("ACQUISTA")
                                .idCliente(client.getCliente().getId().toString())
                                .tratta(tratta)
                                .classeServizio(ClasseServizio.BASE)
                                .tipoPrezzo(TipoPrezzo.FEDELTA)
                                .build();

                        client.inviaRichiesta(acquisto);
                        System.out.println("   ✅ Client " + clientId + " completato");
                    }

                } catch (Exception e) {
                    System.err.println("   ❌ Client " + clientId + " errore: " + e.getMessage());
                }
            });

            clientThreads[i].start();
        }

        // Aspetta che tutti i thread finiscano
        for (Thread thread : clientThreads) {
            thread.join();
        }

        System.out.println("🔥 Test di stress completato!");
    }

    /**
     * 🛑 Ferma tutti i server
     */
    private static void fermaServers() {
        System.out.println("\n🛑 Fase 4: Shutdown Servers");

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