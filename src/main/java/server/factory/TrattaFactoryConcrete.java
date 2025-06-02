package factory;

import enums.ClasseServizio;
import model.Prezzo;
import model.Tratta;
import model.Treno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class TrattaFactoryConcrete implements TrattaFactory {

    private final TrattaGenerationConfig config;
    private final Random random;

    public TrattaFactoryConcrete(TrattaGenerationConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public TrattaFactoryConcrete() {
        this(TrattaGenerationConfig.defaultConfig());
    }

    @Override
    public List<Tratta> generaTratte(LocalDate data) {
        List<Tratta> tratte = new ArrayList<>();

        for (int i = 0; i < config.getNumeroTrattePerGiorno(); i++) {
            Tratta tratta = generaSingolaTratta(data, i);
            tratte.add(tratta);
        }

        return tratte;
    }

    private Tratta generaSingolaTratta(LocalDate data, int indice) {
        String partenza = selezionaStazioneRandom();
        String arrivo = selezionaStazioneDiversa(partenza);
        LocalTime ora = generaOrarioRandom();
        int binario = random.nextInt(config.getMaxBinario()) + 1;
        Treno treno = generaTrenoRandom(indice);
        Map<ClasseServizio, Prezzo> prezzi = generaPrezziPerTutteLeClassi();

        return new Tratta(UUID.randomUUID(), partenza, arrivo, data, ora, binario, treno, prezzi);
    }

    private String selezionaStazioneRandom() {
        List<String> stazioni = config.getStazioni();
        return stazioni.get(random.nextInt(stazioni.size()));
    }

    private String selezionaStazioneDiversa(String partenza) {
        List<String> stazioni = config.getStazioni();
        String arrivo;
        do {
            arrivo = stazioni.get(random.nextInt(stazioni.size()));
        } while (arrivo.equals(partenza));
        return arrivo;
    }

    private LocalTime generaOrarioRandom() {
        int oraMin = config.getOrarioInizio().getHour();
        int oraMax = config.getOrarioFine().getHour();
        int oraRandom = oraMin + random.nextInt(oraMax - oraMin);
        return LocalTime.of(oraRandom, 0);
    }

    private Treno generaTrenoRandom(int indice) {
        List<String> tipiTreno = config.getTipiTreno();
        String tipo = tipiTreno.get(random.nextInt(tipiTreno.size()));

        return new Treno.Builder()
                .numero(config.getNumeroTrenoBase() + indice)
                .tipologia(tipo)
                .capienzaTotale(config.getCapienzaBase() + random.nextInt(50))
                .wifiDisponibile(random.nextBoolean())
                .preseElettriche(random.nextBoolean())
                .ariaCondizionata(true)
                .serviziRistorazione(random.nextBoolean() ? "Snack, Bevande" : "Nessuno")
                .accessibileDisabili(true)
                .nomeCommerciale(tipo + " " + (config.getNumeroTrenoBase() + indice))
                .build();
    }

    private Map<ClasseServizio, Prezzo> generaPrezziPerTutteLeClassi() {
        Map<ClasseServizio, Prezzo> prezzi = new HashMap<>();

        for (ClasseServizio classe : ClasseServizio.values()) {
            double prezzoBase = config.getPrezzoBase() + random.nextDouble() * config.getVariazionePrezzoMax();
            double moltiplicatore = getMoltiplicatoreClasse(classe);

            double prezzoIntero = prezzoBase * moltiplicatore;
            double prezzoPromo = prezzoIntero * config.getScontoPromozione();
            double prezzoFedelta = prezzoIntero * config.getScontoFedelta();

            prezzi.put(classe, new Prezzo(prezzoIntero, prezzoPromo, prezzoFedelta));
        }

        return prezzi;
    }

    private double getMoltiplicatoreClasse(ClasseServizio classe) {
        return switch (classe) {
            case BASE -> 1.0;
            case ARGENTO -> 1.3;
            case GOLD -> 1.6;
        };
    }

    // ================================================================================
    // 📋 CLASSE DI CONFIGURAZIONE - INNER CLASS
    // ================================================================================

    /**
     * 🔧 CONFIGURAZIONE per generazione tratte
     */
    public static class TrattaGenerationConfig {
        private final List<String> stazioni;
        private final int numeroTrattePerGiorno;
        private final LocalTime orarioInizio;
        private final LocalTime orarioFine;
        private final List<String> tipiTreno;
        private final int numeroTrenoBase;
        private final int capienzaBase;
        private final int maxBinario;
        private final double prezzoBase;
        private final double variazionePrezzoMax;
        private final double scontoPromozione;
        private final double scontoFedelta;

        public TrattaGenerationConfig(List<String> stazioni, int numeroTrattePerGiorno,
                                      LocalTime orarioInizio, LocalTime orarioFine,
                                      List<String> tipiTreno, int numeroTrenoBase,
                                      int capienzaBase, int maxBinario, double prezzoBase,
                                      double variazionePrezzoMax, double scontoPromozione,
                                      double scontoFedelta) {
            this.stazioni = List.copyOf(stazioni);
            this.numeroTrattePerGiorno = numeroTrattePerGiorno;
            this.orarioInizio = orarioInizio;
            this.orarioFine = orarioFine;
            this.tipiTreno = List.copyOf(tipiTreno);
            this.numeroTrenoBase = numeroTrenoBase;
            this.capienzaBase = capienzaBase;
            this.maxBinario = maxBinario;
            this.prezzoBase = prezzoBase;
            this.variazionePrezzoMax = variazionePrezzoMax;
            this.scontoPromozione = scontoPromozione;
            this.scontoFedelta = scontoFedelta;
        }

        public static TrattaGenerationConfig defaultConfig() {
            return new TrattaGenerationConfig(
                    List.of("ReggioCalabria", "Rende", "Strongoli", "Milano", "Roma", "Napoli", "Torino", "Firenze"),
                    5,
                    LocalTime.of(6, 0),
                    LocalTime.of(22, 0),
                    List.of("Frecciarossa", "Frecciargento", "Regionale"),
                    100,
                    80,
                    10,
                    15.0,
                    25.0,
                    0.8,
                    0.6
            );
        }

        public static TrattaGenerationConfig testConfig() {
            return new TrattaGenerationConfig(
                    List.of("TestA", "TestB", "TestC"),
                    2,
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    List.of("TestTreno"),
                    200,
                    50,
                    5,
                    10.0,
                    0.0,
                    0.9,
                    0.8
            );
        }

        // Getters
        public List<String> getStazioni() { return stazioni; }
        public int getNumeroTrattePerGiorno() { return numeroTrattePerGiorno; }
        public LocalTime getOrarioInizio() { return orarioInizio; }
        public LocalTime getOrarioFine() { return orarioFine; }
        public List<String> getTipiTreno() { return tipiTreno; }
        public int getNumeroTrenoBase() { return numeroTrenoBase; }
        public int getCapienzaBase() { return capienzaBase; }
        public int getMaxBinario() { return maxBinario; }
        public double getPrezzoBase() { return prezzoBase; }
        public double getVariazionePrezzoMax() { return variazionePrezzoMax; }
        public double getScontoPromozione() { return scontoPromozione; }
        public double getScontoFedelta() { return scontoFedelta; }
    }
}