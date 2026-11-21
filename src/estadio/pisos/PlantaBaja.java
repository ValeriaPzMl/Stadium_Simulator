package estadio.pisos;

import estadio.agentes.Aficionado;
import estadio.agentes.Cocinero;
import estadio.agentes.Guardia;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Area;
import estadio.comun.Bitacora;
import estadio.comun.Boleto;
import estadio.comun.Config;
import estadio.comun.Escenario;
import estadio.comun.EscenarioException;
import estadio.comun.Punto;
import estadio.comun.Tiempo;
import estadio.comun.TipoBoleto;
import estadio.recursos.Asientos;
import estadio.recursos.BanoGeneral;
import estadio.recursos.Cajas;
import estadio.recursos.Mostrador;
import estadio.recursos.Torniquetes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servidor S1: cancha, gradas bajas, la única entrada del estadio, baño general y tienda.
 * Crea los recursos del piso, arranca a guardias y cocineros y genera la llegada de aficionados.
 *
 * <p>Todas las posiciones salen de {@code resources/escenarios/s1.txt}; si falta alguna, el
 * constructor falla con un mensaje que dice qué línea agregar.
 */
public class PlantaBaja {

    private final Config config;
    private final Escenario escenario;
    private final Torniquetes torniquetes;
    private final Asientos asientos;
    private final BanoGeneral bano;
    private final Cajas cajas;
    private final Mostrador mostrador;

    private final List<Punto> puntoTorniquete;
    private final List<Punto> puntoRevision;
    private final List<Punto> puntoCruce;
    private final List<Punto> puestosGuardia;
    private final Area aparicion;
    private final Area fila;
    private final Punto salida;
    private final Punto caseta;
    private final List<Punto> cubiculos;
    private final Area filaBano;
    private final List<Punto> puntoCaja;
    private final Area filaTienda;
    private final Area recoger;
    private final List<Punto> cocinas;
    private final Punto indicadorPorciones;

    public PlantaBaja(Config config, Escenario escenario) {
        this.config = config;
        this.escenario = escenario;

        List<Punto> torn = escenario.lista("torniquete");
        List<Punto> rev = escenario.lista("revision");
        List<Punto> cruce = escenario.lista("cruce");
        if (rev.size() != torn.size() || cruce.size() != torn.size()) {
            throw new EscenarioException(escenario.getFuente() + ": las listas torniquete, revision y cruce "
                    + "deben tener los mismos puntos (tienen " + torn.size() + ", " + rev.size() + " y "
                    + cruce.size() + ")");
        }
        int t = config.getTorniquetes();
        if (t > torn.size()) {
            throw new IllegalArgumentException("--torniquetes=" + t + " pero " + escenario.getFuente()
                    + " solo define " + torn.size() + " torniquetes");
        }
        this.puntoTorniquete = List.copyOf(torn.subList(0, t));
        this.puntoRevision = List.copyOf(rev.subList(0, t));
        this.puntoCruce = List.copyOf(cruce.subList(0, t));
        this.puestosGuardia = escenario.lista("puesto_guardia");
        this.aparicion = escenario.area("aparicion");
        this.fila = escenario.area("fila");
        this.salida = escenario.punto("salida");
        this.caseta = escenario.punto("caseta");
        if (escenario.getAsientos().isEmpty()) {
            throw new EscenarioException(escenario.getFuente()
                    + " no define asientos (agrega una línea: asientos <x1> <y1> <x2> <y2> <cuántos>)");
        }

        this.cubiculos = primeros(escenario.lista("bano"), config.getBanos(), "banos", "cubículos de baño");
        this.filaBano = escenario.area("fila_bano");
        this.puntoCaja = primeros(escenario.lista("caja"), config.getCajas(), "cajas", "cajas");
        this.filaTienda = escenario.area("fila_tienda");
        this.recoger = escenario.area("recoger");
        this.cocinas = escenario.lista("cocina");
        this.indicadorPorciones = escenario.punto("porciones");

        this.torniquetes = new Torniquetes(t);
        this.asientos = new Asientos(escenario.getAsientos());
        this.bano = new BanoGeneral(cubiculos.size());
        this.cajas = new Cajas(puntoCaja.size());
        this.mostrador = new Mostrador(config.getPorciones());
    }

    /** Los primeros {@code n} puntos de una lista; falla si el escenario define menos. */
    private List<Punto> primeros(List<Punto> lista, int n, String opcion, String que) {
        if (n > lista.size()) {
            throw new IllegalArgumentException("--" + opcion + "=" + n + " pero " + escenario.getFuente()
                    + " solo define " + lista.size() + " " + que);
        }
        return List.copyOf(lista.subList(0, n));
    }

    /** Arranca a los guardias y al hilo que genera las llegadas. */
    public void iniciar(RegistroAgentes registro) {
        for (int i = 0; i < config.getGuardias(); i++) {
            // Si hay más guardias que puestos, los que sobran se forman detrás (hacia arriba)
            Punto puesto = puestosGuardia.get(i % puestosGuardia.size())
                    .desplazado(0, -24.0 * (i / puestosGuardia.size()));
            registro.registrarYArrancar(new Guardia(this, puesto));
        }
        for (int i = 0; i < config.getCocineros(); i++) {
            Punto cocina = cocinas.get(i % cocinas.size()).desplazado(0, -22.0 * (i / cocinas.size()));
            registro.registrarYArrancar(new Cocinero(this, cocina));
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
                Tiempo.dormir(config.tiempo(Config.LLEGADA).alAzar());
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

    // ------------------------------------------------------------ para los agentes y la interfaz

    public Config getConfig() {
        return config;
    }

    public Escenario getEscenario() {
        return escenario;
    }

    public Torniquetes getTorniquetes() {
        return torniquetes;
    }

    public Asientos getAsientos() {
        return asientos;
    }

    /** Donde se para el aficionado mientras revisan su boleto en el torniquete {@code lugar}. */
    public Punto getPuntoTorniquete(int lugar) {
        return puntoTorniquete.get(lugar);
    }

    /** Donde se para el guardia que revisa el torniquete {@code lugar}. */
    public Punto getPuntoRevision(int lugar) {
        return puntoRevision.get(lugar);
    }

    /** A donde llega el aficionado al cruzar el torniquete {@code lugar}. */
    public Punto getPuntoCruce(int lugar) {
        return puntoCruce.get(lugar);
    }

    public Punto puntoDeAparicion() {
        return aparicion.puntoAlAzar();
    }

    public Punto puntoEnFila() {
        return fila.puntoAlAzar();
    }

    public Punto getSalida() {
        return salida;
    }

    public Punto getCaseta() {
        return caseta;
    }

    public BanoGeneral getBano() {
        return bano;
    }

    public Cajas getCajas() {
        return cajas;
    }

    public Mostrador getMostrador() {
        return mostrador;
    }

    public Punto getCubiculo(int lugar) {
        return cubiculos.get(lugar);
    }

    public Punto puntoEnFilaBano() {
        return filaBano.puntoAlAzar();
    }

    public Punto getPuntoCaja(int lugar) {
        return puntoCaja.get(lugar);
    }

    public Punto puntoEnFilaTienda() {
        return filaTienda.puntoAlAzar();
    }

    public Punto puntoParaRecoger() {
        return recoger.puntoAlAzar();
    }

    public Punto getIndicadorPorciones() {
        return indicadorPorciones;
    }
}
