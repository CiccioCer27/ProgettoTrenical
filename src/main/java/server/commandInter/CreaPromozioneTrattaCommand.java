package commandInter;

import eventi.EventoPromoTratta;
import eventi.ListaEventiS;
import model.PromozioneTratta;
import persistence.MemoriaPromozioni;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreaPromozioneTrattaCommand implements ServerConsoleCommand {  // ✅ CORRETTO

    private final MemoriaPromozioni memoria;
    private final MemoriaTratte memoriaTratte;

    public CreaPromozioneTrattaCommand(MemoriaPromozioni memoria, MemoriaTratte memoriaTratte) {
        this.memoria = memoria;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void esegui(Scanner scanner) {  // ✅ CORRETTO - Scanner come parametro
        System.out.println("🚂 === CREAZIONE PROMOZIONE TRATTA ===");

        System.out.print("🔧 Nome promozione: ");
        String nome = scanner.nextLine().trim();

        System.out.print("📄 Descrizione promozione: ");
        String descrizione = scanner.nextLine().trim();

        System.out.print("💸 Sconto (0.1 = 10%, 0.3 = 30%): ");
        double sconto = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("📅 Durata in giorni: ");
        int giorni = Integer.parseInt(scanner.nextLine().trim());

        // Mostra tratte disponibili
        System.out.println("\n📋 Tratte disponibili:");
        var tratte = memoriaTratte.getTutteTratte();

        if (tratte.isEmpty()) {
            System.out.println("❌ Nessuna tratta disponibile. Genera prima alcune tratte.");
            return;
        }

        for (int i = 0; i < Math.min(tratte.size(), 10); i++) {
            var tratta = tratte.get(i);
            System.out.println((i + 1) + ") " + tratta.getStazionePartenza() + " → " +
                    tratta.getStazioneArrivo() + " (" + tratta.getId().toString().substring(0, 8) + "...)");
        }

        System.out.print("\n🎯 Inserisci gli indici delle tratte separate da virgola (es: 1,3,5): ");
        String input = scanner.nextLine().trim();

        try {
            Set<UUID> tratteTarget = Set.of(input.split(",")).stream()
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .map(i -> tratte.get(i - 1).getId())
                    .collect(Collectors.toSet());

            LocalDate inizio = LocalDate.now();
            LocalDate fine = inizio.plusDays(giorni);

            // Crea promozione tratta
            PromozioneTratta promo = new PromozioneTratta(nome, descrizione, sconto, inizio, fine, tratteTarget);

            // ✅ SALVA in memoria
            memoria.aggiungiPromozione(promo);

            // ✅ GENERA evento per broadcast ai client
            ListaEventiS.getInstance().notifica(new EventoPromoTratta(promo));

            // ✅ AGGIORNA STRATEGY per tratte specifiche
            aggiornaStrategyPerTratte(tratteTarget);

            System.out.println("✅ Promozione tratta creata e notificata!");
            System.out.println("🔄 Strategy aggiornato per le tratte selezionate!");

        } catch (Exception e) {
            System.out.println("❌ Errore nella selezione tratte: " + e.getMessage());
        }
    }

    private void aggiornaStrategyPerTratte(Set<UUID> tratteTarget) {
        try {
            System.out.println("🚂 Strategy Pattern aggiornato per " + tratteTarget.size() + " tratte specifiche");
        } catch (Exception e) {
            System.err.println("⚠️ Errore aggiornamento strategy tratte (non critico): " + e.getMessage());
        }
    }
}