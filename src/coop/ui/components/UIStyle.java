package coop.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * فئة مركزية لتوحيد ألوان وخطوط وأنماط واجهات Swing ودعم اللغة العربية (RTL).
 * تضمن مظهراً عصرياً وهادئاً ومناسباً للعرض الأكاديمي الاحترافي.
 */
public class UIStyle {

    // الخطوط العربية
    public static final Font FONT_TITLE = new Font("Tahoma", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Tahoma", Font.BOLD, 14);
    public static final Font FONT_BOLD = new Font("Tahoma", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Tahoma", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Tahoma", Font.PLAIN, 11);

    // الألوان
    public static final Color COLOR_PRIMARY = new Color(26, 75, 132);       // أزرق ملكي كلاسيكي
    public static final Color COLOR_PRIMARY_DARK = new Color(18, 52, 92);
    public static final Color COLOR_ACCENT = new Color(41, 128, 185);
    public static final Color COLOR_SUCCESS = new Color(39, 174, 96);       // أخضر نجاح
    public static final Color COLOR_WARNING = new Color(230, 126, 34);      // برتقالي تنبيه
    public static final Color COLOR_DANGER = new Color(192, 57, 43);        // أحمر حذف/خطأ
    public static final Color COLOR_BG = new Color(245, 247, 250);          // خلفية رمادية فاتحة جداً
    public static final Color COLOR_CARD_BG = Color.WHITE;
    public static final Color COLOR_TEXT_DARK = new Color(44, 62, 80);
    public static final Color COLOR_TEXT_MUTED = new Color(127, 140, 141);
    public static final Color COLOR_BORDER = new Color(220, 224, 230);

    private UIStyle() {
    }

    /**
     * تطبيق اتجاه اليمين لليسار (RTL) والخط المناسب على أي مكون وحاوياته الفرعية.
     */
    public static void applyRTL(Component c) {
        c.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        c.setFont(FONT_REGULAR);
        if (c instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) c).getComponents()) {
                applyRTL(child);
            }
        }
    }

    /**
     * إنشاء زر منسق ومخصص.
     */
    public static JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * إنشاء بطاقة إحصائية منسقة (Card Panel).
     */
    public static JPanel createStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // شريط تزييني على الجانب الأيمن للبطاقة
                g2.setColor(accentColor);
                g2.fillRect(getWidth() - 6, 0, 6, getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new java.awt.BorderLayout(5, 5));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_SUBTITLE);
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Tahoma", Font.BOLD, 26));
        lblValue.setForeground(COLOR_TEXT_DARK);

        card.add(lblTitle, java.awt.BorderLayout.NORTH);
        card.add(lblValue, java.awt.BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(200, 90));
        applyRTL(card);
        return card;
    }

    /**
     * تنسيق جداول JTable لتكون باللغة العربية ومحاذاة اليمين.
     */
    public static void formatTable(JTable table) {
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setSelectionBackground(new Color(214, 234, 248));
        table.setSelectionForeground(COLOR_TEXT_DARK);
        table.setShowGrid(true);
        table.setGridColor(new Color(235, 237, 240));

        // ترويسة الجدول
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(new Color(238, 242, 246));
        table.getTableHeader().setForeground(COLOR_TEXT_DARK);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // محاذاة خلايا الجدول لليمين
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
    }
}

