package estadio.red;

/** El piso que recibe a los aficionados que llegan por la red. */
@FunctionalInterface
public interface ReceptorAgentes {

    /**
     * Crea y arranca el hilo del aficionado en este piso. Si lanza una excepción, el origen
     * recibe un ERROR y lo vuelve a intentar.
     */
    void recibir(AgenteDTO dto);
}
