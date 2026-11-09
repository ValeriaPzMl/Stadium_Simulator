package estadio.agentes;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de todos los agentes del simulador. Cada agente es un hilo con:
 * <ul>
 *   <li>un <b>estado del dominio</b> (lo que la persona está haciendo, p. ej. VIENDO_PARTIDO),
 *       definido por cada subclase con su propio enum;</li>
 *   <li>una posición (x, y) en el {@link estadio.comun.Mapa} del piso.</li>
 * </ul>
 * El estado del <b>hilo</b> (RUNNABLE, WAITING...) lo da {@link Thread#getState()}.
 *
 * <p>La interfaz lee estado y posición desde el EDT mientras el agente los cambia desde su
 * propio hilo; por eso los campos son {@code volatile}. Una lectura puede ver x nueva con y
 * vieja, pero eso solo afecta un cuadro de la animación.
 */
public abstract class Agente extends Thread {

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    /** Pausa entre pasos al caminar (ms) y distancia por paso. */
    private static final long MS_POR_PASO = 30;
    private static final double PIXELES_POR_PASO = 3.0;

    private final int idAgente;
    private volatile Enum<?> estado;
    private volatile double x;
    private volatile double y;

    protected Agente(double x, double y, Enum<?> estadoInicial) {
        this.idAgente = SIGUIENTE_ID.getAndIncrement();
        this.x = x;
        this.y = y;
        this.estado = estadoInicial;
        setName(getTipo() + "-" + idAgente);
        setDaemon(true); // al cerrar la ventana no deben mantener viva la JVM
    }

    /** Comportamiento del agente (su máquina de estados). */
    protected abstract void vivir() throws InterruptedException;

    /** Nombre del tipo de agente para la tabla, p. ej. "Aficionado". */
    public abstract String getTipo();

    /** Color con el que se dibuja. */
    public abstract Color getColor();

    /** Estado del dominio que se muestra cuando el hilo ya terminó. */
    protected abstract Enum<?> estadoFinal();

    @Override
    public final void run() {
        try {
            vivir();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            estado = estadoFinal();
        }
    }

    protected void cambiarEstado(Enum<?> nuevo) {
        estado = nuevo;
    }

    /** Camina en línea recta hasta (destinoX, destinoY), un paso cada {@value #MS_POR_PASO} ms. */
    protected void caminarHacia(double destinoX, double destinoY) throws InterruptedException {
        while (true) {
            double dx = destinoX - x;
            double dy = destinoY - y;
            double distancia = Math.hypot(dx, dy);
            if (distancia <= PIXELES_POR_PASO) {
                x = destinoX;
                y = destinoY;
                return;
            }
            x += dx / distancia * PIXELES_POR_PASO;
            y += dy / distancia * PIXELES_POR_PASO;
            Thread.sleep(MS_POR_PASO);
        }
    }

    public int getIdAgente() {
        return idAgente;
    }

    public Enum<?> getEstado() {
        return estado;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
