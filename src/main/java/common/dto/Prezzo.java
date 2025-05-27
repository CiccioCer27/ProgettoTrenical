package dto;

public class Prezzo {
    private final double prezzoIntero;
    private final double prezzoPromo;
    private final double prezzoFedelta;

    public Prezzo(double prezzoIntero, double prezzoPromo, double prezzoFedelta) {
        this.prezzoIntero = prezzoIntero;
        this.prezzoPromo = prezzoPromo;
        this.prezzoFedelta = prezzoFedelta;
    }

    public double getPrezzoIntero() { return prezzoIntero; }
    public double getPrezzoPromo() { return prezzoPromo; }
    public double getPrezzoFedelta() { return prezzoFedelta; }

    @Override
    public String toString() {
        return "Prezzo{" +
                "prezzoIntero=" + prezzoIntero +
                ", prezzoPromo=" + prezzoPromo +
                ", prezzoFedelta=" + prezzoFedelta +
                '}';
    }
}