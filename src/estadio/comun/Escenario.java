package estadio.comun;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Escenario de un piso: su imagen PNG y los lugares a donde pueden ir los agentes, leídos de
 * {@code resources/escenarios/sN.txt}. El formato está en {@code PLAN.md}, sección "Fase 2.5".
 *
 * <p>Un escenario no cambia después de cargarse (todos sus campos son finales e inmutables), así
 * que los hilos de los agentes pueden pedir rutas al mismo tiempo sin sincronizar nada.
 */
public final class Escenario {

    public static final int ANCHO_POR_DEFECTO = 1920;
    public static final int ALTO_POR_DEFECTO = 1080;

    /** Si existe esta carpeta se lee de disco: así se edita el .txt sin recompilar. */
    private static final Path CARPETA_DISCO = Path.of("resources", "escenarios");
    /** Si no, se lee de dentro del jar. */
    private static final String CARPETA_JAR = "/escenarios/";

    /** Pasillo entre dos nodos (se puede recorrer en ambos sentidos). */
    public record Conexion(String desde, String hasta) {
    }

    @FunctionalInterface
    private interface CargadorImagen {
        /** @return la imagen, o {@code null} si el archivo no existe */
        BufferedImage cargar(String archivo) throws IOException;
    }

    private final String fuente;
    private final BufferedImage imagen;
    private final int ancho;
    private final int alto;
    private final Map<String, Punto> puntos;
    private final Map<String, List<Punto>> listas;
    private final Map<String, Area> areas;
    private final List<Punto> asientos;
    private final Map<String, Punto> nodos;
    private final List<Conexion> conexiones;

    // Grafo de pasillos en forma de matriz: posicionNodo[i] y peso[i][j] (infinito = sin pasillo)
    private final Punto[] posicionNodo;
    private final double[][] peso;

    private Escenario(Lector l) {
        this.fuente = l.fuente;
        this.imagen = l.imagen;
        this.ancho = l.ancho;
        this.alto = l.alto;
        this.puntos = Collections.unmodifiableMap(l.puntos);
        Map<String, List<Punto>> copiaListas = new LinkedHashMap<>();
        l.listas.forEach((nombre, lista) -> copiaListas.put(nombre, List.copyOf(lista)));
        this.listas = Collections.unmodifiableMap(copiaListas);
        this.areas = Collections.unmodifiableMap(l.areas);
        this.asientos = List.copyOf(l.asientos);
        this.nodos = Collections.unmodifiableMap(l.nodos);
        this.conexiones = List.copyOf(l.conexiones);

        List<String> nombres = new ArrayList<>(nodos.keySet());
        int n = nombres.size();
        posicionNodo = new Punto[n];
        peso = new double[n][n];
        for (int i = 0; i < n; i++) {
            posicionNodo[i] = nodos.get(nombres.get(i));
            Arrays.fill(peso[i], Double.POSITIVE_INFINITY);
        }
        for (Conexion c : conexiones) {
            int a = nombres.indexOf(c.desde());
            int b = nombres.indexOf(c.hasta());
            peso[a][b] = peso[b][a] = posicionNodo[a].distancia(posicionNodo[b]);
        }
    }

    // ------------------------------------------------------------ carga

    /** Carga {@code sN.txt} del piso: de disco si existe {@code resources/escenarios/}, si no del jar. */
    public static Escenario cargar(Piso piso) {
        String archivo = "s" + piso.getNumero() + ".txt";
        Path enDisco = CARPETA_DISCO.resolve(archivo);
        try {
            if (Files.isRegularFile(enDisco)) {
                String texto = Files.readString(enDisco, StandardCharsets.UTF_8);
                return leer(enDisco.toString(), texto, nombre -> {
                    Path ruta = CARPETA_DISCO.resolve(nombre);
                    return Files.isRegularFile(ruta) ? ImageIO.read(ruta.toFile()) : null;
                });
            }
            try (InputStream in = Escenario.class.getResourceAsStream(CARPETA_JAR + archivo)) {
                if (in == null) {
                    throw new EscenarioException("No se encontró el escenario " + archivo
                            + " (ni en " + CARPETA_DISCO + " ni dentro del jar)");
                }
                String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return leer(archivo + " (jar)", texto, nombre -> {
                    try (InputStream img = Escenario.class.getResourceAsStream(CARPETA_JAR + nombre)) {
                        return img == null ? null : ImageIO.read(img);
                    }
                });
            }
        } catch (IOException e) {
            throw new EscenarioException("No se pudo leer el escenario " + archivo + ": " + e.getMessage(), e);
        }
    }

    /** Lee un escenario desde texto, sin imagen (para pruebas). */
    public static Escenario desdeTexto(String fuente, String texto) {
        return leer(fuente, texto, nombre -> null);
    }

    private static Escenario leer(String fuente, String texto, CargadorImagen cargador) {
        Lector lector = new Lector(fuente, cargador);
        lector.leerTodo(texto);
        return new Escenario(lector);
    }

    /** Estado mutable mientras se lee el archivo; al terminar se copia al escenario inmutable. */
    private static final class Lector {
        private record Coordenada(Punto punto, int linea) {
        }

        private final String fuente;
        private final CargadorImagen cargador;
        private BufferedImage imagen;
        private int lineaImagen;
        private int ancho = ANCHO_POR_DEFECTO;
        private int alto = ALTO_POR_DEFECTO;
        private int lineaTamano;
        private final Map<String, Punto> puntos = new LinkedHashMap<>();
        private final Map<String, List<Punto>> listas = new LinkedHashMap<>();
        private final Map<String, Area> areas = new LinkedHashMap<>();
        private final List<Punto> asientos = new ArrayList<>();
        private final Map<String, Punto> nodos = new LinkedHashMap<>();
        private final Map<String, Integer> lineaDeNodo = new LinkedHashMap<>();
        private final List<Conexion> conexiones = new ArrayList<>();
        private final List<Coordenada> coordenadas = new ArrayList<>();

        private int linea;
        private String textoLinea;

        Lector(String fuente, CargadorImagen cargador) {
            this.fuente = fuente;
            this.cargador = cargador;
        }

        void leerTodo(String texto) {
            String[] lineas = texto.split("\\R", -1);
            for (int i = 0; i < lineas.length; i++) {
                linea = i + 1;
                textoLinea = lineas[i];
                String sinComentario = textoLinea;
                int gato = sinComentario.indexOf('#');
                if (gato >= 0) {
                    sinComentario = sinComentario.substring(0, gato);
                }
                sinComentario = sinComentario.strip();
                if (!sinComentario.isEmpty()) {
                    instruccion(sinComentario.split("\\s+"));
                }
            }
            validarAlFinal();
        }

        private void instruccion(String[] p) {
            switch (p[0]) {
                case "imagen" -> {
                    argumentos(p, 1, "imagen <archivo.png>");
                    if (lineaImagen != 0) {
                        throw error("'imagen' ya se definió en la línea " + lineaImagen);
                    }
                    lineaImagen = linea;
                    try {
                        imagen = cargador.cargar(p[1]);
                    } catch (IOException e) {
                        throw error("no se pudo leer la imagen " + p[1] + ": " + e.getMessage());
                    }
                    if (imagen == null) {
                        throw error("no se encontró la imagen " + p[1]);
                    }
                }
                case "tamano" -> {
                    argumentos(p, 2, "tamano <ancho> <alto>");
                    lineaTamano = linea;
                    ancho = (int) positivo(p[1]);
                    alto = (int) positivo(p[2]);
                }
                case "punto" -> {
                    argumentos(p, 3, "punto <nombre> <x> <y>");
                    if (puntos.containsKey(p[1])) {
                        throw error("el punto '" + p[1] + "' ya existe");
                    }
                    puntos.put(p[1], coordenada(p[2], p[3]));
                }
                case "lista" -> {
                    argumentos(p, 3, "lista <nombre> <x> <y>");
                    listas.computeIfAbsent(p[1], k -> new ArrayList<>()).add(coordenada(p[2], p[3]));
                }
                case "asientos" -> {
                    argumentos(p, 5, "asientos <x1> <y1> <x2> <y2> <cuántos>");
                    Punto desde = coordenada(p[1], p[2]);
                    Punto hasta = coordenada(p[3], p[4]);
                    int cuantos = (int) positivo(p[5]);
                    for (int i = 0; i < cuantos; i++) {
                        double t = cuantos == 1 ? 0 : i / (double) (cuantos - 1);
                        asientos.add(new Punto(desde.x() + (hasta.x() - desde.x()) * t,
                                desde.y() + (hasta.y() - desde.y()) * t));
                    }
                }
                case "area" -> {
                    argumentos(p, 5, "area <nombre> <x1> <y1> <x2> <y2>");
                    if (areas.containsKey(p[1])) {
                        throw error("el área '" + p[1] + "' ya existe");
                    }
                    Punto a = coordenada(p[2], p[3]);
                    Punto b = coordenada(p[4], p[5]);
                    areas.put(p[1], new Area(a.x(), a.y(), b.x(), b.y()));
                }
                case "nodo" -> {
                    argumentos(p, 3, "nodo <nombre> <x> <y>");
                    if (nodos.containsKey(p[1])) {
                        throw error("el nodo '" + p[1] + "' ya existe");
                    }
                    nodos.put(p[1], coordenada(p[2], p[3]));
                    lineaDeNodo.put(p[1], linea);
                }
                case "conexion" -> {
                    if (p.length < 3) {
                        throw error("se esperaba: conexion <nodo1> <nodo2> [<nodo3> ...]");
                    }
                    for (int i = 1; i < p.length; i++) {
                        if (!nodos.containsKey(p[i])) {
                            throw error("el nodo '" + p[i] + "' no existe (defínelo antes con: nodo "
                                    + p[i] + " <x> <y>)");
                        }
                    }
                    for (int i = 1; i + 1 < p.length; i++) {
                        if (p[i].equals(p[i + 1])) {
                            throw error("un nodo no se puede conectar consigo mismo: " + p[i]);
                        }
                        conexiones.add(new Conexion(p[i], p[i + 1]));
                    }
                }
                default -> throw error("instrucción desconocida '" + p[0]
                        + "' (válidas: imagen, tamano, punto, lista, asientos, area, nodo, conexion)");
            }
        }

        private void validarAlFinal() {
            if (imagen != null) {
                if (lineaTamano != 0 && (ancho != imagen.getWidth() || alto != imagen.getHeight())) {
                    linea = lineaTamano;
                    textoLinea = null;
                    throw error("'tamano " + ancho + " " + alto + "' no coincide con la imagen ("
                            + imagen.getWidth() + "×" + imagen.getHeight() + ")");
                }
                ancho = imagen.getWidth();
                alto = imagen.getHeight();
            }
            for (Coordenada c : coordenadas) {
                Punto pt = c.punto();
                if (pt.x() < 0 || pt.y() < 0 || pt.x() > ancho || pt.y() > alto) {
                    linea = c.linea();
                    textoLinea = null;
                    throw error("el punto " + pt + " queda fuera de la imagen (" + ancho + "×" + alto + ")");
                }
            }
            validarGrafoConexo();
        }

        /** Todos los nodos deben poder alcanzarse entre sí; si no, alguna ruta sería imposible. */
        private void validarGrafoConexo() {
            if (nodos.isEmpty()) {
                return;
            }
            Map<String, List<String>> vecinos = new LinkedHashMap<>();
            nodos.keySet().forEach(n -> vecinos.put(n, new ArrayList<>()));
            for (Conexion c : conexiones) {
                vecinos.get(c.desde()).add(c.hasta());
                vecinos.get(c.hasta()).add(c.desde());
            }
            String primero = nodos.keySet().iterator().next();
            List<String> alcanzados = new ArrayList<>(List.of(primero));
            Deque<String> pendientes = new ArrayDeque<>(alcanzados);
            while (!pendientes.isEmpty()) {
                for (String v : vecinos.get(pendientes.pop())) {
                    if (!alcanzados.contains(v)) {
                        alcanzados.add(v);
                        pendientes.push(v);
                    }
                }
            }
            for (String nodo : nodos.keySet()) {
                if (!alcanzados.contains(nodo)) {
                    linea = lineaDeNodo.get(nodo);
                    textoLinea = null;
                    throw error("el nodo '" + nodo + "' no está conectado con '" + primero
                            + "' (falta una línea 'conexion')");
                }
            }
        }

        private void argumentos(String[] p, int esperados, String forma) {
            if (p.length - 1 != esperados) {
                throw error("se esperaba: " + forma);
            }
        }

        private Punto coordenada(String x, String y) {
            Punto pt = new Punto(numero(x), numero(y));
            coordenadas.add(new Coordenada(pt, linea));
            return pt;
        }

        private double numero(String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw error("'" + s + "' no es un número");
            }
        }

        private double positivo(String s) {
            double d = numero(s);
            if (d < 1 || d != Math.floor(d)) {
                throw error("'" + s + "' debe ser un entero mayor que 0");
            }
            return d;
        }

        private EscenarioException error(String mensaje) {
            String detalle = textoLinea == null ? "" : "\n    > " + textoLinea.strip();
            return new EscenarioException(fuente + ", línea " + linea + ": " + mensaje + detalle);
        }
    }

    // ------------------------------------------------------------ rutas

    /**
     * Ruta por los pasillos: del nodo más cercano a {@code desde}, por el camino más corto
     * (Dijkstra), hasta el nodo más cercano a {@code hasta}, y luego al destino.
     * Sin nodos, la ruta es directa. El primer punto es a donde caminar primero.
     */
    public List<Punto> ruta(Punto desde, Punto hasta) {
        List<Punto> ruta = new ArrayList<>();
        if (posicionNodo.length > 0) {
            int origen = nodoMasCercano(desde);
            int destino = nodoMasCercano(hasta);
            for (int i : caminoMasCorto(origen, destino)) {
                ruta.add(posicionNodo[i]);
            }
        }
        ruta.add(hasta);
        return ruta;
    }

    private int nodoMasCercano(Punto p) {
        int mejor = 0;
        for (int i = 1; i < posicionNodo.length; i++) {
            if (p.distancia(posicionNodo[i]) < p.distancia(posicionNodo[mejor])) {
                mejor = i;
            }
        }
        return mejor;
    }

    /** Dijkstra sobre la matriz de pesos; O(n²), suficiente para unas decenas de nodos. */
    private List<Integer> caminoMasCorto(int origen, int destino) {
        int n = posicionNodo.length;
        double[] distancia = new double[n];
        int[] anterior = new int[n];
        boolean[] listo = new boolean[n];
        Arrays.fill(distancia, Double.POSITIVE_INFINITY);
        Arrays.fill(anterior, -1);
        distancia[origen] = 0;
        for (int vuelta = 0; vuelta < n; vuelta++) {
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!listo[i] && (u < 0 || distancia[i] < distancia[u])) {
                    u = i;
                }
            }
            if (u < 0 || distancia[u] == Double.POSITIVE_INFINITY || u == destino) {
                break;
            }
            listo[u] = true;
            for (int v = 0; v < n; v++) {
                double nueva = distancia[u] + peso[u][v];
                if (nueva < distancia[v]) {
                    distancia[v] = nueva;
                    anterior[v] = u;
                }
            }
        }
        List<Integer> camino = new ArrayList<>();
        for (int v = destino; v != -1; v = anterior[v]) {
            camino.add(0, v);
        }
        return camino;
    }

    // ------------------------------------------------------------ consultas

    public Punto punto(String nombre) {
        Punto p = puntos.get(nombre);
        if (p == null) {
            throw faltante("el punto", nombre, "punto " + nombre + " <x> <y>");
        }
        return p;
    }

    public List<Punto> lista(String nombre) {
        List<Punto> l = listas.get(nombre);
        if (l == null) {
            throw faltante("la lista", nombre, "lista " + nombre + " <x> <y>");
        }
        return l;
    }

    public Area area(String nombre) {
        Area a = areas.get(nombre);
        if (a == null) {
            throw faltante("el área", nombre, "area " + nombre + " <x1> <y1> <x2> <y2>");
        }
        return a;
    }

    private EscenarioException faltante(String que, String nombre, String ejemplo) {
        return new EscenarioException(fuente + " no define " + que + " '" + nombre
                + "' (agrega una línea: " + ejemplo + ")");
    }

    public String getFuente() {
        return fuente;
    }

    /** @return la imagen del piso, o {@code null} si el archivo no define una */
    public BufferedImage getImagen() {
        return imagen;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public Map<String, Punto> getPuntos() {
        return puntos;
    }

    public Map<String, List<Punto>> getListas() {
        return listas;
    }

    public Map<String, Area> getAreas() {
        return areas;
    }

    public List<Punto> getAsientos() {
        return asientos;
    }

    public Map<String, Punto> getNodos() {
        return nodos;
    }

    public List<Conexion> getConexiones() {
        return conexiones;
    }

    @Override
    public String toString() {
        return fuente + " (" + ancho + "×" + alto + ", " + puntos.size() + " puntos, " + listas.size()
                + " listas, " + areas.size() + " áreas, " + asientos.size() + " asientos, "
                + nodos.size() + " nodos, " + conexiones.size() + " pasillos)";
    }
}
