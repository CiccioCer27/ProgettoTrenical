package commandInter;

import eventi.EventoPromoTratta;
import model.PromozioneTratta;
import observer.EventDispatcher;
import persistence.MemoriaPromozioni;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreaPromozioneTrattaCommand implements Runnable {

    private final MemoriaPromozioni memoria;
    private final MemoriaTratte memoriaTratte;
    private final EventDispatcher dispatcher;

    public CreaPromozioneTrattaCommand(MemoriaPromozioni memoria, MemoriaTratte memoriaTratte, EventDispatcher dispatcher) {
        this.memoria = memoria;
        this.memoriaTratte = memoriaTratte;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("🔧 Creazione nuova promozione per tratte");
        System.out.print("Nome promozione: ");
        String nome = scanner.nextLine();

        System.out.print("Descrizione promozione: ");
        String descrizione = scanner.nextLine();

        System.out.print("Sconto (0.1 = 10%, 0.3 = 30%): ");
        double sconto = Double.parseDouble(scanner.nextLine());

        System.out.print("Durata in giorni: ");
        int giorni = Integer.parseInt(scanner.nextLine());

        System.out.println("📋 Tratte disponibili:");
        var tratte = memoriaTratte.getTutteTratte();
        for (int i = 0; i < tratte.size(); i++) {
            System.out.println((i + 1) + ") " + tratte.get(i).getStazionePartenza() + " → " + tratte.get(i).getStazioneArrivo() + " (" + tratte.get(i).getId() + ")");
        }

        System.out.print("Inserisci gli indici delle tratte separate da virgola: ");
        String input = scanner.nextLine();
        Set<UUID> tratteTarget = Set.of(input.split(",")).stream()
                .map(String::trim)
                .map(Integer::parseInt)
                .map(i -> tratte.get(i - 1).getId())
                .collect(Collectors.toSet());

        LocalDate inizio = LocalDate.now();
        LocalDate fine = inizio.plusDays(giorni);

        PromozioneTratta promo = new PromozioneTratta(nome, descrizione, sconto, inizio, fine, tratteTarget);

        memoria.aggiungiPromozione(promo);
        dispatcher.dispatch(new EventoPromoTratta(promo));

        System.out.println("✅ Promozione creata e notificata!");
    }
}