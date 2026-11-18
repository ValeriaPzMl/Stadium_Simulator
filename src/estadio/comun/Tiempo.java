package estadio.comun;

/**
 * Reloj de la simulación. Todas las duraciones se escriben en milisegundos "normales" y se
 * dividen entre el factor de velocidad ({@code --velocidad}), para acelerar pruebas y demos.
 */
public final class Tiempo {

    private static volatile double factor = 1.0;

    private Tiempo() {
    }

    public static void setFactor(double nuevoFactor) {
        if (nuevoFactor <= 0) {
            throw new IllegalArgumentException("La velocidad debe ser mayor que 0");
        }
        factor = nuevoFactor;
    }

    public static double getFactor() {
        return factor;
    }

    /** Convierte una duración normal a la duración real según la velocidad. */
    public static long escalar(long milisegundos) {
        return Math.max(1, Math.round(milisegundos / factor));
    }

    /** {@link Thread#sleep} escalado: el hilo queda en TIMED_WAITING. */
    public static void dormir(long milisegundos) throws InterruptedException {
        Thread.sleep(escalar(milisegundos));
    }
}
