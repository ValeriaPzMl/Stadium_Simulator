package estadio.comun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración de un servidor, leída de la línea de comandos:
 * {@code --piso=1 --puerto=5001 --vecinos=host2:5002,host3:5003}
 */
public final class Config {

    /** Dirección de otro servidor del estadio. */
    public record Vecino(String host, int puerto) {
        @Override
        public String toString() {
            return host + ":" + puerto;
        }
    }

    private final Piso piso;
    private final int puerto;
    private final List<Vecino> vecinos;

    private Config(Piso piso, int puerto, List<Vecino> vecinos) {
        this.piso = piso;
        this.puerto = puerto;
        this.vecinos = Collections.unmodifiableList(vecinos);
    }

    public static Config desdeArgumentos(String[] args) {
        Piso piso = Piso.PLANTA_BAJA;
        Integer puerto = null;
        List<Vecino> vecinos = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--piso=")) {
                piso = Piso.deNumero(entero(arg, "--piso="));
            } else if (arg.startsWith("--puerto=")) {
                puerto = entero(arg, "--puerto=");
            } else if (arg.startsWith("--vecinos=")) {
                vecinos = leerVecinos(arg.substring("--vecinos=".length()));
            } else {
                throw new IllegalArgumentException("Argumento desconocido: " + arg);
            }
        }
        // Por defecto cada piso escucha en 5000 + número de piso
        return new Config(piso, puerto != null ? puerto : 5000 + piso.getNumero(), vecinos);
    }

    private static int entero(String arg, String prefijo) {
        String valor = arg.substring(prefijo.length());
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un número en " + prefijo + " pero llegó: " + valor);
        }
    }

    private static List<Vecino> leerVecinos(String lista) {
        List<Vecino> vecinos = new ArrayList<>();
        for (String item : lista.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            String[] partes = item.trim().split(":");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Vecino inválido (formato host:puerto): " + item);
            }
            vecinos.add(new Vecino(partes[0], entero(partes[1], "")));
        }
        return vecinos;
    }

    public Piso getPiso() {
        return piso;
    }

    public int getPuerto() {
        return puerto;
    }

    public List<Vecino> getVecinos() {
        return vecinos;
    }

    @Override
    public String toString() {
        return piso.getNombre() + " | puerto " + puerto + " | vecinos " + vecinos;
    }
}
