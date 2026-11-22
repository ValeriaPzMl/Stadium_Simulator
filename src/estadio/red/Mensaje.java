package estadio.red;

import estadio.comun.Piso;

import java.io.Serializable;

/**
 * Mensaje entre servidores. Cada envío abre una conexión, manda un mensaje, recibe la respuesta
 * y cierra (ver {@link Protocolo}).
 *
 * @param tipo   qué es
 * @param origen piso del servidor que lo manda (así se sabe qué vecino es cada piso)
 * @param agente solo en {@link Tipo#AGENTE}
 * @param error  solo en {@link Tipo#ERROR}
 */
public record Mensaje(Tipo tipo, Piso origen, AgenteDTO agente, String error) implements Serializable {

    public enum Tipo {
        /** "¿Quién eres?": el vecino contesta con su piso. */
        HOLA,
        /** Un aficionado que llega a este piso. */
        AGENTE,
        /** Recibido; el agente ya existe en el destino. */
        ACK,
        /** No se pudo procesar. */
        ERROR
    }

    public static Mensaje hola(Piso origen) {
        return new Mensaje(Tipo.HOLA, origen, null, null);
    }

    public static Mensaje agente(Piso origen, AgenteDTO dto) {
        return new Mensaje(Tipo.AGENTE, origen, dto, null);
    }

    public static Mensaje ack(Piso origen) {
        return new Mensaje(Tipo.ACK, origen, null, null);
    }

    public static Mensaje error(Piso origen, String texto) {
        return new Mensaje(Tipo.ERROR, origen, null, texto);
    }
}
