package estadio.red;

import estadio.agentes.Agente;
import estadio.agentes.EstadoAgente;
import estadio.comun.Bitacora;
import estadio.comun.Config;
import estadio.comun.Config.Vecino;
import estadio.comun.Escenario;
import estadio.comun.Piso;
import estadio.comun.Punto;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * El lado cliente de la red como agente. Los aficionados que cambian de piso dejan sus datos en
 * la <b>cola de salida</b> (productor-consumidor: ellos producen, este agente consume) y terminan
 * su hilo; este agente los envía al vecino correcto y, si falla, reintenta hasta lograrlo, así
 * ningún aficionado se pierde aunque el otro servidor tarde en arrancar.
 *
 * <p>También descubre qué vecino es cada piso mandando HOLA a la lista de {@code --vecinos}.
 */
public class ClienteRed extends Agente {

    public enum Estado implements EstadoAgente {
        DESCUBRIENDO_VECINOS(121, 134, 203),
        ESPERANDO_ENVIO(57, 73, 171),
        ENVIANDO(92, 107, 192),
        REINTENTANDO(198, 40, 40),
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

    private static final long MS_DESCUBRIR = 3000;
    private static final long MS_REINTENTO = 1500;

    private record Envio(Piso destino, AgenteDTO agente) {
    }

    private final Config config;
    private final BlockingQueue<Envio> salida = new LinkedBlockingQueue<>();
    /** Qué vecino es cada piso; lo escribe este hilo y lo lee la interfaz. */
    private final Map<Piso, Vecino> conocidos = new ConcurrentHashMap<>();

    /** Encolados y aún sin ACK (en la cola o enviándose): siguen existiendo, "en tránsito". */
    private final AtomicInteger enTransito = new AtomicInteger();
    private final AtomicInteger enviados = new AtomicInteger();
    private final AtomicInteger reintentos = new AtomicInteger();

    public ClienteRed(Config config, Escenario escenario) {
        super(escenario, new Punto(0, 0), Estado.DESCUBRIENDO_VECINOS);
        this.config = config;
    }

    /** Productor: un aficionado deja sus datos para viajar a {@code destino}. No bloquea. */
    public void enviar(Piso destino, AgenteDTO agente) {
        enTransito.incrementAndGet();
        salida.add(new Envio(destino, agente));
    }

    @Override
    protected void vivir() throws InterruptedException {
        descubrir();
        while (true) {
            cambiarEstado(Estado.ESPERANDO_ENVIO);
            Envio envio = salida.poll(MS_DESCUBRIR, TimeUnit.MILLISECONDS); // WAITING con tiempo
            if (envio == null) {
                descubrir(); // sin trabajo: aprovecha para ver qué vecinos están vivos
                continue;
            }
            entregar(envio);
        }
    }

    /** Insiste hasta que el destino confirme con ACK. */
    private void entregar(Envio envio) throws InterruptedException {
        while (true) {
            Vecino vecino = conocidos.get(envio.destino());
            if (vecino == null) {
                descubrir();
                vecino = conocidos.get(envio.destino());
            }
            if (vecino != null) {
                cambiarEstado(Estado.ENVIANDO);
                try {
                    Mensaje respuesta = Protocolo.enviar(vecino, Mensaje.agente(config.getPiso(), envio.agente()));
                    if (respuesta.tipo() == Mensaje.Tipo.ACK) {
                        enviados.incrementAndGet();
                        enTransito.decrementAndGet();
                        Bitacora.info("Aficionado folio " + envio.agente().boleto().folio() + " entregado a "
                                + envio.destino().getNombre());
                        return;
                    }
                    Bitacora.info("El vecino respondió " + respuesta.tipo() + ": " + respuesta.error());
                } catch (IOException e) {
                    Bitacora.info("No se pudo enviar a " + vecino + ": " + e.getMessage());
                    conocidos.remove(envio.destino(), vecino);
                }
            }
            cambiarEstado(Estado.REINTENTANDO);
            reintentos.incrementAndGet();
            Thread.sleep(MS_REINTENTO);
        }
    }

    /** Pregunta a cada vecino configurado qué piso es. */
    private void descubrir() {
        cambiarEstado(Estado.DESCUBRIENDO_VECINOS);
        for (Vecino v : config.getVecinos()) {
            try {
                Mensaje respuesta = Protocolo.enviar(v, Mensaje.hola(config.getPiso()));
                if (respuesta.tipo() == Mensaje.Tipo.ACK && respuesta.origen() != null) {
                    if (conocidos.put(respuesta.origen(), v) == null) {
                        Bitacora.info("Vecino " + v + " es " + respuesta.origen().getNombre());
                    }
                }
            } catch (IOException e) {
                conocidos.values().remove(v);
            }
        }
    }

    public boolean conoceA(Piso piso) {
        return conocidos.containsKey(piso);
    }

    /** Texto corto para la barra: "S3 ✓ · S2 ✗". */
    public String estadoVecinos() {
        StringBuilder sb = new StringBuilder();
        for (Piso p : Piso.values()) {
            if (p != config.getPiso()) {
                sb.append(sb.length() == 0 ? "" : " · ").append('S').append(p.getNumero())
                        .append(conocidos.containsKey(p) ? " conectado" : " sin conexión");
            }
        }
        return sb.toString();
    }

    public int getEnCola() {
        return salida.size();
    }

    public int getEnTransito() {
        return enTransito.get();
    }

    public int getEnviados() {
        return enviados.get();
    }

    public int getReintentos() {
        return reintentos.get();
    }

    @Override
    public boolean esVisible() {
        return false;
    }

    @Override
    public String getTipo() {
        return "ClienteRed";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return Estado.FUERA;
    }
}
