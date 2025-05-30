package model;

import java.time.LocalDate;
import java.util.UUID;

public class PromozioneFedelta implements Promozione {
    private final String nome;
    private final String descrizione;
    private final double sconto;
    private final LocalDate dataInizio;
    private final LocalDate dataFine;

    public PromozioneFedelta(String nome, String descrizione, double sconto,
                             LocalDate dataInizio, LocalDate dataFine) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.sconto = sconto;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    @Override
    public boolean isAttiva(LocalDate data) {
        return (data.isEqual(dataInizio) || data.isAfter(dataInizio)) &&
                (data.isEqual(dataFine) || data.isBefore(dataFine));
    }

    @Override
    public boolean siApplicaAllaTratta(UUID idTratta) {
        return true;
    }

    @Override
    public boolean applicabileSoloCartaFedelta() {
        return true;
    }

    @Override
    public double getSconto() {
        return sconto;
    }

    @Override
    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public LocalDate getDataInizio() {
        return dataInizio;
    }

    @Override
    public LocalDate getDataFine() {
        return dataFine;
    }
}