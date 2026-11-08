package coop.exception;

/**
 * استثناء مخصص لأخطاء التحقق من صحة البيانات والمدخلات.
 * يُستخدم عند مخالفة قواعد الإدخال (حقول فارغة، بيانات غير مطابقة، تكرار...).
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

