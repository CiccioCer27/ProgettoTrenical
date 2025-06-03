package observer;

import dto.PromozioneDTO;
import eventi.Evento;
import eventi.EventoPromozione;
import observer.Observer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 🔔 CENTRO AVVISI CENTRALIZZATO - Thread-Safe
 *
 * Gestisce tutte le notifiche del client:
 * - Promozioni (dalle API)
 * - Notifiche tratte (gRPC streams)
 * - Avvisi sistema
 * - Scadenze biglietti
 */
public class CentroAvvisi implements Observer {

    // ✅ TIPI DI AVVISO
    public enum TipoAvviso {
        PROMOZIONE("🎉", "Promozione"),
        NOTIFICA_TRATTA("🚂", "Notifica Tratta"),
        SCADENZA_BIGLIETTO("⏰", "Scadenza"),
        AVVISO_SISTEMA("📢", "Sistema"),
        CONFERMA_OPERAZIONE("✅", "Conferma");

        private final String icona;
        private final String descrizione;

        TipoAvviso(String icona, String descrizione) {
            this.icona = icona;
            this.descrizione = descrizione;
        }

        public String getIcona() { return icona; }
        public String getDescrizione() { return descrizione; }
    }

    // ✅ PRIORITÀ AVVISI
    public enum Priorita {
        BASSA(1, "💚"),
        MEDIA(2, "💛"),
        ALTA(3, "🟠"),
        CRITICA(4, "🔴");

        private final int livello;
        private final String icona;

        Priorita(int livello, String icona) {
            this.livello = livello;
            this.icona = icona;
        }

        public int getLivello() { return livello; }
        public String getIcona() { return icona; }
    }

    // ✅ CLASSE AVVISO UNIFICATA
    public static class Avviso {
        private final String id;
        private final TipoAvviso tipo;
        private final Priorita priorita;
        private final String titolo;
        private final String messaggio;
        private final LocalDateTime timestamp;
        private final LocalDateTime scadenza;
        private boolean letto;
        private final Map<String, Object> datiExtra;

        public Avviso(TipoAvviso tipo, Priorita priorita, String titolo, String messaggio) {
            this(tipo, priorita, titolo, messaggio, null, new HashMap<>());
        }

        public Avviso(TipoAvviso tipo, Priorita priorita, String titolo, String messaggio,
                      LocalDateTime scadenza, Map<String, Object> datiExtra) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.tipo = tipo;
            this.priorita = priorita;
            this.titolo = titolo;
            this.messaggio = messaggio;
            this.timestamp = LocalDateTime.now();
            this.scadenza = scadenza;
            this.letto = false;
            this.datiExtra = datiExtra != null ? new HashMap<>(datiExtra) : new HashMap<>();
        }

        // Getters
        public String getId() { return id; }
        public TipoAvviso getTipo() { return tipo; }
        public Priorita getPriorita() { return priorita; }
        public String getTitolo() { return titolo; }
        public String getMessaggio() { return messaggio; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public LocalDateTime getScadenza() { return scadenza; }
        public boolean isLetto() { return letto; }
        public Map<String, Object> getDatiExtra() { return new HashMap<>(datiExtra); }

        public void setLetto(boolean letto) { this.letto = letto; }

        public boolean isScaduto() {
            return scadenza != null && LocalDateTime.now().isAfter(scadenza);
        }

        public long getMinutiDalCreazione() {
            return Duration.between(timestamp, LocalDateTime.now()).toMinutes();
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String statoLetto = letto ? "📖" : "📬";
            String tempoRelativo = getTempoRelativo();

            return String.format("%s %s %s [%s] %s - %s (%s)",
                    statoLetto,
                    priorita.getIcona(),
                    tipo.getIcona(),
                    timestamp.format(formatter),
                    titolo,
                    messaggio,
                    tempoRelativo);
        }

        private String getTempoRelativo() {
            long minuti = getMinutiDalCreazione();
            if (minuti < 1) return "ora";
            if (minuti < 60) return minuti + "m fa";
            long ore = minuti / 60;
            if (ore < 24) return ore + "h fa";
            long giorni = ore / 24;
            return giorni + "g fa";
        }
    }

    // ✅ STRUTTURE DATI THREAD-SAFE
    private final List<Avviso> avvisi = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, Runnable> timerScadenze = new ConcurrentHashMap<>();

    // ✅ CONFIGURAZIONE
    private static final int MAX_AVVISI = 100; // Limite per evitare memory leak
    private static final int GIORNI_RETENTION = 7; // Rimuovi avvisi più vecchi di 7 giorni

    // ✅ LOCK per operazioni atomiche
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ✅ STATISTICHE
    private volatile int avvisiTotali = 0;
    private volatile int promozioniRicevute = 0;
    private volatile int notificheTratteRicevute = 0;

    public CentroAvvisi() {
        // Avvia pulizia automatica ogni ora
        scheduler.scheduleAtFixedRate(this::pulisciAvvisiScaduti,
                1, 1, TimeUnit.HOURS);

        System.out.println("🔔 CentroAvvisi inizializzato - Thread safety attiva");
    }

    // ✅ IMPLEMENTAZIONE OBSERVER per eventi promozioni
    @Override
    public void aggiorna(Evento evento) {
        if (evento instanceof EventoPromozione ep) {
            aggiungiPromozione(ep.getPromozione());
        }
    }

    // ================================================================================
    // 🎉 GESTIONE PROMOZIONI
    // ================================================================================

    /**
     * Aggiunge una promozione al centro avvisi
     */
    public void aggiungiPromozione(PromozioneDTO promozione) {
        lock.writeLock().lock();
        try {
            Map<String, Object> datiExtra = new HashMap<>();
            datiExtra.put("promozione", promozione);
            datiExtra.put("dataInizio", promozione.getDataInizio());
            datiExtra.put("dataFine", promozione.getDataFine());

            Avviso avviso = new Avviso(
                    TipoAvviso.PROMOZIONE,
                    Priorita.MEDIA,
                    "Nuova Promozione!",
                    promozione.getDescrizione(),
                    promozione.getDataFine(),
                    datiExtra
            );

            aggiungiAvvisoInterno(avviso);
            promozioniRicevute++;

            // Timer per rimozione automatica quando scade
            programmaRimozioneScadenza(avviso);

            System.out.println("🎉 CENTRO AVVISI: Promozione aggiunta - " + promozione.getNome());

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Ottieni solo le promozioni attive
     */
    public List<PromozioneDTO> getPromozioniAttive() {
        lock.readLock().lock();
        try {
            return avvisi.stream()
                    .filter(a -> a.getTipo() == TipoAvviso.PROMOZIONE)
                    .filter(a -> !a.isScaduto())
                    .map(a -> (PromozioneDTO) a.getDatiExtra().get("promozione"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ================================================================================
    // 🚂 GESTIONE NOTIFICHE TRATTE (per integrazione gRPC)
    // ================================================================================

    /**
     * Aggiunge notifica da gRPC stream delle tratte
     */
    public void aggiungiNotificaTratta(String messaggio, String trattaId, Priorita priorita) {
        lock.writeLock().lock();
        try {
            Map<String, Object> datiExtra = new HashMap<>();
            datiExtra.put("trattaId", trattaId);
            datiExtra.put("tipoNotifica", "MODIFICA_TRATTA");

            Avviso avviso = new Avviso(
                    TipoAvviso.NOTIFICA_TRATTA,
                    priorita != null ? priorita : Priorita.ALTA,
                    "Aggiornamento Tratta",
                    messaggio,
                    LocalDateTime.now().plusHours(24), // Le notifiche tratta durano 24h
                    datiExtra
            );

            aggiungiAvvisoInterno(avviso);
            notificheTratteRicevute++;

            System.out.println("🚂 CENTRO AVVISI: Notifica tratta aggiunta - " + messaggio);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Aggiunge notifica cambio binario
     */
    public void aggiungiCambioBinario(String trattaId, String nuovoBinario, String vecchioBinario) {
        String messaggio = String.format("Cambio binario: %s → %s", vecchioBinario, nuovoBinario);

        Map<String, Object> datiExtra = new HashMap<>();
        datiExtra.put("trattaId", trattaId);
        datiExtra.put("nuovoBinario", nuovoBinario);
        datiExtra.put("vecchioBinario", vecchioBinario);
        datiExtra.put("tipoNotifica", "CAMBIO_BINARIO");

        Avviso avviso = new Avviso(
                TipoAvviso.NOTIFICA_TRATTA,
                Priorita.ALTA,
                "Cambio Binario",
                messaggio,
                LocalDateTime.now().plusHours(12),
                datiExtra
        );

        aggiungiAvvisoInterno(avviso);
    }

    /**
     * Aggiunge notifica ritardo
     */
    public void aggiungiRitardo(String trattaId, int minutiRitardo, String nuovoOrario) {
        String messaggio = String.format("Ritardo di %d minuti - Nuovo orario: %s", minutiRitardo, nuovoOrario);

        Map<String, Object> datiExtra = new HashMap<>();
        datiExtra.put("trattaId", trattaId);
        datiExtra.put("minutiRitardo", minutiRitardo);
        datiExtra.put("nuovoOrario", nuovoOrario);
        datiExtra.put("tipoNotifica", "RITARDO");

        Priorita priorita = minutiRitardo > 30 ? Priorita.CRITICA :
                minutiRitardo > 15 ? Priorita.ALTA : Priorita.MEDIA;

        Avviso avviso = new Avviso(
                TipoAvviso.NOTIFICA_TRATTA,
                priorita,
                "Ritardo Treno",
                messaggio,
                LocalDateTime.now().plusHours(8),
                datiExtra
        );

        aggiungiAvvisoInterno(avviso);
    }

    // ================================================================================
    // ⏰ GESTIONE SCADENZE BIGLIETTI
    // ================================================================================

    /**
     * Aggiunge avviso scadenza prenotazione
     */
    public void aggiungiScadenzaPrenotazione(String bigliettoId, LocalDateTime scadenza) {
        Map<String, Object> datiExtra = new HashMap<>();
        datiExtra.put("bigliettoId", bigliettoId);
        datiExtra.put("scadenza", scadenza);

        long minutiAllaScadenza = Duration.between(LocalDateTime.now(), scadenza).toMinutes();
        String messaggio = String.format("Prenotazione scade tra %d minuti", minutiAllaScadenza);

        Priorita priorita = minutiAllaScadenza < 5 ? Priorita.CRITICA :
                minutiAllaScadenza < 15 ? Priorita.ALTA : Priorita.MEDIA;

        Avviso avviso = new Avviso(
                TipoAvviso.SCADENZA_BIGLIETTO,
                priorita,
                "Scadenza Prenotazione",
                messaggio,
                scadenza,
                datiExtra
        );

        aggiungiAvvisoInterno(avviso);

        // Timer per promemoria a 5 e 1 minuto dalla scadenza
        programmaPromemoria(bigliettoId, scadenza);
    }

    // ================================================================================
    // 📢 GESTIONE AVVISI SISTEMA
    // ================================================================================

    /**
     * Aggiunge avviso generico di sistema
     */
    public void aggiungiAvvisoSistema(String titolo, String messaggio, Priorita priorita) {
        Avviso avviso = new Avviso(
                TipoAvviso.AVVISO_SISTEMA,
                priorita != null ? priorita : Priorita.BASSA,
                titolo,
                messaggio
        );

        aggiungiAvvisoInterno(avviso);
    }

    /**
     * Aggiunge conferma operazione
     */
    public void aggiungiConfermaOperazione(String operazione, boolean successo) {
        String titolo = successo ? "Operazione Completata" : "Operazione Fallita";
        String messaggio = operazione + (successo ? " completata con successo" : " non riuscita");
        Priorita priorita = successo ? Priorita.BASSA : Priorita.MEDIA;

        Avviso avviso = new Avviso(
                TipoAvviso.CONFERMA_OPERAZIONE,
                priorita,
                titolo,
                messaggio,
                LocalDateTime.now().plusHours(2), // Conferme durano 2 ore
                null
        );

        aggiungiAvvisoInterno(avviso);
    }

    // ================================================================================
    // 📋 GESTIONE E VISUALIZZAZIONE AVVISI
    // ================================================================================

    /**
     * Ottieni tutti gli avvisi (ordinati per priorità e timestamp)
     */
    public List<Avviso> getTuttiGliAvvisi() {
        lock.readLock().lock();
        try {
            return avvisi.stream()
                    .filter(a -> !a.isScaduto())
                    .sorted((a1, a2) -> {
                        // Prima per priorità (decrescente)
                        int priorityCompare = Integer.compare(a2.getPriorita().getLivello(), a1.getPriorita().getLivello());
                        if (priorityCompare != 0) return priorityCompare;

                        // Poi per timestamp (più recenti prima)
                        return a2.getTimestamp().compareTo(a1.getTimestamp());
                    })
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Ottieni avvisi per tipo
     */
    public List<Avviso> getAvvisiPerTipo(TipoAvviso tipo) {
        return getTuttiGliAvvisi().stream()
                .filter(a -> a.getTipo() == tipo)
                .collect(Collectors.toList());
    }

    /**
     * Ottieni solo avvisi non letti
     */
    public List<Avviso> getAvvisiNonLetti() {
        return getTuttiGliAvvisi().stream()
                .filter(a -> !a.isLetto())
                .collect(Collectors.toList());
    }

    /**
     * Ottieni avvisi per priorità
     */
    public List<Avviso> getAvvisiPerPriorita(Priorita priorita) {
        return getTuttiGliAvvisi().stream()
                .filter(a -> a.getPriorita() == priorita)
                .collect(Collectors.toList());
    }

    /**
     * Marca avviso come letto
     */
    public boolean marcaComeLetto(String idAvviso) {
        lock.readLock().lock();
        try {
            return avvisi.stream()
                    .filter(a -> a.getId().equals(idAvviso))
                    .findFirst()
                    .map(a -> {
                        a.setLetto(true);
                        return true;
                    })
                    .orElse(false);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Marca tutti gli avvisi come letti
     */
    public int marcaTuttiComeLetti() {
        lock.readLock().lock();
        try {
            return (int) avvisi.stream()
                    .filter(a -> !a.isLetto())
                    .peek(a -> a.setLetto(true))
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Rimuovi avviso specifico
     */
    public boolean rimuoviAvviso(String idAvviso) {
        lock.writeLock().lock();
        try {
            return avvisi.removeIf(a -> a.getId().equals(idAvviso));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ================================================================================
    // 📊 STATISTICHE E UTILITÀ
    // ================================================================================

    /**
     * Statistiche del centro avvisi
     */
    public String getStatistiche() {
        lock.readLock().lock();
        try {
            long nonLetti = avvisi.stream().filter(a -> !a.isLetto()).count();
            long attivi = avvisi.stream().filter(a -> !a.isScaduto()).count();

            return String.format("CentroAvvisi: %d attivi (%d non letti) | Tot: %d promozioni, %d tratte",
                    attivi, nonLetti, promozioniRicevute, notificheTratteRicevute);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Statistiche dettagliate
     */
    public void stampaStatistiche() {
        lock.readLock().lock();
        try {
            System.out.println("\n🔔 STATISTICHE CENTRO AVVISI:");
            System.out.println("   📊 Totale avvisi: " + avvisi.size());
            System.out.println("   📬 Non letti: " + avvisi.stream().filter(a -> !a.isLetto()).count());
            System.out.println("   ✅ Attivi: " + avvisi.stream().filter(a -> !a.isScaduto()).count());
            System.out.println("   🎉 Promozioni ricevute: " + promozioniRicevute);
            System.out.println("   🚂 Notifiche tratte: " + notificheTratteRicevute);

            System.out.println("\n📋 Per tipo:");
            for (TipoAvviso tipo : TipoAvviso.values()) {
                long count = avvisi.stream().filter(a -> a.getTipo() == tipo).count();
                if (count > 0) {
                    System.out.println("   " + tipo.getIcona() + " " + tipo.getDescrizione() + ": " + count);
                }
            }

            System.out.println("\n🚨 Per priorità:");
            for (Priorita priorita : Priorita.values()) {
                long count = avvisi.stream()
                        .filter(a -> !a.isScaduto())
                        .filter(a -> a.getPriorita() == priorita)
                        .count();
                if (count > 0) {
                    System.out.println("   " + priorita.getIcona() + " " + priorita.name() + ": " + count);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    // ================================================================================
    // 🔧 METODI PRIVATI DI SUPPORTO
    // ================================================================================

    private void aggiungiAvvisoInterno(Avviso avviso) {
        avvisi.add(avviso);
        avvisiTotali++;

        // Mantieni limite massimo
        if (avvisi.size() > MAX_AVVISI) {
            rimuoviAvvisiVecchi();
        }
    }

    private void programmaRimozioneScadenza(Avviso avviso) {
        if (avviso.getScadenza() == null) return;

        long secondiAllaScadenza = Duration.between(LocalDateTime.now(), avviso.getScadenza()).getSeconds();
        if (secondiAllaScadenza <= 0) return;

        String timerId = "scadenza_" + avviso.getId();
        // ✅ CORREZIONE: Rimuovi cast sbagliato
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            rimuoviAvviso(avviso.getId());
            timerScadenze.remove(timerId);
            System.out.println("⏰ Avviso scaduto rimosso: " + avviso.getTitolo());
        }, secondiAllaScadenza, TimeUnit.SECONDS);

        // Salva una versione semplificata per cleanup
        timerScadenze.put(timerId, () -> task.cancel(false));
    }

    private void programmaPromemoria(String bigliettoId, LocalDateTime scadenza) {
        // Promemoria 5 minuti prima
        long secondiA5Min = Duration.between(LocalDateTime.now(), scadenza.minusMinutes(5)).getSeconds();
        if (secondiA5Min > 0) {
            scheduler.schedule(() -> {
                aggiungiAvvisoSistema("Promemoria",
                        "La tua prenotazione scade tra 5 minuti!", Priorita.ALTA);
            }, secondiA5Min, TimeUnit.SECONDS);
        }

        // Promemoria 1 minuto prima
        long secondiA1Min = Duration.between(LocalDateTime.now(), scadenza.minusMinutes(1)).getSeconds();
        if (secondiA1Min > 0) {
            scheduler.schedule(() -> {
                aggiungiAvvisoSistema("URGENTE",
                        "La tua prenotazione scade tra 1 minuto!", Priorita.CRITICA);
            }, secondiA1Min, TimeUnit.SECONDS);
        }
    }

    private void pulisciAvvisiScaduti() {
        lock.writeLock().lock();
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(GIORNI_RETENTION);
            long rimossiLong = avvisi.stream()
                    .filter(a -> a.isScaduto() || a.getTimestamp().isBefore(limite))
                    .count();
            avvisi.removeIf(a -> a.isScaduto() || a.getTimestamp().isBefore(limite));
            int rimossi = (int) rimossiLong;

            if (rimossi > 0) {
                System.out.println("🧹 Centro Avvisi: rimossi " + rimossi + " avvisi scaduti/vecchi");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rimuoviAvvisiVecchi() {
        // Rimuovi i 20 avvisi più vecchi quando si supera il limite
        avvisi.stream()
                .sorted((a1, a2) -> a1.getTimestamp().compareTo(a2.getTimestamp()))
                .limit(20)
                .map(Avviso::getId)
                .forEach(this::rimuoviAvviso);
    }

    // ================================================================================
    // 🛑 SHUTDOWN
    // ================================================================================

    /**
     * Shutdown con cleanup delle risorse
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            System.out.println("🛑 CentroAvvisi: Shutdown in corso...");

            // Cancella tutti i timer
            timerScadenze.clear();

            // Shutdown scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Statistiche finali
            stampaStatistiche();

            System.out.println("✅ CentroAvvisi shutdown completato");
        } finally {
            lock.writeLock().unlock();
        }
    }
}