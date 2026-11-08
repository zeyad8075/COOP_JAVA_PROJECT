package coop;

import coop.dao.DBConnection;
import coop.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * نقطة الانطلاق الرئيسية لتطبيق إدارة التدريب الميداني (COOP).
 * تقوم بتهيئة مظهر الواجهة (Look and Feel)، بدء اتصال قاعدة البيانات،
 * وتشغيل الإطار الرئيسي عبر Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        // 1. ضبط مظهر النظام الموحد (System Look and Feel) لدعم الخطوط والعناصر بسلاسة
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // 2. التحقق من اتصال قاعدة بيانات Derby وتهيئة الجداول مبكراً
        try {
            DBConnection.getConnection();
        } catch (Exception e) {
            System.err.println("تحذير عند بدء تشغيل قاعدة البيانات: " + e.getMessage());
        }

        // 3. تشغيل الواجهة الرسومية عبر SwingUtilities.invokeLater
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

