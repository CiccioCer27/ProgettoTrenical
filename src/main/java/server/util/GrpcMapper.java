package util;

import dto.*;
import enums.ClasseServizio;
import enums.StatoBiglietto;
import enums.TipoPrezzo;
import grpc.*;
import model.Biglietto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GrpcMapper {

    // 🔁 RichiestaDTO ➜ RichiestaGrpc
    public static RichiestaGrpc toGrpc(RichiestaDTO dto) {
        RichiestaGrpc.Builder builder = RichiestaGrpc.newBuilder()
                .setTipo(dto.getTipo())
                .setIdCliente(dto.getIdCliente() != null ? dto.getIdCliente() : "")
                .setMessaggioExtra(dto.getMessaggioExtra() != null ? dto.getMessaggioExtra() : "");

        // ✅ Aggiungi mappings mancanti
        if (dto.getTratta() != null) {
            builder.setTrattaId(dto.getTratta().getId().toString());
        }
        if (dto.getBiglietto() != null) {
            builder.setBigliettoId(dto.getBiglietto().getId().toString());
        }
        if (dto.getClasseServizio() != null) {
            builder.setClasseServizio(dto.getClasseServizio().name());
        }
        if (dto.getTipoPrezzo() != null) {
            builder.setTipoPrezzo(dto.getTipoPrezzo().name());
        }
        if (dto.getPenale() != null) {
            builder.setPenale(dto.getPenale());
        }
        if (dto.getData() != null) {
            builder.setData(dto.getData().toString());
        }
        if (dto.getPartenza() != null) {
            builder.setPartenza(dto.getPartenza());
        }
        if (dto.getArrivo() != null) {
            builder.setArrivo(dto.getArrivo());
        }
        if (dto.getTipoTreno() != null) {
            builder.setTipoTreno(dto.getTipoTreno());
        }
        if (dto.getFasciaOraria() != null) {
            builder.setFasciaOraria(dto.getFasciaOraria());
        }

        return builder.build();
    }

    // 🔁 RichiestaGrpc ➜ RichiestaDTO
    public static RichiestaDTO toDTO(RichiestaGrpc grpc) {
        RichiestaDTO.Builder builder = new RichiestaDTO.Builder()
                .tipo(grpc.getTipo())
                .idCliente(grpc.getIdCliente().isEmpty() ? null : grpc.getIdCliente())
                .messaggioExtra(grpc.getMessaggioExtra().isEmpty() ? null : grpc.getMessaggioExtra());

        // ✅ Gestisci TrattaId -> TrattaDTO (minimale per ID)
        if (!grpc.getTrattaId().isEmpty()) {
            TrattaDTO trattaMinimale = new TrattaDTO(
                    UUID.fromString(grpc.getTrattaId()),
                    "", "", null, null, 0, null, null
            );
            builder.tratta(trattaMinimale);
        }

        // ✅ Gestisci BigliettoId -> BigliettoDTO (minimale per ID)
        if (!grpc.getBigliettoId().isEmpty()) {
            BigliettoDTO bigliettoMinimale = new BigliettoDTO(
                    UUID.fromString(grpc.getBigliettoId()),
                    null, null, null, null, 0.0, null
            );
            builder.biglietto(bigliettoMinimale);
        }

        // ✅ Gestisci campi opzionali con valori di default
        if (!grpc.getClasseServizio().isEmpty()) {
            builder.classeServizio(ClasseServizio.valueOf(grpc.getClasseServizio()));
        }
        if (!grpc.getTipoPrezzo().isEmpty()) {
            builder.tipoPrezzo(TipoPrezzo.valueOf(grpc.getTipoPrezzo()));
        } else {
            // Default per prenotazioni che non specificano il tipo prezzo
            builder.tipoPrezzo(TipoPrezzo.INTERO);
        }
        if (grpc.getPenale() > 0) {
            builder.penale(grpc.getPenale());
        }
        if (!grpc.getData().isEmpty()) {
            builder.data(LocalDate.parse(grpc.getData()));
        }
        if (!grpc.getPartenza().isEmpty()) {
            builder.partenza(grpc.getPartenza());
        }
        if (!grpc.getArrivo().isEmpty()) {
            builder.arrivo(grpc.getArrivo());
        }
        if (!grpc.getTipoTreno().isEmpty()) {
            builder.tipoTreno(grpc.getTipoTreno());
        }
        if (!grpc.getFasciaOraria().isEmpty()) {
            builder.fasciaOraria(grpc.getFasciaOraria());
        }

        return builder.build();
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

    // 🔁 BigliettoGrpc ➜ BigliettoDTO - ⚠️ PROBLEMA: Dati incompleti
    public static BigliettoDTO fromGrpc(BigliettoGrpc g) {
        // ⚠️ Questo metodo ha limitazioni - i dati cliente/tratta sono minimali
        return new BigliettoDTO(
                UUID.fromString(g.getId()),
                new ClienteDTO(
                        UUID.fromString(g.getIdCliente()),
                        "", "", "", false, 0, "", 0, ""
                ),
                new TrattaDTO(UUID.fromString(g.getIdTratta()), "", "", null, null, 0, null, null),
                ClasseServizio.valueOf(g.getClasse()),
                g.getConCartaFedelta() ? TipoPrezzo.FEDELTA : TipoPrezzo.INTERO,
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
                null, // ⚠️ TrenoDTO non disponibile da gRPC
                null  // ⚠️ Prezzi non disponibili da gRPC
        );
    }
}