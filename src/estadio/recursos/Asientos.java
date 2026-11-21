package estadio.recursos;

import estadio.agentes.Agente;
import estadio.comun.Bitacora;
import estadio.comun.Punto;

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
        private final Punto posicion;
        private Agente ocupante; // protegido por synchronized(this)

        private Asiento(int numero, Punto posicion) {
            this.numero = numero;
            this.posicion = posicion;
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

        public Punto getPosicion() {
            return posicion;
        }
    }

    private final List<Asiento> asientos;
    private final AtomicInteger sentados = new AtomicInteger();
    private final AtomicInteger conflictos = new AtomicInteger();

    /** Un asiento por posición, numerados desde 0 en el orden dado. */
    public Asientos(List<Punto> posiciones) {
        List<Asiento> lista = new ArrayList<>();
        for (Punto p : posiciones) {
            lista.add(new Asiento(lista.size(), p));
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
