package coop.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * فئة تمثل جهة أو مؤسسة التدريب الميداني (Organization).
 * تطبق مبدأ التغليف (Encapsulation).
 */
public class Organization implements Serializable {

    private int orgId;
    private String orgName;

    public Organization() {
    }

    public Organization(int orgId, String orgName) {
        this.orgId = orgId;
        this.orgName = orgName;
    }

    public Organization(String orgName) {
        this.orgName = orgName;
    }

    public int getOrgId() {
        return orgId;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization)) return false;
        Organization that = (Organization) o;
        return orgId == that.orgId && Objects.equals(orgName, that.orgName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, orgName);
    }

    @Override
    public String toString() {
        return orgName != null ? orgName : "";
    }
}

