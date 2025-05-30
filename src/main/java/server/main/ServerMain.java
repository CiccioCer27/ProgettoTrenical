package main;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import factory.TrattaFactoryConcrete;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.*;
import persistence.*;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Classe principale per avviare il server TreniCal
 */
public class ServerMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Avvio Server TreniCal");

        // 1️⃣ Avvia il server della banca
        Server bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato sulla porta " + BANCA_PORT);

        // 2️⃣ Prepara le componenti del server principale
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        // Genera alcune tratte di test se non ci sono
        if (memoriaTratte.getTutteTratte().isEmpty()) {
            TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
            for (int i = 1; i <= 3; i++) {
                List<Tratta> tratteGiorno = factory.generaTratte(LocalDate.now().plusDays(i));
                tratteGiorno.forEach(memoriaTratte::aggiungiTratta);
            }
            System.out.println("✅ Generate tratte di test per i prossimi 3 giorni");
        }

        // Event system
        EventDispatcher dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // Registra listeners
        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClienti));
        dispatcher.registra(new EventoLoggerListener());
        dispatcher.registra(new NotificaEventiListener(notificaDispatcher, memoriaTratte));

        // Client per comunicare con la banca
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);

        // Handler delle richieste
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, dispatcher, bancaClient
        );

        // Service implementation
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // 3️⃣ Avvia il server principale
        Server server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        System.out.println("✅ Server TreniCal avviato sulla porta " + SERVER_PORT);
        System.out.println("📊 Tratte disponibili: " + memoriaTratte.getTutteTratte().size());
        System.out.println("\n" + trenicalService.getStats());

        // 4️⃣ Gestisce lo shutdown graceful
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown signal ricevuto...");
            trenicalService.shutdown();
            server.shutdown();
            bancaServer.shutdown();

            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
                if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    bancaServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                bancaServer.shutdownNow();
                Thread.currentThread().interrupt();
            }

            System.out.println("🏁 Server terminato correttamente");
        }));

        // 5️⃣ Attendi input per terminare
        System.out.println("\n⌨️  Premi INVIO per terminare il server...");
        new Scanner(System.in).nextLine();

        // 6️⃣ Termina i server
        trenicalService.shutdown();
        server.shutdown();
        bancaServer.shutdown();

        if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
            server.shutdownNow();
        }
        if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
            bancaServer.shutdownNow();
        }

        System.out.println("🏁 Server terminato!");
    }
}