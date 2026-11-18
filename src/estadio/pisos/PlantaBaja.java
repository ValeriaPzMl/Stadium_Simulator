package estadio.pisos;

import estadio.agentes.Aficionado;
import estadio.agentes.Guardia;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Bitacora;
import estadio.comun.Boleto;
import estadio.comun.Config;
import estadio.comun.Mapa;
import estadio.comun.Tiempo;
import estadio.comun.TipoBoleto;
import estadio.recursos.Asientos;
import estadio.recursos.Torniquetes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servidor S1: cancha, gradas bajas y la única entrada del estadio.
 * Crea los recursos del piso, arranca a los guardias y genera la llegada de aficionados.
 *
 * <p>Plano (coordenadas del {@link Mapa}, 800 × 600):
 * cancha arriba, 4 filas de asientos debajo, torniquetes y fila de entrada abajo.
 */
public class PlantaBaja {

    // Plano
    public static final double CANCHA_X = 120, CANCHA_Y = 30, CANCHA_ANCHO = 560, CANCHA_ALTO = 200;
    private static final int FILAS_ASIENTOS = 4, COLUMNAS_ASIENTOS = 20;
    private static final double ASIENTO_X = 115, ASIENTO_Y = 275, ASIENTO_SEP_X = 30, ASIENTO_SEP_Y = 25;
    public static final double TORNIQUETES_Y = 470;
    private static final double TORNIQUETES_SEPARACION = 70;
    public static final double FILA_Y_MIN = 520, FILA_Y_MAX = 565;
    public static final double SALIDA_X = 760, SALIDA_Y = 590;
    public static final double CASETA_X = 60, CASETA_Y = 470;

    private final Config config;
    private final Torniquetes torniquetes;
    private final Asientos asientos;

    public PlantaBaja(Config config) {
        this.config = config;
        this.torniquetes = new Torniquetes(posicionesTorniquetes(config.getTorniquetes()), TORNIQUETES_Y);
        this.asientos = new Asientos(FILAS_ASIENTOS, COLUMNAS_ASIENTOS, ASIENTO_X, ASIENTO_Y,
                ASIENTO_SEP_X, ASIENTO_SEP_Y);
    }

    /** Torniquetes centrados horizontalmente. */
    private static double[] posicionesTorniquetes(int cantidad) {
        double[] xs = new double[cantidad];
        double inicio = Mapa.ANCHO / 2.0 - (cantidad - 1) * TORNIQUETES_SEPARACION / 2;
        for (int i = 0; i < cantidad; i++) {
            xs[i] = inicio + i * TORNIQUETES_SEPARACION;
        }
        return xs;
    }

    /** Arranca a los guardias y al hilo que genera las llegadas. */
    public void iniciar(RegistroAgentes registro) {
        for (int i = 0; i < config.getGuardias(); i++) {
            // Cada guardia tiene su puesto junto a un torniquete (se reparten si hay más guardias)
            int lugar = i % torniquetes.getCapacidad();
            double puestoX = torniquetes.getX(lugar) + 22 + (i / torniquetes.getCapacidad()) * 12;
            registro.registrarYArrancar(new Guardia(torniquetes, puestoX, TORNIQUETES_Y));
        }

        Thread llegadas = new Thread(() -> generarLlegadas(registro), "Llegadas");
        llegadas.setDaemon(true);
        llegadas.start();
    }

    private void generarLlegadas(RegistroAgentes registro) {
        List<Boleto> boletos = emitirBoletos();
        try {
            for (Boleto boleto : boletos) {
                registro.registrarYArrancar(new Aficionado(boleto, this));
                Tiempo.dormir(ThreadLocalRandom.current().nextLong(300, 900));
            }
            Bitacora.info("Llegaron los " + boletos.size() + " aficionados");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Emite un boleto por aficionado. Un porcentaje son falsos y otro porcentaje repiten el
     * asiento de un boleto anterior (duplicados), para provocar conflictos de asiento.
     */
    private List<Boleto> emitirBoletos() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Integer> libres = new ArrayList<>();
        for (int i = 0; i < asientos.getCantidad(); i++) {
            libres.add(i);
        }
        Collections.shuffle(libres);

        List<Boleto> boletos = new ArrayList<>();
        List<Integer> asignados = new ArrayList<>();
        for (int folio = 1; folio <= config.getAficionados(); folio++) {
            int dado = rnd.nextInt(100);
            boolean falso = dado < config.getPorcentajeFalsos();
            boolean duplicado = !falso && dado < config.getPorcentajeFalsos() + config.getPorcentajeDuplicados()
                    && !asignados.isEmpty();
            int asiento;
            if (falso) {
                asiento = rnd.nextInt(asientos.getCantidad());
            } else if (duplicado || libres.isEmpty()) {
                asiento = asignados.get(rnd.nextInt(asignados.size()));
            } else {
                asiento = libres.remove(libres.size() - 1);
                asignados.add(asiento);
            }
            boletos.add(new Boleto(folio, TipoBoleto.GENERAL, asiento, !falso));
        }
        return boletos;
    }

    // ------------------------------------------------------------ para los agentes

    public Torniquetes getTorniquetes() {
        return torniquetes;
    }

    public Asientos getAsientos() {
        return asientos;
    }

    /** Lugar al azar en la zona de fila frente a los torniquetes. */
    public double[] puntoEnFila() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double ancho = Math.max(160, torniquetes.getCapacidad() * TORNIQUETES_SEPARACION);
        return new double[] {
                Mapa.ANCHO / 2.0 + rnd.nextDouble(-ancho / 2, ancho / 2),
                rnd.nextDouble(FILA_Y_MIN, FILA_Y_MAX)
        };
    }
}
