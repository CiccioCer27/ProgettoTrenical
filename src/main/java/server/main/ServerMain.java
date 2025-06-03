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
 * 🔒 SERVER MAIN - CON SHUTDOWN OTTIMIZZATO MemoriaOsservatori
 *
 * AGGIORNAMENTO: Gestione corretta shutdown di tutte le componenti di memoria
 */
public class ServerMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    // ✅ RIFERIMENTI GLOBALI per shutdown graceful
    private static MemoriaBiglietti memoriaBiglietti;
    private static MemoriaClientiFedeli memoriaClienti;
    private static MemoriaTratte memoriaTratte;
    private static MemoriaPromozioni memoriaPromozioni;
    private static MemoriaOsservatori memoriaOsservatori; // ✅ AGGIUNTO
    private static TrenicalServiceImpl trenicalService;

    public static void main(String[] args) throws Exception {
        System.out.println("🔒 AVVIO SERVER TRENICAL con SHUTDOWN OTTIMIZZATO");

        // 1️⃣ Server Banca
        Server bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato sulla porta " + BANCA_PORT);

        // 2️⃣ Componenti memoria THREAD-SAFE
        System.out.println("💾 Inizializzazione componenti memoria ottimizzate...");

        memoriaBiglietti = new MemoriaBiglietti();
        memoriaClienti = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        memoriaPromozioni = new MemoriaPromozioni();
        memoriaOsservatori = new MemoriaOsservatori(); // ✅ INIZIALIZZAZIONE OTTIMIZZATA

        System.out.println("💾 Componenti memoria caricate (OTTIMIZZATE):");
        System.out.println("   🎫 Biglietti: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🚂 Tratte: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎉 Promozioni: " + memoriaPromozioni.getPromozioniAttive().size());
        System.out.println("   👁️ " + memoriaOsservatori.getStatistiche()); // ✅ STATISTICHE OTTIMIZZATE

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
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClienti, memoriaTratte, bancaClient, memoriaOsservatori
        );

        // 4️⃣ Dispatcher per notifiche gRPC
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // 5️⃣ Servizio gRPC
        trenicalService = new TrenicalServiceImpl(
                notificaDispatcher, handler, memoriaPromozioni
        );

        // 6️⃣ Observer Pattern con Dependency Injection
        System.out.println("🔄 Configurazione Observer Pattern per broadcast promozioni...");

        NotificaEventiListener notificaListener = new NotificaEventiListener(
                notificaDispatcher,
                memoriaTratte,
                memoriaBiglietti,
                trenicalService
        );
        ListaEventiS.getInstance().aggiungi(notificaListener);

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
        System.out.println("⚡ MemoriaOsservatori: OTTIMIZZATA UUID");
        System.out.println("📊 " + memoriaBiglietti.getStatistiche());
        System.out.println("📊 " + trenicalService.getStreamStats());

        // 8️⃣ ✅ SHUTDOWN HOOK OTTIMIZZATO
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown signal ricevuto...");

            // ✅ SHUTDOWN COMPONENTI NELL'ORDINE CORRETTO
            shutdownGracefully(trenicalService, server, bancaServer);
        }));

        // 9️⃣ Attendi terminazione
        System.out.println("\n⌨️  Premi INVIO per terminare il server...");
        System.out.println("🎯 Prova a creare promozioni da console admin - verranno broadcasted!");
        new Scanner(System.in).nextLine();

        // 🔟 ✅ SHUTDOWN MANUALE OTTIMIZZATO
        shutdownGracefully(trenicalService, server, bancaServer);

        System.out.println("🏁 Server con memoria ottimizzata terminato!");
    }

    /**
     * ✅ SHUTDOWN GRACEFUL OTTIMIZZATO per tutte le componenti
     */
    private static void shutdownGracefully(TrenicalServiceImpl trenicalService,
                                           Server server,
                                           Server bancaServer) {
        System.out.println("🛑 Avvio shutdown graceful ottimizzato...");

        try {
            // ✅ STEP 1: Shutdown servizi gRPC
            if (trenicalService != null) {
                System.out.println("📡 Shutdown TrenicalService...");
                trenicalService.shutdown();
                System.out.println("✅ TrenicalService terminato");
            }

            // ✅ STEP 2: Shutdown server principali
            if (server != null) {
                System.out.println("🚪 Shutdown Server TreniCal...");
                server.shutdown();
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
                System.out.println("✅ Server TreniCal terminato");
            }

            if (bancaServer != null) {
                System.out.println("🏦 Shutdown Server Banca...");
                bancaServer.shutdown();
                if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    bancaServer.shutdownNow();
                }
                System.out.println("✅ Server Banca terminato");
            }

            // ✅ STEP 3: SHUTDOWN MEMORIA OTTIMIZZATA (ORDINE IMPORTANTE!)
            System.out.println("💾 Shutdown componenti memoria...");

            // MemoriaOsservatori per PRIMA (ha salvataggio asincrono)
            if (memoriaOsservatori != null) {
                System.out.println("👁️ Shutdown MemoriaOsservatori ottimizzata...");
                memoriaOsservatori.shutdown();
                System.out.println("✅ MemoriaOsservatori terminata con salvataggio finale");
            }

            // Altre memorie (hanno salvataggio sincrono)
            if (memoriaBiglietti != null) {
                System.out.println("🎫 Salvataggio finale biglietti...");
                memoriaBiglietti.salva();
                System.out.println("✅ MemoriaBiglietti salvata");
            }

            if (memoriaTratte != null) {
                // MemoriaTratte non ha shutdown esplicito, salvataggio automatico
                System.out.println("✅ MemoriaTratte - salvataggio automatico");
            }

            if (memoriaPromozioni != null) {
                // MemoriaPromozioni non ha shutdown esplicito, salvataggio automatico
                System.out.println("✅ MemoriaPromozioni - salvataggio automatico");
            }

            // ✅ STEP 4: STATISTICHE FINALI
            System.out.println("\n📊 STATISTICHE FINALI SHUTDOWN:");
            if (memoriaBiglietti != null) {
                System.out.println("   🎫 " + memoriaBiglietti.getStatistiche());
            }
            if (memoriaOsservatori != null) {
                System.out.println("   👁️ " + memoriaOsservatori.getStatistiche());
            }

            System.out.println("✅ Shutdown graceful completato con successo!");

        } catch (InterruptedException e) {
            System.err.println("⚠️ Interruzione durante shutdown graceful");

            // ✅ SHUTDOWN FORZATO in caso di interruzione
            if (server != null) server.shutdownNow();
            if (bancaServer != null) bancaServer.shutdownNow();

            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Errore durante shutdown graceful: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
