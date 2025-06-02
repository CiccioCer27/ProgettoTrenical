package main;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import factory.TrattaFactoryConcrete;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.GrpcNotificaDispatcher;
import observer.NotificaEventiListener;
import observer.EventoLoggerListener;
import eventi.ListaEventiS;
import persistence.*;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 🔒 SERVER MAIN THREAD-SAFE - OBSERVER PATTERN REFACTORED
 *
 * DESIGN DECISION: Observer Pattern utilizzato SOLO per cross-cutting concerns:
 * - Notifiche gRPC inter-servizio (NotificaEventiListener)
 * - Audit e logging (EventoLoggerListener)
 *
 * La persistenza core è responsabilità diretta dei Command per evitare
 * duplicazione e garantire atomicità delle operazioni.
 */
public class ServerMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    public static void main(String[] args) throws Exception {
        System.out.println("🔒 AVVIO SERVER TRENICAL THREAD-SAFE (Observer Refactored)");

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

        // 3️⃣ Client banca e handler SEMPLIFICATO (SENZA EventDispatcher complesso)
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);

        // ✅ REFACTORED: Handler SENZA EventDispatcher per persistenza
        // I Command si prendono la responsabilità diretta della persistenza
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient
        );

        // 4️⃣ Observer Pattern SOLO per Cross-Cutting Concerns
        System.out.println("🔄 Configurazione Observer Pattern (Solo Cross-Cutting Concerns)...");

        // Dispatcher per notifiche gRPC
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // ✅ MANTIENI: Observer per notifiche inter-servizio
        NotificaEventiListener notificaListener = new NotificaEventiListener(
                notificaDispatcher, memoriaTratte);
        ListaEventiS.getInstance().aggiungi(notificaListener);

        // ✅ MANTIENI: Observer per audit/logging
        EventoLoggerListener loggerListener = new EventoLoggerListener();
        ListaEventiS.getInstance().aggiungi(loggerListener);

        System.out.println("   ✅ NotificaEventiListener registrato (gRPC cross-domain)");
        System.out.println("   ✅ EventoLoggerListener registrato (audit/logging)");
        System.out.println("   ❌ Listener per persistenza core RIMOSSI (responsabilità Command)");

        // 5️⃣ Servizio gRPC
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // 6️⃣ Server principale
        Server server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        System.out.println("✅ Server TreniCal THREAD-SAFE avviato sulla porta " + SERVER_PORT);
        System.out.println("🔒 Modalità: CONTROLLO CAPIENZA ATOMICO ATTIVO");
        System.out.println("🎯 Observer Pattern: SOLO Cross-Cutting Concerns");
        System.out.println("📊 " + memoriaBiglietti.getStatistiche());

        // 7️⃣ Shutdown graceful
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

        // 8️⃣ Attendi terminazione
        System.out.println("\n⌨️  Premi INVIO per terminare il server...");
        new Scanner(System.in).nextLine();

        // 9️⃣ Termina servers
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
        System.out.println("🎯 Observer Pattern Refactored: SUCCESS!");
    }
}