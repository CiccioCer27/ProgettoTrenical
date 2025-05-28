package model;

public class Prezzo {
    private final double intero;
    private final double promozione;
    private final double fedelta;

    public Prezzo(double intero, double promozione, double fedelta) {
        this.intero = intero;
        this.promozione = promozione;
        this.fedelta = fedelta;
    }

    public double getIntero() { return intero; }
    public double getPromozione() { return promozione; }
    public double getFedelta() { return fedelta; }
}