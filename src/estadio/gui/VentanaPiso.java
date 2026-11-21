package estadio.gui;

import estadio.agentes.RegistroAgentes;
import estadio.comun.Config;
import estadio.comun.Escenario;
import estadio.comun.EscenarioException;
import estadio.comun.Punto;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Ventana principal de un servidor: plano del piso con su barra de contadores a la izquierda y
 * tabla de estados a la derecha. Con una {@link CapaEditor} funciona como editor de escenario.
 */
public class VentanaPiso extends JFrame {

    /** ~30 cuadros por segundo para la animación. */
    private static final int MS_ANIMACION = 33;
    /** La tabla y la barra no necesitan refrescarse tan seguido. */
    private static final int MS_TABLA = 200;

    private final Config config;
    private final PanelPiso panel;
    private final JLabel barra;

    public VentanaPiso(Config config, RegistroAgentes registro, Escenario escenario, Capa capa) {
        super("Estadio Paralelo – " + config.getPiso().getNombre()
                + (capa instanceof CapaEditor ? " (editor de escenario)" : ""));
        this.config = config;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel titulo = new JLabel(config.getPiso().getNombre()
                + "   ·   puerto " + config.getPuerto() + "   ·   vecinos " + config.getVecinos());
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        panel = new PanelPiso(registro, escenario, capa);
        barra = new JLabel(capa.resumen());
        barra.setFont(barra.getFont().deriveFont(Font.PLAIN, 13f));
        barra.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel plano = new JPanel(new BorderLayout());
        plano.add(panel, BorderLayout.CENTER);
        plano.add(barra, BorderLayout.SOUTH);

        TablaEstados tabla = new TablaEstados(registro);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(420, 600));

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, plano, scrollTabla);
        division.setResizeWeight(0.75);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(division, BorderLayout.CENTER);
        setContentPane(contenido);

        // Los javax.swing.Timer disparan en el EDT, así que pueden tocar componentes directamente
        new Timer(MS_ANIMACION, e -> panel.repaint()).start();
        if (capa instanceof CapaEditor editor) {
            activarEditor(editor);
        } else {
            new Timer(MS_TABLA, e -> {
                tabla.refrescar();
                barra.setText(capa.resumen());
            }).start();
        }

        pack();
        setLocationRelativeTo(null);
    }

    /** Clic = coordenadas (se copian al portapapeles); R = volver a leer el .txt del escenario. */
    private void activarEditor(CapaEditor editor) {
        String ayuda = "  ·  clic copia \"x y\"  ·  R recarga el archivo";
        barra.setText(editor.resumen() + ayuda);

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Punto p = panel.aImagen(e.getPoint());
                barra.setForeground(Color.BLACK);
                barra.setText(String.format("x = %d   y = %d%s", Math.round(p.x()), Math.round(p.y()), ayuda));
            }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Punto p = panel.aImagen(e.getPoint());
                String texto = Math.round(p.x()) + " " + Math.round(p.y());
                editor.setUltimoClic(p);
                System.out.println(texto);
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
                    barra.setText("Copiado: " + texto + ayuda);
                } catch (IllegalStateException | SecurityException ex) {
                    barra.setText(texto + " (no se pudo copiar al portapapeles)" + ayuda);
                }
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('r'), "recargar");
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('R'), "recargar");
        panel.getActionMap().put("recargar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Escenario nuevo = Escenario.cargar(config.getPiso());
                    editor.setEscenario(nuevo);
                    panel.setEscenario(nuevo);
                    barra.setForeground(new Color(46, 125, 50));
                    barra.setText("Recargado: " + nuevo + ayuda);
                } catch (EscenarioException ex) {
                    System.err.println(ex.getMessage());
                    barra.setForeground(new Color(198, 40, 40));
                    barra.setText("<html>" + escaparHtml(ex.getMessage()).replace("\n", "<br>") + "</html>");
                }
            }
        });
    }

    private static String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
