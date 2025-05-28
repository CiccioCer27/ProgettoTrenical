package Assembler;

import dto.TrenoDTO;
import model.Treno;

public class AssemblerTreno {

    // Converte da Treno a TrenoDTO
    public static TrenoDTO toDTO(Treno treno) {
        return new TrenoDTO(
                treno.getId(),
                treno.getNumero(),
                treno.getTipologia(),
                treno.getCapienzaTotale(),
                treno.isWifiDisponibile(),
                treno.isPreseElettriche(),
                treno.isAriaCondizionata(),
                treno.getServiziRistorazione(),
                treno.isAccessibileDisabili(),
                treno.getNomeCommerciale()
        );
    }

    // Converte da TrenoDTO a Treno (usando il Builder)
    public static Treno fromDTO(TrenoDTO dto) {
        return new Treno.Builder()
                .withId(dto.getId())
                .numero(dto.getNumero())
                .tipologia(dto.getTipologia())
                .capienzaTotale(dto.getCapienzaTotale())
                .wifiDisponibile(dto.isWifiDisponibile())
                .preseElettriche(dto.isPreseElettriche())
                .ariaCondizionata(dto.isAriaCondizionata())
                .serviziRistorazione(dto.getServiziRistorazione())
                .accessibileDisabili(dto.isAccessibileDisabili())
                .nomeCommerciale(dto.getNomeCommerciale())
                .build();
    }
}