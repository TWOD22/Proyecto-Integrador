package sigep.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Simple file-based logger for writes to Usuario.Habilitado_Asig. */
public final class HabilitadoLogger {
    private static final Object LOCK = new Object();
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = LOG_DIR + File.separator + "habilitado_changes.log";
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private HabilitadoLogger() {}

    public static void logChange(String cedula, Integer oldVal, Integer newVal, String reason) {
        synchronized (LOCK) {
            try {
                File d = new File(LOG_DIR);
                if (!d.exists()) d.mkdirs();
                try (FileWriter w = new FileWriter(LOG_FILE, true)) {
                    String ts = LocalDateTime.now().format(F);
                    w.append(ts).append(" | ")
                     .append(cedula == null ? "<null>" : cedula).append(" | ")
                     .append(oldVal == null ? "<null>" : Integer.toString(oldVal)).append(" -> ")
                     .append(newVal == null ? "<null>" : Integer.toString(newVal)).append(" | ")
                     .append(reason == null ? "" : reason).append(System.lineSeparator());
                }
            } catch (IOException ignored) {}
        }
    }
}
