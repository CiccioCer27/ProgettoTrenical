package commandInter;

import eventi.EventoModificaTratta;
import eventi.ListaEventiS;
import model.Tratta;
import persistence.MemoriaTratte;
import util.InputUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class ModificaTrattaCommand implements ServerConsoleCommand {

    private final MemoriaTratte memoriaTratte;

    public ModificaTrattaCommand(MemoriaTratte memoriaTratte) {
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void esegui(Scanner scanner) {
        List<Tratta> tratte = memoriaTratte.getTutteTratte();
        if (tratte.isEmpty()) {
            System.out.println("❌ Nessuna tratta presente in memoria.");
            return;
        }

        System.out.println("📍 Tratte disponibili:");
        tratte.forEach(t ->
                System.out.println("→ " + t.getId() + " | " + t.getStazionePartenza() + " → " + t.getStazioneArrivo() +
                        " | Ora: " + t.getOra() + " | Binario: " + t.getBinario()));

        System.out.print("✏️ Inserisci ID tratta da modificare: ");
        UUID id;
        try {
            id = UUID.fromString(scanner.nextLine().trim());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ ID non valido.");
            return;
        }

        Tratta tratta = memoriaTratte.getTrattaById(id);
        if (tratta == null) {
            System.out.println("❌ Tratta non trovata.");
            return;
        }

        System.out.print("⚙️ Modificare (1=ora, 2=binario, 3=entrambi): ");
        String scelta = scanner.nextLine().trim();

        LocalTime nuovaOra = tratta.getOra();
        int nuovoBinario = tratta.getBinario();

        if (scelta.equals("1") || scelta.equals("3")) {
            nuovaOra = InputUtils.leggiOra(scanner, "⌚ Nuova ora (HH:mm): ");
        }
        if (scelta.equals("2") || scelta.equals("3")) {
            nuovoBinario = InputUtils.leggiInt(scanner, "🛤 Nuovo binario: ");
        }

        Tratta trattaModificata = new Tratta(
                tratta.getId(),
                tratta.getStazionePartenza(),
                tratta.getStazioneArrivo(),
                tratta.getData(),
                nuovaOra,
                nuovoBinario,
                tratta.getTreno(),
                tratta.getPrezzi()
        );

        memoriaTratte.rimuoviTratteDelGiorno(tratta.getData());
        memoriaTratte.aggiungiTratta(trattaModificata);

        ListaEventiS.getInstance().notifica(new EventoModificaTratta(trattaModificata.getId()));
        System.out.println("✅ Tratta aggiornata. Notifica inviata ai client.");
    }
}