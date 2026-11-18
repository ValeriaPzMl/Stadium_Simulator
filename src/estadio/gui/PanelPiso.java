package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Mapa;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Dibuja el plano del piso y a sus agentes. Todo se dibuja en coordenadas del {@link Mapa}
 * y se escala al tamaño real del panel.
 */
public class PanelPiso extends JPanel {

    /** Lo que se dibuja debajo de los agentes (el plano de cada piso). */
    @FunctionalInterface
    public interface Fondo {
        void dibujar(Graphics2D g);
    }

    private static final int RADIO = 7;
    private static final Color FONDO = new Color(38, 50, 56);

    private final RegistroAgentes registro;
    private final Fondo fondo;

    public PanelPiso(RegistroAgentes registro, Fondo fondo) {
        this.registro = registro;
        this.fondo = fondo;
        setBackground(FONDO);
        setPreferredSize(new Dimension(Mapa.ANCHO, Mapa.ALTO));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double escala = Math.min(getWidth() / (double) Mapa.ANCHO, getHeight() / (double) Mapa.ALTO);
        g2.translate((getWidth() - Mapa.ANCHO * escala) / 2, (getHeight() - Mapa.ALTO * escala) / 2);
        g2.scale(escala, escala);
        g2.clipRect(0, 0, Mapa.ANCHO, Mapa.ALTO);

        fondo.dibujar(g2);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        for (Agente a : registro.todos()) {
            if (a.getState() == Thread.State.TERMINATED) {
                continue;
            }
            int cx = (int) a.getX();
            int cy = (int) a.getY();
            g2.setColor(a.getColor());
            g2.fillOval(cx - RADIO, cy - RADIO, RADIO * 2, RADIO * 2);
            g2.setColor(Color.BLACK);
            g2.drawOval(cx - RADIO, cy - RADIO, RADIO * 2, RADIO * 2);
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(a.getIdAgente()), cx + RADIO + 1, cy + 3);
        }
        g2.dispose();
    }
}
