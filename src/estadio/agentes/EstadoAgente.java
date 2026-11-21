package estadio.agentes;

import java.awt.Color;

/**
 * Lo que implementan los enums {@code Estado} de cada agente: cada estado del dominio tiene su
 * color, que se usa para el relleno del círculo, la tabla de estados y la leyenda.
 */
public interface EstadoAgente {

    Color color();

    /** El nombre del estado (lo da el enum). */
    String name();
}
