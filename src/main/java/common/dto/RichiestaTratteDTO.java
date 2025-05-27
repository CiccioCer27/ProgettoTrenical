package dto;

import enums.ClasseServizio;

import java.time.LocalDate;

public class RichiestaTratteDTO {

    private final LocalDate data;
    private final String fasciaOraria;
    private final String stazionePartenza;
    private final String stazioneArrivo;
    private final String tipoTreno;
    private final ClasseServizio classeServizio;

    public RichiestaTratteDTO(LocalDate data, String fasciaOraria, String stazionePartenza,
                              String stazioneArrivo, String tipoTreno, ClasseServizio classeServizio) {
        this.data = data;
        this.fasciaOraria = fasciaOraria;
        this.stazionePartenza = stazionePartenza;
        this.stazioneArrivo = stazioneArrivo;
        this.tipoTreno = tipoTreno;
        this.classeServizio = classeServizio;
    }

    public LocalDate getData() {
        return data;
    }

    public String getFasciaOraria() {
        return fasciaOraria;
    }

    public String getStazionePartenza() {
        return stazionePartenza;
    }

    public String getStazioneArrivo() {
        return stazioneArrivo;
    }

    public String getTipoTreno() {
        return tipoTreno;
    }

    public ClasseServizio getClasseServizio() {
        return classeServizio;
    }
}