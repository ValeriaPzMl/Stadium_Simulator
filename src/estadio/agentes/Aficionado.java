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

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aficionado con boleto (ver {@code entrega1/ANALISIS.md}, sección 5.1).
 * Entra por los torniquetes, se sienta y, mientras ve el partido, su hambre y sus ganas de ir al
 * baño suben; en la zona de decisión va al baño o a la tienda y regresa a su asiento.
 */
public class Aficionado extends Agente {

    public enum Estado {
        CAMINANDO_A_ENTRADA, EN_FILA_TORNIQUETE, SIENDO_REVISADO, RECHAZADO,
        BUSCANDO_ASIENTO, ASIENTO_OCUPADO, VIENDO_PARTIDO,
        CAMINANDO_AL_BANO, EN_FILA_BANO, EN_BANO,
        CAMINANDO_A_TIENDA, EN_FILA_CAJA, PAGANDO, ESPERANDO_COMIDA, REGRESANDO_A_ASIENTO, COMIENDO,
        SALIENDO, FUERA
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
        int rondas = rnd.nextInt(4, 9);
        for (int i = 0; i < rondas; i++) {
            cambiarEstado(Estado.VIENDO_PARTIDO);
            Tiempo.dormir(rnd.nextLong(3000, 6000));
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
                Tiempo.dormir(rnd.nextLong(3000, 5000));
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
            Tiempo.dormir(ThreadLocalRandom.current().nextLong(2000, 4000));
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
            Tiempo.dormir(ThreadLocalRandom.current().nextLong(1000, 2000));
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
    public Color getColor() {
        Enum<?> estado = getEstado();
        if (estado == Estado.RECHAZADO || estado == Estado.ASIENTO_OCUPADO) {
            return new Color(229, 57, 53);
        }
        if (estado == Estado.COMIENDO) {
            return new Color(124, 179, 66);
        }
        return new Color(66, 165, 245);
    }

    @Override
    protected Enum<?> estadoFinal() {
        return Estado.FUERA;
    }
}
