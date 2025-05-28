package model;

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

    public Tratta(UUID id, String stazionePartenza, String stazioneArrivo,
                  LocalDate data, LocalTime ora, int binario,
                  Treno treno, Map<ClasseServizio, Prezzo> prezzi) {
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