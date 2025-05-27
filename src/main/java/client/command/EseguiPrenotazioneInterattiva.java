package command;

import dto.*;
import enums.ClasseServizio;
import eventi.EventoPrenota;
import eventi.ListaEventi;
import service.ClientService;

import java.util.List;
import java.util.Scanner;

public class EseguiPrenotazioneInterattiva implements Command {

    private final ClientService clientService;

    public EseguiPrenotazioneInterattiva(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void esegui() {
        Scanner scanner = new Scanner(System.in);
        var cliente = clientService.getCliente();

        System.out.println("Inserisci parametri filtro (data;partenza;arrivo;fascia):");
        String filtro = scanner.nextLine();

        RichiestaDTO richiesta = new RichiestaDTO.Builder()
                .tipo("FILTRA")
                .messaggioExtra(filtro)
                .build();

        RispostaDTO risposta = clientService.inviaRichiesta(richiesta);
        List<TrattaDTO> tratte = risposta.getTratte();

        if (tratte == null || tratte.isEmpty()) {
            System.out.println("❌ Nessuna tratta trovata.");
            return;
        }

        System.out.println("📍 Tratte disponibili:");
        for (int i = 0; i < tratte.size(); i++) {
            System.out.println((i + 1) + ") " + tratte.get(i));
        }

        System.out.print("Scegli il numero della tratta da prenotare: ");
        String inputScelta = scanner.nextLine();

        int scelta;
        try {
            scelta = Integer.parseInt(inputScelta);
        } catch (NumberFormatException e) {
            System.out.println("❌ Inserisci un numero valido.");
            return;
        }

        if (scelta < 1 || scelta > tratte.size()) {
            System.out.println("❌ Scelta non valida.");
            return;
        }

        TrattaDTO sceltaTratta = tratte.get(scelta - 1);

        System.out.print("Inserisci classe (BASE, ARGENTO, GOLD): ");
        ClasseServizio classe;
        try {
            classe = ClasseServizio.valueOf(scanner.nextLine().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Classe non valida.");
            return;
        }

        RichiestaDTO richiestaPrenota = new RichiestaDTO.Builder()
                .tipo("PRENOTA")
                .idCliente(cliente.getId().toString())
                .tratta(sceltaTratta)
                .classeServizio(classe)
                .build();

        System.out.println("➡️ Invio richiesta di prenotazione...");
        RispostaDTO rispostaPrenota = clientService.inviaRichiesta(richiestaPrenota);

        if (rispostaPrenota.getBiglietto() != null) {
            BigliettoDTO biglietto = rispostaPrenota.getBiglietto();
            ListaEventi.getInstance().notifica(new EventoPrenota(biglietto));
            System.out.println("✅ Prenotazione avvenuta: " + biglietto);
        } else {
            System.out.println("❌ Prenotazione fallita: " + rispostaPrenota.getMessaggio());
        }
    }
}