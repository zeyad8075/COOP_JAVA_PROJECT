package coop.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * فئة تمثل التخصص الأكاديمي (Specialization).
 * تطبق مبدأ التغليف (Encapsulation).
 */
public class Specialization implements Serializable {

    private int specId;
    private String specName;

    public Specialization() {
    }

    public Specialization(int specId, String specName) {
        this.specId = specId;
        this.specName = specName;
    }

    public Specialization(String specName) {
        this.specName = specName;
    }

    public int getSpecId() {
        return specId;
    }

    public void setSpecId(int specId) {
        this.specId = specId;
    }

    public String getSpecName() {
        return specName;
    }

    public void setSpecName(String specName) {
        this.specName = specName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Specialization)) return false;
        Specialization that = (Specialization) o;
        return specId == that.specId && Objects.equals(specName, that.specName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(specId, specName);
    }

    @Override
    public String toString() {
        return specName != null ? specName : "";
    }
}

