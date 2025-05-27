package dto;

import java.util.UUID;
import enums.StatoBiglietto;
import enums.ClasseServizio;
import enums.TipoPrezzo;

public class BigliettoDTO {
    private final UUID id;
    private final ClienteDTO cliente;
    private final TrattaDTO tratta;
    private final ClasseServizio classeServizio;
    private final TipoPrezzo tipoPrezzo;
    private final double prezzoEffettivo;
    private final StatoBiglietto stato;

    public BigliettoDTO(UUID id, ClienteDTO cliente, TrattaDTO tratta,
                        ClasseServizio classeServizio, TipoPrezzo tipoPrezzo,
                        double prezzoEffettivo, StatoBiglietto stato) {
        this.id = id;
        this.cliente = cliente;
        this.tratta = tratta;
        this.classeServizio = classeServizio;
        this.tipoPrezzo = tipoPrezzo;
        this.prezzoEffettivo = prezzoEffettivo;
        this.stato = stato;
    }

    public UUID getId() { return id; }
    public ClienteDTO getCliente() { return cliente; }
    public TrattaDTO getTratta() { return tratta; }
    public ClasseServizio getClasseServizio() { return classeServizio; }
    public TipoPrezzo getTipoPrezzo() { return tipoPrezzo; }
    public double getPrezzoEffettivo() { return prezzoEffettivo; }
    public StatoBiglietto getStato() { return stato; }

    @Override
    public String toString() {
        return "BigliettoDTO{" +
                "id=" + id +
                ", cliente=" + cliente +
                ", tratta=" + tratta +
                ", classeServizio=" + classeServizio +
                ", tipoPrezzo=" + tipoPrezzo +
                ", prezzoEffettivo=" + prezzoEffettivo +
                ", stato=" + stato +
                '}';
    }
}