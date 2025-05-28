package Assembler;

import dto.PrezzoDTO;
import model.Prezzo;

public class AssemblerPrezzo {

    public static PrezzoDTO toDTO(Prezzo model) {
        return new PrezzoDTO(
                model.getIntero(),
                model.getPromozione(),
                model.getFedelta()
        );
    }

    public static Prezzo fromDTO(PrezzoDTO dto) {
        return new Prezzo(
                dto.getPrezzoIntero(),
                dto.getPrezzoPromo(),
                dto.getPrezzoFedelta()
        );
    }
}