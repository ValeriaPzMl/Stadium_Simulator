package estadio.recursos;

import estadio.comun.Bitacora;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zona crítica de búfer &gt; 1: N lugares numerados (cubículos, cajas...) protegidos por un
 * {@link Semaphore} justo con N permisos. Quien llega hace {@code acquire()} (si no hay lugar,
 * su hilo queda en WAITING), toma un lugar libre y al terminar lo devuelve con {@code release()}.
 *
 * <p>Saber <i>qué</i> lugar tomó cada quien (para dibujarlo) es un dato aparte, protegido con
 * {@code synchronized}: el semáforo garantiza que siempre hay uno libre.
 */
public class ZonaConCupo {

    private final String nombre;
    private final int capacidad;
    private final Semaphore cupo;
    private final boolean[] ocupado; // protegido por synchronized(this)

    private final AtomicInteger enFila = new AtomicInteger();
    private final AtomicInteger enUso = new AtomicInteger();
    private final AtomicInteger maximoEnUso = new AtomicInteger();
    private final AtomicInteger atendidos = new AtomicInteger();

    public ZonaConCupo(String nombre, int capacidad) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.cupo = new Semaphore(capacidad, true);
        this.ocupado = new boolean[capacidad];
    }

    /** Espera un lugar libre (WAITING si están todos ocupados). @return el número de lugar */
    public int entrar() throws InterruptedException {
        enFila.incrementAndGet();
        try {
            cupo.acquire();
        } finally {
            enFila.decrementAndGet();
        }
        int lugar = tomarLugarLibre();
        int ahora = enUso.incrementAndGet();
        maximoEnUso.accumulateAndGet(ahora, Math::max);
        if (ahora > capacidad) {
            Bitacora.invarianteRoto(nombre + ": " + ahora + " personas con solo " + capacidad + " lugares");
        }
        return lugar;
    }

    public void salir(int lugar) {
        synchronized (this) {
            ocupado[lugar] = false;
        }
        enUso.decrementAndGet();
        atendidos.incrementAndGet();
        cupo.release();
    }

    private synchronized int tomarLugarLibre() {
        for (int i = 0; i < capacidad; i++) {
            if (!ocupado[i]) {
                ocupado[i] = true;
                return i;
            }
        }
        throw new IllegalStateException(nombre + ": no hay lugar libre aunque el semáforo dio permiso");
    }

    public synchronized boolean estaOcupado(int lugar) {
        return ocupado[lugar];
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getEnFila() {
        return enFila.get();
    }

    public int getEnUso() {
        return enUso.get();
    }

    public int getMaximoEnUso() {
        return maximoEnUso.get();
    }

    public int getAtendidos() {
        return atendidos.get();
    }
}
