package estadio.red;

import estadio.agentes.Agente;
import estadio.agentes.EstadoAgente;
import estadio.comun.Bitacora;
import estadio.comun.Escenario;
import estadio.comun.Piso;
import estadio.comun.Punto;

import java.awt.Color;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * El lado servidor de la red como agente (ver {@code entrega1/ANALISIS.md}, sección 5.7): espera
 * conexiones en el puerto del piso, lee un {@link Mensaje} y, si trae un aficionado, le pide al
 * piso que cree su hilo. Contesta ACK solo cuando el agente ya existe aquí, así el origen sabe
 * que puede olvidarlo sin que se pierda.
 */
public class ServidorRed extends Agente {

    public enum Estado implements EstadoAgente {
        ESPERANDO_CONEXION(0, 137, 123),
        ATENDIENDO(77, 182, 172),
        CREANDO_AGENTE(0, 77, 64),
        CERRADO(117, 117, 117);

        private final Color color;

        Estado(int r, int g, int b) {
            this.color = new Color(r, g, b);
        }

        @Override
        public Color color() {
            return color;
        }
    }

    private final ServerSocket socket;
    private final Piso piso;
    private final ReceptorAgentes receptor;
    private final AtomicInteger recibidos = new AtomicInteger();

    /** @param socket ya abierto en el puerto del piso (así un puerto ocupado falla al arrancar) */
    public ServidorRed(ServerSocket socket, Piso piso, ReceptorAgentes receptor, Escenario escenario) {
        super(escenario, new Punto(0, 0), Estado.ESPERANDO_CONEXION);
        this.socket = socket;
        this.piso = piso;
        this.receptor = receptor;
    }

    @Override
    protected void vivir() {
        Bitacora.info("Escuchando en el puerto " + socket.getLocalPort());
        while (!socket.isClosed()) {
            cambiarEstado(Estado.ESPERANDO_CONEXION);
            try (Socket conexion = socket.accept()) { // bloqueado en E/S hasta que alguien se conecte
                cambiarEstado(Estado.ATENDIENDO);
                conexion.setSoTimeout(Protocolo.MS_LECTURA);
                Mensaje mensaje = Protocolo.leer(conexion.getInputStream());
                Protocolo.responder(conexion, atender(mensaje));
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    Bitacora.info("Conexión fallida: " + e.getMessage());
                }
            }
        }
    }

    private Mensaje atender(Mensaje mensaje) {
        return switch (mensaje.tipo()) {
            case HOLA -> Mensaje.ack(piso);
            case AGENTE -> {
                cambiarEstado(Estado.CREANDO_AGENTE);
                try {
                    receptor.recibir(mensaje.agente());
                    recibidos.incrementAndGet();
                    Bitacora.info("Llegó el aficionado folio " + mensaje.agente().boleto().folio()
                            + " desde " + mensaje.origen().getNombre());
                    yield Mensaje.ack(piso);
                } catch (RuntimeException e) {
                    Bitacora.info("No se pudo recibir al aficionado: " + e.getMessage());
                    yield Mensaje.error(piso, e.getMessage());
                }
            }
            default -> Mensaje.error(piso, "Mensaje inesperado: " + mensaje.tipo());
        };
    }

    public int getRecibidos() {
        return recibidos.get();
    }

    @Override
    public boolean esVisible() {
        return false;
    }

    @Override
    public String getTipo() {
        return "ServidorRed";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return Estado.CERRADO;
    }
}
