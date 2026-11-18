package estadio.comun;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Log en consola con hora e hilo, para seguir lo que pasa y revisar invariantes. */
public final class Bitacora {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Bitacora() {
    }

    public static void info(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [" + Thread.currentThread().getName() + "] "
                + mensaje);
    }

    /** Una condición que nunca debería ocurrir; si aparece, hay un error de sincronización. */
    public static void invarianteRoto(String mensaje) {
        System.err.println("[" + LocalTime.now().format(HORA) + "] [" + Thread.currentThread().getName()
                + "] ¡INVARIANTE ROTO! " + mensaje);
    }
}
