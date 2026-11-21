package estadio.agentes;

import estadio.comun.Config;
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

    public enum Estado implements EstadoAgente {
        CAMINANDO_A_COCINA(255, 224, 178),
        PREPARANDO(251, 140, 0),
        COLOCANDO(255, 183, 77),
        ESPERANDO_ESPACIO(191, 54, 12),
        DESCANSANDO(215, 204, 200),
        FUERA(117, 117, 117);

        private final Color color;

        Estado(int r, int g, int b) {
            this.color = new Color(r, g, b);
        }

        @Override
        public Color color() {
            return color;
        }
    }

    private final Config config;
    private final Mostrador mostrador;
    private final Punto cocina;

    public Cocinero(PlantaBaja planta, Punto cocina) {
        // Llega por la parte de atrás de la tienda
        super(planta.getEscenario(), cocina.desplazado(0, -40), Estado.CAMINANDO_A_COCINA);
        this.config = planta.getConfig();
        this.mostrador = planta.getMostrador();
        this.cocina = cocina;
    }

    @Override
    protected void vivir() throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        caminarHacia(cocina);
        while (true) {
            cambiarEstado(Estado.PREPARANDO);
            Tiempo.dormir(config.tiempo(Config.PREPARAR).alAzar());

            cambiarEstado(Estado.COLOCANDO);
            if (!mostrador.intentarPoner()) {
                // Mostrador lleno: espera (wait) dentro del monitor hasta que alguien compre
                cambiarEstado(Estado.ESPERANDO_ESPACIO);
                mostrador.poner();
            }

            // Zona de decisión: seguir cocinando o descansar un rato
            if (rnd.nextInt(100) < config.getProbDescansoCocinero()) {
                cambiarEstado(Estado.DESCANSANDO);
                Tiempo.dormir(config.tiempo(Config.DESCANSO_COCINERO).alAzar());
            }
        }
    }

    @Override
    public String getTipo() {
        return "Cocinero";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return Estado.FUERA;
    }
}
