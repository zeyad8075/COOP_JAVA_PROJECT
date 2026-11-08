package coop.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * فئة مجردة تمثل الشخص، تجسد مفهوم الوراثة (Inheritance) والتغليف (Encapsulation).
 * تجمع الخصائص المشتركة بين الطالب والمرشد الأكاديمي (المعرف والاسم).
 */
public abstract class Person implements Serializable {

    private int id;
    private String name;

    public Person() {
    }

    public Person(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Person(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        return id == person.id && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}

