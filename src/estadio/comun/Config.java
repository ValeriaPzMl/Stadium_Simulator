package estadio.comun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuración de un servidor, leída de la línea de comandos. Ejemplo:
 * {@code --piso=1 --puerto=5001 --vecinos=host2:5002,host3:5003 --aficionados=40 --torniquetes=2}
 */
public final class Config {

    /** Dirección de otro servidor del estadio. */
    public record Vecino(String host, int puerto) {
        @Override
        public String toString() {
            return host + ":" + puerto;
        }
    }

    private static final Set<String> OPCIONES = Set.of(
            "piso", "puerto", "vecinos", "aficionados", "guardias", "torniquetes",
            "falsos", "duplicados", "velocidad", "editor");

    /** Opciones que se pueden escribir sin valor: {@code --editor} equivale a {@code --editor=true}. */
    private static final Set<String> BANDERAS = Set.of("editor");

    public static final String USO = "Uso: java -jar estadio.jar --piso=<1|2|3> [--puerto=<n>] "
            + "[--vecinos=host:puerto,...] [--aficionados=40] [--guardias=2] [--torniquetes=3] "
            + "[--falsos=10] [--duplicados=5] [--velocidad=1.0] [--editor]";

    private final Piso piso;
    private final int puerto;
    private final List<Vecino> vecinos;
    private final int aficionados;
    private final int guardias;
    private final int torniquetes;
    private final int porcentajeFalsos;
    private final int porcentajeDuplicados;
    private final double velocidad;
    private final boolean editor;

    private Config(Map<String, String> opciones) {
        piso = Piso.deNumero(entero(opciones, "piso", 1, 1, 3));
        puerto = entero(opciones, "puerto", 5000 + piso.getNumero(), 1, 65535);
        vecinos = Collections.unmodifiableList(leerVecinos(opciones.getOrDefault("vecinos", "")));
        aficionados = entero(opciones, "aficionados", 40, 0, 1000);
        guardias = entero(opciones, "guardias", 2, 0, 50);
        torniquetes = entero(opciones, "torniquetes", 3, 1, 10);
        porcentajeFalsos = entero(opciones, "falsos", 10, 0, 100);
        porcentajeDuplicados = entero(opciones, "duplicados", 5, 0, 100 - porcentajeFalsos);
        velocidad = decimal(opciones, "velocidad", 1.0);
        editor = booleano(opciones, "editor");
    }

    public static Config desdeArgumentos(String[] args) {
        Map<String, String> opciones = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && BANDERAS.contains(arg.substring(2))) {
                opciones.put(arg.substring(2), "true");
                continue;
            }
            int igual = arg.indexOf('=');
            if (!arg.startsWith("--") || igual < 0) {
                throw new IllegalArgumentException("Argumento inválido (formato --opcion=valor): " + arg);
            }
            String nombre = arg.substring(2, igual);
            if (!OPCIONES.contains(nombre)) {
                throw new IllegalArgumentException("Opción desconocida: --" + nombre);
            }
            opciones.put(nombre, arg.substring(igual + 1));
        }
        return new Config(opciones);
    }

    private static int entero(Map<String, String> opciones, String nombre, int porDefecto, int min, int max) {
        String valor = opciones.get(nombre);
        if (valor == null) {
            return porDefecto;
        }
        int n;
        try {
            n = Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un número en --" + nombre + " pero llegó: " + valor);
        }
        if (n < min || n > max) {
            throw new IllegalArgumentException("--" + nombre + " debe estar entre " + min + " y " + max);
        }
        return n;
    }

    private static double decimal(Map<String, String> opciones, String nombre, double porDefecto) {
        String valor = opciones.get(nombre);
        if (valor == null) {
            return porDefecto;
        }
        try {
            double d = Double.parseDouble(valor);
            if (d <= 0) {
                throw new IllegalArgumentException("--" + nombre + " debe ser mayor que 0");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un número en --" + nombre + " pero llegó: " + valor);
        }
    }

    private static boolean booleano(Map<String, String> opciones, String nombre) {
        String valor = opciones.getOrDefault(nombre, "false");
        if (!valor.equals("true") && !valor.equals("false")) {
            throw new IllegalArgumentException("--" + nombre + " debe ser true o false");
        }
        return Boolean.parseBoolean(valor);
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
            try {
                vecinos.add(new Vecino(partes[0], Integer.parseInt(partes[1])));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Puerto inválido en vecino: " + item);
            }
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

    public int getAficionados() {
        return aficionados;
    }

    public int getGuardias() {
        return guardias;
    }

    public int getTorniquetes() {
        return torniquetes;
    }

    public int getPorcentajeFalsos() {
        return porcentajeFalsos;
    }

    public int getPorcentajeDuplicados() {
        return porcentajeDuplicados;
    }

    public double getVelocidad() {
        return velocidad;
    }

    /** Modo editor de escenario: muestra los puntos del .txt y no corre la simulación. */
    public boolean isEditor() {
        return editor;
    }

    @Override
    public String toString() {
        return piso.getNombre() + " | puerto " + puerto + " | vecinos " + vecinos
                + " | aficionados " + aficionados + " | guardias " + guardias + " | torniquetes " + torniquetes
                + " | falsos " + porcentajeFalsos + "% | duplicados " + porcentajeDuplicados + "%"
                + " | velocidad x" + velocidad + (editor ? " | EDITOR" : "");
    }
}
