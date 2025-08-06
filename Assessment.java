package dashboard;

import java.io.Serializable;

class Assessment {
    String title;
    String course;
    String dueDate;

    Assessment(String title, String course, String dueDate) {
        this.title = title;
        this.course = course;
        this.dueDate = dueDate;
    }
}
