package estadio.agentes;

import estadio.comun.Punto;
import estadio.comun.Tiempo;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Mostrador;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cocinero de la tienda (ver {@code entrega1/ANALISIS.md}, sección 5.3): el productor del
 * {@link Mostrador}. Prepara porciones y las coloca; si el mostrador está lleno, espera.
 */
public class Cocinero extends Agente {

    public enum Estado { CAMINANDO_A_COCINA, PREPARANDO, COLOCANDO, ESPERANDO_ESPACIO, DESCANSANDO, FUERA }

    private static final double PROBABILIDAD_DESCANSO = 0.1;

    private final Mostrador mostrador;
    private final Punto cocina;

    public Cocinero(PlantaBaja planta, Punto cocina) {
        // Llega por la parte de atrás de la tienda
        super(planta.getEscenario(), cocina.desplazado(0, -40), Estado.CAMINANDO_A_COCINA);
        this.mostrador = planta.getMostrador();
        this.cocina = cocina;
    }

    @Override
    protected void vivir() throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        caminarHacia(cocina);
        while (true) {
            cambiarEstado(Estado.PREPARANDO);
            Tiempo.dormir(rnd.nextLong(1500, 3000));

            cambiarEstado(Estado.COLOCANDO);
            if (!mostrador.intentarPoner()) {
                // Mostrador lleno: espera (wait) dentro del monitor hasta que alguien compre
                cambiarEstado(Estado.ESPERANDO_ESPACIO);
                mostrador.poner();
            }

            // Zona de decisión: seguir cocinando o descansar un rato
            if (rnd.nextDouble() < PROBABILIDAD_DESCANSO) {
                cambiarEstado(Estado.DESCANSANDO);
                Tiempo.dormir(rnd.nextLong(3000, 6000));
            }
        }
    }

    @Override
    public String getTipo() {
        return "Cocinero";
    }

    @Override
    public Color getColor() {
        return new Color(251, 140, 0);
    }

    @Override
    protected Enum<?> estadoFinal() {
        return Estado.FUERA;
    }
}
