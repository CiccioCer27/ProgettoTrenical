package command;
import dto.BigliettoDTO;
import model.Wallet;

import java.util.List;

public class VisualizzaBigliettiCommand implements Command {
    private final Wallet wallet;

    public VisualizzaBigliettiCommand(Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public void esegui() {
        System.out.println("\n💼 I TUOI BIGLIETTI");
        System.out.println("-".repeat(20));

        try {
            List<BigliettoDTO> confermati = wallet.getBigliettiConfermati();
            List<BigliettoDTO> prenotazioni = wallet.getBigliettiNonConfermati();

            System.out.println("✅ BIGLIETTI CONFERMATI (" + confermati.size() + "):");
            if (confermati.isEmpty()) {
                System.out.println("   Nessun biglietto confermato");
            } else {
                for (int i = 0; i < confermati.size(); i++) {
                    System.out.println("   " + (i + 1) + ") " + formatBigliettoCompleto(confermati.get(i)));
                }

                // ✅ CALCOLA SPESA TOTALE
                double spesaTotale = confermati.stream()
                        .mapToDouble(BigliettoDTO::getPrezzoEffettivo)
                        .sum();
                System.out.println("   💰 Spesa totale confermati: €" + String.format("%.2f", spesaTotale));
            }

            System.out.println("\n📝 PRENOTAZIONI (" + prenotazioni.size() + "):");
            if (prenotazioni.isEmpty()) {
                System.out.println("   Nessuna prenotazione attiva");
            } else {
                for (int i = 0; i < prenotazioni.size(); i++) {
                    System.out.println("   " + (i + 1) + ") " + formatBigliettoCompleto(prenotazioni.get(i)) + " ⏰");
                }

                double costoPrenotazioni = prenotazioni.stream()
                        .mapToDouble(BigliettoDTO::getPrezzoEffettivo)
                        .sum();
                System.out.println("   💰 Costo da confermare: €" + String.format("%.2f", costoPrenotazioni));
            }

            // ✅ STATISTICHE TOTALI
            if (!confermati.isEmpty() || !prenotazioni.isEmpty()) {
                System.out.println("\n📊 RIEPILOGO:");
                System.out.println("   🎫 Totale biglietti: " + (confermati.size() + prenotazioni.size()));

                // Raggruppa per tratta
                java.util.Map<String, Integer> tratteCount = new java.util.HashMap<>();
                confermati.forEach(b -> {
                    if (b.getTratta() != null) {
                        String tratta = b.getTratta().getStazionePartenza() + " → " + b.getTratta().getStazioneArrivo();
                        tratteCount.merge(tratta, 1, Integer::sum);
                    }
                });
                prenotazioni.forEach(b -> {
                    if (b.getTratta() != null) {
                        String tratta = b.getTratta().getStazionePartenza() + " → " + b.getTratta().getStazioneArrivo();
                        tratteCount.merge(tratta, 1, Integer::sum);
                    }
                });

                if (!tratteCount.isEmpty()) {
                    System.out.println("   🚂 Tratte più usate:");
                    tratteCount.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .limit(3)
                            .forEach(entry ->
                                    System.out.println("      " + entry.getKey() + ": " + entry.getValue() + " viaggio/i"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Errore visualizzazione biglietti: " + e.getMessage());
        }
    }

    /**
     * ✅ FORMATO BIGLIETTO COMPLETO con tutti i dettagli reali
     */
    private String formatBigliettoCompleto(BigliettoDTO biglietto) {
        try {
            StringBuilder sb = new StringBuilder();

            // ID biglietto
            sb.append("ID:").append(biglietto.getId().toString().substring(0, 8)).append("...");

            // Tratta con dati reali
            if (biglietto.getTratta() != null) {
                sb.append(" | ").append(biglietto.getTratta().getStazionePartenza())
                        .append(" → ").append(biglietto.getTratta().getStazioneArrivo());

                if (biglietto.getTratta().getData() != null) {
                    sb.append(" | ").append(biglietto.getTratta().getData());
                }

                if (biglietto.getTratta().getOra() != null) {
                    sb.append(" ").append(biglietto.getTratta().getOra());
                }

                sb.append(" | Bin.").append(biglietto.getTratta().getBinario());
            } else {
                sb.append(" | Tratta: N/A");
            }

            // Classe e tipo prezzo
            sb.append(" | ").append(biglietto.getClasseServizio());
            if (biglietto.getTipoPrezzo() != null) {
                sb.append(" (").append(biglietto.getTipoPrezzo()).append(")");
            }

            // Prezzo reale
            sb.append(" | €").append(String.format("%.2f", biglietto.getPrezzoEffettivo()));

            // Stato
            if (biglietto.getStato() != null) {
                String statoIcon = biglietto.getStato().name().equals("CONFERMATO") ? "✅" : "⏳";
                sb.append(" ").append(statoIcon);
            }

            return sb.toString();

        } catch (Exception e) {
            return "Biglietto ID:" + biglietto.getId().toString().substring(0, 8) + "... | Errore visualizzazione: " + e.getMessage();
        }
    }
}