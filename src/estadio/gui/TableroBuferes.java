package estadio.gui;

import estadio.agentes.Agente;
import estadio.agentes.RegistroAgentes;
import estadio.comun.Piso;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tablero del punto d del 2º avance: cuántos agentes hay por tipo y, para cada búfer o zona
 * crítica de todo el simulador, cuántos esperan y cuántos están dentro. Los búferes de este piso
 * vienen en vivo de su {@link Capa}; los demás aparecen con la fase en que se programan.
 */
public class TableroBuferes extends JPanel {

    private static final String[] COLUMNAS = {"Búfer", "Piso", "Tipo", "Capacidad", "Esperando", "Dentro", "Nota"};

    /** Todos los búferes del diseño (entrega1/ANALISIS.md, sección 2), con la fase que los programa. */
    private record Planeado(String letra, String nombre, Piso piso, String tipo, String capacidad, int fase) {
    }

    private static final List<Planeado> PLANEADOS = List.of(
            new Planeado("T", "Torniquetes", Piso.PLANTA_BAJA, FilaBufer.BUFER_M, "T", 2),
            new Planeado("A", "Asientos", Piso.PLANTA_BAJA, FilaBufer.BUFER_1, "1 c/u", 2),
            new Planeado("Bg", "Baño general", Piso.PLANTA_BAJA, FilaBufer.BUFER_M, "N", 3),
            new Planeado("Cj", "Cajas", Piso.PLANTA_BAJA, FilaBufer.BUFER_M, "C", 3),
            new Planeado("M", "Mostrador", Piso.PLANTA_BAJA, FilaBufer.PROD_CONS, "K", 3),
            new Planeado("E", "Escalera (un sentido a la vez)", Piso.PLANTA_BAJA, FilaBufer.BUFER_M, "E", 7),
            new Planeado("Ll", "Cola de llamados al vendedor", Piso.PLANTA_BAJA, FilaBufer.PROD_CONS, "L", 5),
            new Planeado("A", "Asientos populares", Piso.GRADA_ALTA, FilaBufer.BUFER_1, "1 c/u", 5),
            new Planeado("Bg", "Baño general", Piso.GRADA_ALTA, FilaBufer.BUFER_M, "N", 5),
            new Planeado("Cj", "Cajas", Piso.GRADA_ALTA, FilaBufer.BUFER_M, "C", 5),
            new Planeado("M", "Mostrador", Piso.GRADA_ALTA, FilaBufer.PROD_CONS, "K", 5),
            new Planeado("Ll", "Cola de llamados al vendedor", Piso.GRADA_ALTA, FilaBufer.PROD_CONS, "L", 5),
            new Planeado("Bp", "Baño de palco", Piso.PALCOS, FilaBufer.BUFER_1, "1 de 1", 6),
            new Planeado("Pd", "Cola de pedidos", Piso.PALCOS, FilaBufer.PROD_CONS, "P", 6),
            new Planeado("M", "Mostrador de la cocina", Piso.PALCOS, FilaBufer.PROD_CONS, "K", 6),
            new Planeado("Bj", "Bandejas de meseros", Piso.PALCOS, FilaBufer.BUFER_M, "B", 6),
            new Planeado("Mc", "Montacargas", Piso.PALCOS, FilaBufer.BUFER_1, "1 de 1", 6));

    /** Última fase terminada: lo planeado hasta aquí ya existe en el servidor de su piso. */
    private static final int ULTIMA_FASE = 4;

    private final RegistroAgentes registro;
    private final Capa capa;
    private final Piso piso;
    private final JLabel agentes = new JLabel();
    private final Modelo modelo = new Modelo();

    public TableroBuferes(RegistroAgentes registro, Capa capa, Piso piso) {
        super(new BorderLayout());
        this.registro = registro;
        this.capa = capa;
        this.piso = piso;
        agentes.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        agentes.setVerticalAlignment(JLabel.TOP);

        JTable tabla = new JTable(modelo);
        tabla.setRowSelectionAllowed(false);
        tabla.setFillsViewportHeight(true);
        tabla.setDefaultRenderer(Object.class, new Render());
        int[] anchos = {150, 32, 70, 85, 70, 85, 190};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        add(agentes, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void refrescar() {
        // Agentes por tipo: vivos ahora y creados en total
        Map<String, int[]> porTipo = new LinkedHashMap<>();
        for (Agente a : registro.todos()) {
            int[] c = porTipo.computeIfAbsent(a.getTipo(), k -> new int[2]);
            c[1]++;
            if (a.getState() != Thread.State.TERMINATED) {
                c[0]++;
            }
        }
        StringBuilder html = new StringBuilder("<html><b>Agentes en este servidor</b><br>");
        porTipo.forEach((tipo, c) -> html.append("Agente ").append(tipo).append(": hay <b>").append(c[0])
                .append("</b> (").append(c[1]).append(" creados)<br>"));
        if (porTipo.isEmpty()) {
            html.append("Todavía no hay agentes en este piso.<br>");
        }
        agentes.setText(html.append("</html>").toString());

        // Búferes: en vivo los de la capa; el resto según la fase
        List<FilaBufer> enVivo = capa.buferes();
        List<FilaBufer> filas = new ArrayList<>(enVivo);
        for (Planeado p : PLANEADOS) {
            String pisoCorto = "S" + p.piso().getNumero();
            boolean yaEsta = enVivo.stream().anyMatch(f -> f.letra().equals(p.letra()) && f.piso().equals(pisoCorto));
            if (yaEsta) {
                continue;
            }
            String nota = p.fase() <= ULTIMA_FASE && p.piso() != piso
                    ? "en el servidor " + pisoCorto
                    : "Fase " + p.fase();
            filas.add(new FilaBufer(p.letra(), p.nombre(), pisoCorto, p.tipo(), p.capacidad(), "—", "—", nota));
        }
        modelo.filas = filas;
        modelo.fireTableDataChanged();
    }

    private static final class Modelo extends AbstractTableModel {
        private List<FilaBufer> filas = List.of();

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int c) {
            return COLUMNAS[c];
        }

        @Override
        public Object getValueAt(int f, int c) {
            FilaBufer b = filas.get(f);
            return switch (c) {
                case 0 -> b.letra() + " · " + b.nombre();
                case 1 -> b.piso();
                case 2 -> b.tipo();
                case 3 -> b.capacidad();
                case 4 -> b.esperando();
                case 5 -> b.dentro();
                default -> b.nota();
            };
        }

        boolean enVivo(int f) {
            return !filas.get(f).dentro().equals("—");
        }
    }

    /** Los búferes en vivo en negro; los planeados o de otro servidor, en gris. */
    private final class Render extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel, boolean foco,
                                                       int fila, int columna) {
            JLabel celda = (JLabel) super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            boolean vivo = modelo.enVivo(fila);
            celda.setForeground(vivo ? Color.BLACK : Color.GRAY);
            celda.setToolTipText(String.valueOf(valor));
            celda.setFont(celda.getFont().deriveFont(vivo && columna >= 4 && columna <= 5 ? Font.BOLD : Font.PLAIN));
            return celda;
        }
    }
}
