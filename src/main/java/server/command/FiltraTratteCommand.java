package command;

import Assembler.AssemblerTratta;
import dto.RichiestaDTO;
import dto.RispostaDTO;
import dto.TrattaDTO;
import model.Tratta;
import persistence.MemoriaTratte;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FiltraTratteCommand implements ServerCommand {

    private final RichiestaDTO richiesta;
    private final MemoriaTratte memoria;

    public FiltraTratteCommand(RichiestaDTO richiesta, MemoriaTratte memoria) {
        this.richiesta = richiesta;
        this.memoria = memoria;
    }

    @Override
    public RispostaDTO esegui(RichiestaDTO ignored) {
        List<Tratta> tutte = memoria.getTutteTratte();

        List<Tratta> filtrate = tutte.stream()
                .filter(t -> richiesta.getData() == null || t.getData().equals(richiesta.getData()))
                .filter(t -> richiesta.getPartenza() == null || t.getStazionePartenza().equalsIgnoreCase(richiesta.getPartenza()))
                .filter(t -> richiesta.getArrivo() == null || t.getStazioneArrivo().equalsIgnoreCase(richiesta.getArrivo()))
                .filter(t -> richiesta.getTipoTreno() == null || t.getTreno().getTipologia().equalsIgnoreCase(richiesta.getTipoTreno()))
                .filter(t -> richiesta.getClasseServizio() == null || t.getPrezzi().containsKey(richiesta.getClasseServizio()))
                .filter(t -> {
                    if (richiesta.getFasciaOraria() == null) return true;
                    LocalTime ora = t.getOra();
                    return switch (richiesta.getFasciaOraria().toUpperCase(Locale.ROOT)) {
                        case "MATTINA" -> ora.isBefore(LocalTime.NOON);
                        case "POMERIGGIO" -> ora.isAfter(LocalTime.NOON) && ora.isBefore(LocalTime.of(18, 0));
                        case "SERA" -> ora.isAfter(LocalTime.of(18, 0));
                        default -> true;
                    };
                })
                .toList();

        List<TrattaDTO> risultati = filtrate.stream()
                .map(AssemblerTratta::toDTO)
                .collect(Collectors.toList());

        return new RispostaDTO("OK", "Trovate " + risultati.size() + " util", risultati);
    }
}