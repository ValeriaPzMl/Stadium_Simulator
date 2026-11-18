package estadio.agentes;

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

    public enum Estado { CAMINANDO_A_PUESTO, ESPERANDO_AFICIONADO, REVISANDO_BOLETO, DESCANSANDO, FUERA }

    /** Tiempo máximo de espera antes de pasar a la zona de decisión. */
    private static final long ESPERA_MAXIMA_MS = 5000;
    private static final double PROBABILIDAD_DESCANSO = 0.3;

    private final Torniquetes torniquetes;
    private final double puestoX;
    private final double puestoY;

    public Guardia(Torniquetes torniquetes, double puestoX, double puestoY) {
        super(PlantaBaja.CASETA_X, PlantaBaja.CASETA_Y, Estado.CAMINANDO_A_PUESTO);
        this.torniquetes = torniquetes;
        this.puestoX = puestoX;
        this.puestoY = puestoY;
    }

    @Override
    protected void vivir() throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        caminarHacia(puestoX, puestoY);
        while (true) {
            cambiarEstado(Estado.ESPERANDO_AFICIONADO);
            Solicitud solicitud = torniquetes.esperarAficionado(ESPERA_MAXIMA_MS);

            if (solicitud == null) {
                // Zona de decisión: nadie llegó a tiempo
                if (rnd.nextDouble() < PROBABILIDAD_DESCANSO) {
                    descansar();
                }
                continue;
            }

            cambiarEstado(Estado.REVISANDO_BOLETO);
            caminarHacia(torniquetes.getX(solicitud.getLugar()) + 18, torniquetes.getY());
            Tiempo.dormir(rnd.nextLong(800, 1600));
            torniquetes.darVeredicto(solicitud, solicitud.getBoleto().autentico());
            caminarHacia(puestoX, puestoY);
        }
    }

    private void descansar() throws InterruptedException {
        cambiarEstado(Estado.DESCANSANDO);
        caminarHacia(PlantaBaja.CASETA_X, PlantaBaja.CASETA_Y);
        Tiempo.dormir(ThreadLocalRandom.current().nextLong(4000, 8000));
        cambiarEstado(Estado.CAMINANDO_A_PUESTO);
        caminarHacia(puestoX, puestoY);
    }

    @Override
    public String getTipo() {
        return "Guardia";
    }

    @Override
    public Color getColor() {
        return new Color(33, 33, 33);
    }

    @Override
    protected Enum<?> estadoFinal() {
        return Estado.FUERA;
    }
}
