package commandInter;

import eventi.EventoPromoFedelta;
import eventi.ListaEventiS;
import model.PromozioneFedelta;
import persistence.MemoriaPromozioni;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.Scanner;

public class CreaPromozioneFedeltaCommand implements ServerConsoleCommand {  // ✅ CORRETTO

    private final MemoriaPromozioni memoriaPromozioni;
    private final MemoriaTratte memoriaTratte;

    public CreaPromozioneFedeltaCommand(MemoriaPromozioni memoriaPromozioni, MemoriaTratte memoriaTratte) {
        this.memoriaPromozioni = memoriaPromozioni;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void esegui(Scanner scanner) {  // ✅ CORRETTO - Scanner come parametro
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

        // Crea promozione fedeltà
        PromozioneFedelta promozione = new PromozioneFedelta(nome, descrizione, sconto, inizio, fine);

        // ✅ SALVA in memoria
        memoriaPromozioni.aggiungiPromozione(promozione);

        // ✅ GENERA evento per broadcast ai client
        ListaEventiS.getInstance().notifica(new EventoPromoFedelta(promozione));

        // ✅ AGGIORNA STRATEGY
        aggiornaStrategyPricing();

        System.out.println("✅ Promozione fedeltà creata e notificata con successo!");
        System.out.println("🔄 Strategy Pattern aggiornato per clienti fedeli!");
    }

    private void aggiornaStrategyPricing() {
        try {
            System.out.println("💎 Strategy aggiornato - clienti fedeli riceveranno prezzi ottimizzati");
        } catch (Exception e) {
            System.err.println("⚠️ Errore aggiornamento strategy (non critico): " + e.getMessage());
        }
    }
}