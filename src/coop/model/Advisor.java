package coop.model;

/**
 * فئة تمثل المرشد الأكاديمي (Advisor).
 * ترث من الفئة المجردة Person وتطبق مبدأ الوراثة والتغليف.
 */
public class Advisor extends Person {

    public Advisor() {
        super();
    }

    public Advisor(int id, String name) {
        super(id, name);
    }

    public Advisor(String name) {
        super(name);
    }

    public int getAdvisorId() {
        return getId();
    }

    public void setAdvisorId(int advisorId) {
        setId(advisorId);
    }

    public String getAdvisorName() {
        return getName();
    }

    public void setAdvisorName(String advisorName) {
        setName(advisorName);
    }
}

