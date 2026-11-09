package estadio;

import estadio.agentes.AgentePrueba;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Config;
import estadio.gui.VentanaPiso;

import javax.swing.SwingUtilities;

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
            System.err.println("Uso: java -jar estadio.jar --piso=<1|2|3> [--puerto=<n>] "
                    + "[--vecinos=host:puerto,...] [--prueba=<agentes>]");
            System.exit(1);
            return;
        }

        System.out.println("Iniciando " + config);
        RegistroAgentes registro = new RegistroAgentes();

        // Toda la interfaz se crea en el hilo de eventos de Swing (EDT)
        SwingUtilities.invokeLater(() -> new VentanaPiso(config, registro).setVisible(true));

        for (int i = 0; i < config.getAgentesPrueba(); i++) {
            registro.registrarYArrancar(new AgentePrueba());
        }
    }
}
