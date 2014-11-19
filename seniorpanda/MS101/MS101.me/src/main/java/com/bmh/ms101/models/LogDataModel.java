package com.bmh.ms101.models;

import java.util.List;

public class LogDataModel extends BaseDataModel {
    List<TakenDataModel> medsTakenData = null;
    List<SymptomDataModel> symptomsData = null;

    public List<TakenDataModel> getMedsTakenData() {
        return medsTakenData;
    }

    public void setMedsTakenData(List<TakenDataModel> medsTakenData) {
        this.medsTakenData = medsTakenData;
    }

    public List<SymptomDataModel> getSymptomsData() {
        return symptomsData;
    }

    public void setSymptomsData(List<SymptomDataModel> symptomsData) {
        this.symptomsData = symptomsData;
    }
}
