package coop.dao;

import java.util.List;

/**
 * واجهة عامة عامية (Generic DAO Interface) تمثل عمليات الوصول للبيانات الأساسية.
 * تجسد مفاهيم التجريد (Abstraction) والتعددية (Polymorphism) في هندسة البرمجيات.
 *
 * @param <T> نوع الكيان الممثل في قاعدة البيانات.
 */
public interface Dao<T> {

    /**
     * البحث عن كيان بواسطة معرفه الأساسي.
     *
     * @param id المعرف الأساسي للكيان.
     * @return الكيان المطابق أو null إذا لم يوجد.
     */
    T findById(int id);

    /**
     * استرجاع جميع السجلات المخزنة في الجدول.
     *
     * @return قائمة بجميع الكيانات.
     */
    List<T> findAll();

    /**
     * إدخال كيان جديد في قاعدة البيانات وتحديث معرفه التلقائي.
     *
     * @param entity الكيان المراد إدخاله.
     * @return الكيان بعد حفظه مع المعرف المولد تلقائياً.
     */
    T insert(T entity);

    /**
     * تعديل بيانات كيان موجود مسبقاً.
     *
     * @param entity الكيان مع البيانات المحدثة.
     * @return true إذا تم التعديل بنجاح، false إذا لم يوجد السجل.
     */
    boolean update(T entity);

    /**
     * حذف سجل بواسطة معرفه الأساسي.
     *
     * @param id المعرف الأساسي للسجل المراد حذفه.
     * @return true إذا تم الحذف بنجاح، false خلاف ذلك.
     */
    boolean delete(int id);
}

