package command;

import dto.*;
import enums.ClasseServizio;
import enums.TipoPrezzo;
import eventi.EventoAcquisto;
import eventi.EventoPrenota;
import eventi.ListaEventi;
import model.Wallet;
import service.ClientService;

import java.util.List;
import java.util.Scanner;

public class EseguiAcquistoInterattivo implements Command {

    private final ClientService clientService;

    public EseguiAcquistoInterattivo(ClientService clientService) {
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

        int scelta = leggiNumeroValido(scanner, 1, tratte.size());
        TrattaDTO trattaScelta = tratte.get(scelta - 1);

        System.out.print("Inserisci classe (BASE, ARGENTO, GOLD): ");
        ClasseServizio classe = ClasseServizio.valueOf(scanner.nextLine().toUpperCase());

        System.out.print("Inserisci tipo prezzo (BASE, FLEX): ");
        TipoPrezzo prezzo = TipoPrezzo.valueOf(scanner.nextLine().toUpperCase());

        RichiestaDTO acquisto = new RichiestaDTO.Builder()
                .tipo("ACQUISTA")
                .idCliente(cliente.getId().toString())
                .tratta(trattaScelta)
                .classeServizio(classe)
                .tipoPrezzo(prezzo)
                .build();

        System.out.println("➡️ Invio richiesta di acquisto...");
        RispostaDTO rispostaAcquisto = clientService.inviaRichiesta(acquisto);

        if (rispostaAcquisto.getBiglietto() != null) {
            BigliettoDTO biglietto = rispostaAcquisto.getBiglietto();
            ListaEventi.getInstance().notifica(new EventoAcquisto(biglietto));
            System.out.println("✅ Acquisto riuscito: " + biglietto);
        } else {
            System.out.println("❌ Acquisto fallito: " + rispostaAcquisto.getMessaggio());
        }
    }

    private int leggiNumeroValido(Scanner scanner, int min, int max) {
        while (true) {
            System.out.print("👉 Inserisci un numero tra " + min + " e " + max + ": ");
            String input = scanner.nextLine();
            try {
                int numero = Integer.parseInt(input);
                if (numero >= min && numero <= max) {
                    return numero;
                } else {
                    System.out.println("❌ Numero fuori dal range valido.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Inserisci un numero valido.");
            }
        }
    }
}