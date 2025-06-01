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
 * 🖥️ CLIENT TRENICAL - INTERFACCIA ESSENZIALE (CORRETTA)
 */
public class ClientMain {

    private static final int SERVER_PORT = 9090;
    private static ClientService clientService;
    private static Scanner scanner;
    private static Wallet wallet;
    private static WalletPromozioni walletPromozioni;

    public static void main(String[] args) {
        System.out.println("🚂 ===== TRENICAL CLIENT =====");
        System.out.println("📡 Connessione al server...");

        scanner = new Scanner(System.in);

        try {
            clientService = new ClientService("localhost", SERVER_PORT);
            System.out.println("✅ Connesso al server TreniCal!");

            setupWallet();

            boolean continua = true;
            while (continua) {
                continua = mostraMenuPrincipale();
            }

        } catch (Exception e) {
            System.err.println("❌ Errore connessione server: " + e.getMessage());
            System.err.println("💡 Assicurati che il server sia avviato su porta " + SERVER_PORT);
        } finally {
            cleanup();
        }
    }

    private static void setupWallet() {
        wallet = new Wallet();
        walletPromozioni = new WalletPromozioni();
        ListaEventi.getInstance().aggiungiObserver(wallet);
        ListaEventi.getInstance().aggiungiObserver(walletPromozioni);
        System.out.println("💼 Wallet attivato e collegato al sistema eventi");
    }

    private static boolean mostraMenuPrincipale() {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("🏠 MENU PRINCIPALE");
        System.out.println("=".repeat(40));

        try {
            ClienteDTO cliente = clientService.getCliente();

            if (cliente == null) {
                System.out.println("⚠️  DEVI PRIMA REGISTRARTI");
                System.out.println("1. 👤 Registra cliente");
                System.out.println("0. 🚪 Esci");
            } else {
                System.out.println("👤 Cliente: " + cliente.getNome() + " " + cliente.getCognome());
                System.out.println("💳 Carta Fedeltà: " + (cliente.isFedelta() ? "✅ Attiva" : "❌ Non attiva"));

                // 💼 Mostra statistiche wallet
                System.out.println("💼 Biglietti confermati: " + wallet.getBigliettiConfermati().size());
                System.out.println("📝 Prenotazioni: " + wallet.getBigliettiNonConfermati().size());
                System.out.println("🎉 Promozioni attive: " + walletPromozioni.getPromozioniAttive().size());

                System.out.println("\n📋 OPERAZIONI:");
                System.out.println("1. 🔍 Cerca tratte");
                System.out.println("2. 📝 Prenota biglietto");
                System.out.println("3. 💳 Acquista biglietto");
                System.out.println("4. ✅ Conferma prenotazione");
                System.out.println("5. 🔄 Modifica biglietto");
                System.out.println("6. 💎 Attiva carta fedeltà");
                System.out.println("7. 💼 Visualizza biglietti");
                System.out.println("8. 🎉 Visualizza promozioni");
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
                    case 0 -> { return false; }
                    default -> System.out.println("❌ Opzione non valida!");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Errore: " + e.getMessage());
        }

        return true;
    }

    private static void registraCliente() {
        System.out.println("\n👤 REGISTRAZIONE CLIENTE");
        System.out.println("-".repeat(25));

        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();

        System.out.print("Cognome: ");
        String cognome = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Età: ");
        int eta = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Città: ");
        String residenza = scanner.nextLine().trim();

        System.out.print("Cellulare: ");
        String cellulare = scanner.nextLine().trim();

        clientService.attivaCliente(nome, cognome, email, eta, residenza, cellulare);
        System.out.println("✅ Cliente registrato!");
    }

    private static void cercaTratte() {
        System.out.println("\n🔍 RICERCA TRATTE");
        System.out.println("-".repeat(18));

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
            System.out.println("\n📋 TRATTE TROVATE:");
            for (int i = 0; i < risposta.getTratte().size(); i++) {
                System.out.println((i + 1) + ") " + formatTratta(risposta.getTratte().get(i)));
            }
        } else {
            System.out.println("❌ Nessuna tratta trovata");
        }

        pausa();
    }

    private static void prenotaBiglietto() {
        System.out.println("\n📝 PRENOTAZIONE BIGLIETTO");
        System.out.println("-".repeat(25));

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

            // 🔄 GENERA EVENTO PER WALLET
            ClientEventAdapter.processaRisposta(risposta, "PRENOTA");

        } else {
            System.out.println("❌ Prenotazione fallita: " + risposta.getMessaggio());
        }

        pausa();
    }

    private static void acquistaBiglietto() {
        System.out.println("\n💳 ACQUISTO BIGLIETTO");
        System.out.println("-".repeat(20));

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

            // 🔄 GENERA EVENTO PER WALLET
            ClientEventAdapter.processaRisposta(risposta, "ACQUISTA");

            // Mostra subito aggiornamento
            System.out.println("💼 Biglietti nel wallet: " + wallet.getBigliettiConfermati().size());

        } else {
            System.out.println("❌ Acquisto fallito: " + risposta.getMessaggio());
        }

        pausa();
    }

    private static void confermaPrenotazione() {
        System.out.println("\n✅ CONFERMA PRENOTAZIONE");
        System.out.println("-".repeat(25));

        List<BigliettoDTO> prenotazioni = wallet.getBigliettiNonConfermati();

        if (prenotazioni.isEmpty()) {
            System.out.println("ℹ️ Non hai prenotazioni da confermare");
            pausa();
            return;
        }

        System.out.println("📋 TUE PRENOTAZIONI:");
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

            // 🔄 GENERA EVENTO PER WALLET
            ClientEventAdapter.processaRisposta(risposta, "CONFERMA");

        } else {
            System.out.println("❌ Conferma fallita: " + risposta.getMessaggio());
        }

        pausa();
    }

    private static void modificaBiglietto() {
        System.out.println("\n🔄 MODIFICA BIGLIETTO");
        System.out.println("-".repeat(20));

        List<BigliettoDTO> biglietti = wallet.getBigliettiConfermati();

        if (biglietti.isEmpty()) {
            System.out.println("ℹ️ Non hai biglietti da modificare");
            pausa();
            return;
        }

        System.out.println("📋 TUOI BIGLIETTI:");
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

            // 🔄 GENERA EVENTO MODIFICA PER WALLET
            if (risposta.getBiglietto() != null) {
                ClientEventAdapter.processaModifica(bigliettoOriginale, risposta.getBiglietto());
                // Debug immediato
                System.out.println("🔍 DEBUG MAIN: Controllo immediato wallet dopo adapter...");
                System.out.println("   Confermati: " + wallet.getBigliettiConfermati().size());
                System.out.println("   Non confermati: " + wallet.getBigliettiNonConfermati().size());
            }

        } else {
            System.out.println("❌ Modifica fallita: " + risposta.getMessaggio());
        }

        pausa();
    }

    private static void attivaCartaFedelta() {
        System.out.println("\n💎 CARTA FEDELTÀ");
        System.out.println("-".repeat(15));

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

        pausa();
    }

    private static void visualizzaBiglietti() {
        System.out.println("\n💼 I TUOI BIGLIETTI");
        System.out.println("-".repeat(20));

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

        pausa();
    }

    private static void visualizzaPromozioni() {
        System.out.println("\n🎉 PROMOZIONI ATTIVE");
        System.out.println("-".repeat(20));

        List<PromozioneDTO> promozioni = walletPromozioni.getPromozioniAttive();

        if (promozioni.isEmpty()) {
            System.out.println("ℹ️ Nessuna promozione attiva");
        } else {
            for (int i = 0; i < promozioni.size(); i++) {
                PromozioneDTO promo = promozioni.get(i);
                System.out.println((i + 1) + ") " + promo.getNome() + " - " + promo.getDescrizione());
            }
        }

        pausa();
    }

    // Metodi di utilità
    private static TrattaDTO selezionaTratta() {
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
    }

    private static ClasseServizio selezionaClasse() {
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
    }

    private static TipoPrezzo selezionaTipoPrezzo() {
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
    }

    private static String formatTratta(TrattaDTO tratta) {
        return String.format("%s → %s | %s %s | Bin.%d",
                tratta.getStazionePartenza(),
                tratta.getStazioneArrivo(),
                tratta.getData(),
                tratta.getOra(),
                tratta.getBinario());
    }

    private static String formatBiglietto(BigliettoDTO biglietto) {
        return String.format("ID:%s | %s → %s | %s | €%.2f",
                biglietto.getId().toString().substring(0, 8),
                biglietto.getTratta().getStazionePartenza(),
                biglietto.getTratta().getStazioneArrivo(),
                biglietto.getClasseServizio(),
                biglietto.getPrezzoEffettivo());
    }

    private static void pausa() {
        System.out.print("\n⏎ Premi INVIO...");
        scanner.nextLine();
    }

    private static void cleanup() {
        if (scanner != null) {
            scanner.close();
        }
        System.out.println("👋 Arrivederci!");
    }
}