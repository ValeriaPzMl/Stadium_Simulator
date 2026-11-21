package estadio.gui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/** Circulito de color para tablas y leyenda, con borde opcional como el de los agentes. */
public class MuestraColor implements Icon {

    private final Color relleno;
    private final int tamano;
    private final Color borde;
    private final Stroke trazo;

    public MuestraColor(Color relleno, int tamano) {
        this(relleno, tamano, Color.BLACK, null);
    }

    public MuestraColor(Color relleno, int tamano, Color borde, Stroke trazo) {
        this.relleno = relleno;
        this.tamano = tamano;
        this.borde = borde;
        this.trazo = trazo;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int margen = trazo == null ? 1 : 3;
        g2.setColor(relleno);
        g2.fillOval(x + margen, y + margen, tamano - 2 * margen, tamano - 2 * margen);
        g2.setColor(borde);
        if (trazo != null) {
            g2.setStroke(trazo);
        }
        g2.drawOval(x + margen, y + margen, tamano - 2 * margen, tamano - 2 * margen);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return tamano;
    }

    @Override
    public int getIconHeight() {
        return tamano;
    }
}
