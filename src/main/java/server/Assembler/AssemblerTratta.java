package Assembler;

import Assembler.AssemblerPrezzo;
import Assembler.AssemblerTreno;
import dto.PrezzoDTO;
import dto.TrattaDTO;
import dto.TrenoDTO;
import enums.ClasseServizio;
import model.Prezzo;
import model.Tratta;
import model.Treno;

import java.util.HashMap;
import java.util.Map;

public class AssemblerTratta {

    public static TrattaDTO toDTO(Tratta tratta) {
        Map<ClasseServizio, PrezzoDTO> prezziDTO = new HashMap<>();
        tratta.getPrezzi().forEach((classe, prezzo) ->
                prezziDTO.put(classe, AssemblerPrezzo.toDTO(prezzo)));

        TrenoDTO trenoDTO = AssemblerTreno.toDTO(tratta.getTreno());

        return new TrattaDTO(
                tratta.getId(),
                tratta.getStazionePartenza(),
                tratta.getStazioneArrivo(),
                tratta.getData(),
                tratta.getOra(),
                tratta.getBinario(),
                trenoDTO,
                prezziDTO
        );
    }

    public static Tratta fromDTO(TrattaDTO dto) {
        Map<ClasseServizio, Prezzo> prezzi = new HashMap<>();
        dto.getPrezzi().forEach((classe, prezzoDTO) ->
                prezzi.put(classe, AssemblerPrezzo.fromDTO(prezzoDTO)));

        Treno treno = AssemblerTreno.fromDTO(dto.getTreno());

        return new Tratta(
                dto.getId(),
                dto.getStazionePartenza(),
                dto.getStazioneArrivo(),
                dto.getData(),
                dto.getOra(),
                dto.getBinario(),
                treno,
                prezzi
        );
    }
}