package estadio.agentes;

import estadio.comun.Bitacora;
import estadio.comun.Boleto;
import estadio.comun.Mapa;
import estadio.comun.Tiempo;
import estadio.pisos.PlantaBaja;
import estadio.recursos.Asientos.Asiento;
import estadio.recursos.Torniquetes;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aficionado con boleto (ver {@code entrega1/ANALISIS.md}, sección 5.1).
 * Fase 2: entra por los torniquetes, se sienta, ve el partido y se va.
 */
public class Aficionado extends Agente {

    public enum Estado {
        CAMINANDO_A_ENTRADA, EN_FILA_TORNIQUETE, SIENDO_REVISADO, RECHAZADO,
        BUSCANDO_ASIENTO, ASIENTO_OCUPADO, VIENDO_PARTIDO, SALIENDO, FUERA
    }

    private final Boleto boleto;
    private final PlantaBaja planta;

    public Aficionado(Boleto boleto, PlantaBaja planta) {
        super(ThreadLocalRandom.current().nextDouble(300, 500), Mapa.ALTO, Estado.CAMINANDO_A_ENTRADA);
        this.boleto = boleto;
        this.planta = planta;
    }

    @Override
    protected void vivir() throws InterruptedException {
        double[] fila = planta.puntoEnFila();
        caminarHacia(fila[0], fila[1]);

        if (!pasarTorniquete()) {
            cambiarEstado(Estado.RECHAZADO);
            Bitacora.info("Rechazado, boleto " + boleto);
            salirDelEstadio();
            return;
        }

        cambiarEstado(Estado.BUSCANDO_ASIENTO);
        Asiento asiento = planta.getAsientos().get(boleto.asiento());
        caminarHacia(asiento.getX(), asiento.getY());
        if (!asiento.ocupar(this)) {
            cambiarEstado(Estado.ASIENTO_OCUPADO);
            Tiempo.dormir(1500);
            salirDelEstadio();
            return;
        }

        try {
            cambiarEstado(Estado.VIENDO_PARTIDO);
            Tiempo.dormir(ThreadLocalRandom.current().nextLong(15_000, 30_000));
        } finally {
            asiento.liberar(this);
        }
        salirDelEstadio();
    }

    /** Fila → torniquete → revisión con el guardia. Devuelve si el boleto fue aceptado. */
    private boolean pasarTorniquete() throws InterruptedException {
        Torniquetes torniquetes = planta.getTorniquetes();
        cambiarEstado(Estado.EN_FILA_TORNIQUETE);
        int lugar = torniquetes.entrar();
        try {
            caminarHacia(torniquetes.getX(lugar), torniquetes.getY() + 15);
            cambiarEstado(Estado.SIENDO_REVISADO);
            boolean aceptado = torniquetes.pedirRevision(boleto, lugar);
            if (aceptado) {
                caminarHacia(torniquetes.getX(lugar), torniquetes.getY() - 25); // cruza el torniquete
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
        caminarHacia(PlantaBaja.SALIDA_X, PlantaBaja.SALIDA_Y);
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
        return new Color(66, 165, 245);
    }

    @Override
    protected Enum<?> estadoFinal() {
        return Estado.FUERA;
    }
}
