package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Mapa;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Dibuja el plano del piso y a sus agentes, escalando el {@link Mapa} al tamaño del panel. */
public class PanelPiso extends JPanel {

    private static final int RADIO = 9;
    private static final Color CANCHA = new Color(46, 125, 50);

    private final RegistroAgentes registro;

    public PanelPiso(RegistroAgentes registro) {
        this.registro = registro;
        setBackground(CANCHA);
        setPreferredSize(new Dimension(Mapa.ANCHO, Mapa.ALTO));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double escala = Math.min(getWidth() / (double) Mapa.ANCHO, getHeight() / (double) Mapa.ALTO);
        double margenX = (getWidth() - Mapa.ANCHO * escala) / 2;
        double margenY = (getHeight() - Mapa.ALTO * escala) / 2;

        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect((int) margenX, (int) margenY, (int) (Mapa.ANCHO * escala), (int) (Mapa.ALTO * escala));

        for (Agente a : registro.todos()) {
            if (a.getState() == Thread.State.TERMINATED) {
                continue;
            }
            int cx = (int) (margenX + a.getX() * escala);
            int cy = (int) (margenY + a.getY() * escala);
            g2.setColor(a.getColor());
            g2.fillOval(cx - RADIO, cy - RADIO, RADIO * 2, RADIO * 2);
            g2.setColor(Color.BLACK);
            g2.drawOval(cx - RADIO, cy - RADIO, RADIO * 2, RADIO * 2);
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(a.getIdAgente()), cx + RADIO + 2, cy + 4);
        }
        g2.dispose();
    }
}
