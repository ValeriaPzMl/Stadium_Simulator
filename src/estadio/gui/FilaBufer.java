package estadio.gui;

/**
 * Un renglón del tablero de búferes (punto d del 2º avance). Todo es texto para poder mostrar
 * "m de n", "—" en lo que no aplica o una nota como "Fase 6".
 */
public record FilaBufer(String letra, String nombre, String piso, String tipo, String capacidad,
                        String esperando, String dentro, String nota) {

    public static final String BUFER_1 = "Búfer 1";
    public static final String BUFER_M = "Búfer m";
    public static final String PROD_CONS = "Prod-cons";
}
