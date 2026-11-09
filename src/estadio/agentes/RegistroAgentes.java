package estadio.agentes;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lista de agentes de un piso, compartida entre los hilos de los agentes y el EDT.
 * Se usa {@link CopyOnWriteArrayList} porque la interfaz la recorre ~30 veces por segundo
 * y solo se modifica cuando entra o sale un agente.
 */
public class RegistroAgentes {

    private final List<Agente> agentes = new CopyOnWriteArrayList<>();

    /** Agrega el agente al piso y arranca su hilo. */
    public void registrarYArrancar(Agente agente) {
        agentes.add(agente);
        agente.start();
    }

    /**
     * Vista de solo lectura. Recorrerla es seguro aunque otro hilo agregue agentes:
     * el iterador de {@link CopyOnWriteArrayList} trabaja sobre una foto de la lista.
     */
    public List<Agente> todos() {
        return Collections.unmodifiableList(agentes);
    }
}
