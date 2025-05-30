package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import enums.TipoPrezzo;

public class Prezzo {

    private final double intero;
    private final double promozione;
    private final double fedelta;

    @JsonCreator
    public Prezzo(
            @JsonProperty("intero") double intero,
            @JsonProperty("promozione") double promozione,
            @JsonProperty("fedelta") double fedelta) {
        this.intero = intero;
        this.promozione = promozione;
        this.fedelta = fedelta;
    }

    public double getIntero() {
        return intero;
    }

    public double getPromozione() {
        return promozione;
    }

    public double getFedelta() {
        return fedelta;
    }

    public double getPrezzo(TipoPrezzo tipo) {
        return switch (tipo) {
            case INTERO -> intero;
            case PROMOZIONE -> promozione;
            case FEDELTA -> fedelta;
        };
    }

    @Override
    public String toString() {
        return "Prezzo{" +
                "intero=" + intero +
                ", promozione=" + promozione +
                ", fedelta=" + fedelta +
                '}';
    }
}