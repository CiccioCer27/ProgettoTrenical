package factory;

import enums.ClasseServizio;
import model.Prezzo;
import model.Tratta;
import model.Treno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class TrattaFactoryConcrete implements TrattaFactory {

    private static final List<String> STAZIONI = List.of("ReggioCalabria","Rende","Strongoli","Milano", "Roma", "Napoli", "Torino", "Firenze");

    @Override
    public List<Tratta> generaTratte(LocalDate data) {
        List<Tratta> tratte = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            String partenza = STAZIONI.get(random.nextInt(STAZIONI.size()));
            String arrivo;
            do {
                arrivo = STAZIONI.get(random.nextInt(STAZIONI.size()));
            } while (arrivo.equals(partenza));

            int binario = random.nextInt(10) + 1;
            LocalTime ora = LocalTime.of(random.nextInt(10) + 8, 0);

            Treno treno = new Treno.Builder()
                    .numero(100 + i)
                    .tipologia("Frecciarossa")
                    .capienzaTotale(100)
                    .wifiDisponibile(true)
                    .preseElettriche(true)
                    .ariaCondizionata(true)
                    .serviziRistorazione("Snack, Bevande")
                    .accessibileDisabili(true)
                    .nomeCommerciale("Treno " + i)
                    .build();

            Map<ClasseServizio, Prezzo> prezzi = new HashMap<>();
            for (ClasseServizio cs : ClasseServizio.values()) {
                double base = 10 + random.nextDouble() * 20;
                double promo = base * 0.8;
                double fed = base * 0.6;
                prezzi.put(cs, new Prezzo(
                        base * getMoltiplicatoreClasse(cs),
                        promo * getMoltiplicatoreClasse(cs),
                        fed * getMoltiplicatoreClasse(cs)
                ));
            }

            Tratta tratta = new Tratta(UUID.randomUUID(), partenza, arrivo, data, ora, binario, treno, prezzi);
            tratte.add(tratta);
        }

        return tratte;
    }

    private double getMoltiplicatoreClasse(ClasseServizio classe) {
        return switch (classe) {
            case BASE -> 1.0;
            case ARGENTO -> 1.3;
            case GOLD -> 1.6;
        };
    }
}