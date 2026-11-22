package estadio;

import estadio.agentes.RegistroAgentes;
import estadio.comun.Bitacora;
import estadio.comun.Config;
import estadio.comun.Escenario;
import estadio.comun.EscenarioException;
import estadio.comun.Piso;
import estadio.comun.Tiempo;
import estadio.gui.Capa;
import estadio.gui.CapaEditor;
import estadio.gui.CapaGradaAlta;
import estadio.gui.CapaPlantaBaja;
import estadio.gui.DialogoConfig;
import estadio.gui.VentanaPiso;
import estadio.pisos.GradaAlta;
import estadio.pisos.PlantaBaja;
import estadio.red.ClienteRed;
import estadio.red.ReceptorAgentes;
import estadio.red.ServidorRed;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;

/**
 * Punto de entrada de cada servidor del estadio.
 * Ejemplo: {@code java -jar estadio.jar --piso=1 --puerto=5001 --vecinos=localhost:5002,localhost:5003}
 * Sin argumentos abre la ventana de configuración; {@code --sin-dialogo} arranca directo.
 * Editor de escenario: {@code java -jar estadio.jar --piso=1 --editor}
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Config config;
        try {
            config = Config.desdeArgumentos(args);
        } catch (IllegalArgumentException e) {
            terminarConError(e.getMessage() + "\n" + Config.USO, false);
            return;
        }

        // Ventana de configuración, prellenada con los argumentos (se salta con --sin-dialogo o --editor)
        boolean interactivo = !config.isSinDialogo() && !config.isEditor();
        if (interactivo) {
            config = pedirConfiguracion(config);
            if (config == null) {
                System.out.println("Configuración cancelada");
                System.exit(0);
                return;
            }
        }

        System.out.println("Iniciando " + config);
        Tiempo.setFactor(config.getVelocidad());
        RegistroAgentes registro = new RegistroAgentes();

        Escenario escenario;
        PlantaBaja plantaBaja = null;
        ClienteRed cliente = null;
        ServidorRed servidor = null;
        Capa capa;
        try {
            escenario = Escenario.cargar(config.getPiso());
            Bitacora.info("Escenario " + escenario);
            if (config.isEditor()) {
                capa = new CapaEditor(escenario);
            } else {
                // Red: el servidor escucha en el puerto del piso; el cliente envía a los vecinos
                ServerSocket socket = abrirPuerto(config.getPuerto());
                cliente = new ClienteRed(config, escenario);
                ReceptorAgentes receptor;
                switch (config.getPiso()) {
                    case PLANTA_BAJA -> {
                        plantaBaja = new PlantaBaja(config, escenario);
                        receptor = plantaBaja;
                        capa = new CapaPlantaBaja(plantaBaja);
                    }
                    case GRADA_ALTA -> {
                        GradaAlta grada = new GradaAlta(config, escenario, registro, cliente);
                        receptor = grada;
                        capa = new CapaGradaAlta(grada);
                    }
                    default -> {
                        receptor = dto -> {
                            throw new IllegalStateException("S2 todavía no recibe aficionados (Fase 6)");
                        };
                        capa = enConstruccion(config.getPiso());
                    }
                }
                servidor = new ServidorRed(socket, config.getPiso(), receptor, escenario);
            }
        } catch (EscenarioException | IllegalArgumentException e) {
            terminarConError(e.getMessage(), interactivo);
            return;
        }

        // Toda la interfaz se crea en el hilo de eventos de Swing (EDT)
        Config configFinal = config;
        Escenario escenarioFinal = escenario;
        SwingUtilities.invokeLater(() -> new VentanaPiso(configFinal, registro, escenarioFinal, capa).setVisible(true));

        if (plantaBaja != null) {
            plantaBaja.iniciar(registro, cliente);
        }
        if (servidor != null) {
            registro.registrarYArrancar(servidor);
            registro.registrarYArrancar(cliente);
        }
    }

    private static ServerSocket abrirPuerto(int puerto) {
        try {
            return new ServerSocket(puerto);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo abrir el puerto " + puerto
                    + " (¿ya hay otro servidor corriendo en él?): " + e.getMessage());
        }
    }

    /** Muestra la ventana de configuración en el EDT y espera a que se cierre. */
    private static Config pedirConfiguracion(Config inicial) {
        Config[] elegida = new Config[1];
        try {
            SwingUtilities.invokeAndWait(() -> elegida[0] = DialogoConfig.mostrar(inicial));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("No se pudo abrir la ventana de configuración", e.getCause());
        }
        return elegida[0];
    }

    /** S2 y S3 se construyen en las fases 4 a 6; por ahora solo muestran su imagen. */
    private static Capa enConstruccion(Piso piso) {
        return new Capa() {
            @Override
            public void dibujar(Graphics2D g) {
            }

            @Override
            public String resumen() {
                return piso.getNombre() + ": en construcción";
            }
        };
    }

    private static void terminarConError(String mensaje, boolean conVentana) {
        System.err.println("Error: " + mensaje);
        if (conVentana) {
            try {
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null, mensaje,
                        "Estadio Paralelo – no se pudo iniciar", JOptionPane.ERROR_MESSAGE));
            } catch (InterruptedException | InvocationTargetException ignorada) {
                // Ya se escribió en stderr
            }
        }
        System.exit(1);
    }
}
