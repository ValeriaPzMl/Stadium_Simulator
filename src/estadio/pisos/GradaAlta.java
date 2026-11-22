package estadio.pisos;

import estadio.agentes.AficionadoGradaAlta;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Area;
import estadio.comun.Config;
import estadio.comun.Escenario;
import estadio.comun.Piso;
import estadio.comun.Punto;
import estadio.red.AgenteDTO;
import estadio.red.ClienteRed;
import estadio.red.ReceptorAgentes;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor S3: grada alta. Aquí no se generan aficionados: llegan de S1 por la red.
 * Fase 4: los recibe, ven el partido en una zona temporal y regresan a S1.
 */
public class GradaAlta implements ReceptorAgentes {

    private final Config config;
    private final Escenario escenario;
    private final Punto escalera;
    private final Area zonaPopular;

    private final RegistroAgentes registro;
    private final ClienteRed cliente;
    private final AtomicInteger llegaron = new AtomicInteger();
    private final AtomicInteger bajaron = new AtomicInteger();

    public GradaAlta(Config config, Escenario escenario, RegistroAgentes registro, ClienteRed cliente) {
        this.config = config;
        this.escenario = escenario;
        this.escalera = escenario.punto("escalera");
        this.zonaPopular = escenario.area("zona_popular");
        this.registro = registro;
        this.cliente = cliente;
    }

    @Override
    public void recibir(AgenteDTO dto) {
        registro.registrarYArrancar(new AficionadoGradaAlta(dto, this));
        llegaron.incrementAndGet();
    }

    /** Un aficionado baja: sus datos van a la cola de salida de la red. */
    public void migrar(AgenteDTO dto, Piso destino) {
        bajaron.incrementAndGet();
        cliente.enviar(destino, dto);
    }

    public Config getConfig() {
        return config;
    }

    public Escenario getEscenario() {
        return escenario;
    }

    public Punto getEscalera() {
        return escalera;
    }

    public Punto puntoEnZonaPopular() {
        return zonaPopular.puntoAlAzar();
    }

    public ClienteRed getCliente() {
        return cliente;
    }

    public RegistroAgentes getRegistro() {
        return registro;
    }

    public int getLlegaron() {
        return llegaron.get();
    }

    public int getBajaron() {
        return bajaron.get();
    }
}
