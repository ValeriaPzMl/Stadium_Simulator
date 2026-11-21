package estadio.gui;

import estadio.comun.Punto;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Asientos;
import estadio.recursos.Torniquetes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Lo dinámico de S1 encima de {@code s1.png}: asientos libres u ocupados y torniquetes en uso.
 * Los contadores van en la barra bajo el plano.
 */
public class CapaPlantaBaja implements Capa {

    private static final Color ASIENTO_LIBRE = new Color(189, 189, 189);
    private static final Color ASIENTO_OCUPADO = new Color(30, 136, 229);
    private static final Color BORDE = new Color(66, 66, 66);
    private static final Color TORNIQUETE_LIBRE = new Color(102, 187, 106, 70);
    private static final Color TORNIQUETE_OCUPADO = new Color(255, 112, 67, 120);
    private static final int MEDIO_TORNIQUETE = 38;

    private final PlantaBaja planta;

    public CapaPlantaBaja(PlantaBaja planta) {
        this.planta = planta;
    }

    @Override
    public void dibujar(Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        for (Asientos.Asiento a : planta.getAsientos().todos()) {
            Punto p = a.getPosicion();
            g.setColor(a.estaOcupado() ? ASIENTO_OCUPADO : ASIENTO_LIBRE);
            g.fillRoundRect((int) p.x() - 9, (int) p.y() - 7, 18, 14, 5, 5);
            g.setColor(BORDE);
            g.drawRoundRect((int) p.x() - 9, (int) p.y() - 7, 18, 14, 5, 5);
        }

        Torniquetes t = planta.getTorniquetes();
        for (int i = 0; i < t.getCapacidad(); i++) {
            Punto p = planta.getPuntoTorniquete(i);
            g.setColor(t.estaOcupado(i) ? TORNIQUETE_OCUPADO : TORNIQUETE_LIBRE);
            g.fillRoundRect((int) p.x() - MEDIO_TORNIQUETE, (int) p.y() - MEDIO_TORNIQUETE,
                    MEDIO_TORNIQUETE * 2, MEDIO_TORNIQUETE * 2, 14, 14);
        }
    }

    @Override
    public String resumen() {
        Torniquetes t = planta.getTorniquetes();
        Asientos a = planta.getAsientos();
        return String.format("<html>Fila: <b>%d</b> &nbsp;·&nbsp; En revisión: <b>%d/%d</b> (máx %d)"
                        + " &nbsp;·&nbsp; Aceptados: <b>%d</b> &nbsp;·&nbsp; Rechazados: <b>%d</b>"
                        + "<br>Sentados: <b>%d/%d</b> &nbsp;·&nbsp; Conflictos de asiento: <b>%d</b></html>",
                t.getEnFila(), t.getEnRevision(), t.getCapacidad(), t.getMaximoEnRevision(),
                t.getAceptados(), t.getRechazados(), a.getSentados(), a.getCantidad(), a.getConflictos());
    }
}
