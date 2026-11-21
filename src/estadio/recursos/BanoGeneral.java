package estadio.recursos;

/** Baño general con N cubículos (búfer Bg, sección crítica 5): {@code Semaphore(N, true)}. */
public class BanoGeneral extends ZonaConCupo {

    public BanoGeneral(int cubiculos) {
        super("Baño general", cubiculos);
    }
}
