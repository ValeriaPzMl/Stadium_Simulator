package estadio.comun;

/** Error en un archivo de escenario; el mensaje indica el archivo y, si aplica, la línea. */
public class EscenarioException extends RuntimeException {

    public EscenarioException(String mensaje) {
        super(mensaje);
    }

    public EscenarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
