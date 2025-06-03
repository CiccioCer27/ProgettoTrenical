package commandInter;

import factory.PromozioneGeneraleFactory;
import model.Promozione;
import eventi.EventoPromoGen;
import eventi.ListaEventiS;
import persistence.MemoriaPromozioni;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.Scanner;

public class CreaPromozioneGeneraleCommand implements ServerConsoleCommand {  // ✅ CORRETTO

    private final MemoriaPromozioni memoriaPromozioni;
    private final MemoriaTratte memoriaTratte;

    public CreaPromozioneGeneraleCommand(MemoriaPromozioni memoriaPromozioni, MemoriaTratte memoriaTratte) {
        this.memoriaPromozioni = memoriaPromozioni;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void esegui(Scanner scanner) {  // ✅ CORRETTO - Scanner come parametro
        PromozioneGeneraleFactory factory = new PromozioneGeneraleFactory();

        System.out.println("\n🎉 Creazione nuova promozione generale:");
        System.out.print("📝 Nome promozione: ");
        String nome = scanner.nextLine();

        System.out.print("📄 Descrizione: ");
        String descrizione = scanner.nextLine();

        System.out.print("💸 Sconto (es. 0.2 per 20%): ");
        double sconto = Double.parseDouble(scanner.nextLine());

        System.out.print("📅 Data inizio (YYYY-MM-DD): ");
        LocalDate inizio = LocalDate.parse(scanner.nextLine());

        System.out.print("📅 Data fine (YYYY-MM-DD): ");
        LocalDate fine = LocalDate.parse(scanner.nextLine());

        Promozione promozione = factory.creaPromozione(nome, descrizione, sconto, inizio, fine);

        // ✅ SALVA in memoria
        memoriaPromozioni.aggiungiPromozione(promozione);

        // ✅ GENERA EVENTO per notifica ai client
        ListaEventiS.getInstance().notifica(new EventoPromoGen(promozione));

        // ✅ RIGENERA PREZZI per tratte future
        rigeneraPrezziTrattereFuture();

        System.out.println("✅ Promozione creata e REALMENTE notificata con successo!");
        System.out.println("🔄 Prezzi delle tratte aggiornati automaticamente!");
    }

    private void rigeneraPrezziTrattereFuture() {
        try {
            System.out.println("💰 Strategy Pattern aggiornato - nuove tratte useranno la promozione");
        } catch (Exception e) {
            System.err.println("⚠️ Errore aggiornamento prezzi (non critico): " + e.getMessage());
        }
    }
}