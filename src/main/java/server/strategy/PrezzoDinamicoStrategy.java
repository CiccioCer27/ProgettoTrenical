package strategy;

import model.Tratta;
import model.Promozione;
import enums.ClasseServizio;
import enums.TipoPrezzo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 📈 STRATEGIA PREZZO DINAMICO
 * Modifica i prezzi in base a:
 * - Domanda prevista (orari di punta)
 * - Giorni della settimana
 * - Stagionalità
 * - Tempo rimanente alla partenza
 */
public class PrezzoDinamicoStrategy implements PrezzoStrategy {

    @Override
    public PrezzoCalcolato calcolaPrezzoFinale(Tratta tratta, ClasseServizio classeServizio,
                                               TipoPrezzo tipoPrezzo, boolean isClienteFedele,
                                               UUID idCliente, List<Promozione> promozioniAttive) {

        double prezzoBase = tratta.getPrezzi().get(classeServizio).getPrezzo(tipoPrezzo);
        double moltiplicatore = 1.0;
        List<String> fattori = new ArrayList<>();

        // 🕐 FATTORE ORARIO (rush hour)
        LocalTime ora = tratta.getOra();
        if ((ora.isAfter(LocalTime.of(7, 0)) && ora.isBefore(LocalTime.of(9, 30))) ||
                (ora.isAfter(LocalTime.of(17, 30)) && ora.isBefore(LocalTime.of(20, 0)))) {
            moltiplicatore += 0.15; // +15% negli orari di punta
            fattori.add("Orario di punta (+15%)");
        } else if (ora.isAfter(LocalTime.of(22, 0)) || ora.isBefore(LocalTime.of(6, 0))) {
            moltiplicatore -= 0.10; // -10% negli orari notturni
            fattori.add("Orario notturno (-10%)");
        }

        // 📅 FATTORE GIORNO SETTIMANA
        LocalDate data = tratta.getData();
        if (data.getDayOfWeek() == java.time.DayOfWeek.FRIDAY ||
                data.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            moltiplicatore += 0.10; // +10% venerdì e domenica
            fattori.add("Weekend (+10%)");
        }

        // ⏰ FATTORE ANTICIPO PRENOTAZIONE
        long giorniAnticipo = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), data);
        if (giorniAnticipo >= 30) {
            moltiplicatore -= 0.20; // -20% per prenotazioni molto anticipate
            fattori.add("Prenotazione anticipata (-20%)");
        } else if (giorniAnticipo <= 1) {
            moltiplicatore += 0.25; // +25% per prenotazioni last-minute
            fattori.add("Last minute (+25%)");
        } else if (giorniAnticipo <= 7) {
            moltiplicatore += 0.10; // +10% per prenotazioni sotto settimana
            fattori.add("Prenotazione ravvicinata (+10%)");
        }

        // 🎯 FATTORE CLASSE SERVIZIO
        switch (classeServizio) {
            case GOLD -> {
                moltiplicatore += 0.05; // Piccolo extra per classe premium
                fattori.add("Classe Gold (+5%)");
            }
            case BASE -> {
                moltiplicatore -= 0.05; // Piccolo sconto per classe base
                fattori.add("Classe Base (-5%)");
            }
        }

        // ✅ APPLICA MOLTIPLICATORE
        double prezzoFinale = prezzoBase * moltiplicatore;
        double differenza = prezzoFinale - prezzoBase;

        String descrizione = "Prezzo dinamico";
        if (!fattori.isEmpty()) {
            descrizione += ": " + String.join(", ", fattori);
        }

        return new PrezzoCalcolato(
                prezzoBase,
                prezzoFinale,
                Math.abs(differenza),
                descrizione,
                fattori,
                tipoPrezzo
        );
    }

    @Override
    public boolean isApplicabile(Tratta tratta, ClasseServizio classeServizio, boolean isClienteFedele) {
        // Applicabile solo per tratte future (non same-day)
        return tratta.getData().isAfter(LocalDate.now());
    }

    @Override
    public int getPriorita() {
        return 5; // Priorità media
    }

    @Override
    public String getNome() {
        return "Dinamico";
    }
}