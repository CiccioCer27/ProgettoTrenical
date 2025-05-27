package command;

import dto.*;
import eventi.EventoConferma;
import eventi.ListaEventi;
import model.Wallet;
import service.ClientService;

import java.util.List;
import java.util.Scanner;

public class ConfermaPrenotazioneCommand implements Command {

    private final ClientService clientService;
    private final Wallet wallet;

    public ConfermaPrenotazioneCommand(ClientService clientService, Wallet wallet) {
        this.clientService = clientService;
        this.wallet = wallet;
    }

    @Override
    public void esegui() {
        var cliente = clientService.getCliente();
        List<BigliettoDTO> nonConfermati = wallet.getBigliettiNonConfermati();

        if (nonConfermati.isEmpty()) {
            System.out.println("ℹ️ Nessun biglietto prenotato da confermare.");
            return;
        }

        System.out.println("📋 Biglietti prenotati:");
        for (int i = 0; i < nonConfermati.size(); i++) {
            System.out.println((i + 1) + ") " + nonConfermati.get(i));
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Scegli il numero del biglietto da confermare: ");
        String input = scanner.nextLine();

        int scelta;
        try {
            scelta = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("❌ Input non valido.");
            return;
        }

        if (scelta < 1 || scelta > nonConfermati.size()) {
            System.out.println("❌ Selezione fuori intervallo.");
            return;
        }

        BigliettoDTO biglietto = nonConfermati.get(scelta - 1);
        RichiestaDTO richiesta = new RichiestaDTO.Builder()
                .tipo("CONFERMA")
                .idCliente(cliente.getId().toString())
                .messaggioExtra(biglietto.getId().toString())
                .build();

        RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

        if (risposta.getBiglietto() != null) {
            BigliettoDTO confermato = risposta.getBiglietto();
            ListaEventi.getInstance().notifica(new EventoConferma(confermato));
            System.out.println("✅ Prenotazione confermata: " + confermato);

            // ✅ Iscrizione automatica alle notifiche della tratta
            if (confermato.getTratta() != null) {
                clientService.avviaNotificheTratta(confermato.getTratta());
                System.out.println("📡 Iscritto automaticamente agli aggiornamenti per la tratta confermata.");
            }

        } else {
            System.out.println("❌ Errore nella conferma: " + risposta.getMessaggio());
        }
    }
}