package estadio.gui;

import estadio.comun.Config;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/** Ventana principal de un servidor. Por ahora solo muestra qué piso es. */
public class VentanaPiso extends JFrame {

    public VentanaPiso(Config config) {
        super("Estadio Paralelo – " + config.getPiso().getNombre());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel titulo = new JLabel(config.getPiso().getNombre(), SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 28f));

        JLabel detalle = new JLabel("Puerto " + config.getPuerto() + "   ·   Vecinos " + config.getVecinos(),
                SwingConstants.CENTER);
        detalle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(new Color(46, 125, 50)); // verde cancha
        titulo.setForeground(Color.WHITE);
        detalle.setForeground(Color.WHITE);
        contenido.add(titulo, BorderLayout.CENTER);
        contenido.add(detalle, BorderLayout.SOUTH);

        setContentPane(contenido);
        setPreferredSize(new Dimension(1000, 700));
        pack();
        setLocationRelativeTo(null);
    }
}
