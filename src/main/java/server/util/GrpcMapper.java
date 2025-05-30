package util;

import dto.*;
import enums.ClasseServizio;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import grpc.*;
import model.Biglietto;
import model.Tratta;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GrpcMapper {

    // 🔁 RichiestaDTO ➜ RichiestaGrpc
    public static RichiestaGrpc toGrpc(RichiestaDTO dto) {
        return RichiestaGrpc.newBuilder()
                .setTipo(dto.getTipo())
                .setIdCliente(dto.getIdCliente())
                .setMessaggioExtra(dto.getMessaggioExtra() != null ? dto.getMessaggioExtra() : "")
                .build();
    }

    // 🔁 RichiestaGrpc ➜ RichiestaDTO
    public static RichiestaDTO toDTO(RichiestaGrpc grpc) {
        return new RichiestaDTO.Builder()
                .tipo(grpc.getTipo())
                .idCliente(grpc.getIdCliente())
                .messaggioExtra(grpc.getMessaggioExtra())
                .build();
    }

    // 🔁 RispostaDTO ➜ RispostaGrpc
    public static RispostaGrpc fromDTO(RispostaDTO dto) {
        RispostaGrpc.Builder builder = RispostaGrpc.newBuilder()
                .setEsito(dto.getEsito())
                .setMessaggio(dto.getMessaggio());

        if (dto.getBiglietto() != null) {
            builder.setBiglietto(toGrpc(dto.getBiglietto()));
        }

        if (dto.getTratte() != null && !dto.getTratte().isEmpty()) {
            builder.addAllTratte(dto.getTratte().stream()
                    .map(GrpcMapper::toGrpc)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    // 🔁 RispostaGrpc ➜ RispostaDTO
    public static RispostaDTO fromGrpc(RispostaGrpc grpc) {
        BigliettoDTO biglietto = grpc.hasBiglietto() ? fromGrpc(grpc.getBiglietto()) : null;

        List<TrattaDTO> tratte = grpc.getTratteList().stream()
                .map(GrpcMapper::fromGrpc)
                .collect(Collectors.toList());

        Object dati = biglietto != null ? biglietto : (!tratte.isEmpty() ? tratte : null);

        return new RispostaDTO(
                grpc.getEsito(),
                grpc.getMessaggio(),
                dati
        );
    }

    // 🔁 BigliettoDTO ➜ BigliettoGrpc
    public static BigliettoGrpc toGrpc(BigliettoDTO b) {
        return BigliettoGrpc.newBuilder()
                .setId(b.getId().toString())
                .setIdCliente(b.getCliente().getId().toString())
                .setIdTratta(b.getTratta().getId().toString())
                .setClasse(b.getClasseServizio().name())
                .setTipoAcquisto(b.getStato().name())
                .setPrezzoPagato(b.getPrezzoEffettivo())
                .setDataAcquisto(LocalDate.now().toString())
                .setConCartaFedelta(b.getTipoPrezzo() == TipoPrezzo.FEDELTA)
                .build();
    }

    // 🔁 BigliettoGrpc ➜ BigliettoDTO
    public static BigliettoDTO fromGrpc(BigliettoGrpc g) {
        return new BigliettoDTO(
                UUID.fromString(g.getId()),
                new ClienteDTO(
                        UUID.fromString(g.getIdCliente()),
                        "", "", "", false, 0, "", 0, ""
                ),
                new TrattaDTO(UUID.fromString(g.getIdTratta()), "", "", null, null, 0, null, null),
                ClasseServizio.valueOf(g.getClasse()),
                TipoPrezzo.INTERO,
                g.getPrezzoPagato(),
                g.getTipoAcquisto().equalsIgnoreCase("ACQUISTO") ? StatoBiglietto.CONFERMATO : StatoBiglietto.NON_CONFERMATO
        );
    }

    // 🔁 TrattaDTO ➜ TrattaGrpc
    public static TrattaGrpc toGrpc(TrattaDTO t) {
        return TrattaGrpc.newBuilder()
                .setId(t.getId().toString())
                .setStazionePartenza(t.getStazionePartenza())
                .setStazioneArrivo(t.getStazioneArrivo())
                .setData(t.getData().toString())
                .setOra(t.getOra().toString())
                .setBinario(String.valueOf(t.getBinario()))
                .setTipoTreno(t.getTreno() != null ? t.getTreno().getNomeCommerciale() : "Sconosciuto")
                .build();
    }

    // 🔁 TrattaGrpc ➜ TrattaDTO
    public static TrattaDTO fromGrpc(TrattaGrpc g) {
        return new TrattaDTO(
                UUID.fromString(g.getId()),
                g.getStazionePartenza(),
                g.getStazioneArrivo(),
                LocalDate.parse(g.getData()),
                LocalTime.parse(g.getOra()),
                Integer.parseInt(g.getBinario()),
                null,
                null
        );
    }
}