package estadio.agentes;

import estadio.agentes.Aficionado.Estado;
import estadio.comun.Bitacora;
import estadio.comun.Config;
import estadio.comun.Piso;
import estadio.comun.Tiempo;
import estadio.pisos.GradaAlta;
import estadio.red.AgenteDTO;

/**
 * El mismo aficionado de boleto Popular, ya en S3: llegó por la red y su hilo es nuevo, pero
 * trae los datos de S1 (boleto, hambre, vejiga). Usa los mismos estados que {@link Aficionado}.
 *
 * <p>Fase 4: ve el partido en una zona temporal y regresa a S1 para salir. Asientos, baño y
 * tienda de S3 llegan en la Fase 5.
 */
public class AficionadoGradaAlta extends Agente {

    private final AgenteDTO datos;
    private final GradaAlta grada;
    private boolean migrado;

    public AficionadoGradaAlta(AgenteDTO datos, GradaAlta grada) {
        super(grada.getEscenario(), grada.getEscalera(), Estado.BUSCANDO_ASIENTO);
        this.datos = datos;
        this.grada = grada;
    }

    @Override
    protected void vivir() throws InterruptedException {
        Config config = grada.getConfig();
        caminarA(grada.puntoEnZonaPopular());

        long rondas = config.getRondas().alAzar();
        for (int i = 0; i < rondas; i++) {
            cambiarEstado(Estado.VIENDO_PARTIDO);
            Tiempo.dormir(config.tiempo(Config.RONDA).alAzar());
        }

        // De regreso a S1 por la escalera
        cambiarEstado(Estado.CAMINANDO_A_ESCALERA);
        caminarA(grada.getEscalera());
        cambiarEstado(Estado.EN_ESCALERA);
        Tiempo.dormir(config.tiempo(Config.ESCALERA).alAzar());
        grada.migrar(new AgenteDTO(datos.boleto(), datos.vejiga(), datos.hambre(), Piso.GRADA_ALTA, getIdAgente()),
                Piso.PLANTA_BAJA);
        migrado = true;
        Bitacora.info("Aficionado folio " + datos.boleto().folio() + " baja a S1");
    }

    public AgenteDTO getDatos() {
        return datos;
    }

    @Override
    public String getTipo() {
        return "Aficionado";
    }

    @Override
    protected EstadoAgente estadoFinal() {
        return migrado ? Estado.MIGRADO : Estado.FUERA;
    }
}
