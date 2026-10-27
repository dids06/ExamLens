package com.examlens.model;

public class ExamAvailability {
    private long id;
    private long examId;
    private Integer dayOfWeek;
    private java.time.LocalDate specificDate;
    private int availableMinutes;
    private String label;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public java.time.LocalDate getSpecificDate() { return specificDate; }
    public void setSpecificDate(java.time.LocalDate specificDate) { this.specificDate = specificDate; }
    public int getAvailableMinutes() { return availableMinutes; }
    public void setAvailableMinutes(int availableMinutes) { this.availableMinutes = availableMinutes; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDayLabel() {
        if (specificDate != null) {
            return specificDate.toString();
        }
        if (dayOfWeek == null) {
            return "Any day";
        }
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? days[dayOfWeek] : "Day " + dayOfWeek;
    }

    public String getHoursDisplay() {
        int h = availableMinutes / 60;
        int m = availableMinutes % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }
}
