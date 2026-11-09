package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla con el estado de cada agente: el estado del dominio y el estado real del hilo
 * ({@link Thread#getState()}). Se debe llamar {@link #refrescar()} desde el EDT.
 */
public class TablaEstados extends JTable {

    private static final String[] COLUMNAS = {"ID", "Tipo", "Estado del agente", "Estado del hilo"};

    private static final Map<Thread.State, Color> COLORES = new EnumMap<>(Thread.State.class);
    static {
        COLORES.put(Thread.State.NEW, new Color(224, 224, 224));
        COLORES.put(Thread.State.RUNNABLE, new Color(200, 230, 201));
        COLORES.put(Thread.State.BLOCKED, new Color(255, 205, 210));
        COLORES.put(Thread.State.WAITING, new Color(255, 224, 178));
        COLORES.put(Thread.State.TIMED_WAITING, new Color(187, 222, 251));
        COLORES.put(Thread.State.TERMINATED, new Color(189, 189, 189));
    }

    private final RegistroAgentes registro;
    private final Modelo modelo = new Modelo();

    public TablaEstados(RegistroAgentes registro) {
        this.registro = registro;
        setModel(modelo);
        setFillsViewportHeight(true);
        getColumnModel().getColumn(0).setMaxWidth(50);
        getColumnModel().getColumn(3).setCellRenderer(new RenderEstadoHilo());
    }

    /** Toma una foto del estado actual de los agentes y repinta la tabla. */
    public void refrescar() {
        modelo.filas = registro.todos().stream()
                .map(a -> new Fila(a.getIdAgente(), a.getTipo(), a.getEstado().name(), a.getState()))
                .toList();
        modelo.fireTableDataChanged();
    }

    /** Copia inmutable de un agente, para que la tabla no cambie mientras se dibuja. */
    private record Fila(int id, String tipo, String estado, Thread.State estadoHilo) {
    }

    private static final class Modelo extends AbstractTableModel {
        private List<Fila> filas = List.of();

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Fila f = filas.get(fila);
            return switch (columna) {
                case 0 -> f.id();
                case 1 -> f.tipo();
                case 2 -> f.estado();
                default -> f.estadoHilo();
            };
        }
    }

    private static final class RenderEstadoHilo extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
                                                       boolean foco, int fila, int columna) {
            JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
            if (!seleccionado && valor instanceof Thread.State estado) {
                celda.setBackground(COLORES.get(estado));
            }
            return celda;
        }
    }
}
