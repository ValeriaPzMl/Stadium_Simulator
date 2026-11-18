package estadio.comun;

/** Zona a la que da acceso un boleto. */
public enum TipoBoleto {
    GENERAL(Piso.PLANTA_BAJA),
    PALCO(Piso.PALCOS),
    POPULAR(Piso.GRADA_ALTA);

    private final Piso piso;

    TipoBoleto(Piso piso) {
        this.piso = piso;
    }

    /** Piso donde está el asiento de este tipo de boleto. */
    public Piso getPiso() {
        return piso;
    }
}
