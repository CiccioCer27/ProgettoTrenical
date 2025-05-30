package commandInter;

import factory.PromozioneGeneraleFactory;
import model.Promozione;
import observer.EventDispatcher;
import eventi.EventoPromoGen;
import persistence.MemoriaPromozioni;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

        double sconto;
        while (true) {
            try {
                System.out.print("💸 Sconto (es. 0.2 per 20%): ");
                sconto = Double.parseDouble(scanner.nextLine());
                if (sconto > 0 && sconto <= 1) break;
                System.out.println("❌ Inserisci uno sconto valido tra 0 e 1.");
            } catch (NumberFormatException e) {
                System.out.println("❌ Formato non valido. Riprova.");
            }
        }

        LocalDate inizio;
        while (true) {
            try {
                System.out.print("📅 Data inizio (YYYY-MM-DD): ");
                inizio = LocalDate.parse(scanner.nextLine());
                break;
            } catch (DateTimeParseException e) {
                System.out.println("❌ Data non valida. Riprova.");
            }
        }

        LocalDate fine;
        while (true) {
            try {
                System.out.print("📅 Data fine (YYYY-MM-DD): ");
                fine = LocalDate.parse(scanner.nextLine());
                if (fine.isAfter(inizio)) break;
                System.out.println("❌ La data di fine deve essere dopo quella di inizio.");
            } catch (DateTimeParseException e) {
                System.out.println("❌ Data non valida. Riprova.");
            }
        }

        Promozione promozione = factory.creaPromozione(nome, descrizione, sconto, inizio, fine);
        memoriaPromozioni.aggiungiPromozione(promozione);
        dispatcher.dispatch(new EventoPromoGen(promozione));

        System.out.println("✅ Promozione creata e notificata con successo!");
    }
}