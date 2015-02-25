package org.cm.podd.report.model;

/**
 * Created by sudarat on 2/24/15 AD.
 */
public class VisualizationAdministrationArea {
    private long id;
    private String name;
    private String parentName;

    private int totalReport;
    private int positiveReport;
    private int negativeReport;
    private String volunteers;
    private String animalType;
    private String timeRanges;
    private String grade;

    private int month;
    private int year;

    public VisualizationAdministrationArea(long id, String name, String parentName) {
        this.id = id;
        this.name = name;
        this.parentName = parentName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public int getTotalReport() {
        return totalReport;
    }

    public void setTotalReport(int totalReport) {
        this.totalReport = totalReport;
    }

    public int getPositiveReport() {
        return positiveReport;
    }

    public void setPositiveReport(int positiveReport) {
        this.positiveReport = positiveReport;
    }

    public int getNegativeReport() {
        return negativeReport;
    }

    public void setNegativeReport(int negativeReport) {
        this.negativeReport = negativeReport;
    }

    public String getVolunteers() {
        return volunteers;
    }

    public void setVolunteers(String volunteers) {
        this.volunteers = volunteers;
    }

    public String getAnimalType() {
        return animalType;
    }

    public void setAnimalType(String animalType) {
        this.animalType = animalType;
    }

    public String getTimeRanges() {
        return timeRanges;
    }

    public void setTimeRanges(String timeRanges) {
        this.timeRanges = timeRanges;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return name;
    }

}
