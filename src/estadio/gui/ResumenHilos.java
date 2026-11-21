package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tablero del punto c del 2º avance: cuántos hilos hay en cada {@link Thread.State}, por tipo de
 * agente y en total. Se refresca desde el EDT con {@link #refrescar()}.
 */
public class ResumenHilos extends JTable {

    private static final Thread.State[] ESTADOS = Thread.State.values();

    private final RegistroAgentes registro;
    private final Modelo modelo = new Modelo();

    public ResumenHilos(RegistroAgentes registro) {
        this.registro = registro;
        setModel(modelo);
        setRowSelectionAllowed(false);
        setFillsViewportHeight(true);
        DefaultTableCellRenderer centro = new Render();
        for (int c = 0; c < modelo.getColumnCount(); c++) {
            getColumnModel().getColumn(c).setCellRenderer(centro);
        }
        getColumnModel().getColumn(0).setPreferredWidth(90);
        getTableHeader().setDefaultRenderer(new RenderEncabezado());
    }

    public void refrescar() {
        Map<String, int[]> porTipo = new LinkedHashMap<>();
        int[] total = new int[ESTADOS.length + 1];
        for (Agente a : registro.todos()) {
            int[] fila = porTipo.computeIfAbsent(a.getTipo(), k -> new int[ESTADOS.length + 1]);
            int i = a.getState().ordinal();
            fila[i]++;
            fila[ESTADOS.length]++;
            total[i]++;
            total[ESTADOS.length]++;
        }
        List<Object[]> filas = new ArrayList<>();
        porTipo.forEach((tipo, cuenta) -> filas.add(fila(tipo, cuenta)));
        filas.add(fila("TOTAL", total));
        modelo.filas = filas;
        modelo.fireTableDataChanged();
    }

    private static Object[] fila(String tipo, int[] cuenta) {
        Object[] f = new Object[cuenta.length + 1];
        f[0] = tipo;
        for (int i = 0; i < cuenta.length; i++) {
            f[i + 1] = cuenta[i];
        }
        return f;
    }

    private static final class Modelo extends AbstractTableModel {
        private List<Object[]> filas = List.of();

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return ESTADOS.length + 2;
        }

        @Override
        public String getColumnName(int c) {
            if (c == 0) {
                return "Tipo";
            }
            return c <= ESTADOS.length ? ESTADOS[c - 1].name().replace("_", " ") : "Total";
        }

        @Override
        public Object getValueAt(int f, int c) {
            return filas.get(f)[c];
        }
    }

    /** Centra los números, pone en negritas la fila TOTAL y colorea las columnas no vacías. */
    private static final class Render extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel, boolean foco,
                                                       int fila, int columna) {
            JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            celda.setHorizontalAlignment(columna == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            boolean total = fila == tabla.getRowCount() - 1;
            celda.setFont(celda.getFont().deriveFont(total ? Font.BOLD : Font.PLAIN));
            boolean hayHilos = valor instanceof Integer n && n > 0;
            if (columna >= 1 && columna <= ESTADOS.length && hayHilos) {
                celda.setBackground(ColoresHilo.fondo(ESTADOS[columna - 1]));
            } else {
                celda.setBackground(tabla.getBackground());
            }
            return celda;
        }
    }

    private static final class RenderEncabezado extends DefaultTableCellRenderer {
        RenderEncabezado() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel, boolean foco,
                                                       int fila, int columna) {
            JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            celda.setFont(celda.getFont().deriveFont(Font.BOLD, 11f));
            celda.setBackground(columna >= 1 && columna <= ESTADOS.length
                    ? ColoresHilo.fondo(ESTADOS[columna - 1]) : new java.awt.Color(238, 238, 238));
            celda.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, java.awt.Color.GRAY));
            return celda;
        }
    }
}
