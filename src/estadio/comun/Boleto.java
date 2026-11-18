package estadio.comun;

/**
 * Boleto de un aficionado.
 *
 * @param folio     número único del boleto
 * @param tipo      zona del estadio
 * @param asiento   número de asiento asignado (un boleto duplicado repite el de otro)
 * @param autentico {@code false} si es falso; el guardia lo rechaza
 */
public record Boleto(int folio, TipoBoleto tipo, int asiento, boolean autentico) {
}
