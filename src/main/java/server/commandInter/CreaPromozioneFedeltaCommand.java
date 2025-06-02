package commandInter;

import eventi.EventoPromoFedelta;
import eventi.ListaEventiS;
import model.PromozioneFedelta;
import persistence.MemoriaPromozioni;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * 💎 CREA PROMOZIONE FEDELTÀ COMMAND - FIXED
 *
 * FIX: Ora salva in memoria E genera evento per broadcast
 */
public class CreaPromozioneFedeltaCommand implements Runnable {

    private final MemoriaPromozioni memoriaPromozioni;

    // ✅ CONSTRUCTOR con dependency injection
    public CreaPromozioneFedeltaCommand(MemoriaPromozioni memoriaPromozioni) {
        this.memoriaPromozioni = memoriaPromozioni;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("💎 === CREAZIONE PROMOZIONE FEDELTÀ ===");

        System.out.print("🎯 Nome promozione: ");
        String nome = scanner.nextLine().trim();

        System.out.print("📝 Descrizione: ");
        String descrizione = scanner.nextLine().trim();

        System.out.print("💸 Sconto fedeltà (es. 0.30 per 30%): ");
        double sconto = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("📅 Data inizio (YYYY-MM-DD): ");
        LocalDate inizio = LocalDate.parse(scanner.nextLine().trim());

        System.out.print("📅 Data fine (YYYY-MM-DD): ");
        LocalDate fine = LocalDate.parse(scanner.nextLine().trim());

        // Crea promozione
        PromozioneFedelta promozione = new PromozioneFedelta(nome, descrizione, sconto, inizio, fine);

        // ✅ FIX 1: SALVA in memoria (persistenza)
        memoriaPromozioni.aggiungiPromozione(promozione);
        System.out.println("💾 Promozione salvata in memoria");

        // ✅ FIX 2: GENERA evento per broadcast ai client
        ListaEventiS.getInstance().notifica(new EventoPromoFedelta(promozione));
        System.out.println("📡 Evento generato per broadcast ai client");

        System.out.println("✅ Promozione fedeltà creata e notificata con successo!");
        System.out.println("🎯 Nome: " + nome);
        System.out.println("💸 Sconto: " + (sconto * 100) + "%");
        System.out.println("📅 Periodo: " + inizio + " → " + fine);
    }
}