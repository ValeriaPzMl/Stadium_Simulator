package estadio.red;

import estadio.comun.Boleto;
import estadio.comun.Piso;

import java.io.Serializable;

/**
 * Lo que viaja por la red cuando un aficionado cambia de piso: no se manda el hilo (eso no se
 * puede), sino sus datos. El servidor destino crea un hilo nuevo con ellos.
 *
 * @param boleto    el boleto (identifica a la persona por su folio)
 * @param vejiga    nivel 0–100, para que siga igual en el otro piso
 * @param hambre    nivel 0–100
 * @param origen    piso de donde viene
 * @param idOrigen  id del agente en el servidor de origen (para la bitácora)
 */
public record AgenteDTO(Boleto boleto, int vejiga, int hambre, Piso origen, int idOrigen) implements Serializable {
}
