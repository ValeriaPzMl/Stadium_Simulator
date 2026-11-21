package estadio.agentes;

import estadio.comun.Bitacora;
import estadio.comun.Boleto;
import estadio.comun.Punto;
import estadio.comun.Tiempo;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Asientos.Asiento;
import estadio.recursos.BanoGeneral;
import estadio.recursos.Cajas;
import estadio.recursos.Torniquetes;

import estadio.comun.Config;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aficionado con boleto (ver {@code entrega1/ANALISIS.md}, sección 5.1).
 * Entra por los torniquetes, se sienta y, mientras ve el partido, su hambre y sus ganas de ir al
 * baño suben; en la zona de decisión va al baño o a la tienda y regresa a su asiento.
 */
public class Aficionado extends Agente {

    /** Estados del dominio; el color agrupa por actividad (entrada, partido, baño, tienda...). */
    public enum Estado implements EstadoAgente {
        CAMINANDO_A_ENTRADA(144, 164, 174),
        EN_FILA_TORNIQUETE(96, 125, 139),
        SIENDO_REVISADO(253, 216, 53),
        RECHAZADO(229, 57, 53),
        BUSCANDO_ASIENTO(129, 212, 250),
        ASIENTO_OCUPADO(183, 28, 28),
        VIENDO_PARTIDO(30, 136, 229),
        CAMINANDO_AL_BANO(206, 147, 216),
        EN_FILA_BANO(171, 71, 188),
        EN_BANO(106, 27, 154),
        CAMINANDO_A_TIENDA(255, 204, 128),
        EN_FILA_CAJA(255, 152, 0),
        PAGANDO(230, 81, 0),
        ESPERANDO_COMIDA(255, 193, 7),
        REGRESANDO_A_ASIENTO(77, 208, 225),
        COMIENDO(124, 179, 66),
        SALIENDO(189, 189, 189),
        FUERA(117, 117, 117);

        private final Color color;

        Estado(int r, int g, int b) {
            this.color = new Color(r, g, b);
        }

        @Override
        public Color color() {
            return color;
        }
    }

    /** Al llegar a este nivel (0–100) va al baño o a comprar comida. */
    private static final int URGENTE = 100;

    private final Boleto boleto;
    private final PlantaBaja planta;
    private int vejiga;
    private int hambre;

    public Aficionado(Boleto boleto, PlantaBaja planta) {
        super(planta.getEscenario(), planta.puntoDeAparicion(), Estado.CAMINANDO_A_ENTRADA);
        this.boleto = boleto;
        this.planta = planta;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        this.vejiga = rnd.nextInt(0, 70);
        this.hambre = rnd.nextInt(0, 70);
    }

    @Override
    protected void vivir() throws InterruptedException {
        caminarHacia(planta.puntoEnFila());

        if (!pasarTorniquete()) {
            cambiarEstado(Estado.RECHAZADO);
            Bitacora.info("Rechazado, boleto " + boleto);
            caminarHacia(planta.puntoEnFila()); // se regresa por donde vino
            salirDelEstadio();
            return;
        }

        cambiarEstado(Estado.BUSCANDO_ASIENTO);
        Asiento asiento = planta.getAsientos().get(boleto.asiento());
        caminarA(asiento.getPosicion());
        if (!asiento.ocupar(this)) {
            cambiarEstado(Estado.ASIENTO_OCUPADO);
            Tiempo.dormir(1500);
            salirDelEstadio();
            return;
        }

        try {
            verPartido(asiento);
        } finally {
            asiento.liberar(this);
        }
        salirDelEstadio();
    }

    /**
     * Ve el partido por rondas. Después de cada ronda está en la zona de decisión: si la vejiga
     * o el hambre llegaron a {@value #URGENTE} va al baño o a la tienda; si no, sigue viendo.
     * Mientras está fuera, el asiento sigue siendo suyo.
     */
    private void verPartido(Asiento asiento) throws InterruptedException {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Config config = planta.getConfig();
        long rondas = config.getRondas().alAzar();
        for (int i = 0; i < rondas; i++) {
            cambiarEstado(Estado.VIENDO_PARTIDO);
            Tiempo.dormir(config.tiempo(Config.RONDA).alAzar());
            vejiga += rnd.nextInt(10, 35);
            hambre += rnd.nextInt(5, 30);

            if (vejiga >= URGENTE) {
                irAlBano();
                vejiga = 0;
                regresarAlAsiento(asiento);
            } else if (hambre >= URGENTE) {
                comprarComida();
                hambre = 0;
                regresarAlAsiento(asiento);
                cambiarEstado(Estado.COMIENDO);
                Tiempo.dormir(config.tiempo(Config.COMER).alAzar());
            }
        }
    }

    /** Fila del baño → cubículo (zona crítica de N lugares) → sale. */
    private void irAlBano() throws InterruptedException {
        BanoGeneral bano = planta.getBano();
        cambiarEstado(Estado.CAMINANDO_AL_BANO);
        Punto fila = planta.puntoEnFilaBano();
        caminarA(fila);
        cambiarEstado(Estado.EN_FILA_BANO);
        int cubiculo = bano.entrar(); // WAITING si los N cubículos están ocupados
        try {
            caminarHacia(planta.getCubiculo(cubiculo));
            cambiarEstado(Estado.EN_BANO);
            Tiempo.dormir(planta.getConfig().tiempo(Config.BANO).alAzar());
        } finally {
            bano.salir(cubiculo);
        }
        caminarHacia(fila);
    }

    /** Paga en una caja (zona crítica de C lugares) y luego toma su porción del mostrador (monitor). */
    private void comprarComida() throws InterruptedException {
        Cajas cajas = planta.getCajas();
        cambiarEstado(Estado.CAMINANDO_A_TIENDA);
        caminarA(planta.puntoEnFilaTienda());
        cambiarEstado(Estado.EN_FILA_CAJA);
        int caja = cajas.entrar();
        try {
            caminarHacia(planta.getPuntoCaja(caja));
            cambiarEstado(Estado.PAGANDO);
            Tiempo.dormir(planta.getConfig().tiempo(Config.PAGO).alAzar());
        } finally {
            cajas.salir(caja);
        }
        caminarHacia(planta.puntoParaRecoger());
        cambiarEstado(Estado.ESPERANDO_COMIDA);
        planta.getMostrador().tomar(); // WAITING si no hay porciones
    }

    private void regresarAlAsiento(Asiento asiento) throws InterruptedException {
        cambiarEstado(Estado.REGRESANDO_A_ASIENTO);
        caminarA(asiento.getPosicion());
    }

    /** Fila → torniquete → revisión con el guardia. Devuelve si el boleto fue aceptado. */
    private boolean pasarTorniquete() throws InterruptedException {
        Torniquetes torniquetes = planta.getTorniquetes();
        cambiarEstado(Estado.EN_FILA_TORNIQUETE);
        int lugar = torniquetes.entrar();
        try {
            caminarHacia(planta.getPuntoTorniquete(lugar));
            cambiarEstado(Estado.SIENDO_REVISADO);
            boolean aceptado = torniquetes.pedirRevision(boleto, lugar);
            if (aceptado) {
                caminarHacia(planta.getPuntoCruce(lugar)); // cruza el torniquete
            }
            return aceptado;
        } finally {
            torniquetes.salir(lugar);
        }
    }

    private void salirDelEstadio() throws InterruptedException {
        if (getEstado() != Estado.RECHAZADO && getEstado() != Estado.ASIENTO_OCUPADO) {
            cambiarEstado(Estado.SALIENDO);
        }
        caminarA(planta.getSalida());
    }

    public Boleto getBoleto() {
        return boleto;
    }

    @Override
    public String getTipo() {
        return "Aficionado";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return Estado.FUERA;
    }
}
