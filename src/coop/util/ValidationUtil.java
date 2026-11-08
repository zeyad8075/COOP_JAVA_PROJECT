package coop.util;

import java.util.regex.Pattern;

/**
 * فئة مساعدة للتحقق من صحة البيانات وتنظيف النصوص العربية وإزالة المسافات الزائدة،
 * وتوفير خوارزميات مقارنة النصوص لحساب درجة التشابه واكتشاف الأخطاء الإملائية.
 */
public class ValidationUtil {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("[\\u064B-\\u0652\\u0670\\u0640]"); // التشكيل والتطويل

    private ValidationUtil() {
    }

    /**
     * تنظيف النص من المسافات الزائدة في البداية والنهاية والمسافات المتعددة بين الكلمات،
     * وإزالة محارف التحكم الخفية ومحرف BOM.
     */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        // إزالة BOM ومحارف المسافات غير المرئية
        String s = text.replace("\uFEFF", "")
                       .replace("\u200B", "")
                       .replace("\u00A0", " ");
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /**
     * التحقق مما إذا كان النص فارغاً أو يحتوي مسافات فقط.
     */
    public static boolean isEmpty(String text) {
        return text == null || clean(text).isEmpty();
    }

    /**
     * توحيد النص العربي للمقارنة الذكية (Normalize for Comparison).
     * - توحيد أشكال الهمزات (أ، إ، آ -> ا).
     * - توحيد التاء المربوطة والهاء في النهاية (ة -> ه).
     * - توحيد الألف المقصورة والياء (ى -> ي).
     * - إزالة علامات التشكيل وحركات الإعراب والتطويل.
     */
    public static String normalizeArabicForComparison(String text) {
        if (text == null) return "";
        String s = clean(text).toLowerCase();
        // إزالة التشكيل والتطويل
        s = DIACRITICS.matcher(s).replaceAll("");
        // توحيد الألف
        s = s.replace('أ', 'ا')
             .replace('إ', 'ا')
             .replace('آ', 'ا');
        // توحيد الياء والألف المقصورة
        s = s.replace('ى', 'ي');
        // توحيد التاء المربوطة
        s = s.replace('ة', 'ه');
        return s;
    }

    /**
     * حساب درجة التشابه بين نصين (0.0 تعني اختلاف تام، 1.0 تعني تطابق تام).
     * تعتمد على خوارزمية Levenshtein Distance بعد توحيد المحارف العربية.
     */
    public static double calculateSimilarity(String s1, String s2) {
        String n1 = normalizeArabicForComparison(s1);
        String n2 = normalizeArabicForComparison(s2);

        if (n1.equals(n2)) return 1.0;
        if (n1.isEmpty() || n2.isEmpty()) return 0.0;

        int distance = computeLevenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * خوارزمية Levenshtein Distance لحساب عدد التعديلات اللازمة لتحويل نص إلى آخر.
     */
    public static int computeLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(
                    1 + Math.min(costs[j], costs[j - 1]),
                    s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    /**
     * التحقق من الاسم: ألا يكون فارغاً ويحتوي على حرفين على الأقل.
     */
    public static boolean isValidName(String name) {
        String c = clean(name);
        return c.length() >= 2;
    }
}

