package command;

import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import service.ClientService;

import java.util.Scanner;

public class CercaTratteCommand implements Command {
    private final ClientService clientService;
    private final Scanner scanner;

    public CercaTratteCommand(ClientService clientService, Scanner scanner) {
        this.clientService = clientService;
        this.scanner = scanner;
    }

    @Override
    public void esegui() {
        System.out.println("\n🔍 RICERCA TRATTE");
        System.out.println("-".repeat(18));

        try {
            System.out.print("Filtro (data;partenza;arrivo;fascia) [vuoto=tutte]: ");
            String filtro = scanner.nextLine().trim();
            if (filtro.isEmpty()) filtro = ";;;";

            RichiestaDTO richiesta = new RichiestaDTO.Builder()
                    .tipo("FILTRA")
                    .messaggioExtra(filtro)
                    .build();

            RispostaDTO risposta = clientService.inviaRichiesta(richiesta);

            if (risposta.getTratte() != null && !risposta.getTratte().isEmpty()) {
                System.out.println("\n📋 TRATTE TROVATE (" + risposta.getTratte().size() + "):");
                for (int i = 0; i < Math.min(risposta.getTratte().size(), 15); i++) {
                    TrattaDTO tratta = risposta.getTratte().get(i);
                    System.out.println((i + 1) + ") " + formatTratta(tratta));
                }
                if (risposta.getTratte().size() > 15) {
                    System.out.println("... e altre " + (risposta.getTratte().size() - 15) + " tratte");
                }
            } else {
                System.out.println("❌ Nessuna tratta trovata");
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca: " + e.getMessage());
        }
    }

    private String formatTratta(TrattaDTO tratta) {
        try {
            return String.format("%s → %s | %s %s | Bin.%d | Treno: %s",
                    tratta.getStazionePartenza(),
                    tratta.getStazioneArrivo(),
                    tratta.getData(),
                    tratta.getOra(),
                    tratta.getBinario(),
                    tratta.getTreno() != null ? tratta.getTreno().getNomeCommerciale() : "N/A");
        } catch (Exception e) {
            return "Tratta non valida: " + e.getMessage();
        }
    }
}