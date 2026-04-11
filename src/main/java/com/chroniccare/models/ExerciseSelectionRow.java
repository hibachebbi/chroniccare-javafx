package com.chroniccare.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ExerciseSelectionRow {
    private final Exercise exercise;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public ExerciseSelectionRow(Exercise exercise) {
        this.exercise = exercise;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public String getNom() {
        return exercise.getNom();
    }

    public int getDuree() {
        return exercise.getDuree();
    }

    public String getRepetitionsDisplay() {
        return exercise.getRepetitions() != null ? String.valueOf(exercise.getRepetitions()) : "-";
    }

    public String getEvenementTitreDisplay() {
        return exercise.getEvenementTitre() != null ? exercise.getEvenementTitre() : "Disponible";
    }
}
