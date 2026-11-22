package estadio.gui;

import estadio.pisos.GradaAlta;
import estadio.red.ClienteRed;

import java.awt.Graphics2D;
import java.util.List;

/** S3 en la Fase 4: solo contadores de quién llegó y quién regresó (el plano es la imagen). */
public class CapaGradaAlta implements Capa {

    private final GradaAlta grada;

    public CapaGradaAlta(GradaAlta grada) {
        this.grada = grada;
    }

    @Override
    public void dibujar(Graphics2D g) {
        // Nada dinámico todavía: asientos, baño y tienda de S3 llegan en la Fase 5
    }

    @Override
    public String resumen() {
        long aqui = grada.getRegistro().todos().stream()
                .filter(a -> a.getTipo().equals("Aficionado") && a.getState() != Thread.State.TERMINATED)
                .count();
        ClienteRed c = grada.getCliente();
        return String.format("<html>Aficionados en S3: <b>%d</b> &nbsp;·&nbsp; llegaron de S1: <b>%d</b>"
                        + " &nbsp;·&nbsp; bajaron a S1: <b>%d</b>"
                        + "<br>Red · %s &nbsp;·&nbsp; en tránsito: <b>%d</b> &nbsp;·&nbsp; entregados: <b>%d</b>"
                        + " &nbsp;·&nbsp; reintentos: <b>%d</b></html>",
                aqui, grada.getLlegaron(), grada.getBajaron(), c.estadoVecinos(), c.getEnTransito(),
                c.getEnviados(), c.getReintentos());
    }

    @Override
    public List<FilaBufer> buferes() {
        return List.of(CapaPlantaBaja.filaColaSalida(grada.getCliente(), "S3"));
    }
}
