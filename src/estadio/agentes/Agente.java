package estadio.agentes;

import estadio.comun.Escenario;
import estadio.comun.Punto;
import estadio.comun.Tiempo;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de todos los agentes del simulador. Cada agente es un hilo con:
 * <ul>
 *   <li>un <b>estado del dominio</b> (lo que la persona está haciendo, p. ej. VIENDO_PARTIDO),
 *       definido por cada subclase con su propio enum;</li>
 *   <li>una posición (x, y) en pixeles de la imagen del {@link Escenario} de su piso.</li>
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
    private static final double PIXELES_POR_PASO = 7.0;

    private final int idAgente;
    private final Escenario escenario;
    private volatile Enum<?> estado;
    private volatile double x;
    private volatile double y;

    protected Agente(Escenario escenario, Punto inicio, Enum<?> estadoInicial) {
        this.idAgente = SIGUIENTE_ID.getAndIncrement();
        this.escenario = escenario;
        this.x = inicio.x();
        this.y = inicio.y();
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

    /**
     * Camina en línea recta hasta {@code destino}, un paso cada {@value #MS_POR_PASO} ms.
     * Úsalo para trayectos cortos sin muros en medio (p. ej. de la fila al torniquete).
     * Con {@code --velocidad} mayor a 1 los pasos son más largos, no más frecuentes.
     */
    protected void caminarHacia(Punto destino) throws InterruptedException {
        double paso = PIXELES_POR_PASO * Tiempo.getFactor();
        while (true) {
            double dx = destino.x() - x;
            double dy = destino.y() - y;
            double distancia = Math.hypot(dx, dy);
            if (distancia <= paso) {
                x = destino.x();
                y = destino.y();
                return;
            }
            x += dx / distancia * paso;
            y += dy / distancia * paso;
            Thread.sleep(MS_POR_PASO);
        }
    }

    /** Camina hasta {@code destino} por los pasillos del escenario, rodeando los muros. */
    protected void caminarA(Punto destino) throws InterruptedException {
        for (Punto p : escenario.ruta(getPosicion(), destino)) {
            caminarHacia(p);
        }
    }

    public Punto getPosicion() {
        return new Punto(x, y);
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
