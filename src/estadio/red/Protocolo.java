package estadio.red;

import estadio.comun.Config.Vecino;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Protocolo entre servidores sobre TCP: una conexión por mensaje. El cliente manda un
 * {@link Mensaje} serializado y espera la respuesta (ACK o ERROR); luego se cierra.
 *
 * <p>Por seguridad, al leer solo se aceptan clases del proyecto y básicas de Java
 * ({@link ObjectInputFilter}): deserializar cualquier clase que llegue por la red es peligroso.
 */
public final class Protocolo {

    static final int MS_CONEXION = 1000;
    static final int MS_LECTURA = 3000;

    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "maxdepth=10;maxrefs=200;maxbytes=65536;estadio.red.*;estadio.comun.*;java.lang.*;!*");

    private Protocolo() {
    }

    /** Envía un mensaje y devuelve la respuesta del vecino. */
    public static Mensaje enviar(Vecino vecino, Mensaje mensaje) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(vecino.host(), vecino.puerto()), MS_CONEXION);
            socket.setSoTimeout(MS_LECTURA);
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            salida.writeObject(mensaje);
            salida.flush();
            return leer(socket.getInputStream());
        }
    }

    static Mensaje leer(InputStream entrada) throws IOException {
        ObjectInputStream in = new ObjectInputStream(entrada);
        in.setObjectInputFilter(FILTRO);
        try {
            Object o = in.readObject();
            if (o instanceof Mensaje m) {
                return m;
            }
            throw new IOException("Se esperaba un Mensaje y llegó " + o.getClass().getName());
        } catch (ClassNotFoundException e) {
            throw new IOException("Clase desconocida en el mensaje: " + e.getMessage(), e);
        }
    }

    static void responder(Socket socket, Mensaje respuesta) throws IOException {
        ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
        salida.writeObject(respuesta);
        salida.flush();
    }
}
