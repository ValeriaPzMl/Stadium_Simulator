package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Escenario;
import estadio.comun.Punto;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Dibuja la imagen del escenario, la capa del piso y a los agentes. Todo se dibuja en pixeles de
 * la imagen y se escala al tamaño real del panel, conservando la proporción.
 */
public class PanelPiso extends JPanel {

    private static final int RADIO = 10;
    private static final double ESCALA_INICIAL = 0.6;
    private static final Color FONDO = new Color(38, 50, 56);
    private static final Color TEXTO = new Color(33, 33, 33);

    private final RegistroAgentes registro;
    private Escenario escenario;
    private final Capa capa;

    // Solo se usan desde el EDT
    private double escala = 1;
    private double desplazamientoX;
    private double desplazamientoY;
    /** Imagen ya escalada al tamaño del panel: escalarla en cada cuadro sería lento. */
    private BufferedImage imagenEscalada;
    private BufferedImage imagenOriginal;

    public PanelPiso(RegistroAgentes registro, Escenario escenario, Capa capa) {
        this.registro = registro;
        this.escenario = escenario;
        this.capa = capa;
        setBackground(FONDO);
        setPreferredSize(new Dimension((int) (escenario.getAncho() * ESCALA_INICIAL),
                (int) (escenario.getAlto() * ESCALA_INICIAL)));
    }

    /** Cambia el escenario (lo usa el editor al recargar). Llamar desde el EDT. */
    public void setEscenario(Escenario escenario) {
        this.escenario = escenario;
        repaint();
    }

    /** Convierte un punto del panel (p. ej. un clic) a pixeles de la imagen. */
    public Punto aImagen(Point p) {
        return new Punto((p.x - desplazamientoX) / escala, (p.y - desplazamientoY) / escala);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ancho = escenario.getAncho();
        int alto = escenario.getAlto();
        escala = Math.min(getWidth() / (double) ancho, getHeight() / (double) alto);
        desplazamientoX = (getWidth() - ancho * escala) / 2;
        desplazamientoY = (getHeight() - alto * escala) / 2;

        dibujarImagen(g2, ancho, alto);

        g2.translate(desplazamientoX, desplazamientoY);
        g2.scale(escala, escala);
        g2.clipRect(0, 0, ancho, alto);

        capa.dibujar(g2);
        dibujarAgentes(g2);
        g2.dispose();
    }

    private void dibujarImagen(Graphics2D g2, int ancho, int alto) {
        int w = Math.max(1, (int) Math.round(ancho * escala));
        int h = Math.max(1, (int) Math.round(alto * escala));
        BufferedImage imagen = escenario.getImagen();
        if (imagen == null) {
            g2.setColor(new Color(69, 90, 100));
            g2.fillRect((int) desplazamientoX, (int) desplazamientoY, w, h);
            return;
        }
        if (imagenEscalada == null || imagenOriginal != imagen
                || imagenEscalada.getWidth() != w || imagenEscalada.getHeight() != h) {
            imagenEscalada = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gi = imagenEscalada.createGraphics();
            gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            gi.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gi.drawImage(imagen, 0, 0, w, h, null);
            gi.dispose();
            imagenOriginal = imagen;
        }
        g2.drawImage(imagenEscalada, (int) Math.round(desplazamientoX), (int) Math.round(desplazamientoY), null);
    }

    private void dibujarAgentes(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
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
            g2.setColor(TEXTO);
            g2.drawString(String.valueOf(a.getIdAgente()), cx + RADIO + 2, cy + 5);
        }
    }
}
