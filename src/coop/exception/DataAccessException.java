package coop.exception;

/**
 * استثناء مخصص لأخطاء الوصول إلى قاعدة البيانات وعمليات JDBC.
 * يُستخدم لتغليف أخطاء SQLException وتوفير رسائل واضحة ومفهومة.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

