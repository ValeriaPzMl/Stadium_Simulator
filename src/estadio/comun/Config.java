package estadio.comun;

import estadio.recursos.Mostrador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuración de un servidor. Se arma desde la línea de comandos ({@link #desdeArgumentos}) o
 * desde la ventana de configuración ({@link #desdeOpciones}); ambas pasan por la misma validación.
 * Ejemplo: {@code --piso=1 --vecinos=host2:5002 --aficionados=40 --banos=2 --revision=800-1600}
 */
public final class Config {

    /** Dirección de otro servidor del estadio. */
    public record Vecino(String host, int puerto) {
        @Override
        public String toString() {
            return host + ":" + puerto;
        }
    }

    /**
     * Un timer configurable: su opción ({@code --clave=min-max}, en ms), el texto para la ventana
     * y su valor por defecto. Si {@code unico}, es un solo valor y no un rango.
     */
    public record Temporizador(String clave, String etiqueta, Rango porDefecto, boolean unico) {
    }

    public static final Temporizador LLEGADA = new Temporizador("llegada",
            "Tiempo entre llegadas de aficionados", new Rango(300, 900), false);
    public static final Temporizador ESPERA_GUARDIA = new Temporizador("espera-guardia",
            "Espera del guardia antes de su zona de decisión", new Rango(5000, 5000), true);
    public static final Temporizador REVISION = new Temporizador("revision",
            "Revisión de un boleto", new Rango(800, 1600), false);
    public static final Temporizador DESCANSO_GUARDIA = new Temporizador("descanso-guardia",
            "Descanso del guardia", new Rango(4000, 8000), false);
    public static final Temporizador RONDA = new Temporizador("ronda",
            "Ronda viendo el partido", new Rango(3000, 6000), false);
    public static final Temporizador BANO = new Temporizador("bano",
            "Uso del cubículo del baño", new Rango(2000, 4000), false);
    public static final Temporizador PAGO = new Temporizador("pago",
            "Pago en la caja", new Rango(1000, 2000), false);
    public static final Temporizador COMER = new Temporizador("comer",
            "Comer en su asiento", new Rango(3000, 5000), false);
    public static final Temporizador PREPARAR = new Temporizador("preparar",
            "Preparar una porción", new Rango(1500, 3000), false);
    public static final Temporizador DESCANSO_COCINERO = new Temporizador("descanso-cocinero",
            "Descanso del cocinero", new Rango(3000, 6000), false);

    /** En el orden en que se muestran en la ventana. */
    public static final List<Temporizador> TEMPORIZADORES = List.of(LLEGADA, ESPERA_GUARDIA, REVISION,
            DESCANSO_GUARDIA, RONDA, BANO, PAGO, COMER, PREPARAR, DESCANSO_COCINERO);

    private static final long MAXIMO_MS = 600_000;

    private static final Set<String> OPCIONES = Stream.concat(
            Stream.of("piso", "puerto", "vecinos", "aficionados", "guardias", "torniquetes", "falsos", "duplicados",
                    "velocidad", "editor", "sin-dialogo", "banos", "cajas", "cocineros", "porciones", "rondas",
                    "prob-descanso-guardia", "prob-descanso-cocinero"),
            TEMPORIZADORES.stream().map(Temporizador::clave)).collect(Collectors.toUnmodifiableSet());

    /** Opciones que se pueden escribir sin valor: {@code --editor} equivale a {@code --editor=true}. */
    private static final Set<String> BANDERAS = Set.of("editor", "sin-dialogo");

    public static final String USO = "Uso: java -jar estadio.jar [--piso=1|2|3] [--puerto=<n>] "
            + "[--vecinos=host:puerto,...] [--aficionados=40] [--guardias=2] [--cocineros=2] [--torniquetes=3] "
            + "[--banos=3] [--cajas=3] [--porciones=8] [--falsos=10] [--duplicados=5] [--rondas=4-9] "
            + "[--prob-descanso-guardia=30] [--prob-descanso-cocinero=10] [--velocidad=1.0] "
            + "[--<timer>=min-max en ms, timers: "
            + TEMPORIZADORES.stream().map(Temporizador::clave).collect(Collectors.joining(", "))
            + "] [--sin-dialogo] [--editor]";

    private final Piso piso;
    private final int puerto;
    private final List<Vecino> vecinos;
    private final int aficionados;
    private final int guardias;
    private final int cocineros;
    private final int torniquetes;
    private final int banos;
    private final int cajas;
    private final int porciones;
    private final int porcentajeFalsos;
    private final int porcentajeDuplicados;
    private final Rango rondas;
    private final int probDescansoGuardia;
    private final int probDescansoCocinero;
    private final Map<String, Rango> tiempos;
    private final double velocidad;
    private final boolean editor;
    private final boolean sinDialogo;

    private Config(Map<String, String> opciones) {
        for (String nombre : opciones.keySet()) {
            if (!OPCIONES.contains(nombre)) {
                throw new IllegalArgumentException("Opción desconocida: --" + nombre);
            }
        }
        piso = Piso.deNumero(entero(opciones, "piso", 1, 1, 3));
        puerto = entero(opciones, "puerto", 5000 + piso.getNumero(), 1, 65535);
        vecinos = Collections.unmodifiableList(leerVecinos(opciones.getOrDefault("vecinos", "")));
        aficionados = entero(opciones, "aficionados", 40, 0, 1000);
        guardias = entero(opciones, "guardias", 2, 0, 50);
        cocineros = entero(opciones, "cocineros", 2, 0, 20);
        torniquetes = entero(opciones, "torniquetes", 3, 1, 10);
        banos = entero(opciones, "banos", 3, 1, 10);
        cajas = entero(opciones, "cajas", 3, 1, 10);
        porciones = entero(opciones, "porciones", 8, 1, Mostrador.MAXIMO);
        porcentajeFalsos = entero(opciones, "falsos", 10, 0, 100);
        porcentajeDuplicados = entero(opciones, "duplicados", 5, 0, 100 - porcentajeFalsos);
        rondas = rango(opciones, "rondas", new Rango(4, 9), 1, 100);
        probDescansoGuardia = entero(opciones, "prob-descanso-guardia", 30, 0, 100);
        probDescansoCocinero = entero(opciones, "prob-descanso-cocinero", 10, 0, 100);
        Map<String, Rango> t = new LinkedHashMap<>();
        for (Temporizador temporizador : TEMPORIZADORES) {
            Rango r = rango(opciones, temporizador.clave(), temporizador.porDefecto(), 0, MAXIMO_MS);
            if (temporizador.unico() && r.min() != r.max()) {
                throw new IllegalArgumentException("--" + temporizador.clave() + " es un solo valor, no un rango");
            }
            t.put(temporizador.clave(), r);
        }
        tiempos = Collections.unmodifiableMap(t);
        velocidad = decimal(opciones, "velocidad", 1.0);
        editor = booleano(opciones, "editor");
        sinDialogo = booleano(opciones, "sin-dialogo");
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
            opciones.put(arg.substring(2, igual), arg.substring(igual + 1));
        }
        return new Config(opciones);
    }

    /** Las mismas opciones que en la línea de comandos, sin los guiones (lo usa la ventana). */
    public static Config desdeOpciones(Map<String, String> opciones) {
        return new Config(new HashMap<>(opciones));
    }

    private static int entero(Map<String, String> opciones, String nombre, int porDefecto, int min, int max) {
        String valor = opciones.get(nombre);
        if (valor == null) {
            return porDefecto;
        }
        int n;
        try {
            n = Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un número en --" + nombre + " pero llegó: " + valor);
        }
        if (n < min || n > max) {
            throw new IllegalArgumentException("--" + nombre + " debe estar entre " + min + " y " + max);
        }
        return n;
    }

    private static Rango rango(Map<String, String> opciones, String nombre, Rango porDefecto, long min, long max) {
        String valor = opciones.get(nombre);
        if (valor == null) {
            return porDefecto;
        }
        String[] partes = valor.strip().split("-");
        Rango r;
        try {
            r = Rango.leer(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + nombre + " debe ser un número o un rango min-max, llegó: "
                    + valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("--" + nombre + ": el mínimo (" + partes[0].strip()
                    + ") es mayor que el máximo (" + partes[partes.length - 1].strip() + ")");
        }
        if (r.min() < min || r.max() > max) {
            throw new IllegalArgumentException("--" + nombre + " debe estar entre " + min + " y " + max);
        }
        return r;
    }

    private static double decimal(Map<String, String> opciones, String nombre, double porDefecto) {
        String valor = opciones.get(nombre);
        if (valor == null) {
            return porDefecto;
        }
        double d;
        try {
            d = Double.parseDouble(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Se esperaba un número en --" + nombre + " pero llegó: " + valor);
        }
        if (d <= 0) {
            throw new IllegalArgumentException("--" + nombre + " debe ser mayor que 0");
        }
        return d;
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
            String[] partes = item.strip().split(":");
            if (partes.length != 2 || partes[0].isBlank()) {
                throw new IllegalArgumentException("Vecino inválido (formato host:puerto): " + item.strip());
            }
            int puerto;
            try {
                puerto = Integer.parseInt(partes[1].strip());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Puerto inválido en vecino: " + item.strip());
            }
            if (puerto < 1 || puerto > 65535) {
                throw new IllegalArgumentException("Puerto fuera de rango en vecino: " + item.strip());
            }
            vecinos.add(new Vecino(partes[0].strip(), puerto));
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

    public int getCocineros() {
        return cocineros;
    }

    public int getTorniquetes() {
        return torniquetes;
    }

    /** Cubículos del baño general (N). */
    public int getBanos() {
        return banos;
    }

    /** Cajas de cobro de la tienda (C). */
    public int getCajas() {
        return cajas;
    }

    /** Capacidad del mostrador (K porciones). */
    public int getPorciones() {
        return porciones;
    }

    public int getPorcentajeFalsos() {
        return porcentajeFalsos;
    }

    public int getPorcentajeDuplicados() {
        return porcentajeDuplicados;
    }

    /** Cuántas rondas ve el partido cada aficionado antes de irse. */
    public Rango getRondas() {
        return rondas;
    }

    public int getProbDescansoGuardia() {
        return probDescansoGuardia;
    }

    public int getProbDescansoCocinero() {
        return probDescansoCocinero;
    }

    /** Duración configurada de un timer, en ms (antes de aplicar la velocidad). */
    public Rango tiempo(Temporizador temporizador) {
        return tiempos.get(temporizador.clave());
    }

    public double getVelocidad() {
        return velocidad;
    }

    /** Modo editor de escenario: muestra los puntos del .txt y no corre la simulación. */
    public boolean isEditor() {
        return editor;
    }

    /** Arrancar directo con estos valores, sin mostrar la ventana de configuración. */
    public boolean isSinDialogo() {
        return sinDialogo;
    }

    @Override
    public String toString() {
        return piso.getNombre() + " | puerto " + puerto + " | vecinos " + vecinos
                + " | aficionados " + aficionados + " | guardias " + guardias + " | cocineros " + cocineros
                + " | torniquetes " + torniquetes + " | baños " + banos + " | cajas " + cajas
                + " | porciones " + porciones + " | falsos " + porcentajeFalsos + "% | duplicados "
                + porcentajeDuplicados + "% | rondas " + rondas + " | tiempos " + tiempos
                + " | velocidad x" + velocidad + (editor ? " | EDITOR" : "");
    }
}
