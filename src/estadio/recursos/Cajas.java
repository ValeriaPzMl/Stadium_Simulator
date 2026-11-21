package estadio.recursos;

/** Cajas de cobro de la tienda (búfer Cj, sección crítica 8): {@code Semaphore(C, true)}. */
public class Cajas extends ZonaConCupo {

    public Cajas(int cajas) {
        super("Cajas", cajas);
    }
}
