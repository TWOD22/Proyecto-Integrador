package sigep;

import javax.swing.*;
import java.awt.*;

public class BadgeView extends JPanel implements Runnable {
    private final StatusModel model;
    private JLabel inner;
    public BadgeView(StatusModel model, boolean compact){
        this.model = model;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT,0,0));
        // Let the badge size be determined by its content so it adapts to text length.
        // Keep compact flag for potential future tweaks but do not enforce fixed size.
        model.addListener(this);
        updateState();
    }

    @Override public void run(){ SwingUtilities.invokeLater(this::updateState); }

    public void updateState(){
        try {
            int s = model.getEstado();
            if (inner != null) { remove(inner); inner = null; }
            String txt; Color bg, fg, bd;
            if (s == 3) { inner = UIFactory.badgeRealizando(); }
            else if (s == 2) { txt = "✓ Listo"; bg = Theme.GREEN_BG; fg = Theme.GREEN_TEXT; bd = Theme.GREEN_BORDER; inner = UIFactory.badge(txt,bg,fg,bd); }
            else if (s == 1) { txt = "⏳ Esperando asignación"; bg = Theme.BLUE_BG; fg = Theme.BLUE_TEXT; bd = Theme.BLUE_BORDER; inner = UIFactory.badge(txt, bg, fg, bd); }
            else { txt = "⏳ Pendiente por documentos"; bg = Theme.YELLOW_BG; fg = Theme.YELLOW_TEXT; bd = Theme.YELLOW_BORDER; inner = UIFactory.badge(txt, bg, fg, bd); }
            add(inner);
            revalidate(); repaint();
        } catch (Exception ignored) {}
    }

    public void dispose(){ model.removeListener(this); }
}
