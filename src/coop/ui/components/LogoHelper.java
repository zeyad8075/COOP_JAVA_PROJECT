package coop.ui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * فئة مساعدة لتحميل ورسم شعار الجامعة (University_Logo.svg).
 * تدعم قراءة ملف SVG ومعالجته بدقة وجودة عالية عبر مكتبة JSVG،
 * مع توفير مسار احتياطي لقراءة النسخة النقطية PNG تلقائياً عند الحاجة.
 */
public class LogoHelper {

    private static final String SVG_PATH = "University_Logo.svg";
    private static final String PNG_PATH = "University_Logo.png";

    private static ImageIcon cachedHeaderIcon = null;

    private LogoHelper() {
    }

    /**
     * استرجاع أيقونة الشعار بحجم مخصص مع الحفاظ على النسبة والتناسب الأصلية.
     *
     * @param targetHeight الارتفاع المطلوب للصورة بالبكسل.
     * @return كائن ImageIcon للشعار أو null في حال عدم العثور عليه.
     */
    public static ImageIcon getLogoIcon(int targetHeight) {
        if (cachedHeaderIcon != null && cachedHeaderIcon.getIconHeight() == targetHeight) {
            return cachedHeaderIcon;
        }

        BufferedImage img = null;

        // 1. محاولة قراءة ملف SVG مباشرة بجودة متجهة عبر JSVG
        File svgFile = new File(SVG_PATH);
        if (svgFile.exists()) {
            try {
                com.github.weisj.jsvg.parser.SVGLoader loader = new com.github.weisj.jsvg.parser.SVGLoader();
                com.github.weisj.jsvg.SVGDocument doc = loader.load(svgFile.toURI().toURL());
                if (doc != null) {
                    float originalWidth = doc.size().width;
                    float originalHeight = doc.size().height;
                    double ratio = originalWidth / (double) originalHeight;
                    int targetWidth = (int) Math.round(targetHeight * ratio);

                    img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = img.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                    doc.render((java.awt.Component) null, g2, new com.github.weisj.jsvg.attributes.ViewBox(0, 0, targetWidth, targetHeight));
                    g2.dispose();
                }
            } catch (Throwable t) {
                System.err.println("ملاحظة: تعذر تحميل ملف SVG مباشرة: " + t.getMessage());
            }
        }

        // 2. المحاولة البديلة: قراءة ملف PNG إذا وُجد
        if (img == null) {
            File pngFile = new File(PNG_PATH);
            if (pngFile.exists()) {
                try {
                    BufferedImage raw = ImageIO.read(pngFile);
                    if (raw != null) {
                        double ratio = raw.getWidth() / (double) raw.getHeight();
                        int targetWidth = (int) Math.round(targetHeight * ratio);
                        Image scaled = raw.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                        cachedHeaderIcon = new ImageIcon(scaled);
                        return cachedHeaderIcon;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (img != null) {
            cachedHeaderIcon = new ImageIcon(img);
            return cachedHeaderIcon;
        }

        return null;
    }

    /**
     * إنشاء حاوية مخصصة للشعار بزوايا دائرية وخلفية بيضاء نقية
     * لإبراز ألوان شعار الجامعة الرسمية (الأزرق والسماوي) على شريط الهيدر الداكن.
     *
     * @param targetHeight ارتفاع الشعار.
     * @return كائن JPanel جاهز للعرض في الهيدر.
     */
    public static JPanel createHeaderLogoBadge(int targetHeight) {
        ImageIcon icon = getLogoIcon(targetHeight);
        if (icon == null) {
            return null;
        }

        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 3));
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel lbl = new JLabel(icon);
        lbl.setToolTipText("جامعة طيبة — كلية إدارة الأعمال بينبع");
        badge.add(lbl);

        return badge;
    }
}

