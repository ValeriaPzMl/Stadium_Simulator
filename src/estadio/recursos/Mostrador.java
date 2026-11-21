package estadio.recursos;

import estadio.comun.Bitacora;

/**
 * Mostrador de comida con capacidad para K porciones (búfer M, sección crítica 7).
 * Es un <b>monitor</b> clásico de productor-consumidor:
 * <ul>
 *   <li>los cocineros (productores) llaman a {@link #poner()}; si está lleno hacen {@code wait()};</li>
 *   <li>los aficionados (consumidores) llaman a {@link #tomar()}; si está vacío hacen {@code wait()};</li>
 *   <li>cada cambio hace {@code notifyAll()} para despertar al otro lado.</li>
 * </ul>
 * La condición se revisa en un {@code while} y no en un {@code if}: al despertar, otro hilo pudo
 * haber tomado la porción primero (y también existen los despertares espurios).
 */
public class Mostrador {

    /** Máximo de K que se puede configurar ("m de n" en la ventana). */
    public static final int MAXIMO = 20;

    private final int capacidad;

    // Todo lo siguiente está protegido por el candado del objeto (synchronized)
    private int porciones;
    private int esperandoComida;
    private int esperandoEspacio;
    private int vendidas;

    public Mostrador(int capacidad) {
        this.capacidad = capacidad;
    }

    /** Productor: coloca una porción, esperando si el mostrador está lleno. */
    public synchronized void poner() throws InterruptedException {
        esperandoEspacio++;
        try {
            while (porciones == capacidad) {
                wait(); // libera el candado; el hilo queda en WAITING
            }
        } finally {
            esperandoEspacio--;
        }
        porciones++;
        verificar();
        notifyAll();
    }

    /** Productor sin esperar: coloca una porción solo si hay espacio. */
    public synchronized boolean intentarPoner() {
        if (porciones == capacidad) {
            return false;
        }
        porciones++;
        verificar();
        notifyAll();
        return true;
    }

    /** Consumidor: toma una porción, esperando si el mostrador está vacío. */
    public synchronized void tomar() throws InterruptedException {
        esperandoComida++;
        try {
            while (porciones == 0) {
                wait();
            }
        } finally {
            esperandoComida--;
        }
        porciones--;
        vendidas++;
        verificar();
        notifyAll();
    }

    /** Se llama con el candado tomado. */
    private void verificar() {
        if (porciones < 0 || porciones > capacidad) {
            Bitacora.invarianteRoto("Mostrador con " + porciones + " porciones (capacidad " + capacidad + ")");
        }
    }

    public int getCapacidad() {
        return capacidad;
    }

    public synchronized int getPorciones() {
        return porciones;
    }

    public synchronized int getEsperandoComida() {
        return esperandoComida;
    }

    public synchronized int getEsperandoEspacio() {
        return esperandoEspacio;
    }

    public synchronized int getVendidas() {
        return vendidas;
    }
}
