package estadio.gui;

import estadio.agentes.RegistroAgentes;
import estadio.comun.Config;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/** Ventana principal de un servidor: plano del piso a la izquierda, tabla de estados a la derecha. */
public class VentanaPiso extends JFrame {

    /** ~30 cuadros por segundo para la animación. */
    private static final int MS_ANIMACION = 33;
    /** La tabla no necesita refrescarse tan seguido. */
    private static final int MS_TABLA = 200;

    public VentanaPiso(Config config, RegistroAgentes registro) {
        super("Estadio Paralelo – " + config.getPiso().getNombre());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel titulo = new JLabel(config.getPiso().getNombre()
                + "   ·   puerto " + config.getPuerto() + "   ·   vecinos " + config.getVecinos());
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        PanelPiso panel = new PanelPiso(registro);
        TablaEstados tabla = new TablaEstados(registro);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setPreferredSize(new Dimension(420, 600));

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, scrollTabla);
        division.setResizeWeight(0.7);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(division, BorderLayout.CENTER);
        setContentPane(contenido);

        // Los javax.swing.Timer disparan en el EDT, así que pueden tocar componentes directamente
        new Timer(MS_ANIMACION, e -> panel.repaint()).start();
        new Timer(MS_TABLA, e -> tabla.refrescar()).start();

        pack();
        setLocationRelativeTo(null);
    }
}
