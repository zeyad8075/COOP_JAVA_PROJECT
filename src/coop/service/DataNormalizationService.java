package coop.service;

import coop.util.ValidationUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * خدمة ذكية لاكتشاف القيم المرجعية المتشابهة واقتراح توحيدها للمستخدم.
 * تكتشف الاختلافات الإملائية ورسم الهمزات والتاء المربوطة والمسافات (مثل: الغرفة التجارية vs الغرفه التجاريه).
 * لا تدمج أي قيمتين تلقائياً إلا بعد موافقة صريحة من المستخدم.
 */
public class DataNormalizationService {

    private static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * فحص مجموعة من القيم واكتشاف التشابه فيما بينها ومع القيم المخزنة مسبقاً في قاعدة البيانات.
     *
     * @param incomingValues القيم المستخرجة من ملف البيانات.
     * @param existingDbValues القيم الموجودة مسبقاً في قاعدة البيانات.
     * @return خريطة باقتراحات التوحيد: (الاسم القياسي المقترح -> قائمة بالصيغ المختلفة المتشابهة معه).
     */
    public Map<String, List<String>> findSimilarGroups(Collection<String> incomingValues, Collection<String> existingDbValues) {
        Map<String, List<String>> suggestions = new LinkedHashMap<>();
        if (incomingValues == null || incomingValues.isEmpty()) {
            return suggestions;
        }

        // إزالة الفراغات والتكرارات الأولية
        Set<String> uniqueIncoming = new LinkedHashSet<>();
        for (String v : incomingValues) {
            String cln = ValidationUtil.clean(v);
            if (!cln.isEmpty()) {
                uniqueIncoming.add(cln);
            }
        }

        // مقارنة القيم الواردة مع قيم قاعدة البيانات الموجودة مسبقاً
        Set<String> matchedIncoming = new LinkedHashSet<>();
        if (existingDbValues != null) {
            for (String inc : uniqueIncoming) {
                for (String dbVal : existingDbValues) {
                    if (inc.equalsIgnoreCase(dbVal)) continue; // متطابق تماماً
                    if (isSimilar(inc, dbVal)) {
                        suggestions.computeIfAbsent(dbVal, k -> new ArrayList<>()).add(inc);
                        matchedIncoming.add(inc);
                        break;
                    }
                }
            }
        }

        // فحص التشابه الداخلي بين القيم الواردة في الملف نفسه (التي لم تُطابق مع قاعدة البيانات)
        List<String> remaining = new ArrayList<>(uniqueIncoming);
        remaining.removeAll(matchedIncoming);

        boolean[] visited = new boolean[remaining.size()];
        for (int i = 0; i < remaining.size(); i++) {
            if (visited[i]) continue;

            String base = remaining.get(i);
            List<String> group = new ArrayList<>();

            for (int j = i + 1; j < remaining.size(); j++) {
                if (!visited[j]) {
                    String other = remaining.get(j);
                    if (isSimilar(base, other)) {
                        group.add(other);
                        visited[j] = true;
                    }
                }
            }

            if (!group.isEmpty()) {
                visited[i] = true;
                group.add(0, base);
                String bestCandidate = pickBestStandardSpelling(group);
                suggestions.put(bestCandidate, group);
            }
        }

        return suggestions;
    }

    /**
     * التحقق مما إذا كان النصان متشابهين بصورة تستوجب مراجعة التوحيد.
     */
    public boolean isSimilar(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String cln1 = ValidationUtil.clean(s1);
        String cln2 = ValidationUtil.clean(s2);
        if (cln1.isEmpty() || cln2.isEmpty() || cln1.equalsIgnoreCase(cln2)) return false;

        // 1. تطابق كامل بعد توحيد المحارف العربية (همزات، تاء مربوطة، ياء مقصورة)
        String norm1 = ValidationUtil.normalizeArabicForComparison(cln1);
        String norm2 = ValidationUtil.normalizeArabicForComparison(cln2);
        if (norm1.equals(norm2)) {
            return true;
        }

        // 2. نسبة تشابه عالية بناءً على مسافة التعديل (Levenshtein)
        double sim = ValidationUtil.calculateSimilarity(cln1, cln2);
        return sim >= SIMILARITY_THRESHOLD;
    }

    /**
     * اختيار الصيغة الأصح إملائياً من بين مجموعة الصيغ المتشابهة (تفضيل التاء المربوطة ة والهمزات الصحيحة).
     */
    private String pickBestStandardSpelling(List<String> candidates) {
        String best = candidates.get(0);
        int highestScore = scoreSpelling(best);

        for (int i = 1; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            int score = scoreSpelling(candidate);
            if (score > highestScore) {
                highestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * تقييم جودة الكتابة الإملائية (منح درجات للهمزات الصحيحة والتاء المربوطة).
     */
    private int scoreSpelling(String s) {
        int score = 0;
        for (char c : s.toCharArray()) {
            if (c == 'أ' || c == 'إ' || c == 'آ') score += 2;
            if (c == 'ة') score += 3;
            if (c == 'ي') score += 1;
        }
        return score;
    }

    /**
     * استرجاع القيمة النهائية بعد تطبيق التوحيدات المعتمدة من المستخدم.
     */
    public String resolveNormalizedValue(String rawValue, Map<String, String> approvedMappings) {
        String cleaned = ValidationUtil.clean(rawValue);
        if (approvedMappings != null && approvedMappings.containsKey(cleaned)) {
            return approvedMappings.get(cleaned);
        }
        return cleaned;
    }
}

