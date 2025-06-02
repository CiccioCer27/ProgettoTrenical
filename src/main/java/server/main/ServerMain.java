package main;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import factory.TrattaFactoryConcrete;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.GrpcNotificaDispatcher;
import persistence.*;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 🔒 SERVER MAIN THREAD-SAFE
 *
 * Versione semplificata che rimuove l'EventDispatcher
 * per eliminare le race conditions.
 */
public class ServerMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    public static void main(String[] args) throws Exception {
        System.out.println("🔒 AVVIO SERVER TRENICAL THREAD-SAFE");

        // 1️⃣ Server Banca
        Server bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato sulla porta " + BANCA_PORT);

        // 2️⃣ Componenti memoria THREAD-SAFE
        MemoriaBiglietti memoriaBiglietti = new MemoriaBiglietti();
        MemoriaClientiFedeli memoriaClienti = new MemoriaClientiFedeli();
        MemoriaTratte memoriaTratte = new MemoriaTratte();
        MemoriaPromozioni memoriaPromozioni = new MemoriaPromozioni();

        System.out.println("💾 Componenti memoria caricate (THREAD-SAFE):");
        System.out.println("   🎫 Biglietti: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🚂 Tratte: " + memoriaTratte.getTutteTratte().size());

        // Genera tratte se necessario
        if (memoriaTratte.getTutteTratte().isEmpty()) {
            System.out.println("📋 Generazione tratte iniziali...");
            TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
            for (int i = 1; i <= 3; i++) {
                List<Tratta> tratteGiorno = factory.generaTratte(LocalDate.now().plusDays(i));
                tratteGiorno.forEach(memoriaTratte::aggiungiTratta);
            }
            System.out.println("✅ Generate " + memoriaTratte.getTutteTratte().size() + " tratte");
        }

        // 3️⃣ Client banca e handler SEMPLIFICATO
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);

        // ⚠️ IMPORTANTE: Niente EventDispatcher = Niente race conditions
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient
        );

        // 4️⃣ Solo notifiche gRPC (senza eventi interni)
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // 5️⃣ Server principale
        Server server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        System.out.println("✅ Server TreniCal THREAD-SAFE avviato sulla porta " + SERVER_PORT);
        System.out.println("🔒 Modalità: CONTROLLO CAPIENZA ATOMICO ATTIVO");
        System.out.println("📊 " + memoriaBiglietti.getStatistiche());

        // 6️⃣ Shutdown graceful
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

        // 7️⃣ Attendi terminazione
        System.out.println("\n⌨️  Premi INVIO per terminare il server...");
        new Scanner(System.in).nextLine();

        // 8️⃣ Termina servers
        trenicalService.shutdown();
        server.shutdown();
        bancaServer.shutdown();

        if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
            server.shutdownNow();
        }
        if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
            bancaServer.shutdownNow();
        }

        System.out.println("🏁 Server THREAD-SAFE terminato!");
    }
}