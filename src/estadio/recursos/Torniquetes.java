package estadio.recursos;

import estadio.comun.Bitacora;
import estadio.comun.Boleto;
import estadio.comun.Tiempo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entrada del estadio (búfer T, zona crítica de capacidad &gt; 1).
 *
 * <p><b>Capacidad:</b> un {@link Semaphore} justo con T permisos. Cada aficionado toma uno para
 * pasar a un torniquete y lo devuelve al cruzarlo, así nunca hay más de T en revisión.
 *
 * <p><b>Cita guardia ↔ aficionado</b> (patrón de "la peluquería"):
 * <ol>
 *   <li>El aficionado deja su {@link Solicitud} en la cola y hace {@code aficionadoListo.release()}.</li>
 *   <li>El guardia hace {@code aficionadoListo.acquire()}, toma la solicitud y revisa el boleto.</li>
 *   <li>El guardia hace {@code veredicto.release()} y el aficionado, que esperaba en
 *       {@code veredicto.acquire()}, continúa con el resultado.</li>
 * </ol>
 */
public class Torniquetes {

    /** Lo que un aficionado le entrega a un guardia para que lo revise. */
    public static final class Solicitud {
        private final Boleto boleto;
        private final int lugar;
        private final Semaphore veredicto = new Semaphore(0);
        private boolean aceptado; // lo escribe el guardia antes de release(); el semáforo da la visibilidad

        private Solicitud(Boleto boleto, int lugar) {
            this.boleto = boleto;
            this.lugar = lugar;
        }

        public Boleto getBoleto() {
            return boleto;
        }

        public int getLugar() {
            return lugar;
        }
    }

    private final int capacidad;
    private final double[] posicionX;
    private final double posicionY;

    private final Semaphore lugares;
    private final boolean[] ocupado; // protegido por synchronized(this)

    private final Semaphore aficionadoListo = new Semaphore(0);
    private final Queue<Solicitud> porRevisar = new ConcurrentLinkedQueue<>();

    // Contadores para la interfaz y para comprobar invariantes
    private final AtomicInteger enFila = new AtomicInteger();
    private final AtomicInteger enRevision = new AtomicInteger();
    private final AtomicInteger maximoEnRevision = new AtomicInteger();
    private final AtomicInteger aceptados = new AtomicInteger();
    private final AtomicInteger rechazados = new AtomicInteger();

    /**
     * @param posicionX coordenada X de cada torniquete; su longitud es la capacidad T
     * @param posicionY coordenada Y común de los torniquetes
     */
    public Torniquetes(double[] posicionX, double posicionY) {
        this.capacidad = posicionX.length;
        this.posicionX = posicionX.clone();
        this.posicionY = posicionY;
        this.lugares = new Semaphore(capacidad, true);
        this.ocupado = new boolean[capacidad];
    }

    // ------------------------------------------------------------ lado del aficionado

    /** Espera turno en la fila y devuelve el número de torniquete asignado. */
    public int entrar() throws InterruptedException {
        enFila.incrementAndGet();
        try {
            lugares.acquire(); // WAITING si los T torniquetes están ocupados
        } finally {
            enFila.decrementAndGet();
        }
        int lugar = tomarLugarLibre();
        int ahora = enRevision.incrementAndGet();
        maximoEnRevision.accumulateAndGet(ahora, Math::max);
        if (ahora > capacidad) {
            Bitacora.invarianteRoto(ahora + " aficionados en revisión con solo " + capacidad + " torniquetes");
        }
        return lugar;
    }

    /** Pide al guardia que revise el boleto y espera el veredicto. */
    public boolean pedirRevision(Boleto boleto, int lugar) throws InterruptedException {
        Solicitud solicitud = new Solicitud(boleto, lugar);
        porRevisar.add(solicitud);
        aficionadoListo.release();
        solicitud.veredicto.acquire(); // WAITING hasta que un guardia responda
        return solicitud.aceptado;
    }

    /** Libera el torniquete después de cruzarlo (o de ser rechazado). */
    public void salir(int lugar) {
        synchronized (this) {
            ocupado[lugar] = false;
        }
        enRevision.decrementAndGet();
        lugares.release();
    }

    /** Siempre hay uno libre porque el semáforo deja pasar como máximo T aficionados. */
    private synchronized int tomarLugarLibre() {
        for (int i = 0; i < capacidad; i++) {
            if (!ocupado[i]) {
                ocupado[i] = true;
                return i;
            }
        }
        throw new IllegalStateException("No hay torniquete libre aunque el semáforo dio permiso");
    }

    // ------------------------------------------------------------ lado del guardia

    /**
     * Espera a que un aficionado pida revisión, como máximo {@code milisegundos} (escalados).
     *
     * @return la solicitud a revisar, o {@code null} si nadie llegó a tiempo
     */
    public Solicitud esperarAficionado(long milisegundos) throws InterruptedException {
        if (!aficionadoListo.tryAcquire(Tiempo.escalar(milisegundos), TimeUnit.MILLISECONDS)) {
            return null;
        }
        // Por cada permiso de aficionadoListo hay exactamente una solicitud en la cola
        return porRevisar.poll();
    }

    /** El guardia comunica el resultado y despierta al aficionado. */
    public void darVeredicto(Solicitud solicitud, boolean aceptado) {
        solicitud.aceptado = aceptado;
        (aceptado ? aceptados : rechazados).incrementAndGet();
        solicitud.veredicto.release();
    }

    // ------------------------------------------------------------ consultas

    public int getCapacidad() {
        return capacidad;
    }

    public double getX(int lugar) {
        return posicionX[lugar];
    }

    public double getY() {
        return posicionY;
    }

    public synchronized boolean estaOcupado(int lugar) {
        return ocupado[lugar];
    }

    public int getEnFila() {
        return enFila.get();
    }

    public int getEnRevision() {
        return enRevision.get();
    }

    public int getMaximoEnRevision() {
        return maximoEnRevision.get();
    }

    public int getAceptados() {
        return aceptados.get();
    }

    public int getRechazados() {
        return rechazados.get();
    }
}
