package main;

import adapter.ClientEventAdapter;
import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import eventi.ListaEventi;
import model.Wallet;
import model.WalletPromozioni;
import service.ClientService;

import java.util.List;
import java.util.Scanner;

/**
 * 🖥️ CLIENT TRENICAL - INTERFACCIA ESSENZIALE (THREAD-SAFE CORRETTA)
 */
public class ClientMain {

    private static final int SERVER_PORT = 9090;
    private static ClientService clientService;
    private static Scanner scanner;
    private static Wallet wallet;
    private static WalletPromozioni walletPromozioni;

    public static void main(String[] args) {
        System.out.println("🚂 ===== TRENICAL CLIENT THREAD-SAFE =====");
        System.out.println("📡 Connessione al server...");

        scanner = new Scanner(System.in);

        try {
            clientService = new ClientService("localhost", SERVER_PORT);
            System.out.println("✅ Connesso al server TreniCal!");

            setupWalletThreadSafe();

            boolean continua = true;
            while (continua) {
                continua = mostraMenuPrincipale();
            }

        } catch (Exception e) {
            System.err.println("❌ Errore connessione server: " + e.getMessage());
            System.err.println("💡 Assicurati che il server sia avviato su porta " + SERVER_PORT);
        } finally {
            cleanupThreadSafe();
        }
    }

    /**
     * ✅ SETUP WALLET THREAD-SAFE
     */
    private static void setupWalletThreadSafe() {
        System.out.println("💼 Inizializzazione wallet thread-safe...");

        try {
            wallet = new Wallet();
            walletPromozioni = new WalletPromozioni();

            // ✅ REGISTRAZIONE THREAD-SAFE
            ListaEventi.getInstance().aggiungiObserver(wallet);
            ListaEventi.getInstance().aggiungiObserver(walletPromozioni);

            System.out.println("✅ Wallet thread-safe attivato e collegato al sistema eventi");

            // ✅ STREAM PROMOZIONI CON ERROR HANDLING
            avviaStreamPromozioniSafe();

            System.out.println("💼 Setup wallet completato - Thread safety: ATTIVA");

        } catch (Exception e) {
            System.err.println("❌ Errore setup wallet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ STREAM PROMOZIONI CON GESTIONE ERRORI
     */
    private static void avviaStreamPromozioniSafe() {
        try {
            // ✅ CORREZIONE: Usa il package corretto per PromozioneGrpcListener
            grpc.PromozioneGrpcListener promoListener = new grpc.PromozioneGrpcListener("localhost", SERVER_PORT);
            promoListener.avviaStreamPromozioni();
            System.out.println("📡 ✅ Stream promozioni attivato con successo");
        } catch (Exception e) {
            System.err.println("⚠️ Errore connessione stream promozioni: " + e.getMessage());
            System.err.println("💡 Le promozioni potrebbero non essere ricevute in tempo reale");
        }
    }

    private static boolean mostraMenuPrincipale() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🏠 MENU PRINCIPALE TRENICAL (THREAD-SAFE)");
        System.out.println("=".repeat(50));

        try {
            ClienteDTO cliente = clientService.getCliente();

            if (cliente == null) {
                System.out.println("⚠️  DEVI PRIMA REGISTRARTI");
                System.out.println("1. 👤 Registra cliente");
                System.out.println("0. 🚪 Esci");
            } else {
                System.out.println("👤 Cliente: " + cliente.getNome() + " " + cliente.getCognome());
                System.out.println("💳 Carta Fedeltà: " + (cliente.isFedelta() ? "✅ Attiva" : "❌ Non attiva"));

                // 💼 ✅ STATISTICHE WALLET THREAD-SAFE
                mostratStatisticheWalletSafe();

                System.out.println("\n📋 OPERAZIONI:");
                System.out.println("1. 🔍 Cerca tratte");
                System.out.println("2. 📝 Prenota biglietto");
                System.out.println("3. 💳 Acquista biglietto");
                System.out.println("4. ✅ Conferma prenotazione");
                System.out.println("5. 🔄 Modifica biglietto");
                System.out.println("6. 💎 Attiva carta fedeltà");
                System.out.println("7. 💼 Visualizza biglietti");
                System.out.println("8. 🎉 Visualizza promozioni");
                System.out.println("9. 📊 Statistiche wallet");  // ✅ NUOVO
                System.out.println("0. 🚪 Esci");
            }
        } catch (Exception e) {
            System.out.println("⚠️  CLIENTE NON REGISTRATO");
            System.out.println("1. 👤 Registra cliente");
            System.out.println("0. 🚪 Esci");
        }

        System.out.print("\n👉 Scegli: ");
        String input = scanner.nextLine().trim();

        try {
            int scelta = Integer.parseInt(input);
            return eseguiScelta(scelta);
        } catch (NumberFormatException e) {
            System.out.println("❌ Numero non valido!");
            return true;
        }
    }

    /**
     * ✅ STATISTICHE WALLET THREAD-SAFE
     */
    private static void mostratStatisticheWalletSafe() {
        try {
            // ✅ ACCESSO THREAD-SAFE ai wallet
            int confermati = wallet.getBigliettiConfermati().size();
            int prenotazioni = wallet.getBigliettiNonConfermati().size();
            int promozioni = walletPromozioni.getPromozioniAttive().size();

            System.out.println("💼 STATO WALLET (Thread-Safe):");
            System.out.println("   🎫 Biglietti confermati: " + confermati);
            System.out.println("   📝 Prenotazioni attive: " + prenotazioni);
            System.out.println("   🎉 Promozioni disponibili: " + promozioni);

        } catch (Exception e) {
            System.err.println("⚠️ Errore lettura statistiche wallet: " + e.getMessage());
            System.out.println("💼 Biglietti: N/A | Prenotazioni: N/A | Promozioni: N/A");
        }
    }

    private static boolean eseguiScelta(int scelta) {
        try {
            ClienteDTO cliente = null;
            try {
                cliente = clientService.getCliente();
            } catch (Exception e) {
                // Cliente non registrato
            }

            if (cliente == null) {
                switch (scelta) {
                    case 1 -> registraCliente();
                    case 0 -> { return false; }
                    default -> System.out.println("❌ Opzione non valida!");
                }
            } else {
                switch (scelta) {
                    case 1 -> cercaTratte();
                    case 2 -> prenotaBiglietto();
                    case 3 -> acquistaBiglietto();
                    case 4 -> confermaPrenotazione();
                    case 5 -> modificaBiglietto();
                    case 6 -> attivaCartaFedelta();
                    case 7 -> visualizzaBiglietti();
                    case 8 -> visualizzaPromozioni();
                    case 9 -> visualizzaStatisticheDettagliate(); // ✅ NUOVO
                    case 0 -> { return false; }
                    default -> System.out.println("❌ Opzione non valida!");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private static void registraCliente() {
        System.out.println("\n👤 REGISTRAZIONE CLIENTE");
        System.out.println("-".repeat(25));

        try {
            System.out.print("Nome: ");
            String nome = scanner.nextLine().trim();
            if (nome.isEmpty()) {
                System.out.println("❌ Nome non può essere vuoto!");
                return;
            }

            System.out.print("Cognome: ");
            String cognome = scanner.nextLine().trim();
            if (cognome.isEmpty()) {
                System.out.println("❌ Cognome non può essere vuoto!");
                return;
            }

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            if (email.isEmpty() || !email.contains("@")) {
                System.out.println("❌ Email non valida!");
                return;
            }

            System.out.print("Età: ");
            int eta = Integer.parseInt(scanner.nextLine().trim());
            if (eta < 0 || eta > 120) {
                System.out.println("❌ Età non valida!");
                return;
            }

            System.out.print("Città: ");
            String residenza = scanner.nextLine().trim();

            System.out.print("Cellulare: ");
            String cellulare = scanner.nextLine().trim();

            clientService.attivaCliente(nome, cognome, email, eta, residenza, cellulare);
            System.out.println("✅ Cliente registrato con successo!");

        } catch (NumberFormatException e) {
            System.out.println("❌ Età deve essere un numero!");
        } catch (Exception e) {
            System.out.println("❌ Errore registrazione: " + e.getMessage());
        }
    }

    private static void cercaTratte() {
        System.out.println("\n🔍 RICERCA TRATTE");
        System.out.println("-".repeat(18));

        try {
            System.out.print("Data (YYYY-MM-DD) [vuoto=tutte]: ");
            String data = scanner.nextLine().trim();

            System.out.print("Partenza [vuoto=tutte]: ");
            String partenza = scanner.nextLine().trim();

            System.out.print("Arrivo [vuoto=tutte]: ");
            String arrivo = scanner.nextLine().trim();

            System.out.print("Fascia (MATTINA/POMERIGGIO/SERA) [vuoto=tutte]: ");
            String fascia = scanner.nextLine().trim();

            String filtro = data + ";" + partenza + ";" + arrivo + ";" + fascia;

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(filtro)
                    .build();

            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
                System.out.println("\n📋 TRATTE TROVATE (" + risposta.getTratte().size() + "):");
                for (int i = 0; i < risposta.getTratte().size(); i++) {
                    System.out.println((i + 1) + ") " + formatTratta(risposta.getTratte().get(i)));
                }
            } else {
                System.out.println("❌ Nessuna tratta trovata");
            }
        } catch (Exception e) {
            System.out.println("❌ Errore ricerca: " + e.getMessage());
        }

        pausa();
    }

    private static void prenotaBiglietto() {
        System.out.println("\n📝 PRENOTAZIONE BIGLIETTO (THREAD-SAFE)");
        System.out.println("-".repeat(40));

        try {
            TrattaDTO tratta = selezionaTratta();
            if (tratta == null) return;

            ClasseServizio classe = selezionaClasse();
            if (classe == null) return;

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("PRENOTA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(classe)
                    .build();

            System.out.println("⏳ Invio richiesta prenotazione...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Prenotazione effettuata!");
                System.out.println("⏰ Ricorda: hai 10 minuti per confermare");

                // 🔄 ✅ THREAD-SAFE EVENT PROCESSING
                ClientEventAdapter.processaRisposta(risposta, "PRENOTA");

                // ✅ MOSTRA STATO AGGIORNATO
                System.out.println("💼 Prenotazioni aggiornate: " + wallet.getBigliettiNonConfermati().size());

            } else {
                System.out.println("❌ Prenotazione fallita: " + risposta.getMessaggio());
            }
        } catch (Exception e) {
            System.out.println("❌ Errore prenotazione: " + e.getMessage());
        }

        pausa();
    }

    private static void acquistaBiglietto() {
        System.out.println("\n💳 ACQUISTO BIGLIETTO (THREAD-SAFE)");
        System.out.println("-".repeat(35));

        try {
            TrattaDTO tratta = selezionaTratta();
            if (tratta == null) return;

            ClasseServizio classe = selezionaClasse();
            if (classe == null) return;

            TipoPrezzo tipoPrezzo = selezionaTipoPrezzo();
            if (tipoPrezzo == null) return;

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("ACQUISTA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .tratta(tratta)
                    .classeServizio(classe)
                    .tipoPrezzo(tipoPrezzo)
                    .build();

            System.out.println("⏳ Invio richiesta acquisto...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Acquisto completato!");

                // 🔄 ✅ THREAD-SAFE EVENT PROCESSING
                ClientEventAdapter.processaRisposta(risposta, "ACQUISTA");

                // ✅ MOSTRA STATO AGGIORNATO THREAD-SAFE
                System.out.println("💼 Biglietti confermati: " + wallet.getBigliettiConfermati().size());

            } else {
                System.out.println("❌ Acquisto fallito: " + risposta.getMessaggio());
            }
        } catch (Exception e) {
            System.out.println("❌ Errore acquisto: " + e.getMessage());
        }

        pausa();
    }

    private static void confermaPrenotazione() {
        System.out.println("\n✅ CONFERMA PRENOTAZIONE (THREAD-SAFE)");
        System.out.println("-".repeat(40));

        try {
            // ✅ ACCESSO THREAD-SAFE
            List<BigliettoDTO> prenotazioni = wallet.getBigliettiNonConfermati();

            if (prenotazioni.isEmpty()) {
                System.out.println("ℹ️ Non hai prenotazioni da confermare");
                pausa();
                return;
            }

            System.out.println("📋 TUE PRENOTAZIONI (" + prenotazioni.size() + "):");
            for (int i = 0; i < prenotazioni.size(); i++) {
                System.out.println((i + 1) + ") " + formatBiglietto(prenotazioni.get(i)));
            }

            System.out.print("Scegli prenotazione da confermare (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > prenotazioni.size()) {
                System.out.println("❌ Scelta non valida");
                return;
            }

            BigliettoDTO biglietto = prenotazioni.get(scelta - 1);

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("CONFERMA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .biglietto(biglietto)
                    .build();

            System.out.println("⏳ Invio conferma...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Prenotazione confermata!");

                // 🔄 ✅ THREAD-SAFE EVENT PROCESSING
                ClientEventAdapter.processaRisposta(risposta, "CONFERMA");

                // ✅ MOSTRA STATISTICHE AGGIORNATE
                mostratStatisticheWalletSafe();

            } else {
                System.out.println("❌ Conferma fallita: " + risposta.getMessaggio());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Inserisci un numero valido!");
        } catch (Exception e) {
            System.out.println("❌ Errore conferma: " + e.getMessage());
        }

        pausa();
    }

    private static void modificaBiglietto() {
        System.out.println("\n🔄 MODIFICA BIGLIETTO (THREAD-SAFE)");
        System.out.println("-".repeat(35));

        try {
            // ✅ ACCESSO THREAD-SAFE
            List<BigliettoDTO> biglietti = wallet.getBigliettiConfermati();

            if (biglietti.isEmpty()) {
                System.out.println("ℹ️ Non hai biglietti da modificare");
                pausa();
                return;
            }

            System.out.println("📋 TUOI BIGLIETTI (" + biglietti.size() + "):");
            for (int i = 0; i < biglietti.size(); i++) {
                System.out.println((i + 1) + ") " + formatBiglietto(biglietti.get(i)));
            }

            System.out.print("Scegli biglietto da modificare (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > biglietti.size()) {
                System.out.println("❌ Scelta non valida");
                return;
            }

            BigliettoDTO bigliettoOriginale = biglietti.get(scelta - 1);

            System.out.println("\n🔍 Seleziona nuova tratta:");
            TrattaDTO nuovaTratta = selezionaTratta();
            if (nuovaTratta == null) return;

            ClasseServizio nuovaClasse = selezionaClasse();
            if (nuovaClasse == null) return;

            TipoPrezzo tipoPrezzo = selezionaTipoPrezzo();
            if (tipoPrezzo == null) return;

            System.out.print("Penale modifica [default 5.0]: ");
            String penaleStr = scanner.nextLine().trim();
            double penale = penaleStr.isEmpty() ? 5.0 : Double.parseDouble(penaleStr);

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("MODIFICA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .biglietto(bigliettoOriginale)
                    .tratta(nuovaTratta)
                    .classeServizio(nuovaClasse)
                    .tipoPrezzo(tipoPrezzo)
                    .penale(penale)
                    .build();

            System.out.println("⏳ Invio modifica...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Biglietto modificato!");

                // 🔄 ✅ THREAD-SAFE EVENT PROCESSING
                if (risposta.getBiglietto() != null) {
                    ClientEventAdapter.processaModifica(bigliettoOriginale, risposta.getBiglietto());
                }

                // ✅ VERIFICA IMMEDIATA THREAD-SAFE
                System.out.println("💼 Verifica wallet post-modifica:");
                mostratStatisticheWalletSafe();

            } else {
                System.out.println("❌ Modifica fallita: " + risposta.getMessaggio());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Inserisci un numero valido!");
        } catch (Exception e) {
            System.out.println("❌ Errore modifica: " + e.getMessage());
        }

        pausa();
    }

    private static void attivaCartaFedelta() {
        System.out.println("\n💎 CARTA FEDELTÀ");
        System.out.println("-".repeat(15));

        try {
            System.out.println("💳 Costo: €10.00");
            System.out.print("Confermi? (s/N): ");
            String conferma = scanner.nextLine().trim().toLowerCase();

            if (!conferma.equals("s")) {
                System.out.println("❌ Annullato");
                return;
            }

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("CARTA_FEDELTA")
                    .idCliente(clientService.getCliente().getId().toString())
                    .build();

            System.out.println("⏳ Attivazione carta fedeltà...");
            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getEsito().equals("OK")) {
                System.out.println("✅ Carta fedeltà attivata!");
            } else {
                System.out.println("❌ Attivazione fallita: " + risposta.getMessaggio());
            }
        } catch (Exception e) {
            System.out.println("❌ Errore attivazione carta: " + e.getMessage());
        }

        pausa();
    }

    private static void visualizzaBiglietti() {
        System.out.println("\n💼 I TUOI BIGLIETTI (THREAD-SAFE)");
        System.out.println("-".repeat(35));

        try {
            // ✅ ACCESSO THREAD-SAFE
            List<BigliettoDTO> confermati = wallet.getBigliettiConfermati();
            List<BigliettoDTO> prenotazioni = wallet.getBigliettiNonConfermati();

            System.out.println("✅ BIGLIETTI CONFERMATI (" + confermati.size() + "):");
            if (confermati.isEmpty()) {
                System.out.println("   Nessun biglietto");
            } else {
                for (int i = 0; i < confermati.size(); i++) {
                    System.out.println("   " + (i + 1) + ") " + formatBiglietto(confermati.get(i)));
                }
            }

            System.out.println("\n📝 PRENOTAZIONI (" + prenotazioni.size() + "):");
            if (prenotazioni.isEmpty()) {
                System.out.println("   Nessuna prenotazione");
            } else {
                for (int i = 0; i < prenotazioni.size(); i++) {
                    System.out.println("   " + (i + 1) + ") " + formatBiglietto(prenotazioni.get(i)));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Errore lettura biglietti: " + e.getMessage());
        }

        pausa();
    }

    private static void visualizzaPromozioni() {
        System.out.println("\n🎉 PROMOZIONI ATTIVE (THREAD-SAFE)");
        System.out.println("-".repeat(35));

        try {
            // ✅ ACCESSO THREAD-SAFE
            List<PromozioneDTO> promozioni = walletPromozioni.getPromozioniAttive();

            if (promozioni.isEmpty()) {
                System.out.println("ℹ️ Nessuna promozione attiva");
            } else {
                System.out.println("📋 PROMOZIONI DISPONIBILI (" + promozioni.size() + "):");
                for (int i = 0; i < promozioni.size(); i++) {
                    PromozioneDTO promo = promozioni.get(i);
                    System.out.println((i + 1) + ") " + promo.getNome() + " - " + promo.getDescrizione());
                    System.out.println("    📅 Periodo: " + promo.getDataInizio() + " → " + promo.getDataFine());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Errore lettura promozioni: " + e.getMessage());
        }

        pausa();
    }

    /**
     * ✅ NUOVO: Statistiche dettagliate thread-safe
     */
    private static void visualizzaStatisticheDettagliate() {
        System.out.println("\n📊 STATISTICHE DETTAGLIATE WALLET");
        System.out.println("-".repeat(40));

        try {
            // ✅ STATISTICHE WALLET THREAD-SAFE
            if (wallet != null) {
                System.out.println("💼 WALLET BIGLIETTI:");
                System.out.println("   " + wallet.getStatistiche());
            }

            // ✅ STATISTICHE EVENTI THREAD-SAFE
            System.out.println("\n🔔 SISTEMA EVENTI:");
            System.out.println("   " + ListaEventi.getInstance().getStatistiche());

            // ✅ INFO THREAD CORRENTE
            System.out.println("\n🔧 INFO THREAD:");
            System.out.println("   Thread corrente: " + Thread.currentThread().getName());
            System.out.println("   Thread attivi: " + Thread.activeCount());

        } catch (Exception e) {
            System.out.println("❌ Errore lettura statistiche: " + e.getMessage());
        }

        pausa();
    }

    // ============================================================================
    // ✅ METODI DI UTILITÀ (invariati ma con migliore error handling)
    // ============================================================================

    private static TrattaDTO selezionaTratta() {
        try {
            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(";;;")
                    .build();

            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getTratte() == null || risposta.getTratte().isEmpty()) {
                System.out.println("❌ Nessuna tratta disponibile");
                return null;
            }

            System.out.println("📋 TRATTE DISPONIBILI:");
            for (int i = 0; i < risposta.getTratte().size(); i++) {
                System.out.println((i + 1) + ") " + formatTratta(risposta.getTratte().get(i)));
            }

            System.out.print("Scegli tratta (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > risposta.getTratte().size()) {
                return null;
            }

            return risposta.getTratte().get(scelta - 1);
        } catch (Exception e) {
            System.out.println("❌ Errore selezione tratta: " + e.getMessage());
            return null;
        }
    }

    private static ClasseServizio selezionaClasse() {
        try {
            System.out.println("🎭 CLASSI:");
            ClasseServizio[] classi = ClasseServizio.values();
            for (int i = 0; i < classi.length; i++) {
                System.out.println((i + 1) + ") " + classi[i]);
            }

            System.out.print("Scegli classe (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > classi.length) {
                return null;
            }

            return classi[scelta - 1];
        } catch (Exception e) {
            System.out.println("❌ Errore selezione classe: " + e.getMessage());
            return null;
        }
    }

    private static TipoPrezzo selezionaTipoPrezzo() {
        try {
            System.out.println("💰 TIPI PREZZO:");
            TipoPrezzo[] tipi = TipoPrezzo.values();
            for (int i = 0; i < tipi.length; i++) {
                System.out.println((i + 1) + ") " + tipi[i]);
            }

            System.out.print("Scegli tipo (0=annulla): ");
            int scelta = Integer.parseInt(scanner.nextLine().trim());

            if (scelta < 1 || scelta > tipi.length) {
                return null;
            }

            return tipi[scelta - 1];
        } catch (Exception e) {
            System.out.println("❌ Errore selezione tipo prezzo: " + e.getMessage());
            return null;
        }
    }

    private static String formatTratta(TrattaDTO tratta) {
        try {
            return String.format("%s → %s | %s %s | Bin.%d",
                    tratta.getStazionePartenza(),
                    tratta.getStazioneArrivo(),
                    tratta.getData(),
                    tratta.getOra(),
                    tratta.getBinario());
        } catch (Exception e) {
            return "Tratta non valida: " + e.getMessage();
        }
    }

    private static String formatBiglietto(BigliettoDTO biglietto) {
        try {
            return String.format("ID:%s | %s → %s | %s | €%.2f",
                    biglietto.getId().toString().substring(0, 8),
                    biglietto.getTratta().getStazionePartenza(),
                    biglietto.getTratta().getStazioneArrivo(),
                    biglietto.getClasseServizio(),
                    biglietto.getPrezzoEffettivo());
        } catch (Exception e) {
            return "Biglietto non valido: " + e.getMessage();
        }
    }

    private static void pausa() {
        System.out.print("\n⏎ Premi INVIO...");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // Ignora errori di input
        }
    }

    /**
     * ✅ CLEANUP THREAD-SAFE COMPLETO
     */
    private static void cleanupThreadSafe() {
        System.out.println("\n🛑 Shutdown thread-safe in corso...");

        // ✅ SHUTDOWN WALLET THREAD-SAFE
        if (wallet != null) {
            try {
                System.out.println("💼 Chiusura wallet...");
                wallet.shutdown();
                System.out.println("✅ Wallet shutdown completato");
            } catch (Exception e) {
                System.err.println("⚠️ Errore shutdown wallet: " + e.getMessage());
            }
        }

        // ✅ SHUTDOWN WALLET PROMOZIONI THREAD-SAFE
        if (walletPromozioni != null) {
            try {
                System.out.println("🎉 Chiusura wallet promozioni...");
                walletPromozioni.shutdown(); // ✅ CORRETTO: shutdown() invece di push()
                System.out.println("✅ WalletPromozioni shutdown completato");
            } catch (Exception e) {
                System.err.println("⚠️ Errore shutdown wallet promozioni: " + e.getMessage());
            }
        }

        // ✅ RIMOZIONE OBSERVER THREAD-SAFE
        try {
            if (wallet != null) {
                ListaEventi.getInstance().rimuoviObserver(wallet);
                System.out.println("🗑️ Wallet rimosso dagli observer");
            }
            if (walletPromozioni != null) {
                ListaEventi.getInstance().rimuoviObserver(walletPromozioni);
                System.out.println("🗑️ WalletPromozioni rimosso dagli observer");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore rimozione observer: " + e.getMessage());
        }

        // ✅ CHIUSURA CLIENT SERVICE
        try {
            if (clientService != null) {
                // ClientService non ha shutdown esplicito, ma possiamo fare cleanup
                System.out.println("📡 Disconnessione dal server...");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore disconnessione server: " + e.getMessage());
        }

        // ✅ CHIUSURA SCANNER
        if (scanner != null) {
            try {
                scanner.close();
                System.out.println("⌨️ Scanner chiuso");
            } catch (Exception e) {
                System.err.println("⚠️ Errore chiusura scanner: " + e.getMessage());
            }
        }

        // ✅ STATISTICHE FINALI
        try {
            System.out.println("\n📊 STATISTICHE FINALI:");
            if (wallet != null) {
                System.out.println("   " + wallet.getStatistiche());
            }
            System.out.println("   " + ListaEventi.getInstance().getStatistiche());

            System.out.println("\n🧵 INFO THREAD FINALI:");
            System.out.println("   Thread attivi: " + Thread.activeCount());
            System.out.println("   Thread corrente: " + Thread.currentThread().getName());

        } catch (Exception e) {
            System.err.println("⚠️ Errore statistiche finali: " + e.getMessage());
        }

        System.out.println("\n✅ Cleanup thread-safe completato!");
        System.out.println("👋 Arrivederci da TreniCal Thread-Safe!");
    }
}