package estadio.gui;

import estadio.agentes.Aficionado;
import estadio.agentes.Cocinero;
import estadio.agentes.EstadoAgente;
import estadio.agentes.Guardia;
import estadio.red.ClienteRed;
import estadio.red.ServidorRed;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;

/**
 * Leyenda de colores: el relleno del círculo es el estado del agente y el borde es el estado
 * del hilo. Se genera recorriendo los enums, así que un estado nuevo aparece solo.
 */
public class PanelLeyenda extends JPanel {

    public PanelLeyenda() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setBackground(Color.WHITE);

        titulo("Borde del círculo = estado del hilo");
        renglon(new MuestraColor(Color.WHITE, 22, ColoresHilo.BORDE_NORMAL, ColoresHilo.trazo(Thread.State.RUNNABLE)),
                "RUNNABLE / TIMED_WAITING (corriendo, caminando o dormido un tiempo)");
        renglon(new MuestraColor(Color.WHITE, 22, ColoresHilo.BORDE_WAITING, ColoresHilo.trazo(Thread.State.WAITING)),
                "WAITING: esperando un recurso (semáforo, wait() del monitor)");
        renglon(new MuestraColor(Color.WHITE, 22, ColoresHilo.BORDE_BLOCKED, ColoresHilo.trazo(Thread.State.BLOCKED)),
                "BLOCKED: esperando entrar a un synchronized");

        seccion("Aficionado", Aficionado.Estado.values());
        seccion("Guardia", Guardia.Estado.values());
        seccion("Cocinero", Cocinero.Estado.values());
        seccion("ServidorRed", ServidorRed.Estado.values());
        seccion("ClienteRed", ClienteRed.Estado.values());
    }

    private void seccion(String tipo, EstadoAgente[] estados) {
        add(Box.createVerticalStrut(10));
        titulo("Relleno = estado del " + tipo.toLowerCase());
        for (EstadoAgente e : estados) {
            renglon(new MuestraColor(e.color(), 18), e.name());
        }
    }

    private void titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(l);
    }

    private void renglon(MuestraColor muestra, String texto) {
        JLabel l = new JLabel(texto, muestra, JLabel.LEFT);
        l.setIconTextGap(8);
        add(l);
    }
}
