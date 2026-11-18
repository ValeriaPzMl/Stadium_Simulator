package estadio;

import estadio.agentes.RegistroAgentes;
import estadio.comun.Config;
import estadio.comun.Piso;
import estadio.comun.Tiempo;
import estadio.gui.FondoPlantaBaja;
import estadio.gui.PanelPiso;
import estadio.gui.VentanaPiso;
import estadio.pisos.PlantaBaja;

import javax.swing.SwingUtilities;
import java.awt.Color;

/**
 * Punto de entrada de cada servidor del estadio.
 * Ejemplo: {@code java -jar estadio.jar --piso=1 --puerto=5001 --vecinos=localhost:5002,localhost:5003}
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Config config;
        try {
            config = Config.desdeArgumentos(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println(Config.USO);
            System.exit(1);
            return;
        }

        System.out.println("Iniciando " + config);
        Tiempo.setFactor(config.getVelocidad());
        RegistroAgentes registro = new RegistroAgentes();

        PanelPiso.Fondo fondo;
        PlantaBaja plantaBaja = null;
        if (config.getPiso() == Piso.PLANTA_BAJA) {
            plantaBaja = new PlantaBaja(config);
            fondo = new FondoPlantaBaja(plantaBaja);
        } else {
            // S2 y S3 se construyen en las fases 4 a 6
            fondo = g -> {
                g.setColor(Color.WHITE);
                g.drawString(config.getPiso().getNombre() + ": en construcción", 20, 30);
            };
        }

        // Toda la interfaz se crea en el hilo de eventos de Swing (EDT)
        SwingUtilities.invokeLater(() -> new VentanaPiso(config, registro, fondo).setVisible(true));

        if (plantaBaja != null) {
            plantaBaja.iniciar(registro);
        }
    }
}
