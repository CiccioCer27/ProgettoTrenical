package commandInter;

import eventi.EventoPromoFedelta;
import eventi.ListaEventiS;
import model.PromozioneFedelta;

import java.time.LocalDate;
import java.util.Scanner;

public class CreaPromozioneFedeltaCommand implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("🎯 Nome promozione fedeltà:");
        String nome = scanner.nextLine();

        System.out.println("📝 Descrizione:");
        String descrizione = scanner.nextLine();

        System.out.println("💸 Sconto fedeltà (es. 0.30):");
        double sconto = Double.parseDouble(scanner.nextLine());

        System.out.println("📅 Data inizio (AAAA-MM-GG):");
        LocalDate inizio = LocalDate.parse(scanner.nextLine());

        System.out.println("📅 Data fine (AAAA-MM-GG):");
        LocalDate fine = LocalDate.parse(scanner.nextLine());

        PromozioneFedelta promozione = new PromozioneFedelta(nome, descrizione, sconto, inizio, fine);
        ListaEventiS.getInstance().notifica(new EventoPromoFedelta(promozione));

        System.out.println("✅ Promozione fedeltà attivata e notificata.");
    }
}