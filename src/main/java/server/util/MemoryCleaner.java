package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * 🧹 MEMORY CLEANER - Pulisce tutti i dati persistenti
 *
 * Utilità per eliminare completamente la memoria del sistema TreniCal
 */
public class MemoryCleaner {

    private static final String DATA_DIR = "src/main/resources/data/";
    private static final String[] FILES_TO_CLEAN = {
            "biglietti.json",
            "tratte.json",
            "clientiFedeli.json",
            "promozioni.json",
            "osservatoriTratte.json"
    };

    public static void main(String[] args) {
        System.out.println("🧹 ===== TRENICAL MEMORY CLEANER =====");
        System.out.println("⚠️  ATTENZIONE: Questa operazione eliminerà TUTTI i dati!");
        System.out.println();

        // Mostra cosa verrà eliminato
        mostraStatoAttuale();

        // Chiedi conferma
        if (!chiediConferma()) {
            System.out.println("❌ Operazione annullata.");
            return;
        }

        // Esegui pulizia
        pulisciMemoria();

        // Verifica risultato
        verificaPulizia();

        System.out.println("\n✅ ===== PULIZIA COMPLETATA =====");
        System.out.println("💡 Ora puoi riavviare il server per iniziare con memoria pulita!");
    }

    private static void mostraStatoAttuale() {
        System.out.println("📁 STATO ATTUALE MEMORIA:");
        System.out.println("-".repeat(30));

        for (String fileName : FILES_TO_CLEAN) {
            File file = new File(DATA_DIR + fileName);
            if (file.exists()) {
                long size = file.length();
                String status = size > 10 ? "📊 Contiene dati (" + size + " bytes)" : "📄 Vuoto";
                System.out.println("   " + fileName + " - " + status);
            } else {
                System.out.println("   " + fileName + " - ❌ Non esiste");
            }
        }
        System.out.println();
    }

    private static boolean chiediConferma() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("🗑️  OPERAZIONI CHE VERRANNO ESEGUITE:");
        System.out.println("   • Eliminazione di tutti i biglietti venduti");
        System.out.println("   • Eliminazione di tutte le tratte generate");
        System.out.println("   • Reset di tutti i clienti fedeli");
        System.out.println("   • Cancellazione di tutte le promozioni");
        System.out.println("   • Pulizia iscrizioni notifiche");
        System.out.println();

        System.out.print("⚠️  Sei SICURO di voler procedere? (scrivi 'ELIMINA' per confermare): ");
        String input = scanner.nextLine().trim();

        return "ELIMINA".equals(input);
    }

    private static void pulisciMemoria() {
        System.out.println("🧹 Pulizia in corso...");
        System.out.println("-".repeat(20));

        // Crea directory se non esiste
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("📁 Creata directory: " + DATA_DIR);
            }
        }

        int filesProcessed = 0;
        int filesSuccess = 0;

        for (String fileName : FILES_TO_CLEAN) {
            filesProcessed++;
            String filePath = DATA_DIR + fileName;

            try {
                // Crea file con array JSON vuoto
                FileWriter writer = new FileWriter(filePath);

                if (fileName.equals("osservatoriTratte.json")) {
                    // Questo file ha struttura object, non array
                    writer.write("{}");
                } else {
                    // Tutti gli altri hanno struttura array
                    writer.write("[]");
                }

                writer.close();
                filesSuccess++;
                System.out.println("   ✅ " + fileName + " pulito");

            } catch (IOException e) {
                System.err.println("   ❌ Errore pulizia " + fileName + ": " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("📊 RISULTATO PULIZIA:");
        System.out.println("   File processati: " + filesProcessed);
        System.out.println("   File puliti con successo: " + filesSuccess);
        System.out.println("   Errori: " + (filesProcessed - filesSuccess));
    }

    private static void verificaPulizia() {
        System.out.println("\n🔍 VERIFICA POST-PULIZIA:");
        System.out.println("-".repeat(25));

        boolean tuttiPuliti = true;

        for (String fileName : FILES_TO_CLEAN) {
            File file = new File(DATA_DIR + fileName);

            if (file.exists()) {
                long size = file.length();
                boolean isPulito = size <= 10; // File vuoti sono circa 2-3 bytes

                if (isPulito) {
                    System.out.println("   ✅ " + fileName + " - PULITO");
                } else {
                    System.out.println("   ⚠️ " + fileName + " - CONTIENE ANCORA DATI (" + size + " bytes)");
                    tuttiPuliti = false;
                }
            } else {
                System.out.println("   ❓ " + fileName + " - NON ESISTE");
                tuttiPuliti = false;
            }
        }

        System.out.println();
        if (tuttiPuliti) {
            System.out.println("🎉 MEMORIA COMPLETAMENTE PULITA!");
        } else {
            System.out.println("⚠️  Alcuni file potrebbero non essere stati puliti correttamente.");
        }
    }

    /**
     * Metodo di utilità per pulizia rapida senza conferma (per testing)
     */
    public static void pulisciaRapida() {
        System.out.println("🧹 Pulizia rapida memoria...");

        for (String fileName : FILES_TO_CLEAN) {
            try {
                FileWriter writer = new FileWriter(DATA_DIR + fileName);
                if (fileName.equals("osservatoriTratte.json")) {
                    writer.write("{}");
                } else {
                    writer.write("[]");
                }
                writer.close();
            } catch (IOException e) {
                System.err.println("Errore pulizia " + fileName + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Pulizia rapida completata!");
    }

    /**
     * Metodo per verificare se la memoria è vuota
     */
    public static boolean isMemoriaVuota() {
        for (String fileName : FILES_TO_CLEAN) {
            File file = new File(DATA_DIR + fileName);
            if (file.exists() && file.length() > 10) {
                return false;
            }
        }
        return true;
    }

    /**
     * Metodo per ottenere statistiche memoria
     */
    public static void stampaStatistiche() {
        System.out.println("📊 STATISTICHE MEMORIA:");
        long totalSize = 0;
        int existingFiles = 0;

        for (String fileName : FILES_TO_CLEAN) {
            File file = new File(DATA_DIR + fileName);
            if (file.exists()) {
                existingFiles++;
                totalSize += file.length();
                System.out.println("   " + fileName + ": " + file.length() + " bytes");
            }
        }

        System.out.println("   Totale: " + existingFiles + " files, " + totalSize + " bytes");
        System.out.println("   Memoria vuota: " + (isMemoriaVuota() ? "SÌ" : "NO"));
    }
}