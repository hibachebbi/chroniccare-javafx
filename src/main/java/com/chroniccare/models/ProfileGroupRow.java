package com.chroniccare.models;

public class ProfileGroupRow {

    private final String groupName;
    private final int patientCount;
    private final String samplePatients;

    public ProfileGroupRow(String groupName, int patientCount, String samplePatients) {
        this.groupName = groupName;
        this.patientCount = patientCount;
        this.samplePatients = samplePatients;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getPatientCount() {
        return patientCount;
    }

    public String getSamplePatients() {
        return samplePatients;
    }
}

