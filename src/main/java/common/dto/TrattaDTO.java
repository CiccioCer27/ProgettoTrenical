package dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import enums.ClasseServizio;

public class TrattaDTO {
    private final UUID id;
    private final String stazionePartenza;
    private final String stazioneArrivo;
    private final LocalDate data;
    private final LocalTime ora;
    private final int binario;
    private final TrenoDTO treno;
    private final Map<ClasseServizio, Prezzo> prezzi;

    @Override
    public String toString() {
        return "TrattaDTO{" +
                "id=" + id +
                ", stazionePartenza='" + stazionePartenza + '\'' +
                ", stazioneArrivo='" + stazioneArrivo + '\'' +
                ", data=" + data +
                ", ora=" + ora +
                ", binario=" + binario +
                ", treno=" + treno +
                ", prezzi=" + prezzi +
                '}';
    }

    public TrattaDTO(UUID id, String stazionePartenza, String stazioneArrivo,
                     LocalDate data, LocalTime ora, int binario,
                     TrenoDTO treno, Map<ClasseServizio,Prezzo> prezzi) {
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
    public TrenoDTO getTreno() { return treno; }
    public Map<ClasseServizio, Prezzo> getPrezzi() { return prezzi; }
}