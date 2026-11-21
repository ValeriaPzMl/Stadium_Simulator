package estadio.gui;

import java.awt.Graphics2D;
import java.util.List;

/**
 * Lo que cada piso dibuja encima de la imagen del escenario (asientos ocupados, torniquetes en
 * uso...) y el texto de su barra de contadores. Se dibuja en pixeles de la imagen.
 */
public interface Capa {

    void dibujar(Graphics2D g);

    /** Texto de la barra bajo el plano (acepta HTML de Swing). */
    default String resumen() {
        return " ";
    }

    /** Búferes de este piso con sus contadores en vivo (para el tablero). */
    default List<FilaBufer> buferes() {
        return List.of();
    }
}
