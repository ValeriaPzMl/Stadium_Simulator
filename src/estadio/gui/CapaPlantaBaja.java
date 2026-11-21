package estadio.gui;

import estadio.comun.Punto;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Asientos;
import estadio.recursos.Mostrador;
import estadio.recursos.Torniquetes;
import estadio.recursos.ZonaConCupo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Lo dinámico de S1 encima de {@code s1.png}: asientos libres u ocupados, torniquetes, cubículos y
 * cajas en uso, y las porciones del mostrador.
 * Los contadores van en la barra bajo el plano.
 */
public class CapaPlantaBaja implements Capa {

    private static final Color ASIENTO_LIBRE = new Color(189, 189, 189);
    private static final Color ASIENTO_OCUPADO = new Color(30, 136, 229);
    private static final Color BORDE = new Color(66, 66, 66);
    private static final Color TORNIQUETE_LIBRE = new Color(102, 187, 106, 70);
    private static final Color TORNIQUETE_OCUPADO = new Color(255, 112, 67, 120);
    private static final Color PORCION = new Color(255, 202, 40);
    private static final Color PORCION_VACIA = new Color(0, 0, 0, 60);
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

        ZonaConCupo bano = planta.getBano();
        for (int i = 0; i < bano.getCapacidad(); i++) {
            resaltar(g, planta.getCubiculo(i), bano.estaOcupado(i), 46, 54);
        }
        ZonaConCupo cajas = planta.getCajas();
        for (int i = 0; i < cajas.getCapacidad(); i++) {
            resaltar(g, planta.getPuntoCaja(i).desplazado(0, -60), cajas.estaOcupado(i), 56, 50);
        }
        dibujarPorciones(g);
    }

    private static void resaltar(Graphics2D g, Punto centro, boolean ocupado, int medioAncho, int medioAlto) {
        g.setColor(ocupado ? TORNIQUETE_OCUPADO : TORNIQUETE_LIBRE);
        g.fillRoundRect((int) centro.x() - medioAncho, (int) centro.y() - medioAlto, medioAncho * 2, medioAlto * 2,
                14, 14);
    }

    /** Una casilla por lugar del mostrador, llena si hay porción. */
    private void dibujarPorciones(Graphics2D g) {
        Mostrador m = planta.getMostrador();
        int k = m.getCapacidad();
        int hay = m.getPorciones();
        int lado = Math.max(8, Math.min(22, 400 / k - 4));
        int paso = lado + 4;
        Punto centro = planta.getIndicadorPorciones();
        int x0 = (int) (centro.x() - (k * paso - 4) / 2.0);
        int y0 = (int) centro.y() - lado / 2;
        for (int i = 0; i < k; i++) {
            g.setColor(i < hay ? PORCION : PORCION_VACIA);
            g.fillRoundRect(x0 + i * paso, y0, lado, lado, 6, 6);
            g.setColor(BORDE);
            g.drawRoundRect(x0 + i * paso, y0, lado, lado, 6, 6);
        }
    }

    @Override
    public String resumen() {
        Torniquetes t = planta.getTorniquetes();
        Asientos a = planta.getAsientos();
        ZonaConCupo b = planta.getBano();
        ZonaConCupo c = planta.getCajas();
        Mostrador m = planta.getMostrador();
        return String.format("<html>Entrada · fila: <b>%d</b> &nbsp; en revisión: <b>%d/%d</b> (máx %d)"
                        + " &nbsp; aceptados: <b>%d</b> &nbsp; rechazados: <b>%d</b>"
                        + " &nbsp;·&nbsp; Sentados: <b>%d/%d</b> &nbsp; conflictos: <b>%d</b>"
                        + "<br>Baño · fila: <b>%d</b> &nbsp; adentro: <b>%d/%d</b> (máx %d)"
                        + " &nbsp;·&nbsp; Cajas · fila: <b>%d</b> &nbsp; pagando: <b>%d/%d</b>"
                        + " &nbsp;·&nbsp; Mostrador: <b>%d/%d</b> porciones &nbsp; esperando comida: <b>%d</b>"
                        + " &nbsp; cocineros esperando espacio: <b>%d</b> &nbsp; vendidas: <b>%d</b></html>",
                t.getEnFila(), t.getEnRevision(), t.getCapacidad(), t.getMaximoEnRevision(),
                t.getAceptados(), t.getRechazados(), a.getSentados(), a.getCantidad(), a.getConflictos(),
                b.getEnFila(), b.getEnUso(), b.getCapacidad(), b.getMaximoEnUso(),
                c.getEnFila(), c.getEnUso(), c.getCapacidad(),
                m.getPorciones(), m.getCapacidad(), m.getEsperandoComida(), m.getEsperandoEspacio(), m.getVendidas());
    }
}
