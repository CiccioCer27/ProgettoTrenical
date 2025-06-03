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
 * 🔒 SERVER MAIN - AGGIORNATO per Broadcast Promozioni
 *
 * NUOVO: Dependency injection corretta per broadcast promozioni reali
 * MIGLIORAMENTO: TrenicalServiceImpl integrato con sistema eventi server
 */
public class ServerMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    public static void main(String[] args) throws Exception {
        System.out.println("🔒 AVVIO SERVER TRENICAL con BROADCAST PROMOZIONI REALE");

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
        MemoriaOsservatori memoriaOsservatori = new MemoriaOsservatori();

        System.out.println("💾 Componenti memoria caricate:");
        System.out.println("   🎫 Biglietti: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🚂 Tratte: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎉 Promozioni: " + memoriaPromozioni.getPromozioniAttive().size());

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

        // 3️⃣ Client banca e handler
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        // ✅ CORREZIONE: Rimossa parentesi extra
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient, memoriaOsservatori
        );

        // 4️⃣ Dispatcher per notifiche gRPC
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // 5️⃣ ✅ SERVIZIO gRPC (PRIMA della configurazione listeners!)
        TrenicalServiceImpl trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // 6️⃣ ✅ OBSERVER PATTERN con Dependency Injection corretta
        System.out.println("🔄 Configurazione Observer Pattern per broadcast promozioni...");

        // ✅ NUOVO: Listener con riferimento al servizio gRPC per broadcast
        NotificaEventiListener notificaListener = new NotificaEventiListener(
                notificaDispatcher,
                memoriaTratte,
                memoriaBiglietti,  // ✅ AGGIUNTO: MemoriaBiglietti mancava
                trenicalService   // ✅ INJECTION per broadcast promozioni!
        );
        ListaEventiS.getInstance().aggiungi(notificaListener);

        // Listener per audit/logging
        EventoLoggerListener loggerListener = new EventoLoggerListener();
        ListaEventiS.getInstance().aggiungi(loggerListener);

        System.out.println("   ✅ NotificaEventiListener registrato (con broadcast gRPC)");
        System.out.println("   ✅ EventoLoggerListener registrato (audit/logging)");

        // 7️⃣ Server principale
        Server server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();

        System.out.println("✅ Server TreniCal avviato sulla porta " + SERVER_PORT);
        System.out.println("🔒 Modalità: CONTROLLO CAPIENZA ATOMICO ATTIVO");
        System.out.println("📡 Broadcast Promozioni: ATTIVO");
        System.out.println("📊 " + memoriaBiglietti.getStatistiche());
        System.out.println("📊 " + trenicalService.getStreamStats());

        // 8️⃣ Shutdown graceful
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

        // 9️⃣ Attendi terminazione
        System.out.println("\n⌨️  Premi INVIO per terminare il server...");
        System.out.println("🎯 Prova a creare promozioni da console admin - verranno broadcasted!");
        new Scanner(System.in).nextLine();

        // 🔟 Termina servers
        trenicalService.shutdown();
        server.shutdown();
        bancaServer.shutdown();

        if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
            server.shutdownNow();
        }
        if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
            bancaServer.shutdownNow();
        }

        System.out.println("🏁 Server con broadcast promozioni terminato!");
    }
}