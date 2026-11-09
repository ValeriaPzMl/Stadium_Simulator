package estadio.comun;

/** Los tres pisos del estadio; cada uno corre en su propio servidor. */
public enum Piso {
    PLANTA_BAJA(1, "S1 – Planta baja"),
    PALCOS(2, "S2 – Palcos"),
    GRADA_ALTA(3, "S3 – Grada alta");

    private final int numero;
    private final String nombre;

    Piso(int numero, String nombre) {
        this.numero = numero;
        this.nombre = nombre;
    }

    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
    }

    public static Piso deNumero(int numero) {
        for (Piso p : values()) {
            if (p.numero == numero) {
                return p;
            }
        }
        throw new IllegalArgumentException("Piso inválido: " + numero + " (debe ser 1, 2 o 3)");
    }
}
