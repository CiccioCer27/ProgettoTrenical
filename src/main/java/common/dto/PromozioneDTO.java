package dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class PromozioneDTO {

    private final String nome;
    private final String descrizione;
    private final LocalDateTime dataInizio;
    private final LocalDateTime dataFine;

    public PromozioneDTO(String nome, String descrizione, LocalDateTime dataInizio, LocalDateTime dataFine) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public LocalDateTime getDataInizio() {
        return dataInizio;
    }

    public LocalDateTime getDataFine() {
        return dataFine;
    }

    public boolean èAttiva() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(dataInizio) && !now.isAfter(dataFine);
    }

    public boolean èScaduta() {
        return LocalDateTime.now().isAfter(dataFine);
    }

    @Override
    public String toString() {
        return nome + " - " + descrizione + " [" + dataInizio + " → " + dataFine + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromozioneDTO that)) return false;
        return nome.equals(that.nome)
                && Objects.equals(dataInizio, that.dataInizio)
                && Objects.equals(dataFine, that.dataFine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, dataInizio, dataFine);
    }
}