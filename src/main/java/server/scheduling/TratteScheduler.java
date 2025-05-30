package scheduling;

import model.Tratta;
import persistence.MemoriaTratte;
import factory.TrattaFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TratteScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MemoriaTratte memoria;
    private final TrattaFactory factory;

    public TratteScheduler(MemoriaTratte memoria, TrattaFactory factory) {
        this.memoria = memoria;
        this.factory = factory;
    }

    public void avviaSchedulazione() {
        long initialDelay = calcolaDelayMezzanotte();
        scheduler.scheduleAtFixedRate(this::aggiornaTratte, initialDelay, 24, TimeUnit.HOURS);
    }

    private void aggiornaTratte() {
        LocalDate oggi = LocalDate.now();
        memoria.rimuoviTratteDelGiorno(oggi);

        for (int i = 1; i <= 3; i++) {
            LocalDate giorno = oggi.plusDays(i);
            List<Tratta> nuove = factory.generaTratte(giorno);
            nuove.forEach(memoria::aggiungiTratta);
        }

        System.out.println("✅ Tratte aggiornate automaticamente");
    }

    private long calcolaDelayMezzanotte() {
        long oraMillis = System.currentTimeMillis();
        long mezzanotteMillis = LocalDate.now().plusDays(1)
                .atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return (mezzanotteMillis - oraMillis) / 1000; // secondi
    }

    public void ferma() {
        scheduler.shutdownNow();
    }

    // 🔍 Metodo per test manuale
    public void testAggiornaTratte() {
        aggiornaTratte();
    }
}