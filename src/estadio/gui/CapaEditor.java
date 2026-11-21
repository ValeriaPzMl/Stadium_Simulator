package estadio.gui;

import estadio.comun.Area;
import estadio.comun.Escenario;
import estadio.comun.Punto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;

/**
 * Modo editor ({@code --editor}): dibuja todo lo que define el archivo de escenario encima de la
 * imagen, para comprobar que cada coordenada cae donde debe.
 */
public class CapaEditor implements Capa {

    private static final Color PASILLO = new Color(255, 214, 0);
    private static final Color PUNTO = new Color(213, 0, 249);
    private static final Color LISTA = new Color(255, 109, 0);
    private static final Color AREA = new Color(0, 184, 212);
    private static final Color ASIENTO = new Color(30, 136, 229);
    private static final Color CLIC = new Color(244, 67, 54);

    // Los escribe el EDT y los lee el EDT: no necesitan sincronización
    private Escenario escenario;
    private Punto ultimoClic;

    public CapaEditor(Escenario escenario) {
        this.escenario = escenario;
    }

    public void setEscenario(Escenario escenario) {
        this.escenario = escenario;
    }

    public void setUltimoClic(Punto p) {
        this.ultimoClic = p;
    }

    @Override
    public void dibujar(Graphics2D g) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {8f, 6f}, 0f));
        for (Map.Entry<String, Area> e : escenario.getAreas().entrySet()) {
            Area a = e.getValue();
            g.setColor(new Color(AREA.getRed(), AREA.getGreen(), AREA.getBlue(), 50));
            g.fillRect((int) a.x1(), (int) a.y1(), (int) Math.max(1, a.ancho()), (int) Math.max(1, a.alto()));
            g.setColor(AREA);
            g.drawRect((int) a.x1(), (int) a.y1(), (int) Math.max(1, a.ancho()), (int) Math.max(1, a.alto()));
            etiqueta(g, e.getKey(), a.x1(), a.y1() - 4, AREA);
        }

        g.setStroke(new BasicStroke(3f));
        g.setColor(PASILLO);
        for (Escenario.Conexion c : escenario.getConexiones()) {
            Punto a = escenario.getNodos().get(c.desde());
            Punto b = escenario.getNodos().get(c.hasta());
            g.drawLine((int) a.x(), (int) a.y(), (int) b.x(), (int) b.y());
        }
        for (Map.Entry<String, Punto> e : escenario.getNodos().entrySet()) {
            marca(g, e.getValue(), 7, PASILLO);
            etiqueta(g, e.getKey(), e.getValue().x() + 9, e.getValue().y() - 6, PASILLO);
        }

        g.setStroke(new BasicStroke(1.5f));
        List<Punto> asientos = escenario.getAsientos();
        for (int i = 0; i < asientos.size(); i++) {
            Punto p = asientos.get(i);
            g.setColor(ASIENTO);
            g.drawRect((int) p.x() - 9, (int) p.y() - 7, 18, 14);
        }

        for (Map.Entry<String, Punto> e : escenario.getPuntos().entrySet()) {
            marca(g, e.getValue(), 9, PUNTO);
            etiqueta(g, e.getKey(), e.getValue().x() + 11, e.getValue().y() + 5, PUNTO);
        }
        for (Map.Entry<String, List<Punto>> e : escenario.getListas().entrySet()) {
            List<Punto> lista = e.getValue();
            for (int i = 0; i < lista.size(); i++) {
                marca(g, lista.get(i), 6, LISTA);
                etiqueta(g, e.getKey() + "[" + i + "]", lista.get(i).x() + 8, lista.get(i).y() + 18, LISTA);
            }
        }

        if (ultimoClic != null) {
            g.setColor(CLIC);
            g.setStroke(new BasicStroke(2f));
            int x = (int) ultimoClic.x(), y = (int) ultimoClic.y();
            g.drawLine(x - 14, y, x + 14, y);
            g.drawLine(x, y - 14, x, y + 14);
            etiqueta(g, Math.round(ultimoClic.x()) + " " + Math.round(ultimoClic.y()), x + 10, y - 10, CLIC);
        }
    }

    private static void marca(Graphics2D g, Punto p, int radio, Color color) {
        g.setColor(color);
        g.fillOval((int) p.x() - radio, (int) p.y() - radio, radio * 2, radio * 2);
        g.setColor(Color.BLACK);
        g.drawOval((int) p.x() - radio, (int) p.y() - radio, radio * 2, radio * 2);
    }

    /** Texto con contorno oscuro para que se lea sobre cualquier fondo. */
    private static void etiqueta(Graphics2D g, String texto, double x, double y, Color color) {
        int ix = (int) x, iy = (int) y;
        g.setColor(new Color(0, 0, 0, 170));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                g.drawString(texto, ix + dx, iy + dy);
            }
        }
        g.setColor(color);
        g.drawString(texto, ix, iy);
    }

    @Override
    public String resumen() {
        return escenario.toString();
    }
}
