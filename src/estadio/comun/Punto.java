package estadio.comun;

/** Coordenada en pixeles de la imagen del escenario. */
public record Punto(double x, double y) {

    public double distancia(Punto otro) {
        return Math.hypot(otro.x - x, otro.y - y);
    }

    public Punto desplazado(double dx, double dy) {
        return new Punto(x + dx, y + dy);
    }

    @Override
    public String toString() {
        return "(" + Math.round(x) + ", " + Math.round(y) + ")";
    }
}
