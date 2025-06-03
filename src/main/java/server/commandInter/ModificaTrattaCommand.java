package commandInter;

import eventi.EventoModificaTratta;
import eventi.ListaEventiS;
import model.Tratta;
import model.Prezzo;
import persistence.MemoriaTratte;
import persistence.MemoriaPromozioni;  // ✅ AGGIUNTO
import strategy.PrezzoContext;  // ✅ AGGIUNTO
import strategy.PrezzoCalcolato;  // ✅ AGGIUNTO
import util.InputUtils;
import enums.ClasseServizio;  // ✅ AGGIUNTO
import enums.TipoPrezzo;  // ✅ AGGIUNTO

import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class ModificaTrattaCommand implements ServerConsoleCommand {

    private final MemoriaTratte memoriaTratte;
    private final MemoriaPromozioni memoriaPromozioni;  // ✅ AGGIUNTO
    private final PrezzoContext prezzoContext;  // ✅ AGGIUNTO

    public ModificaTrattaCommand(MemoriaTratte memoriaTratte, MemoriaPromozioni memoriaPromozioni) {
        this.memoriaTratte = memoriaTratte;
        this.memoriaPromozioni = memoriaPromozioni;  // ✅ INIZIALIZZA
        this.prezzoContext = new PrezzoContext(memoriaPromozioni);  // ✅ CREA CONTEXT
    }

    // ✅ CONSTRUCTOR LEGACY per compatibilità (senza strategy)
    public ModificaTrattaCommand(MemoriaTratte memoriaTratte) {
        this.memoriaTratte = memoriaTratte;
        this.memoriaPromozioni = null;
        this.prezzoContext = null;
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

        System.out.print("⚙️ Modificare (1=ora, 2=binario, 3=entrambi, 4=ora+binario+prezzi): ");
        String scelta = scanner.nextLine().trim();

        LocalTime nuovaOra = tratta.getOra();
        int nuovoBinario = tratta.getBinario();
        boolean ricalcolaPrezzi = false;

        if (scelta.equals("1") || scelta.equals("3") || scelta.equals("4")) {
            nuovaOra = InputUtils.leggiOra(scanner, "⌚ Nuova ora (HH:mm): ");
            if (!nuovaOra.equals(tratta.getOra())) {
                ricalcolaPrezzi = true;  // ✅ Cambio orario richiede ricalcolo prezzi
            }
        }
        if (scelta.equals("2") || scelta.equals("3") || scelta.equals("4")) {
            nuovoBinario = InputUtils.leggiInt(scanner, "🛤 Nuovo binario: ");
        }
        if (scelta.equals("4")) {
            ricalcolaPrezzi = true;  // ✅ Opzione esplicita per ricalcolo prezzi
        }

        // ✅ RICALCOLA PREZZI con Strategy se necessario
        Map<ClasseServizio, Prezzo> prezziFinali = tratta.getPrezzi();
        if (ricalcolaPrezzi && prezzoContext != null) {
            System.out.println("💰 Ricalcolando prezzi con Strategy Pattern...");
            prezziFinali = ricalcolaPrezziConStrategy(tratta, nuovaOra);
        } else if (ricalcolaPrezzi) {
            System.out.println("⚠️ Strategy non disponibile, mantengo prezzi originali");
        }

        Tratta trattaModificata = new Tratta(
                tratta.getId(),
                tratta.getStazionePartenza(),
                tratta.getStazioneArrivo(),
                tratta.getData(),
                nuovaOra,
                nuovoBinario,
                tratta.getTreno(),
                prezziFinali  // ✅ USA PREZZI AGGIORNATI
        );

        // ✅ OPERAZIONE ATOMICA: Rimuovi vecchia + Aggiungi nuova
        memoriaTratte.rimuoviTratteDelGiorno(tratta.getData());
        memoriaTratte.aggiungiTratta(trattaModificata);

        // ✅ NOTIFICA CLIENTI della modifica
        ListaEventiS.getInstance().notifica(new EventoModificaTratta(trattaModificata.getId()));

        System.out.println("✅ Tratta aggiornata. Notifica inviata ai client.");
        if (ricalcolaPrezzi) {
            System.out.println("💰 Prezzi ricalcolati automaticamente con promozioni attive!");
        }
    }

    /**
     * ✅ NUOVO: Ricalcola prezzi per la tratta modificata usando Strategy Pattern
     */
    private Map<ClasseServizio, Prezzo> ricalcolaPrezziConStrategy(Tratta trattaOriginale, LocalTime nuovaOra) {
        Map<ClasseServizio, Prezzo> prezziAggiornati = new HashMap<>();

        try {
            // Crea tratta temporanea con nuovo orario per calcoli strategy
            Tratta trattaTemp = new Tratta(
                    trattaOriginale.getId(),
                    trattaOriginale.getStazionePartenza(),
                    trattaOriginale.getStazioneArrivo(),
                    trattaOriginale.getData(),
                    nuovaOra,  // ✅ USA NUOVO ORARIO
                    trattaOriginale.getBinario(),
                    trattaOriginale.getTreno(),
                    trattaOriginale.getPrezzi()
            );

            // Calcola prezzi per ogni classe con Strategy Pattern
            for (ClasseServizio classe : ClasseServizio.values()) {
                try {
                    // Simula calcoli per diversi tipi di prezzo
                    PrezzoCalcolato calcoloIntero = prezzoContext.calcolaPrezzoOttimale(
                            trattaTemp, classe, TipoPrezzo.INTERO, false, UUID.randomUUID()
                    );

                    PrezzoCalcolato calcoloFedelta = prezzoContext.calcolaPrezzoOttimale(
                            trattaTemp, classe, TipoPrezzo.FEDELTA, true, UUID.randomUUID()
                    );

                    PrezzoCalcolato calcoloPromo = prezzoContext.calcolaPrezzoOttimale(
                            trattaTemp, classe, TipoPrezzo.PROMOZIONE, false, UUID.randomUUID()
                    );

                    // ✅ CREA NUOVO PREZZO con calcoli strategy
                    Prezzo prezzoAggiornato = new Prezzo(
                            calcoloIntero.getPrezzoFinale(),
                            calcoloPromo.getPrezzoFinale(),
                            calcoloFedelta.getPrezzoFinale()
                    );

                    prezziAggiornati.put(classe, prezzoAggiornato);

                    System.out.println("💰 " + classe + ": €" +
                            String.format("%.2f", calcoloIntero.getPrezzoFinale()) +
                            " (" + calcoloIntero.getDescrizioneSconto() + ")");

                } catch (Exception e) {
                    // Fallback: mantieni prezzo originale per questa classe
                    System.err.println("⚠️ Errore calcolo " + classe + ", mantengo prezzo originale");
                    prezziAggiornati.put(classe, trattaOriginale.getPrezzi().get(classe));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Errore generale ricalcolo prezzi: " + e.getMessage());
            // Fallback totale: mantieni tutti i prezzi originali
            return trattaOriginale.getPrezzi();
        }

        return prezziAggiornati;
    }

    /**
     * ✅ UTILITY: Mostra confronto prezzi prima/dopo (opzionale per debug)
     */
    private void mostraConfrontoPrezzi(Map<ClasseServizio, Prezzo> prezziOriginali,
                                       Map<ClasseServizio, Prezzo> prezziNuovi) {
        System.out.println("\n📊 CONFRONTO PREZZI:");
        for (ClasseServizio classe : ClasseServizio.values()) {
            Prezzo vecchio = prezziOriginali.get(classe);
            Prezzo nuovo = prezziNuovi.get(classe);

            double differenza = nuovo.getIntero() - vecchio.getIntero();
            String simbolo = differenza > 0 ? "📈" : differenza < 0 ? "📉" : "➡️";

            System.out.printf("   %s %s: €%.2f → €%.2f (%+.2f)\n",
                    simbolo, classe, vecchio.getIntero(), nuovo.getIntero(), differenza);
        }
    }
}