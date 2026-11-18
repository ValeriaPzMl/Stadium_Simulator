package estadio.gui;

import estadio.comun.Mapa;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Asientos;
import estadio.recursos.Torniquetes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** Dibuja el plano de S1 (cancha, asientos, torniquetes) y sus contadores, en coordenadas del {@link Mapa}. */
public class FondoPlantaBaja implements PanelPiso.Fondo {

    private static final Color PASTO = new Color(67, 160, 71);
    private static final Color LINEAS = new Color(255, 255, 255, 200);
    private static final Color GRADAS = new Color(97, 97, 97);
    private static final Color ASIENTO_LIBRE = new Color(158, 158, 158);
    private static final Color ASIENTO_OCUPADO = new Color(30, 136, 229);
    private static final Color TORNIQUETE_LIBRE = new Color(255, 213, 79);
    private static final Color TORNIQUETE_OCUPADO = new Color(255, 112, 67);

    private final PlantaBaja planta;

    public FondoPlantaBaja(PlantaBaja planta) {
        this.planta = planta;
    }

    @Override
    public void dibujar(Graphics2D g) {
        dibujarCancha(g);
        dibujarAsientos(g);
        dibujarEntrada(g);
        dibujarContadores(g);
    }

    private void dibujarCancha(Graphics2D g) {
        int x = (int) PlantaBaja.CANCHA_X, y = (int) PlantaBaja.CANCHA_Y;
        int w = (int) PlantaBaja.CANCHA_ANCHO, h = (int) PlantaBaja.CANCHA_ALTO;
        g.setColor(PASTO);
        g.fillRect(x, y, w, h);
        g.setColor(LINEAS);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x, y, w, h);
        g.drawLine(x + w / 2, y, x + w / 2, y + h);
        g.drawOval(x + w / 2 - 35, y + h / 2 - 35, 70, 70);
        g.drawRect(x, y + h / 2 - 50, 60, 100);
        g.drawRect(x + w - 60, y + h / 2 - 50, 60, 100);
    }

    private void dibujarAsientos(Graphics2D g) {
        Asientos asientos = planta.getAsientos();
        g.setColor(GRADAS);
        g.fillRect(95, 258, 610, 110);
        for (Asientos.Asiento a : asientos.todos()) {
            g.setColor(a.estaOcupado() ? ASIENTO_OCUPADO : ASIENTO_LIBRE);
            g.fillRect((int) a.getX() - 8, (int) a.getY() - 6, 16, 12);
        }
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        g.drawString("Gradas generales (" + asientos.getCantidad() + " asientos)", 100, 385);
    }

    private void dibujarEntrada(Graphics2D g) {
        Torniquetes t = planta.getTorniquetes();
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(0, (int) PlantaBaja.TORNIQUETES_Y - 8, Mapa.ANCHO, 16);
        for (int i = 0; i < t.getCapacidad(); i++) {
            g.setColor(t.estaOcupado(i) ? TORNIQUETE_OCUPADO : TORNIQUETE_LIBRE);
            g.fillRect((int) t.getX(i) - 12, (int) t.getY() - 6, 24, 12);
        }
        g.setColor(new Color(121, 85, 72));
        g.fillRect((int) PlantaBaja.CASETA_X - 25, (int) PlantaBaja.CASETA_Y - 20, 50, 40);
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
        g.drawString("Caseta", (int) PlantaBaja.CASETA_X - 18, (int) PlantaBaja.CASETA_Y + 4);
        g.drawString("ENTRADA", Mapa.ANCHO / 2 - 25, (int) PlantaBaja.FILA_Y_MAX + 25);
        g.drawString("SALIDA →", (int) PlantaBaja.SALIDA_X - 55, (int) PlantaBaja.SALIDA_Y);
    }

    private void dibujarContadores(Graphics2D g) {
        Torniquetes t = planta.getTorniquetes();
        Asientos a = planta.getAsientos();
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        g.drawString(String.format(
                "Fila: %d   ·   En revisión: %d/%d (máx %d)   ·   Aceptados: %d   ·   Rechazados: %d",
                t.getEnFila(), t.getEnRevision(), t.getCapacidad(), t.getMaximoEnRevision(),
                t.getAceptados(), t.getRechazados()), 10, 420);
        g.drawString(String.format("Sentados: %d   ·   Conflictos de asiento: %d",
                a.getSentados(), a.getConflictos()), 10, 438);
    }
}
