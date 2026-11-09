package estadio.agentes;

import estadio.comun.Mapa;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Agente temporal de la Fase 1 para probar la animación y la tabla de estados.
 * Pasa por los estados de hilo RUNNABLE (pensando), TIMED_WAITING (caminando, descansando)
 * y TERMINATED (al acabar sus vueltas). Se reemplaza por los agentes reales en la Fase 2.
 */
public class AgentePrueba extends Agente {

    public enum Estado { CAMINANDO, DESCANSANDO, PENSANDO, FUERA }

    private final int vueltas;

    public AgentePrueba() {
        super(aleatorio(Mapa.ANCHO), aleatorio(Mapa.ALTO), Estado.CAMINANDO);
        this.vueltas = ThreadLocalRandom.current().nextInt(3, 8);
    }

    @Override
    protected void vivir() throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < vueltas; i++) {
            cambiarEstado(Estado.CAMINANDO);
            caminarHacia(aleatorio(Mapa.ANCHO), aleatorio(Mapa.ALTO));

            if (rnd.nextBoolean()) {
                cambiarEstado(Estado.DESCANSANDO);
                Thread.sleep(rnd.nextLong(1000, 3000));
            } else {
                cambiarEstado(Estado.PENSANDO);
                ocuparCpu(rnd.nextLong(500, 1500));
            }
        }
    }

    /** Trabajo activo (sin dormir) para que el hilo se vea en RUNNABLE. */
    private static void ocuparCpu(long milisegundos) {
        long fin = System.nanoTime() + milisegundos * 1_000_000;
        while (System.nanoTime() < fin && !Thread.currentThread().isInterrupted()) {
            Thread.onSpinWait();
        }
    }

    private static double aleatorio(int maximo) {
        return ThreadLocalRandom.current().nextDouble(20, maximo - 20);
    }

    @Override
    public String getTipo() {
        return "Prueba";
    }

    @Override
    public Color getColor() {
        return new Color(255, 193, 7);
    }

    @Override
    protected Enum<?> estadoFinal() {
        return Estado.FUERA;
    }
}
