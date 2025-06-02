package command;

import Assembler.AssemblerTratta;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import model.Tratta;
import persistence.MemoriaTratte;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Command Pattern Implementation per filtraggio tratte
 * Encapsula la logica di ricerca e filtraggio come comando riutilizzabile
 */
public class FiltraTratteCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaTratte memoria;

    public FiltraTratteCommand(RichiestaDTO richiesta, MemoriaTratte memoria) {
        this.richiesta = richiesta;
        this.memoria = memoria;
    }

    @Override
    public RispostaDTO esegui() {  // ✅ FIXED: Nessun parametro confusing
        List<Tratta> tutte = memoria.getTutteTratte();

        // ✅ Gestisci il caso del messaggio extra (formato: "data;partenza;arrivo;fascia")
        LocalDate dataFiltro = richiesta.getData();
        String partenzaFiltro = richiesta.getPartenza();
        String arrivoFiltro = richiesta.getArrivo();
        String fasciaFiltro = richiesta.getFasciaOraria();

        if (richiesta.getMessaggioExtra() != null && !richiesta.getMessaggioExtra().isEmpty()) {
            String[] parti = richiesta.getMessaggioExtra().split(";");
            if (parti.length >= 4) {
                try {
                    if (!parti[0].trim().isEmpty()) dataFiltro = LocalDate.parse(parti[0].trim());
                    if (!parti[1].trim().isEmpty()) partenzaFiltro = parti[1].trim();
                    if (!parti[2].trim().isEmpty()) arrivoFiltro = parti[2].trim();
                    if (!parti[3].trim().isEmpty()) fasciaFiltro = parti[3].trim();
                } catch (Exception e) {
                    return new RispostaDTO("KO", "❌ Formato filtro non valido. Usa: data;partenza;arrivo;fascia", null);
                }
            }
        }

        // ✅ Applica i filtri
        final LocalDate dataFinale = dataFiltro;
        final String partenzaFinale = partenzaFiltro;
        final String arrivoFinale = arrivoFiltro;
        final String fasciaFinale = fasciaFiltro;

        List<Tratta> filtrate = tutte.stream()
                .filter(t -> dataFinale == null || t.getData().equals(dataFinale))
                .filter(t -> partenzaFinale == null || t.getStazionePartenza().equalsIgnoreCase(partenzaFinale))
                .filter(t -> arrivoFinale == null || t.getStazioneArrivo().equalsIgnoreCase(arrivoFinale))
                .filter(t -> richiesta.getTipoTreno() == null || t.getTreno().getTipologia().equalsIgnoreCase(richiesta.getTipoTreno()))
                .filter(t -> richiesta.getClasseServizio() == null || t.getPrezzi().containsKey(richiesta.getClasseServizio()))
                .filter(t -> {
                    if (fasciaFinale == null) return true;
                    LocalTime ora = t.getOra();
                    return switch (fasciaFinale.toUpperCase(Locale.ROOT)) {
                        case "MATTINA", "MATTINO" -> ora.isBefore(LocalTime.NOON);
                        case "POMERIGGIO" -> ora.isAfter(LocalTime.NOON) && ora.isBefore(LocalTime.of(18, 0));
                        case "SERA" -> ora.isAfter(LocalTime.of(18, 0));
                        default -> true;
                    };
                })
                .toList();

        List<TrattaDTO> risultati = filtrate.stream()
                .map(AssemblerTratta::toDTO)
                .collect(Collectors.toList());

        return new RispostaDTO("OK", "Trovate " + risultati.size() + " tratte", risultati);
    }
}