package commandInter;

import factory.PromozioneGeneraleFactory;
import model.Promozione;
import observer.EventDispatcher;
import eventi.EventoPromoGen;
import eventi.ListaEventiS;  // ✅ IMPORTANTE: Eventi server
import persistence.MemoriaPromozioni;

import java.time.LocalDate;
import java.util.Scanner;

public class CreaPromozioneGeneraleCommand implements Runnable {

    private final MemoriaPromozioni memoriaPromozioni;
    private final EventDispatcher dispatcher;

    public CreaPromozioneGeneraleCommand(MemoriaPromozioni memoriaPromozioni, EventDispatcher dispatcher) {
        this.memoriaPromozioni = memoriaPromozioni;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
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

        // ✅ FIX: GENERA EVENTO per notifica ai client
        ListaEventiS.getInstance().notifica(new EventoPromoGen(promozione));

        System.out.println("✅ Promozione creata e REALMENTE notificata con successo!");
    }
}
