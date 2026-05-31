package sigep;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.*;

/**
 * Fábrica centralizada de componentes UI de SIGEP.
 * Todos los métodos devuelven componentes ya estilizados.
 *
 * Explicación: usar `UIFactory` cuando se necesite crear botones, campos,
 * labels, paneles o tablas con estilo consistente. Evita repetir código de
 * configuración visual en diferentes paneles.
 */
public final class UIFactory {
    private UIFactory() {}

    // ═══════════════════════════════════════════════════════════════════════
    // PANELES / CONTENEDORES
    // ═══════════════════════════════════════════════════════════════════════

    /** Panel blanco con borde sutil y sombra ligera (card) */
    // Uso: panel tipo tarjeta para encapsular secciones con fondo blanco.
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // sombra
                g2.setColor(new Color(0,0,0,20));
                g2.fillRoundRect(2, 3, getWidth()-2, getHeight()-2, Theme.RADIUS, Theme.RADIUS);
                // fondo
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-2, getHeight()-3, Theme.RADIUS, Theme.RADIUS);
                // borde
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth()-3, getHeight()-4, Theme.RADIUS, Theme.RADIUS);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 4, 2));
        return p;
    }

    /** Panel transparente */
    // Uso: contenedor sin fondo que permite layout personalizado.
    public static JPanel transparent(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }
    public static JPanel transparent() { return transparent(new FlowLayout()); }

    /** Sección con fondo azul suave (like bg-blue-50/50) */
    // Uso: sección informativa destacada con fondo azul claro.
    public static JPanel blueSection() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xeff6ff));
                g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xbfdbfe)),
            new EmptyBorder(16,20,16,20)));
        return p;
    }

    /** Card de fondo gris (gray-50) para barras de acción */
    // Uso: barra de acciones (buscar, botones) con fondo gris y padding.
    public static JPanel actionBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0xf9fafb));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(10,14,10,14)));
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ETIQUETAS
    // ═══════════════════════════════════════════════════════════════════════

    // Atajos para crear `JLabel` con las fuentes y colores definidos en `Theme`.
    public static JLabel h1(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_H1); l.setForeground(Theme.TEXT_PRIMARY); return l; }
    public static JLabel h2(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_H2); l.setForeground(Theme.TEXT_PRIMARY); return l; }
    public static JLabel h3(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_H3); l.setForeground(Theme.TEXT_PRIMARY); return l; }
    public static JLabel body(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_BODY); l.setForeground(Theme.TEXT_SECONDARY); return l; }
    public static JLabel muted(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_BODY); l.setForeground(Theme.TEXT_MUTED); return l; }
    public static JLabel small(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUTED); return l; }
    public static JLabel fieldLabel(String t){ JLabel l=new JLabel(t); l.setFont(Theme.FONT_LABEL); l.setForeground(Theme.TEXT_SECONDARY); return l; }
    public static JLabel sectionLabel(String t){
        JLabel l=new JLabel(t.toUpperCase());
        l.setFont(new Font("SansSerif",Font.BOLD,10));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CAMPOS DE TEXTO
    // ═══════════════════════════════════════════════════════════════════════

    // Campo de texto estilizado con placeholder dibujado cuando está vacío.
    public static JTextField textField(String ph){
        JTextField f = new JTextField(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(getText().isEmpty() && !isFocusOwner()){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm=g2.getFontMetrics();
                    int y=(getHeight()-fm.getHeight())/2+fm.getAscent();
                    g2.drawString(ph, getInsets().left, y);
                    g2.dispose();
                }
            }
        };
        styleInput(f); return f;
    }

    // Campo de contraseña estilizado con placeholder dibujado.
    public static JPasswordField passwordField(String ph){
        JPasswordField f = new JPasswordField(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(getPassword().length==0 && !isFocusOwner()){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setFont(getFont().deriveFont(Font.PLAIN));
                    FontMetrics fm=g2.getFontMetrics();
                    int y=(getHeight()-fm.getHeight())/2+fm.getAscent();
                    g2.drawString(ph, getInsets().left, y);
                    g2.dispose();
                }
            }
        };
        styleInput(f); return f;
    }

    // Área de texto multilinea estilizada (wrap + borde).
    public static JTextArea textArea(String ph, int rows){
        JTextArea a=new JTextArea(rows,0);
        a.setFont(Theme.FONT_BODY);
        a.setForeground(Theme.TEXT_PRIMARY);
        a.setBackground(Theme.BG_INPUT);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(6,10,6,10)));
        return a;
    }

    // Aplica estilo común a componentes de entrada (borde, tamaño, foco).
    private static void styleInput(JTextComponent f){
        f.setFont(Theme.FONT_BODY);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setBackground(Theme.BG_INPUT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            new EmptyBorder(6,10,6,10)));
        f.setPreferredSize(new Dimension(0,34));
        f.addFocusListener(new FocusAdapter(){
            @Override public void focusGained(FocusEvent e){
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_FOCUS,1),
                    new EmptyBorder(6,10,6,10)));
            }
            @Override public void focusLost(FocusEvent e){
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    new EmptyBorder(6,10,6,10)));
            }
        });
    }

    // Crea un `JComboBox` estilizado con renderer personalizado.
    public static JComboBox<String> comboBox(String... items){
        JComboBox<String> cb=new JComboBox<>(items);
        cb.setFont(Theme.FONT_BODY);
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        cb.setPreferredSize(new Dimension(0,34));
        cb.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                JLabel lbl=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);
                lbl.setBorder(new EmptyBorder(5,10,5,10));
                lbl.setFont(Theme.FONT_BODY);
                if(s){lbl.setBackground(Theme.PRIMARY_LIGHT);lbl.setForeground(Theme.PRIMARY);}
                else {lbl.setBackground(Color.WHITE);       lbl.setForeground(Theme.TEXT_PRIMARY);}
                return lbl;
            }
        });
        return cb;
    }

    /** Estila un JComboBox que almacena objetos (muestra toString()). */
    public static <T> void styleComboObject(JComboBox<T> cb){
        cb.setFont(Theme.FONT_BODY);
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        cb.setPreferredSize(new Dimension(0,34));
        cb.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){
                JLabel lbl=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);
                lbl.setBorder(new EmptyBorder(5,10,5,10));
                lbl.setFont(Theme.FONT_BODY);
                if(s){lbl.setBackground(Theme.PRIMARY_LIGHT);lbl.setForeground(Theme.PRIMARY);} 
                else {lbl.setBackground(Color.WHITE); lbl.setForeground(Theme.TEXT_PRIMARY);} 
                return lbl;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BOTONES
    // ═══════════════════════════════════════════════════════════════════════

    /** Botón sólido azul-900 — bg-blue-900 hover:bg-blue-800 */
    // Botón principal destacado para acciones primarias.
    public static JButton primaryBtn(String text){
        JButton b=new JButton(text){
            boolean hov=false;
            {addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){hov=true;repaint();}
                @Override public void mouseExited(MouseEvent e){hov=false;repaint();}
            });}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()?(hov?Theme.PRIMARY_HOVER:Theme.PRIMARY):new Color(0xd1d5db));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),Theme.RADIUS,Theme.RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.FONT_LABEL); b.setForeground(Color.WHITE);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6,12,6,12));
        return b;
    }

    /** Botón outline (borde gris, texto gris) */
    // Botón con estilo outline para acciones secundarias.
    public static JButton outlineBtn(String text){
        JButton b=new JButton(text){
            boolean hov=false;
            {addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){hov=true;repaint();}
                @Override public void mouseExited(MouseEvent e){hov=false;repaint();}
            });}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov?new Color(0xf9fafb):Color.WHITE);
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Theme.RADIUS,Theme.RADIUS);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,Theme.RADIUS,Theme.RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.FONT_LABEL); b.setForeground(Theme.TEXT_SECONDARY);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4,8,4,8));
        return b;
    }

    /** Botón pequeño azul outline (bg-blue-50 text-blue-900 border-blue-200) */
    // Variante azul del botón outline para énfasis suave.
    public static JButton blueOutlineBtn(String text){
        JButton b=new JButton(text){
            boolean hov=false;
            {addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){hov=true;repaint();}
                @Override public void mouseExited(MouseEvent e){hov=false;repaint();}
            });}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov?Theme.PRIMARY_LIGHT:new Color(0xeff6ff));
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Theme.RADIUS,Theme.RADIUS);
                g2.setColor(new Color(0xbfdbfe));
                g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,Theme.RADIUS,Theme.RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.FONT_BADGE); b.setForeground(Theme.PRIMARY);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4,8,4,8));
        return b;
    }

    /** Botón peligro rojo outline */
    // Botón para acciones destructivas o de advertencia.
    public static JButton dangerBtn(String text){
        JButton b=new JButton(text){
            boolean hov=false;
            {addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e){hov=true;repaint();}
                @Override public void mouseExited(MouseEvent e){hov=false;repaint();}
            });}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov?Theme.RED_BG:Color.WHITE);
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,Theme.RADIUS,Theme.RADIUS);
                g2.setColor(Theme.RED_BORDER);
                g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,Theme.RADIUS,Theme.RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.FONT_BADGE); b.setForeground(Theme.RED_TEXT);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(5,10,5,10));
        return b;
    }

    /** Botón verde outline */
    // Botón para acciones de éxito/confirmación (verde).
    public static JButton greenBtn(String text){
        JButton b=outlineBtn(text);
        b.setForeground(Theme.GREEN_TEXT);
        return b;
    }

    /** Botón link pequeño */
    // Botón que se comporta y parece un enlace (sin fondo ni borde).
    public static JButton linkBtn(String text, Color fg){
        JButton b=new JButton(text);
        b.setFont(Theme.FONT_SMALL); b.setForeground(fg);
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(2,4,2,4));
        return b;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BADGES
    // ═══════════════════════════════════════════════════════════════════════

    // Etiqueta tipo badge con fondo, texto y borde personalizables.
    public static JLabel badge(String text, Color bg, Color fg, Color border){
        JLabel l=new JLabel(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 12;
                g2.setColor(bg); g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,arc,arc);
                g2.setColor(border); g2.drawRoundRect(0,0,getWidth()-2,getHeight()-2,arc,arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        // Slightly larger badge font and padding for better visibility in headers
        l.setFont(Theme.FONT_BADGE.deriveFont(Font.BOLD, 14f));
        l.setForeground(fg); l.setOpaque(false);
        l.setBorder(new EmptyBorder(6,14,6,14));
        return l;
    }

    // Badges predefinidos para estados comunes.
    public static JLabel badgeActivo()    { return badge("✓ Activo",    Theme.GREEN_BG,  Theme.GREEN_TEXT,  Theme.GREEN_BORDER); }
    public static JLabel badgeInactivo()  { return badge("✗ Inactivo",  Theme.RED_BG,    Theme.RED_TEXT,    Theme.RED_BORDER); }
    public static JLabel badgeListo()     { return badge("✓ Listo",     Theme.GREEN_BG,  Theme.GREEN_TEXT,  Theme.GREEN_BORDER); }
    public static JLabel badgePendiente() { return badge("⏳ Pendiente", Theme.YELLOW_BG, Theme.YELLOW_TEXT, Theme.YELLOW_BORDER); }
    public static JLabel badgeEnCurso()   { return badge("En práctica",     Theme.BLUE_BG,   Theme.BLUE_TEXT,   Theme.BLUE_BORDER); }
    public static JLabel badgeFinalizada(){ return badge("Finalizada",   Theme.GREEN_BG,  Theme.GREEN_TEXT,  Theme.GREEN_BORDER); }
    public static JLabel badgeRealizando(){ return badge("Realizando práctica",   Theme.GREEN_BG,  Theme.GREEN_TEXT,  Theme.GREEN_BORDER); }
    public static JLabel badgeValido()    { return badge("✓ Aprobado",     Theme.GREEN_BG,  Theme.GREEN_TEXT,  Theme.GREEN_BORDER); }
    public static JLabel badgeModificar() { return badge("✗ Rechazado",  Theme.RED_BG,    Theme.RED_TEXT,    Theme.RED_BORDER); }
    public static JLabel badgeRevision()  { return badge("⏱ En Revisión",Theme.YELLOW_BG, Theme.YELLOW_TEXT, Theme.YELLOW_BORDER); }

    // ═══════════════════════════════════════════════════════════════════════
    // SEPARADORES / UTILIDADES LAYOUT
    // ═══════════════════════════════════════════════════════════════════════

    // Separador horizontal simplificado con color del tema.
    public static JSeparator hSep(){
        JSeparator s=new JSeparator();
        s.setForeground(Theme.BORDER); s.setBackground(Theme.BORDER);
        return s;
    }
    // Espaciadores reutilizables (vertical y horizontal).
    public static Component gap(int h){ return Box.createRigidArea(new Dimension(0,h)); }
    public static Component hgap(int w){ return Box.createRigidArea(new Dimension(w,0)); }

    // ═══════════════════════════════════════════════════════════════════════
    // TABLA
    // ═══════════════════════════════════════════════════════════════════════

    // Aplica estilo consistente a tablas (header, filas, renderers).
    public static void styleTable(JTable t){
        t.setFont(Theme.FONT_BODY);
        t.setForeground(Theme.TEXT_PRIMARY);
        t.setBackground(Color.WHITE);
        t.setGridColor(Theme.BORDER);
        t.setShowVerticalLines(false);
        // Filas más compactas para evitar espacios en blanco innecesarios
        t.setRowHeight(36);
        t.setSelectionBackground(Theme.PRIMARY_LIGHT);
        t.setSelectionForeground(Theme.TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0,0));

        JTableHeader h=t.getTableHeader();
        h.setFont(Theme.FONT_LABEL);
        h.setBackground(new Color(0xf9fafb));
        h.setForeground(Theme.TEXT_PRIMARY);
        h.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Theme.BORDER));
        h.setReorderingAllowed(false);
        ((DefaultTableCellRenderer)h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(
                    JTable tbl,Object val,boolean sel,boolean foc,int row,int col){
                super.getTableCellRendererComponent(tbl,val,sel,foc,row,col);
                // menos padding horizontal y un poco de padding vertical para compactar filas
                setBorder(new EmptyBorder(6,8,6,8));
                setFont(Theme.FONT_BODY);
                if(sel){ setBackground(Theme.PRIMARY_LIGHT); setForeground(Theme.TEXT_PRIMARY); }
                else   { setBackground(row%2==0?Color.WHITE:new Color(0xfafafa)); setForeground(Theme.TEXT_PRIMARY); }
                return this;
            }
        });
    }

    // Envuelve una tabla en `JScrollPane` con bordes y fondo adecuados.
    public static JScrollPane tableScroll(JTable t){
        JScrollPane sp=new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // Envuelve un componente en `JScrollPane` transparente.
    public static JScrollPane scroll(Component c){
        JScrollPane sp=new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BARRA DE BÚSQUEDA
    // ═══════════════════════════════════════════════════════════════════════

    // Campo de búsqueda con placeholder y icono inicial.
    public static JTextField searchField(String ph){ return textField("🔍  " + ph); }

    // ═══════════════════════════════════════════════════════════════════════
    // BARRA DE PROGRESO
    // ═══════════════════════════════════════════════════════════════════════

    // Barra de progreso compacta; `enCurso` altera el color.
    public static JProgressBar progressBar(int value, int max, boolean enCurso){
        JProgressBar pb=new JProgressBar(0,max);
        pb.setValue(value);
        pb.setForeground(enCurso?Theme.PRIMARY:Theme.GREEN_TEXT);
        pb.setBackground(new Color(0xe5e7eb));
        pb.setBorderPainted(false);
        pb.setPreferredSize(new Dimension(0,8));
        return pb;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PANEL CAMPO + ETIQUETA
    // ═══════════════════════════════════════════════════════════════════════

    // Panel que muestra una etiqueta encima de un campo (label + field).
    public static JPanel labeledField(String label, JComponent field){
        JPanel p=new JPanel(new BorderLayout(0,4));
        p.setOpaque(false);
        p.add(fieldLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AVATAR CIRCULAR
    // ═══════════════════════════════════════════════════════════════════════

    // Crea un avatar circular dibujado con iniciales centradas.
    public static JLabel avatar(String initials, int size, Color bg, Color fg){
        JLabel l=new JLabel(initials, SwingConstants.CENTER){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg); g2.fillOval(0,0,getWidth(),getHeight());
                g2.setColor(fg); g2.setFont(Theme.FONT_LABEL);
                FontMetrics fm=g2.getFontMetrics();
                String t=getText();
                g2.drawString(t,(getWidth()-fm.stringWidth(t))/2,fm.getAscent()+(getHeight()-fm.getHeight())/2);
                g2.dispose();
            }
        };
        l.setPreferredSize(new Dimension(size,size));
        l.setMinimumSize(new Dimension(size,size));
        l.setOpaque(false);
        return l;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODAL DIALOG helper
    // ═══════════════════════════════════════════════════════════════════════

    /** Dialog con header azul-900 */
    // Helper para crear un `JDialog` modal base; la UI lo personaliza.
    public static JDialog modalDialog(Window parent, String title){
        JDialog d=new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(false);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        return d;
    }
}
