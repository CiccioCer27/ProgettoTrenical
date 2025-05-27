package factory;

import dto.*;
import enums.ClasseServizio;

import java.time.LocalDate;

public class TratteFactory implements RichiestaFactory {
    private final LocalDate data;
    private final String fasciaOraria;
    private final String stazionePartenza;
    private final String stazioneArrivo;
    private final String tipoTreno;
    private final ClasseServizio classeServizio;

    public TratteFactory(LocalDate data, String fasciaOraria, String stazionePartenza,
                         String stazioneArrivo, String tipoTreno, ClasseServizio classeServizio) {
        this.data = data;
        this.fasciaOraria = fasciaOraria;
        this.stazionePartenza = stazionePartenza;
        this.stazioneArrivo = stazioneArrivo;
        this.tipoTreno = tipoTreno;
        this.classeServizio = classeServizio;
    }

    @Override
    public RichiestaDTO creaRichiesta() {
        return new RichiestaDTO.Builder()
                .tipo("RICERCA_TRATTE")
                .messaggioExtra(String.join(";", data.toString(), fasciaOraria, stazionePartenza,
                        stazioneArrivo, tipoTreno, classeServizio.name()))
                .build();
    }
}