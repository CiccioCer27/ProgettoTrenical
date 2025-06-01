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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 🖥️ INTERFACCIA SERVER SEMPLIFICATA
 *
 * Console di amministrazione essenziale per il sistema TreniCal
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
    private static EventDispatcher dispatcher;

    public static void main(String[] args) {
        System.out.println("🖥️ ===== TRENICAL SERVER - CONSOLE AMMINISTRAZIONE =====");

        scanner = new Scanner(System.in);

        try {
            // Avvia sistema
            avviaSystemaCompleto();

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

    private static void avviaSystemaCompleto() throws Exception {
        System.out.println("🚀 Avvio sistema server...");

        // 1. Server Banca
        bancaServer = ServerBuilder.forPort(BANCA_PORT)
                .addService(new BancaServiceImpl())
                .build()
                .start();
        System.out.println("✅ Server Banca avviato sulla porta " + BANCA_PORT);

        // 2. Componenti memoria
        memoriaBiglietti = new MemoriaBiglietti();
        memoriaClientiFedeli = new MemoriaClientiFedeli();
        memoriaTratte = new MemoriaTratte();
        memoriaPromozioni = new MemoriaPromozioni();

        System.out.println("💾 Componenti memoria caricate:");
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

        // 4. Event system
        dispatcher = new EventDispatcher();
        GrpcNotificaDispatcher notificaDispatcher = new GrpcNotificaDispatcher();

        // Registra listeners
        dispatcher.registra(new MemoriaBigliettiListener(memoriaBiglietti));
        dispatcher.registra(new MemoriaClientiFedeliListener(memoriaClientiFedeli));
        dispatcher.registra(new MemoriaPromozioniListener(memoriaPromozioni));
        dispatcher.registra(new EventoLoggerListener());

        // Registra anche nel sistema eventi server
        ListaEventiS.getInstance().aggiungi(new MemoriaBigliettiListener(memoriaBiglietti));
        ListaEventiS.getInstance().aggiungi(new MemoriaClientiFedeliListener(memoriaClientiFedeli));
        ListaEventiS.getInstance().aggiungi(new MemoriaPromozioniListener(memoriaPromozioni));

        // 5. Client banca e handler richieste
        BancaServiceClient bancaClient = new BancaServiceClient("localhost", BANCA_PORT);
        ServerRequestHandler handler = new ServerRequestHandler(
                memoriaBiglietti, memoriaClientiFedeli, memoriaTratte, dispatcher, bancaClient
        );

        // 6. Servizio gRPC - SENZA AUTO PROMOZIONI
        trenicalService = new TrenicalServiceImpl(notificaDispatcher, handler, memoriaPromozioni) {
            // Override per disabilitare le promozioni automatiche
            @Override
            public void broadcastPromozione(dto.PromozioneDTO promo) {
                // Non fare nulla - promozioni solo manuali
            }
        };

        // 7. Server TreniCal
        server = ServerBuilder.forPort(SERVER_PORT)
                .addService(trenicalService)
                .build()
                .start();
        System.out.println("✅ Server TreniCal avviato sulla porta " + SERVER_PORT);

        System.out.println("🎯 Sistema server operativo!");
    }

    private static boolean mostraMenuAmministrazione() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🏠 CONSOLE AMMINISTRAZIONE TRENICAL");
        System.out.println("=".repeat(50));

        // Statistiche essenziali
        System.out.println("📊 STATO SISTEMA:");
        System.out.println("   🚂 Tratte attive: " + memoriaTratte.getTutteTratte().size());
        System.out.println("   🎫 Biglietti totali: " + memoriaBiglietti.getTuttiIBiglietti().size());
        System.out.println("   🎉 Promozioni attive: " + memoriaPromozioni.getPromozioniAttive().size());

        System.out.println("\n📋 OPERAZIONI DISPONIBILI:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("2. 🚂 Gestione tratte");
        System.out.println("3. 🎉 Gestione promozioni");
        System.out.println("4. 🎫 Gestione biglietti");
        System.out.println("5. 👥 Visualizza clienti fedeli");
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
                case 2 -> menuGestioneTratteSimplificato();
                case 3 -> menuGestionePromozioniSimplificato();
                case 4 -> menuGestioneBiglietti();
                case 5 -> visualizzaClientiFedeli();
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

        List<Tratta> tratte = memoriaTratte.getTutteTratte();

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
                                System.out.println("   🚂 " + formatTratta(tratta));
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
        System.out.println("4. 🎯 Crea promozione per tratte");
        System.out.println("0. ⬅️ Torna al menu principale");

        System.out.print("Scegli operazione: ");
        int scelta = Integer.parseInt(scanner.nextLine().trim());

        switch (scelta) {
            case 1 -> visualizzaPromozioniAttive();
            case 2 -> new CreaPromozioneGeneraleCommand(memoriaPromozioni, dispatcher).run();
            case 3 -> new CreaPromozioneFedeltaCommand().run();
            case 4 -> new CreaPromozioneTrattaCommand(memoriaPromozioni, memoriaTratte, dispatcher).run();
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

        // Raggruppa per data di acquisto
        var bigliettiPerData = bigliettiConfermati.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getDataAcquisto()));

        double revenueConfermati = bigliettiConfermati.stream()
                .mapToDouble(b -> b.getPrezzoPagato())
                .sum();

        System.out.println("📊 STATISTICHE BIGLIETTI CONFERMATI:");
        System.out.println("   🎫 Totale biglietti: " + bigliettiConfermati.size());
        System.out.println("   💰 Revenue totale: €" + String.format("%.2f", revenueConfermati));

        bigliettiPerData.entrySet().stream()
                .sorted(java.util.Map.Entry.<java.time.LocalDate, List<model.Biglietto>>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println("\n📅 " + entry.getKey() + " (" + entry.getValue().size() + " biglietti):");
                    entry.getValue().forEach(biglietto -> {
                        Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
                        String infoTratta = tratta != null ?
                                tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() +
                                        " (" + tratta.getData() + " " + tratta.getOra() + ")" :
                                "Tratta non trovata";

                        System.out.println("   ✅ " + formatBigliettoDettagliato(biglietto, infoTratta));
                    });
                });

        pausaETornaMenu();
    }

    private static void visualizzaBigliettiPrenotati() {
        System.out.println("\n⏳ BIGLIETTI PRENOTATI (NON CONFERMATI)");
        System.out.println("-".repeat(40));

        List<model.Biglietto> bigliettiPrenotati = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> "prenotazione".equalsIgnoreCase(b.getTipoAcquisto()))
                .toList();

        if (bigliettiPrenotati.isEmpty()) {
            System.out.println("ℹ️ Nessuna prenotazione presente");
            pausaETornaMenu();
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        System.out.println("📊 STATISTICHE PRENOTAZIONI:");
        System.out.println("   ⏳ Totale prenotazioni: " + bigliettiPrenotati.size());

        // Conta prenotazioni scadute (oltre 10 minuti)
        long prenotazioniScadute = bigliettiPrenotati.stream()
                .filter(b -> {
                    java.time.LocalDateTime dataPrenotazione = b.getDataAcquisto().atStartOfDay();
                    return java.time.Duration.between(dataPrenotazione, now).toMinutes() > 10;
                })
                .count();

        System.out.println("   ⚠️ Prenotazioni scadute: " + prenotazioniScadute);
        System.out.println("   ✅ Prenotazioni valide: " + (bigliettiPrenotati.size() - prenotazioniScadute));

        System.out.println("\n📋 DETTAGLIO PRENOTAZIONI:");

        bigliettiPrenotati.forEach(biglietto -> {
            Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
            String infoTratta = tratta != null ?
                    tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() +
                            " (" + tratta.getData() + " " + tratta.getOra() + ")" :
                    "Tratta non trovata";

            // Calcola tempo rimanente (simulato basato su data acquisto)
            java.time.LocalDateTime dataPrenotazione = biglietto.getDataAcquisto().atStartOfDay();
            long minutiPassati = java.time.Duration.between(dataPrenotazione, now).toMinutes();
            long minutiRimanenti = Math.max(0, 10 - minutiPassati);

            String statoScadenza = minutiRimanenti > 0 ?
                    "⏰ Scade tra " + minutiRimanenti + " min" :
                    "❌ SCADUTA";

            System.out.println("   ⏳ " + formatBigliettoDettagliato(biglietto, infoTratta) +
                    " | " + statoScadenza);
        });

        if (prenotazioniScadute > 0) {
            System.out.println("\n💡 Suggerimento: Usa l'opzione '4. Pulisci prenotazioni scadute' per rimuoverle");
        }

        pausaETornaMenu();
    }

    private static void pulisciPrenotazioniScadute() {
        System.out.println("\n🧹 PULIZIA PRENOTAZIONI SCADUTE");
        System.out.println("-".repeat(35));

        List<model.Biglietto> tuttiIBiglietti = memoriaBiglietti.getTuttiIBiglietti();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        List<model.Biglietto> bigliettiScaduti = tuttiIBiglietti.stream()
                .filter(b -> "prenotazione".equalsIgnoreCase(b.getTipoAcquisto()))
                .filter(b -> {
                    java.time.LocalDateTime dataPrenotazione = b.getDataAcquisto().atStartOfDay();
                    return java.time.Duration.between(dataPrenotazione, now).toMinutes() > 10;
                })
                .toList();

        if (bigliettiScaduti.isEmpty()) {
            System.out.println("✅ Nessuna prenotazione scaduta da rimuovere");
            pausaETornaMenu();
            return;
        }

        System.out.println("🔍 Trovate " + bigliettiScaduti.size() + " prenotazioni scadute:");
        bigliettiScaduti.forEach(biglietto -> {
            Tratta tratta = memoriaTratte.getTrattaById(biglietto.getIdTratta());
            String infoTratta = tratta != null ?
                    tratta.getStazionePartenza() + "→" + tratta.getStazioneArrivo() :
                    "Tratta sconosciuta";
            System.out.println("   ❌ " + biglietto.getId().toString().substring(0, 8) +
                    " | " + infoTratta + " | €" + String.format("%.2f", biglietto.getPrezzoPagato()));
        });

        System.out.print("\n⚠️ Confermi la rimozione di " + bigliettiScaduti.size() + " prenotazioni? (s/n): ");
        String conferma = scanner.nextLine().trim().toLowerCase();

        if ("s".equals(conferma) || "si".equals(conferma) || "y".equals(conferma) || "yes".equals(conferma)) {
            int rimosse = 0;
            for (model.Biglietto biglietto : bigliettiScaduti) {
                try {
                    memoriaBiglietti.rimuoviBiglietto(biglietto.getId());
                    rimosse++;
                } catch (Exception e) {
                    System.err.println("❌ Errore rimozione biglietto " + biglietto.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Rimosse " + rimosse + " prenotazioni scadute");
            System.out.println("💾 Memoria aggiornata automaticamente");
        } else {
            System.out.println("❌ Operazione annullata");
        }

        pausaETornaMenu();
    }

    private static void visualizzaBigliettiPerTratta() {
        System.out.println("\n🎫 BIGLIETTI PER TRATTA");
        System.out.println("-".repeat(25));

        List<model.Biglietto> tuttiIBiglietti = memoriaBiglietti.getTuttiIBiglietti();

        if (tuttiIBiglietti.isEmpty()) {
            System.out.println("ℹ️ Nessun biglietto presente");
            pausaETornaMenu();
            return;
        }

        // Raggruppa biglietti per tratta
        var bigliettiPerTratta = tuttiIBiglietti.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getIdTratta()));

        System.out.println("📊 BIGLIETTI RAGGRUPPATI PER TRATTA:");

        bigliettiPerTratta.forEach((idTratta, biglietti) -> {
            // Trova informazioni tratta
            Tratta tratta = memoriaTratte.getTrattaById(idTratta);

            if (tratta != null) {
                System.out.println("\n🚂 TRATTA: " + tratta.getId().toString().substring(0, 8) +
                        " | " + tratta.getStazionePartenza() + " → " + tratta.getStazioneArrivo());
                System.out.println("   📅 Data: " + tratta.getData() + " | 🕒 Ora: " + tratta.getOra());
                System.out.println("   🎫 Biglietti venduti: " + biglietti.size());

                // Mostra dettagli biglietti
                biglietti.forEach(biglietto -> {
                    System.out.println("     • " + formatBigliettoDettagliato(biglietto, ""));
                });
            } else {
                System.out.println("\n❓ TRATTA SCONOSCIUTA: " + idTratta.toString().substring(0, 8));
                System.out.println("   🎫 Biglietti: " + biglietti.size());
            }
        });

        pausaETornaMenu();
    }

    private static void visualizzaClientiFedeli() {
        System.out.println("\n👥 CLIENTI FEDELI");
        System.out.println("-".repeat(20));

        // Debug: stampa tutti i biglietti per diagnostica
        System.out.println("🔍 DEBUG: Totale biglietti in memoria: " + memoriaBiglietti.getTuttiIBiglietti().size());

        // Conta biglietti per clienti fedeli
        long bigliettiConFedelta = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.isConCartaFedelta())
                .count();

        long bigliettiTotali = memoriaBiglietti.getTuttiIBiglietti().size();

        double percentualeFedelta = bigliettiTotali > 0 ?
                (bigliettiConFedelta * 100.0 / bigliettiTotali) : 0;

        System.out.println("📊 STATISTICHE FEDELTÀ:");
        System.out.println("   💳 Biglietti con carta fedeltà: " + bigliettiConFedelta + " / " + bigliettiTotali);
        System.out.println("   📈 Percentuale utilizzo fedeltà: " + String.format("%.1f%%", percentualeFedelta));

        // Revenue da clienti fedeli
        double revenueConFedelta = memoriaBiglietti.getTuttiIBiglietti().stream()
                .filter(b -> b.isConCartaFedelta())
                .mapToDouble(b -> b.getPrezzoPagato())
                .sum();

        double revenueTotale = memoriaBiglietti.getTuttiIBiglietti().stream()
                .mapToDouble(b -> b.getPrezzoPagato())
                .sum();

        System.out.println("   💰 Revenue da clienti fedeli: €" + String.format("%.2f", revenueConFedelta));
        System.out.println("   💰 Revenue totale: €" + String.format("%.2f", revenueTotale));

        // Lista dei clienti fedeli (da MemoriaClientiFedeli)
        System.out.println("\n👥 CLIENTI FEDELI REGISTRATI:");

        // Aggiungi metodo debug per verificare clienti fedeli
        List<java.util.UUID> tuttiClienti = memoriaBiglietti.getTuttiIBiglietti().stream()
                .map(b -> b.getIdCliente())
                .distinct()
                .toList();

        System.out.println("🔍 DEBUG: Clienti totali trovati: " + tuttiClienti.size());

        int clientiFedeliCount = 0;
        for (java.util.UUID clienteId : tuttiClienti) {
            boolean isFedele = memoriaClientiFedeli.isClienteFedele(clienteId);
            if (isFedele) {
                clientiFedeliCount++;
                long bigliettiCliente = memoriaBiglietti.getTuttiIBiglietti().stream()
                        .filter(b -> b.getIdCliente().equals(clienteId))
                        .count();

                double spesaCliente = memoriaBiglietti.getTuttiIBiglietti().stream()
                        .filter(b -> b.getIdCliente().equals(clienteId))
                        .mapToDouble(b -> b.getPrezzoPagato())
                        .sum();

                System.out.println("   • " + clienteId.toString().substring(0, 8) +
                        " | " + bigliettiCliente + " biglietti | €" +
                        String.format("%.2f", spesaCliente));
            } else {
                System.out.println("   🔍 Cliente " + clienteId.toString().substring(0, 8) + " NON è fedele");
            }
        }

        if (clientiFedeliCount == 0) {
            System.out.println("   ⚠️ NESSUN CLIENTE FEDELE TROVATO!");
            System.out.println("   💡 Verifica che il comando CartaFedeltaCommand funzioni correttamente");
        }

        pausaETornaMenu();
    }

    // Metodi di utilità
    private static String formatTratta(Tratta tratta) {
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
        return String.format("ID:%s | Cliente:%s | %s %s | %s | €%.2f",
                biglietto.getId().toString().substring(0, 8),
                biglietto.getIdCliente().toString().substring(0, 8),
                cartaFedelta,
                biglietto.getClasse(),
                infoTratta,
                biglietto.getPrezzoPagato());
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
        System.out.println("\n🛑 Arresto sistema...");

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

        System.out.println("👋 Sistema TreniCal arrestato correttamente!");
    }
}