package estadio.gui;

import estadio.comun.Config;
import estadio.comun.Config.Temporizador;
import estadio.comun.Escenario;
import estadio.comun.EscenarioException;
import estadio.comun.Piso;
import estadio.comun.Rango;
import estadio.recursos.Mostrador;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Ventana de configuración que aparece al arrancar (punto b del 2º avance): servidor y vecinos,
 * cuántos agentes se crean, la capacidad de cada búfer como "m de n" y todos los timers.
 * Al iniciar arma las mismas opciones que la línea de comandos y las valida con {@link Config}.
 */
public class DialogoConfig extends JDialog {

    private static final Color GRIS = new Color(117, 117, 117);

    private Config resultado;

    // Servidor y vecinos
    private final JComboBox<Piso> piso = new JComboBox<>(Piso.values());
    private final JSpinner puerto = entero(5001, 1, 65535);
    private final DefaultTableModel vecinos = new DefaultTableModel(new Object[] {"Host", "Puerto"}, 0);

    // Agentes
    private final JSpinner aficionados = entero(40, 0, 1000);
    private final JSpinner guardias = entero(2, 0, 50);
    private final JSpinner cocineros = entero(2, 0, 20);
    private final JSpinner falsos = entero(10, 0, 100);
    private final JSpinner duplicados = entero(5, 0, 100);
    private final JSpinner probGuardia = entero(30, 0, 100);
    private final JSpinner probCocinero = entero(10, 0, 100);

    // Secciones críticas (m de n): n sale de lo que dibuja s1.png
    private final int maxTorniquetes;
    private final int maxBanos;
    private final int maxCajas;
    private final JSpinner torniquetes;
    private final JSpinner banos;
    private final JSpinner cajas;
    private final JSpinner porciones = entero(8, 1, Mostrador.MAXIMO);

    // Tiempos (en segundos en la ventana, en ms en Config)
    private final Map<Temporizador, JSpinner[]> tiempos = new LinkedHashMap<>();
    private final JSpinner rondasMin = entero(4, 1, 100);
    private final JSpinner rondasMax = entero(9, 1, 100);
    private final JSpinner velocidad = segundosConPaso(1.0, 0.1, 20.0, 0.5);

    private final JTextField comando = new JTextField();

    /** Muestra la ventana (modal, desde el EDT). @return la configuración elegida, o null si se canceló */
    public static Config mostrar(Config inicial) {
        DialogoConfig d = new DialogoConfig(inicial);
        d.setVisible(true);
        return d.resultado;
    }

    public DialogoConfig(Config inicial) {
        super((java.awt.Frame) null, "Estadio Paralelo – Configuración", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int[] n = maximosDelEscenario();
        maxTorniquetes = n[0];
        maxBanos = n[1];
        maxCajas = n[2];
        torniquetes = entero(Math.min(3, maxTorniquetes), 1, maxTorniquetes);
        banos = entero(Math.min(3, maxBanos), 1, maxBanos);
        cajas = entero(Math.min(3, maxCajas), 1, maxCajas);
        for (Temporizador t : Config.TEMPORIZADORES) {
            JSpinner min = segundos();
            JSpinner max = t.unico() ? null : segundos();
            tiempos.put(t, new JSpinner[] {min, max});
        }

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Servidor y vecinos", pestanaServidor());
        pestanas.addTab("Agentes", pestanaAgentes());
        pestanas.addTab("Secciones críticas", pestanaSecciones());
        pestanas.addTab("Tiempos", new JScrollPane(pestanaTiempos()));

        cargar(inicial);
        conectarEventos();

        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(pestanas, BorderLayout.CENTER);
        contenido.add(pie(), BorderLayout.SOUTH);
        setContentPane(contenido);
        actualizarComando();
        pack();
        setSize(Math.max(getWidth(), 760), Math.max(getHeight(), 560));
        setLocationRelativeTo(null);
    }

    /** Cuántos torniquetes, cubículos y cajas dibuja el escenario de S1 (la n de "m de n"). */
    private static int[] maximosDelEscenario() {
        try {
            Escenario s1 = Escenario.cargar(Piso.PLANTA_BAJA);
            return new int[] {s1.lista("torniquete").size(), s1.lista("bano").size(), s1.lista("caja").size()};
        } catch (EscenarioException e) {
            return new int[] {10, 10, 10};
        }
    }

    // ------------------------------------------------------------ pestañas

    private JComponent pestanaServidor() {
        Formulario f = new Formulario();
        f.titulo("Este servidor");
        piso.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor,
                                                                    int i, boolean sel, boolean foco) {
                return super.getListCellRendererComponent(lista, ((Piso) valor).getNombre(), i, sel, foco);
            }
        });
        f.renglon("Piso", piso, "Cada piso del estadio corre en su propia computadora");
        f.renglon("Puerto", puerto, "Donde este servidor escucha a sus vecinos (por defecto 5000 + piso)");
        f.titulo("Vecinos (los otros pisos)");

        JTable tabla = new JTable(vecinos);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new java.awt.Dimension(340, 110));
        JButton agregar = new JButton("Agregar");
        agregar.addActionListener(e -> {
            int p = 5001;
            while (p == (int) puerto.getValue() || puertoEnTabla(p)) {
                p++;
            }
            vecinos.addRow(new Object[] {"localhost", String.valueOf(p)});
        });
        JButton quitar = new JButton("Quitar");
        quitar.addActionListener(e -> {
            if (tabla.isEditing()) {
                tabla.getCellEditor().stopCellEditing();
            }
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                vecinos.removeRow(fila);
            }
        });
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.add(agregar);
        botones.add(quitar);
        JPanel caja = new JPanel(new BorderLayout(0, 4));
        caja.add(scroll, BorderLayout.CENTER);
        caja.add(botones, BorderLayout.SOUTH);
        f.renglon("", caja, "Ej. localhost / 5003 en la misma PC, o 192.168.1.20 / 5003 en otra");
        return f.panel();
    }

    private JComponent pestanaAgentes() {
        Formulario f = new Formulario();
        f.titulo("Cuántos agentes se crean (S1)");
        f.renglon("Aficionados", aficionados, "Llegan uno por uno a la entrada");
        f.renglon("Guardias", guardias, "Revisan boletos en los torniquetes");
        f.renglon("Cocineros", cocineros, "Producen porciones para el mostrador (con 0, nadie come)");
        f.renglon("Meseros, vendedores de gradas", deshabilitado("Fases 5 y 6"), "Se agregan en las siguientes fases");
        f.titulo("Boletos");
        f.renglon("% falsos", falsos, "El guardia los rechaza en el torniquete");
        f.renglon("% duplicados", duplicados, "Auténticos pero con asiento repetido: el conflicto se ve en el asiento");
        f.titulo("Zona de decisión");
        f.renglon("% descanso del guardia", probGuardia, "Si nadie llega a tiempo, probabilidad de ir a la caseta");
        f.renglon("% descanso del cocinero", probCocinero, "Después de cada porción");
        return f.panel();
    }

    private JComponent pestanaSecciones() {
        Formulario f = new Formulario();
        f.titulo("S1 – Planta baja · búfer m (m de n: n es lo que dibuja el escenario)");
        f.renglon("Torniquetes (T)", mDeN(torniquetes, maxTorniquetes), "Semaphore(T, justo) + cita con el guardia");
        f.renglon("Baño general (Bg)", mDeN(banos, maxBanos), "Semaphore(N, justo): cubículos");
        f.renglon("Cajas (Cj)", mDeN(cajas, maxCajas), "Semaphore(C, justo)");
        f.renglon("Mostrador (M)", mDeN(porciones, Mostrador.MAXIMO), "Monitor productor-consumidor (wait/notifyAll)");
        f.titulo("Búfer 1 (sección crítica)");
        f.renglon("Asiento (A)", fijo("1 de 1"), "synchronized por asiento; un duplicado no se puede sentar");
        f.renglon("Baño de palco (Bp)", fijo("1 de 1 · Fase 6"), "ReentrantLock, uno por palco");
        f.renglon("Montacargas (Mc)", fijo("1 de 1 · Fase 6"), "ReentrantLock; con las bandejas, orden fijo contra deadlock");
        f.titulo("Siguientes fases");
        f.renglon("Escalera (E)", deshabilitado("Fase 7"), "Monitor de un solo sentido con Condition");
        f.renglon("Bandejas (Bj), pedidos (Pd)", deshabilitado("Fase 6"), "Palcos");
        f.renglon("Llamados al vendedor (Ll)", deshabilitado("Fase 5"), "Grada alta");
        return f.panel();
    }

    private JComponent pestanaTiempos() {
        Formulario f = new Formulario();
        f.titulo("Duraciones en segundos (mínimo a máximo; cada vez se elige un valor al azar)");
        for (Map.Entry<Temporizador, JSpinner[]> e : tiempos.entrySet()) {
            JSpinner[] s = e.getValue();
            f.renglon(e.getKey().etiqueta(), s[1] == null ? conUnidad(s[0]) : rango(s[0], s[1]),
                    "--" + e.getKey().clave());
        }
        f.titulo("Otros");
        f.renglon("Rondas viendo el partido", rango(rondasMin, rondasMax), "Cuántas rondas antes de irse");
        f.renglon("Velocidad", velocidad, "Multiplica la rapidez de toda la simulación");
        return f.panel();
    }

    private JComponent pie() {
        comando.setEditable(false);
        comando.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JButton porDefecto = new JButton("Valores por defecto");
        porDefecto.addActionListener(e -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("piso", String.valueOf(((Piso) piso.getSelectedItem()).getNumero()));
            cargar(Config.desdeOpciones(m));
        });
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton iniciar = new JButton("Iniciar simulación");
        iniciar.setFont(iniciar.getFont().deriveFont(Font.BOLD));
        iniciar.addActionListener(e -> iniciar());
        getRootPane().setDefaultButton(iniciar);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(porDefecto);
        botones.add(cancelar);
        botones.add(iniciar);
        JPanel pie = new JPanel(new BorderLayout(0, 6));
        JPanel linea = new JPanel(new BorderLayout(6, 0));
        linea.add(new JLabel("Comando equivalente:"), BorderLayout.WEST);
        linea.add(comando, BorderLayout.CENTER);
        pie.add(linea, BorderLayout.NORTH);
        pie.add(botones, BorderLayout.SOUTH);
        return pie;
    }

    // ------------------------------------------------------------ datos

    /** Pone en los controles los valores de una configuración. */
    private void cargar(Config c) {
        piso.setSelectedItem(c.getPiso());
        puerto.setValue(c.getPuerto());
        vecinos.setRowCount(0);
        for (Config.Vecino v : c.getVecinos()) {
            vecinos.addRow(new Object[] {v.host(), String.valueOf(v.puerto())});
        }
        aficionados.setValue(c.getAficionados());
        guardias.setValue(c.getGuardias());
        cocineros.setValue(c.getCocineros());
        falsos.setValue(c.getPorcentajeFalsos());
        ((SpinnerNumberModel) duplicados.getModel()).setMaximum(100 - c.getPorcentajeFalsos());
        duplicados.setValue(c.getPorcentajeDuplicados());
        probGuardia.setValue(c.getProbDescansoGuardia());
        probCocinero.setValue(c.getProbDescansoCocinero());
        torniquetes.setValue(Math.min(c.getTorniquetes(), maxTorniquetes));
        banos.setValue(Math.min(c.getBanos(), maxBanos));
        cajas.setValue(Math.min(c.getCajas(), maxCajas));
        porciones.setValue(c.getPorciones());
        for (Map.Entry<Temporizador, JSpinner[]> e : tiempos.entrySet()) {
            Rango r = c.tiempo(e.getKey());
            e.getValue()[0].setValue(r.min() / 1000.0);
            if (e.getValue()[1] != null) {
                e.getValue()[1].setValue(r.max() / 1000.0);
            }
        }
        rondasMin.setValue((int) c.getRondas().min());
        rondasMax.setValue((int) c.getRondas().max());
        velocidad.setValue(c.getVelocidad());
        habilitarSecciones();
    }

    /** Lee los controles como opciones de línea de comandos (sin los guiones). */
    private Map<String, String> leer() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("piso", String.valueOf(((Piso) piso.getSelectedItem()).getNumero()));
        m.put("puerto", puerto.getValue().toString());
        StringJoiner lista = new StringJoiner(",");
        for (int i = 0; i < vecinos.getRowCount(); i++) {
            String host = String.valueOf(vecinos.getValueAt(i, 0)).strip();
            String p = String.valueOf(vecinos.getValueAt(i, 1)).strip();
            if (!host.isEmpty() || !p.isEmpty()) {
                lista.add(host + ":" + p);
            }
        }
        if (lista.length() > 0) {
            m.put("vecinos", lista.toString());
        }
        m.put("aficionados", aficionados.getValue().toString());
        m.put("guardias", guardias.getValue().toString());
        m.put("cocineros", cocineros.getValue().toString());
        m.put("torniquetes", torniquetes.getValue().toString());
        m.put("banos", banos.getValue().toString());
        m.put("cajas", cajas.getValue().toString());
        m.put("porciones", porciones.getValue().toString());
        m.put("falsos", falsos.getValue().toString());
        m.put("duplicados", duplicados.getValue().toString());
        m.put("rondas", rondasMin.getValue() + "-" + rondasMax.getValue());
        m.put("prob-descanso-guardia", probGuardia.getValue().toString());
        m.put("prob-descanso-cocinero", probCocinero.getValue().toString());
        for (Map.Entry<Temporizador, JSpinner[]> e : tiempos.entrySet()) {
            long min = ms(e.getValue()[0]);
            long max = e.getValue()[1] == null ? min : ms(e.getValue()[1]);
            m.put(e.getKey().clave(), new Rango(min, max).toString());
        }
        m.put("velocidad", String.valueOf(velocidad.getValue()));
        return m;
    }

    private void iniciar() {
        try {
            resultado = Config.desdeOpciones(leer());
            dispose();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Revisa la configuración", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Solo las opciones que difieren del valor por defecto, para que el comando sea corto. */
    private void actualizarComando() {
        Map<String, String> actual = leer();
        Map<String, String> base = new LinkedHashMap<>();
        base.put("piso", actual.get("piso"));
        Map<String, String> defecto = comoOpciones(Config.desdeOpciones(base));
        StringBuilder sb = new StringBuilder("java -jar estadio.jar");
        actual.forEach((clave, valor) -> {
            if (clave.equals("piso") || !valor.equals(defecto.get(clave))) {
                sb.append(" --").append(clave).append('=').append(valor);
            }
        });
        comando.setText(sb.append(" --sin-dialogo").toString());
        comando.setCaretPosition(0);
    }

    /** Las opciones que corresponden a una configuración, en el mismo formato que {@link #leer()}. */
    private Map<String, String> comoOpciones(Config c) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("puerto", String.valueOf(c.getPuerto()));
        m.put("aficionados", String.valueOf(c.getAficionados()));
        m.put("guardias", String.valueOf(c.getGuardias()));
        m.put("cocineros", String.valueOf(c.getCocineros()));
        m.put("torniquetes", String.valueOf(Math.min(c.getTorniquetes(), maxTorniquetes)));
        m.put("banos", String.valueOf(Math.min(c.getBanos(), maxBanos)));
        m.put("cajas", String.valueOf(Math.min(c.getCajas(), maxCajas)));
        m.put("porciones", String.valueOf(c.getPorciones()));
        m.put("falsos", String.valueOf(c.getPorcentajeFalsos()));
        m.put("duplicados", String.valueOf(c.getPorcentajeDuplicados()));
        m.put("rondas", c.getRondas().min() + "-" + c.getRondas().max());
        m.put("prob-descanso-guardia", String.valueOf(c.getProbDescansoGuardia()));
        m.put("prob-descanso-cocinero", String.valueOf(c.getProbDescansoCocinero()));
        for (Temporizador t : Config.TEMPORIZADORES) {
            m.put(t.clave(), c.tiempo(t).toString());
        }
        m.put("velocidad", String.valueOf(c.getVelocidad()));
        return m;
    }

    private void conectarEventos() {
        piso.addActionListener(e -> {
            puerto.setValue(5000 + ((Piso) piso.getSelectedItem()).getNumero());
            habilitarSecciones();
            actualizarComando();
        });
        falsos.addChangeListener(e -> {
            int max = 100 - (int) falsos.getValue();
            ((SpinnerNumberModel) duplicados.getModel()).setMaximum(max);
            if ((int) duplicados.getValue() > max) {
                duplicados.setValue(max);
            }
        });
        // Mínimo y máximo de cada rango se empujan entre sí para que nunca min > max
        for (JSpinner[] par : tiempos.values()) {
            if (par[1] != null) {
                enlazar(par[0], par[1]);
            }
        }
        enlazar(rondasMin, rondasMax);

        JSpinner[] todos = {puerto, aficionados, guardias, cocineros, falsos, duplicados, probGuardia, probCocinero,
                torniquetes, banos, cajas, porciones, rondasMin, rondasMax, velocidad};
        for (JSpinner s : todos) {
            s.addChangeListener(e -> actualizarComando());
        }
        for (JSpinner[] par : tiempos.values()) {
            for (JSpinner s : par) {
                if (s != null) {
                    s.addChangeListener(e -> actualizarComando());
                }
            }
        }
        vecinos.addTableModelListener(e -> actualizarComando());
    }

    private static void enlazar(JSpinner min, JSpinner max) {
        min.addChangeListener(e -> {
            if (((Number) min.getValue()).doubleValue() > ((Number) max.getValue()).doubleValue()) {
                max.setValue(min.getValue());
            }
        });
        max.addChangeListener(e -> {
            if (((Number) max.getValue()).doubleValue() < ((Number) min.getValue()).doubleValue()) {
                min.setValue(max.getValue());
            }
        });
    }

    /** Los búferes de S1 solo aplican cuando este servidor es S1. */
    private void habilitarSecciones() {
        boolean esS1 = piso.getSelectedItem() == Piso.PLANTA_BAJA;
        for (JSpinner s : new JSpinner[] {torniquetes, banos, cajas, porciones}) {
            s.setEnabled(esS1);
        }
    }

    private boolean puertoEnTabla(int p) {
        for (int i = 0; i < vecinos.getRowCount(); i++) {
            if (String.valueOf(p).equals(String.valueOf(vecinos.getValueAt(i, 1)).strip())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ controles

    private static JSpinner entero(int valor, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(valor, max)), min, max, 1));
        s.setEditor(new JSpinner.NumberEditor(s, "#")); // sin separador de miles (5001, no 5,001)
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(5);
        return s;
    }

    private static JSpinner segundos() {
        return segundosConPaso(1.0, 0.0, 600.0, 0.1);
    }

    private static JSpinner segundosConPaso(double valor, double min, double max, double paso) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(valor, min, max, paso));
        s.setEditor(new JSpinner.NumberEditor(s, "0.0#"));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(5);
        return s;
    }

    private static long ms(JSpinner segundos) {
        return Math.round(((Number) segundos.getValue()).doubleValue() * 1000);
    }

    private static JComponent mDeN(JSpinner m, int n) {
        JPanel p = fila();
        p.add(m);
        p.add(new JLabel("de " + n));
        return p;
    }

    private static JComponent rango(JSpinner min, JSpinner max) {
        JPanel p = fila();
        p.add(min);
        p.add(new JLabel("a"));
        p.add(max);
        if (min.getModel() instanceof SpinnerNumberModel m && m.getValue() instanceof Double) {
            p.add(new JLabel("s"));
        }
        return p;
    }

    private static JComponent conUnidad(JSpinner s) {
        JPanel p = fila();
        p.add(s);
        p.add(new JLabel("s"));
        return p;
    }

    private static JComponent fijo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JComponent deshabilitado(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(GRIS);
        return l;
    }

    private static JPanel fila() {
        return new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    }

    /** Formulario de tres columnas (etiqueta, control, ayuda) con títulos de sección. */
    private static final class Formulario {
        private final JPanel panel;
        private int fila;

        Formulario() {
            this.panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }

        void titulo(String texto) {
            JLabel l = new JLabel(texto);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
            GridBagConstraints c = base(0);
            c.gridwidth = 3;
            c.insets = new Insets(fila == 0 ? 0 : 12, 0, 4, 0);
            panel.add(l, c);
            fila++;
        }

        void renglon(String etiqueta, JComponent control, String ayuda) {
            panel.add(new JLabel(etiqueta), base(0));
            GridBagConstraints c = base(1);
            panel.add(control, c);
            JLabel a = new JLabel(ayuda);
            a.setForeground(GRIS);
            a.setFont(a.getFont().deriveFont(Font.PLAIN, 11f));
            GridBagConstraints ca = base(2);
            ca.weightx = 1;
            panel.add(a, ca);
            fila++;
        }

        private GridBagConstraints base(int columna) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = columna;
            c.gridy = fila;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, columna == 0 ? 8 : 6, 2, 6);
            return c;
        }

        JPanel panel() {
            // Empuja todo hacia arriba
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = fila;
            c.weighty = 1;
            panel.add(new JPanel(), c);
            return panel;
        }
    }
}
