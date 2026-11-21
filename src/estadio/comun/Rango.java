package estadio.comun;

import java.util.concurrent.ThreadLocalRandom;

/** Intervalo cerrado [min, max]; para los timers está en milisegundos. */
public record Rango(long min, long max) {

    public Rango {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("rango inválido: " + min + "-" + max);
        }
    }

    public long alAzar() {
        return min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /** Lee "800-1600" o "5000" (un solo valor = mínimo y máximo iguales). */
    public static Rango leer(String texto) {
        String[] partes = texto.strip().split("-");
        if (partes.length < 1 || partes.length > 2) {
            throw new NumberFormatException(texto);
        }
        long min = Long.parseLong(partes[0].strip());
        long max = partes.length == 2 ? Long.parseLong(partes[1].strip()) : min;
        return new Rango(min, max);
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + "-" + max;
    }
}
