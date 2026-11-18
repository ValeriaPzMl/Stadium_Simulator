package estadio.recursos;

import estadio.agentes.Agente;
import estadio.comun.Bitacora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asientos numerados de una zona. Cada asiento es una sección crítica de búfer 1:
 * {@link Asiento#ocupar} y {@link Asiento#liberar} son {@code synchronized} sobre el asiento,
 * así que si dos aficionados con el mismo número (boleto duplicado) llegan a la vez,
 * solo uno se sienta.
 */
public class Asientos {

    public final class Asiento {
        private final int numero;
        private final double x;
        private final double y;
        private Agente ocupante; // protegido por synchronized(this)

        private Asiento(int numero, double x, double y) {
            this.numero = numero;
            this.x = x;
            this.y = y;
        }

        /** @return {@code true} si se sentó; {@code false} si ya estaba ocupado */
        public synchronized boolean ocupar(Agente aficionado) {
            if (ocupante != null) {
                conflictos.incrementAndGet();
                Bitacora.info("Asiento " + numero + " ya ocupado por " + ocupante.getName());
                return false;
            }
            ocupante = aficionado;
            sentados.incrementAndGet();
            return true;
        }

        public synchronized void liberar(Agente aficionado) {
            if (ocupante != aficionado) {
                Bitacora.invarianteRoto(aficionado.getName() + " intentó liberar el asiento " + numero
                        + " que ocupa " + ocupante);
                return;
            }
            ocupante = null;
            sentados.decrementAndGet();
        }

        public synchronized boolean estaOcupado() {
            return ocupante != null;
        }

        public int getNumero() {
            return numero;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    private final List<Asiento> asientos;
    private final AtomicInteger sentados = new AtomicInteger();
    private final AtomicInteger conflictos = new AtomicInteger();

    /** Crea una cuadrícula de filas × columnas asientos, numerados de 0 por filas. */
    public Asientos(int filas, int columnas, double xInicial, double yInicial, double separacionX,
                    double separacionY) {
        List<Asiento> lista = new ArrayList<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                lista.add(new Asiento(lista.size(), xInicial + c * separacionX, yInicial + f * separacionY));
            }
        }
        this.asientos = Collections.unmodifiableList(lista);
    }

    public Asiento get(int numero) {
        return asientos.get(numero);
    }

    public List<Asiento> todos() {
        return asientos;
    }

    public int getCantidad() {
        return asientos.size();
    }

    public int getSentados() {
        return sentados.get();
    }

    /** Veces que alguien llegó a un asiento ya ocupado (boletos duplicados). */
    public int getConflictos() {
        return conflictos.get();
    }
}
