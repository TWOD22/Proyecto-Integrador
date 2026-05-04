package sigep;

import java.awt.*;

/**
 * SIGEP Design Tokens — traducidos desde theme.css
 *
 * Colores principales:
 *   Primary (azul-900) : #1e3a5f
 *   Background page    : #f3f4f6 (gray-100)
 *   Card/white         : #ffffff
 *   Border             : rgba(0,0,0,0.1) → #e5e7eb
 *   Muted bg           : #ececf0
 *   Muted text         : #717182
 */
public final class Theme {
    private Theme() {}

    // ── Colores de marca ─────────────────────────────────────────────────────
    public static final Color PRIMARY          = new Color(0x1e3a5f);  // blue-900
    public static final Color PRIMARY_HOVER    = new Color(0x1a3355);
    public static final Color PRIMARY_LIGHT    = new Color(0xdbeafe);  // blue-100
    public static final Color PRIMARY_MEDIUM   = new Color(0x93c5fd);  // blue-300

    // ── Fondos ───────────────────────────────────────────────────────────────
    public static final Color BG_PAGE         = new Color(0xf3f4f6);
    public static final Color BG_CARD         = Color.WHITE;
    public static final Color BG_INPUT        = new Color(0xf3f3f5);   // --input-background
    public static final Color BG_MUTED        = new Color(0xeceCf0);   // --muted
    public static final Color BG_SIDEBAR      = new Color(0x1e3a5f);   // azul-900
    public static final Color BG_SIDEBAR_HOVER  = new Color(0x243f6a);
    public static final Color BG_SIDEBAR_ACTIVE = new Color(0x2d4f7c);
    public static final Color BG_HEADER       = Color.WHITE;

    // ── Texto ────────────────────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY    = new Color(0x111827);
    public static final Color TEXT_SECONDARY  = new Color(0x374151);
    public static final Color TEXT_MUTED      = new Color(0x717182);   // --muted-foreground
    public static final Color TEXT_SIDEBAR    = new Color(0xbfdbfe);
    public static final Color TEXT_WHITE      = Color.WHITE;

    // ── Bordes ───────────────────────────────────────────────────────────────
    public static final Color BORDER          = new Color(0xe5e7eb);
    public static final Color BORDER_FOCUS    = new Color(0x1e3a5f);

    // ── Estado: Verde (Activo / Válido) ──────────────────────────────────────
    public static final Color GREEN_BG        = new Color(0xdcfce7);
    public static final Color GREEN_TEXT      = new Color(0x166534);
    public static final Color GREEN_BORDER    = new Color(0x86efac);

    // ── Estado: Amarillo (Pendiente) ─────────────────────────────────────────
    public static final Color YELLOW_BG       = new Color(0xfef9c3);
    public static final Color YELLOW_TEXT     = new Color(0x854d0e);
    public static final Color YELLOW_BORDER   = new Color(0xfde047);

    // ── Estado: Rojo (Inactivo / Modificar) ──────────────────────────────────
    public static final Color RED_BG          = new Color(0xfee2e2);
    public static final Color RED_TEXT        = new Color(0x991b1b);
    public static final Color RED_BORDER      = new Color(0xfca5a5);

    // ── Estado: Azul (En Curso) ──────────────────────────────────────────────
    public static final Color BLUE_BG         = new Color(0xdbeafe);
    public static final Color BLUE_TEXT       = new Color(0x1e40af);
    public static final Color BLUE_BORDER     = new Color(0x93c5fd);

    // ── Destructivo ──────────────────────────────────────────────────────────
    public static final Color DESTRUCTIVE     = new Color(0xd4183d);

    // ── Tipografía ───────────────────────────────────────────────────────────
    public static final Font FONT_H1    = new Font("SansSerif", Font.BOLD,  22);
    public static final Font FONT_H2    = new Font("SansSerif", Font.BOLD,  18);
    public static final Font FONT_H3    = new Font("SansSerif", Font.BOLD,  14);
    public static final Font FONT_BODY  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD,  12);
    public static final Font FONT_NAV   = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BADGE = new Font("SansSerif", Font.BOLD,  11);
    public static final Font FONT_MONO  = new Font("Monospaced", Font.BOLD, 11);

    // ── Layout ───────────────────────────────────────────────────────────────
    public static final int SIDEBAR_W  = 240;
    public static final int HEADER_H   = 56;
    public static final int RADIUS     = 4;   // rounded-sm ≈ 4px
    public static final int PADDING    = 24;
    public static final int GAP        = 16;
}
