package commandInter;

import eventi.EventoPromoTratta;
import eventi.ListaEventiS;  // ✅ CORRETTO: Eventi server
import model.PromozioneTratta;
import persistence.MemoriaPromozioni;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🚂 CREA PROMOZIONE TRATTA COMMAND - FIXED
 *
 * FIX: Usa ListaEventiS invece di EventDispatcher
 */
public class CreaPromozioneTrattaCommand implements Runnable {

    private final MemoriaPromozioni memoria;
    private final MemoriaTratte memoriaTratte;

    // ✅ SIMPLIFIED CONSTRUCTOR (senza EventDispatcher)
    public CreaPromozioneTrattaCommand(MemoriaPromozioni memoria, MemoriaTratte memoriaTratte) {
        this.memoria = memoria;
        this.memoriaTratte = memoriaTratte;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

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

        for (int i = 0; i < tratte.size(); i++) {
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
            System.out.println("💾 Promozione salvata in memoria");

            // ✅ FIX: USA ListaEventiS invece di EventDispatcher
            ListaEventiS.getInstance().notifica(new EventoPromoTratta(promo));
            System.out.println("📡 Evento generato per broadcast ai client");

            System.out.println("✅ Promozione tratta creata e notificata!");
            System.out.println("🎯 Nome: " + nome);
            System.out.println("💸 Sconto: " + (sconto * 100) + "%");
            System.out.println("🚂 Tratte coinvolte: " + tratteTarget.size());
            System.out.println("📅 Periodo: " + inizio + " → " + fine);

        } catch (Exception e) {
            System.out.println("❌ Errore nella selezione tratte: " + e.getMessage());
            System.out.println("💡 Usa il formato: 1,2,3 (numeri separati da virgole)");
        }
    }
}
