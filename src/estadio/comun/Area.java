package estadio.comun;

import java.util.concurrent.ThreadLocalRandom;

/** Rectángulo del escenario del que se eligen puntos al azar (fila de entrada, aparición...). */
public record Area(double x1, double y1, double x2, double y2) {

    /** Normaliza las esquinas para que (x1, y1) sea la superior izquierda. */
    public Area {
        double minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        x1 = minX;
        x2 = maxX;
        y1 = minY;
        y2 = maxY;
    }

    public Punto puntoAlAzar() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double x = x1 == x2 ? x1 : rnd.nextDouble(x1, x2);
        double y = y1 == y2 ? y1 : rnd.nextDouble(y1, y2);
        return new Punto(x, y);
    }

    public double ancho() {
        return x2 - x1;
    }

    public double alto() {
        return y2 - y1;
    }
}
