package estadio.agentes;

import estadio.comun.Config;
import estadio.comun.Punto;
import estadio.comun.Tiempo;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Torniquetes;
import estadio.recursos.Torniquetes.Solicitud;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Guardia de la entrada (ver {@code entrega1/ANALISIS.md}, sección 5.2).
 * Espera aficionados en su puesto, revisa boletos y de vez en cuando descansa.
 */
public class Guardia extends Agente {

    public enum Estado implements EstadoAgente {
        CAMINANDO_A_PUESTO(97, 97, 97),
        ESPERANDO_AFICIONADO(33, 33, 33),
        REVISANDO_BOLETO(121, 85, 72),
        DESCANSANDO(188, 170, 164),
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

    private final PlantaBaja planta;
    private final Config config;
    private final Torniquetes torniquetes;
    private final Punto puesto;

    public Guardia(PlantaBaja planta, Punto puesto) {
        super(planta.getEscenario(), planta.getCaseta(), Estado.CAMINANDO_A_PUESTO);
        this.planta = planta;
        this.config = planta.getConfig();
        this.torniquetes = planta.getTorniquetes();
        this.puesto = puesto;
    }

    @Override
    protected void vivir() throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        caminarA(puesto);
        while (true) {
            cambiarEstado(Estado.ESPERANDO_AFICIONADO);
            Solicitud solicitud = torniquetes.esperarAficionado(config.tiempo(Config.ESPERA_GUARDIA).alAzar());

            if (solicitud == null) {
                // Zona de decisión: nadie llegó a tiempo
                if (rnd.nextInt(100) < config.getProbDescansoGuardia()) {
                    descansar();
                }
                continue;
            }

            cambiarEstado(Estado.REVISANDO_BOLETO);
            caminarHacia(planta.getPuntoRevision(solicitud.getLugar()));
            Tiempo.dormir(config.tiempo(Config.REVISION).alAzar());
            torniquetes.darVeredicto(solicitud, solicitud.getBoleto().autentico());
            caminarHacia(puesto);
        }
    }

    private void descansar() throws InterruptedException {
        cambiarEstado(Estado.DESCANSANDO);
        caminarA(planta.getCaseta());
        Tiempo.dormir(config.tiempo(Config.DESCANSO_GUARDIA).alAzar());
        cambiarEstado(Estado.CAMINANDO_A_PUESTO);
        caminarA(puesto);
    }

    @Override
    public String getTipo() {
        return "Guardia";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return Estado.FUERA;
    }
}
