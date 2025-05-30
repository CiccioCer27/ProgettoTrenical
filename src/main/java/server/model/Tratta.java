package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enums.ClasseServizio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public class Tratta {

    private final UUID id;
    private final String stazionePartenza;
    private final String stazioneArrivo;
    private final LocalDate data;
    private final LocalTime ora;
    private final int binario;
    private final Treno treno;
    private final Map<ClasseServizio, Prezzo> prezzi;

    @JsonCreator
    public Tratta(
            @JsonProperty("id") UUID id,
            @JsonProperty("stazionePartenza") String stazionePartenza,
            @JsonProperty("stazioneArrivo") String stazioneArrivo,
            @JsonProperty("data") LocalDate data,
            @JsonProperty("ora") LocalTime ora,
            @JsonProperty("binario") int binario,
            @JsonProperty("treno") Treno treno,
            @JsonProperty("prezzi") Map<ClasseServizio, Prezzo> prezzi
    ) {
        this.id = id;
        this.stazionePartenza = stazionePartenza;
        this.stazioneArrivo = stazioneArrivo;
        this.data = data;
        this.ora = ora;
        this.binario = binario;
        this.treno = treno;
        this.prezzi = prezzi;
    }

    public UUID getId() { return id; }
    public String getStazionePartenza() { return stazionePartenza; }
    public String getStazioneArrivo() { return stazioneArrivo; }
    public LocalDate getData() { return data; }
    public LocalTime getOra() { return ora; }
    public int getBinario() { return binario; }
    public Treno getTreno() { return treno; }
    public Map<ClasseServizio, Prezzo> getPrezzi() { return prezzi; }
}