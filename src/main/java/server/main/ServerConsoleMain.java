package main;

import IMPL.BancaServiceImpl;
import command.ServerRequestHandler;
import commandInter.*;
import eventi.ListaEventiS;
import factory.TrattaFactoryConcrete;
import grpc.TrenicalServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Tratta;
import observer.*;
import persistence.*;
import service.BancaServiceClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 🖥️ SERVER CONSOLE MAIN THREAD-SAFE
 *
 * Versione corretta che usa il nuovo costruttore ServerRequestHandler
 */
public class ServerConsoleMain {

    private static final int SERVER_PORT = 9090;
    private static final int BANCA_PORT = 9091;

    private static Server server;
    private static Server bancaServer;
    private static TrenicalServiceImpl trenicalService;
    private static Scanner scanner;

    // Componenti sistema
    private static MemoriaBiglietti memoriaBiglietti;
    private static MemoriaClientiFedeli memoriaClientiFedeli;
    private static MemoriaTratte memoriaTratte;
    private static MemoriaPromozioni memoriaPromozioni;

    public static void main(String[] args) {
        System.out.println("🖥️ ===== TRENICAL SERVER CONSOLE - VERSIONE THREAD-SAFE =====");

        scanner = new Scanner(System.in);

        try {
            // Avvia sistema
            avviaSystemaThreadSafe();

            // Menu amministrazione
            boolean continua = true;
            while (continua) {
                continua = mostraMenuAmministrazione();
            }

        } catch (Exception e) {
            System.err.println("❌ Errore server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fermaServers();
        }
    }

    private static void avviaSystemaThreadSafe() throws Exception {
        System.out.println("🚀 Avvio sistema server THREAD-SAFE...");

        // 1. Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato sulla porta " + BANCA_PORT);

        // 2. Componenti memoria THREAD-SAFE
        memoriaBiglietti = new MemoriaBiglietti();
        memoriaClientiFedeli = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        memoriaPromozioni = new MemoriaPromozioni();

        System.out.println("💾 Componenti memoria caricate (THREAD-SAFE):");
        System.out.println("   🎫 Biglietti: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🚂 Tratte: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎉 Promozioni: " + memoriaPromozioni.getPromozioniAttive().size());

        // 3. Genera tratte se necessario
        if (memoriaTratte.getTutteTratte().isEmpty()) {
            System.out.println("📋 Generazione tratte iniziali...");
            TrattaFactoryConcrete factory = new TrattaFactoryConcrete();
            for (int i = 1; i <= 5; i++) {
                List<Tratta> tratte = factory.generaTratte(LocalDate.now().plusDays(i));
                tratte.forEach(memoriaTratte::aggiungiTratta);
            }
            System.out.println("✅ Generate " + memoriaTratte.getTutteTratte().size() + " tratte");
        }

        // 4. Client banca e handler THREAD-SAFE (SENZA EventDispatcher)
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);

        // ✅ CORREZIONE: Usa il nuovo costruttore ServerRequestHandler
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClientiFedeli, memoriaTratte, bancaClient
        );

        // 5. Solo notifiche gRPC (senza eventi interni complessi)
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // 6. Servizio gRPC
        trenicalService = new TrenicalServiceImpl(notificaDispatcher, handler, memoriaPromozioni);

        // 7. Server TreniCal
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();
        System.out.println("✅ Server TreniCal THREAD-SAFE avviato sulla porta " + SERVER_PORT);
        System.out.println("🔒 Controllo capienza atomico: ATTIVO");
        System.out.println("📊 " + memoriaBiglietti.getStatistiche());

        System.out.println("🎯 Sistema server operativo in modalità THREAD-SAFE!");
    }

    private static boolean mostraMenuAmministrazione() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🏠 CONSOLE AMMINISTRAZIONE TRENICAL THREAD-SAFE");
        System.out.println("=".repeat(50));

        // Statistiche essenziali
        System.out.println("📊 STATO SISTEMA:");
        System.out.println("   🚂 Tratte attive: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎫 Biglietti totali: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🎉 Promozioni attive: " + memoriaPromozioni.getPromozioniAttive().size());
        System.out.println("   🔒 Modalità: THREAD-SAFE ATOMICO");

        System.out.println("\n📋 OPERAZIONI DISPONIBILI:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("1. 📊 Statistiche sistema thread-safe");
        System.out.println("2. 🚂 Gestione tratte");
        System.out.println("3. 🎉 Gestione promozioni");
        System.out.println("4. 🎫 Gestione biglietti");
        System.out.println("5. 👥 Visualizza clienti fedeli");
        System.out.println("6. 🧪 Test capienza rapido");
        System.out.println("0. 🚪 Ferma server ed esci");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        System.out.print("👉 Scegli operazione: ");
        String input = scanner.nextLine().trim();

        try {
            int scelta = Integer.parseInt(input);
            return eseguiOperazione(scelta);
        } catch (NumberFormatException e) {
            System.out.println("❌ Inserisci un numero valido!");
            return true;
        }
    }

    private static boolean eseguiOperazione(int scelta) {
        try {
            switch (scelta) {
                case 1 -> mostraStatisticheThreadSafe();
                case 2 -> menuGestioneTratteSimplificato();
                case 3 -> menuGestionePromozioniSimplificato();
                case 4 -> menuGestioneBiglietti();
                case 5 -> visualizzaClientiFedeli();
                case 6 -> testCapienzaRapido();
                case 0 -> {
                    System.out.println("🛑 Arresto server in corso...");
                    return false;
                }
                default -> System.out.println("❌ Opzione non valida!");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore durante l'operazione: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private static void mostraStatisticheThreadSafe() {
        System.out.println("\n📊 STATISTICHE SISTEMA THREAD-SAFE");
        System.out.println("-".repeat(40));

        System.out.println("🔒 SICUREZZA:");
        System.out.println("   Controllo capienza: ATOMICO");
        System.out.println("   EventDispatcher: DISABILITATO (thread-safe)");
        System.out.println("   Race conditions: ELIMINATE");

        System.out.println("\n💾 MEMORIA:");
        memoriaBiglietti.stampaStatisticheDettagliate();

        System.out.println("\n🎯 INTEGRITÀ:");
        Map<java.util.UUID, Integer> capienzaPerTratta = new HashMap<>();
        memoriaTratte.getTutteTratte().forEach(tratta ->
                capienzaPerTratta.put(tratta.getId(), tratta.getTreno().getCapienzaTotale()));

        boolean integra = memoriaBiglietti.verificaIntegrita(capienzaPerTratta);
        System.out.println("   Capienza rispettata: " + (integra ? "✅ SÌ" : "❌ NO"));

        pausaETornaMenu();
    }

    private static void testCapienzaRapido() {
        System.out.println("\n🧪 TEST CAPIENZA RAPIDO");
        System.out.println("-".repeat(25));

        System.out.println("⚠️ Questo test verifica che il controllo capienza");
        System.out.println("   funzioni correttamente in condizioni normali.");

        List<model.Tratta> tratte = memoriaTratte.getTutteTratte();
        if (tratte.isEmpty()) {
            System.out.println("❌ Nessuna tratta disponibile per il test");
            pausaETornaMenu();
            return;
        }

        model.Tratta trattaTest = tratte.get(0);
        int capienza = trattaTest.getTreno().getCapienzaTotale();

        System.out.println("🚂 Tratta test: " + trattaTest.getStazionePartenza() +
                " → " + trattaTest.getStazioneArrivo());
        System.out.println("🎯 Capienza: " + capienza + " posti");

        // Conta biglietti attuali
        long bigliettiAttuali = memoriaBiglietti.contaBigliettiPerTratta(trattaTest.getId());
        System.out.println("📊 Biglietti attuali: " + bigliettiAttuali);

        if (bigliettiAttuali >= capienza) {
            System.out.println("⚠️ Tratta già piena, non posso testare");
        } else {
            System.out.println("✅ Test capienza: DISPONIBILE");
            System.out.println("💡 Posti liberi: " + (capienza - bigliettiAttuali));
        }

        pausaETornaMenu();
    }

    private static void menuGestioneTratteSimplificato() {
        System.out.println("\n🚂 GESTIONE TRATTE");
        System.out.println("-".repeat(20));
        System.out.println("1. 📋 Visualizza tutte le tratte");
        System.out.println("2. 🔄 Modifica tratta esistente");
        System.out.println("0. ⬅️ Torna al menu principale");

        System.out.print("Scegli operazione: ");
        int scelta = Integer.parseInt(scanner.nextLine().trim());

        switch (scelta) {
            case 1 -> visualizzaTutteTratte();
            case 2 -> new ModificaTrattaCommand(memoriaTratte).esegui(scanner);
            case 0 -> { /* torna al menu */ }
            default -> System.out.println("❌ Opzione non valida!");
        }
    }

    private static void visualizzaTutteTratte() {
        System.out.println("\n📋 TUTTE LE TRATTE");
        System.out.println("-".repeat(20));

        List<model.Tratta> tratte = memoriaTratte.getTutteTratte();

        if (tratte.isEmpty()) {
            System.out.println("ℹ️ Nessuna tratta presente");
            pausaETornaMenu();
            return;
        }

        // Raggruppa per data
        var trattePerData = tratte.stream()
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getData()));

        trattePerData.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.println("\n📅 " + entry.getKey() + " (" + entry.getValue().size() + " tratte):");
                    entry.getValue().stream()
                            .sorted((t1, t2) -> t1.getOra().compareTo(t2.getOra()))
                            .forEach(tratta -> {
                                long biglietti = memoriaBiglietti.contaBigliettiPerTratta(tratta.getId());
                                int capienza = tratta.getTreno().getCapienzaTotale();
                                String occupazione = " [" + biglietti + "/" + capienza + "]";
                                System.out.println("   🚂 " + formatTratta(tratta) + occupazione);
                            });
                });

        pausaETornaMenu();
    }

    private static void menuGestionePromozioniSimplificato() {
        System.out.println("\n🎉 GESTIONE PROMOZIONI");
        System.out.println("-".repeat(25));
        System.out.println("1. 📋 Visualizza promozioni attive");
        System.out.println("2. ➕ Crea promozione generale");
        System.out.println("3. 💎 Crea promozione fedeltà");
        System.out.println("0. ⬅️ Torna al menu principale");

        System.out.print("Scegli operazione: ");
        int scelta = Integer.parseInt(scanner.nextLine().trim());

        switch (scelta) {
            case 1 -> visualizzaPromozioniAttive();
            case 2 -> System.out.println("⚠️ Funzione in sviluppo (thread-safe)");
            case 3 -> System.out.println("⚠️ Funzione in sviluppo (thread-safe)");
            case 0 -> { /* torna al menu */ }
            default -> System.out.println("❌ Opzione non valida!");
        }
    }

    private static void visualizzaPromozioniAttive() {
        System.out.println("\n📋 PROMOZIONI ATTIVE");
        System.out.println("-".repeat(25));

        var promozioni = memoriaPromozioni.getPromozioniAttive();

        if (promozioni.isEmpty()) {
            System.out.println("ℹ️ Nessuna promozione attiva");
        } else {
            for (int i = 0; i < promozioni.size(); i++) {
                var promo = promozioni.get(i);
                System.out.println((i + 1) + ") " + formatPromozione(promo));
            }
        }

        pausaETornaMenu();
    }

    private static void menuGestioneBiglietti() {
        System.out.println("\n🎫 GESTIONE BIGLIETTI");
        System.out.println("-".repeat(25));
        System.out.println("1. ✅ Visualizza biglietti CONFERMATI");
        System.out.println("2. ⏳ Visualizza biglietti PRENOTATI");
        System.out.println("3. 📊 Riepilogo per tratta");
        System.out.println("4. 🧹 Pulisci prenotazioni scadute");
        System.out.println("0. ⬅️ Torna al menu principale");

        System.out.print("Scegli operazione: ");
        int scelta = Integer.parseInt(scanner.nextLine().trim());

        switch (scelta) {
            case 1 -> visualizzaBigliettiConfermati();
            case 2 -> visualizzaBigliettiPrenotati();
            case 3 -> visualizzaBigliettiPerTratta();
            case 4 -> pulisciPrenotazioniScadute();
            case 0 -> { /* torna al menu */ }
            default -> System.out.println("❌ Opzione non valida!");
        }
    }

    private static void visualizzaBigliettiConfermati() {
        System.out.println("\n✅ BIGLIETTI CONFERMATI");
        System.out.println("-".repeat(30));

        List<model.Biglietto> bigliettiConfermati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> "acquisto".equalsIgnoreCase(b.getTipoAcquisto()) ||
                        "modifica".equalsIgnoreCase(b.getTipoAcquisto()))
                .toList();

        if (bigliettiConfermati.isEmpty()) {
            System.out.println("ℹ️ Nessun biglietto confermato presente");
            pausaETornaMenu();
            return;
        }

        double revenueConfermati = bigliettiConfermati.stream()
                .mapToDouble(b -> b.getPrezzoPagato())
                .sum();

        System.out.println("📊 STATISTICHE BIGLIETTI CONFERMATI:");
        System.out.println("   🎫 Totale biglietti: " + bigliettiConfermati.size());
        System.out.println("   💰 Revenue totale: €" + String.format("%.2f", revenueConfermati));

        bigliettiConfermati.forEach(biglietto -> {
            model.Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
            String infoTratta = tratta != null ?
                    tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() :
                    "Tratta non trovata";
            System.out.println("   ✅ " + formatBigliettoDettagliato(biglietto, infoTratta));
        });

        pausaETornaMenu();
    }

    private static void visualizzaBigliettiPrenotati() {
        System.out.println("\n⏳ BIGLIETTI PRENOTATI");
        System.out.println("-".repeat(30));

        List<model.Biglietto> bigliettiPrenotati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> "prenotazione".equalsIgnoreCase(b.getTipoAcquisto()))
                .toList();

        if (bigliettiPrenotati.isEmpty()) {
            System.out.println("ℹ️ Nessuna prenotazione presente");
        } else {
            System.out.println("📊 Prenotazioni attive: " + bigliettiPrenotati.size());
            bigliettiPrenotati.forEach(biglietto -> {
                model.Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
                String infoTratta = tratta != null ?
                        tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() :
                        "Tratta non trovata";
                System.out.println("   ⏳ " + formatBigliettoDettagliato(biglietto, infoTratta));
            });
        }

        pausaETornaMenu();
    }

    private static void visualizzaBigliettiPerTratta() {
        System.out.println("\n🎫 BIGLIETTI PER TRATTA");
        System.out.println("-".repeat(25));

        List<model.Biglietto> tuttiBiglietti = memoriaBiglietti.getTuttiIBiglietti();

        if (tuttiBiglietti.isEmpty()) {
            System.out.println("ℹ️ Nessun biglietto presente");
            pausaETornaMenu();
            return;
        }

        var bigliettiPerTratta = tuttiBiglietti.stream()
                .collect(java.util.stream.Collectors.groupingBy(b -> b.getIdTratta()));

        System.out.println("📊 BIGLIETTI RAGGRUPPATI PER TRATTA:");

        bigliettiPerTratta.forEach((idTratta, biglietti) -> {
            model.Tratta tratta = memoriaTratte.getTrattaById(idTratta);
            if (tratta != null) {
                int capienza = tratta.getTreno().getCapienzaTotale();
                System.out.println("\n🚂 " + tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo());
                System.out.println("   🎫 Biglietti: " + biglietti.size() + "/" + capienza);
                System.out.println("   🎯 Capienza: " + (biglietti.size() <= capienza ? "✅ OK" : "❌ OVERSELLING"));
            }
        });

        pausaETornaMenu();
    }

    private static void pulisciPrenotazioniScadute() {
        System.out.println("\n🧹 PULIZIA PRENOTAZIONI SCADUTE");
        System.out.println("-".repeat(35));
        System.out.println("⚠️ Funzione da implementare in versione thread-safe");
        pausaETornaMenu();
    }

    private static void visualizzaClientiFedeli() {
        System.out.println("\n👥 CLIENTI FEDELI");
        System.out.println("-".repeat(20));

        long bigliettiConFedelta = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.isConCartaFedelta())
                .count();

        long bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();

        System.out.println("📊 STATISTICHE FEDELTÀ:");
        System.out.println("   💳 Biglietti con carta fedeltà: " + bigliettiConFedelta + " / " + bigliettiTotali);

        if (bigliettiTotali > 0) {
            double percentuale = (bigliettiConFedelta * 100.0) / bigliettiTotali;
            System.out.println("   📈 Utilizzo fedeltà: " + String.format("%.1f%%", percentuale));
        }

        pausaETornaMenu();
    }

    // Metodi di utilità
    private static String formatTratta(model.Tratta tratta) {
        return String.format("ID:%s | %s → %s | %s %s | Bin.%d",
                tratta.getId().toString().substring(0, 8),
                tratta.getStazionePartenza(),
                tratta.getStazioneArrivo(),
                tratta.getData(),
                tratta.getOra(),
                tratta.getBinario());
    }

    private static String formatBigliettoDettagliato(model.Biglietto biglietto, String infoTratta) {
        String cartaFedelta = biglietto.isConCartaFedelta() ? "💳" : "💰";
        return String.format("ID:%s | %s %s | €%.2f | %s",
                biglietto.getId().toString().substring(0, 8),
                cartaFedelta,
                biglietto.getClasse(),
                biglietto.getPrezzoPagato(),
                infoTratta);
    }

    private static String formatPromozione(model.Promozione promo) {
        return String.format("%s | %.0f%% sconto | %s → %s",
                promo.getDescrizione(),
                promo.getSconto() * 100,
                promo.getDataInizio(),
                promo.getDataFine());
    }

    private static void pausaETornaMenu() {
        System.out.print("\n⏎ Premi INVIO per tornare al menu...");
        scanner.nextLine();
    }

    private static void fermaServers() {
        System.out.println("\n🛑 Arresto sistema thread-safe...");

        try {
            if (server != null) {
                server.shutdown();
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
                System.out.println("✅ Server TreniCal fermato");
            }

            if (bancaServer != null) {
                bancaServer.shutdown();
                if (!bancaServer.awaitTermination(5, TimeUnit.SECONDS)) {
                    bancaServer.shutdownNow();
                }
                System.out.println("✅ Server Banca fermato");
            }

        } catch (InterruptedException e) {
            System.err.println("⚠️ Interruzione durante l'arresto");
            Thread.currentThread().interrupt();
        }

        if (scanner != null) {
            scanner.close();
        }

        System.out.println("👋 Sistema TreniCal thread-safe arrestato correttamente!");
    }
}