package estadio.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.EnumMap;
import java.util.Map;

/**
 * Colores de los estados del hilo ({@link Thread.State}), compartidos por la tabla, el resumen y el
 * borde de los círculos: así un mismo estado se ve igual en toda la ventana.
 */
public final class ColoresHilo {

    private static final Map<Thread.State, Color> FONDO = new EnumMap<>(Thread.State.class);
    static {
        FONDO.put(Thread.State.NEW, new Color(224, 224, 224));
        FONDO.put(Thread.State.RUNNABLE, new Color(200, 230, 201));
        FONDO.put(Thread.State.BLOCKED, new Color(248, 187, 208));
        FONDO.put(Thread.State.WAITING, new Color(255, 205, 210));
        FONDO.put(Thread.State.TIMED_WAITING, new Color(187, 222, 251));
        FONDO.put(Thread.State.TERMINATED, new Color(189, 189, 189));
    }

    /** Borde de los círculos: solo resaltan los hilos detenidos esperando un recurso. */
    public static final Color BORDE_WAITING = new Color(211, 47, 47);
    public static final Color BORDE_BLOCKED = new Color(216, 27, 96);
    public static final Color BORDE_NORMAL = Color.BLACK;

    private static final Stroke GRUESO = new BasicStroke(4f);
    private static final Stroke DELGADO = new BasicStroke(1.2f);

    private ColoresHilo() {
    }

    /** Color claro para celdas de tabla. */
    public static Color fondo(Thread.State estado) {
        return FONDO.get(estado);
    }

    public static Color borde(Thread.State estado) {
        return switch (estado) {
            case WAITING -> BORDE_WAITING;
            case BLOCKED -> BORDE_BLOCKED;
            default -> BORDE_NORMAL;
        };
    }

    public static Stroke trazo(Thread.State estado) {
        return estado == Thread.State.WAITING || estado == Thread.State.BLOCKED ? GRUESO : DELGADO;
    }
}
