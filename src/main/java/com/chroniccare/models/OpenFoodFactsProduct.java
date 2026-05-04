package com.chroniccare.models;

public class OpenFoodFactsProduct {
    private String productName;
    private String brands;
    private String nutriScoreGrade;
    private String ingredientsText;
    private Double energyKcal100g;
    private Double sugars100g;
    private Double fat100g;
    private Double proteins100g;
    private Double fiber100g;
    private String imageUrl;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrands() {
        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getNutriScoreGrade() {
        return nutriScoreGrade;
    }

    public void setNutriScoreGrade(String nutriScoreGrade) {
        this.nutriScoreGrade = nutriScoreGrade;
    }

    public String getIngredientsText() {
        return ingredientsText;
    }

    public void setIngredientsText(String ingredientsText) {
        this.ingredientsText = ingredientsText;
    }

    public Double getEnergyKcal100g() {
        return energyKcal100g;
    }

    public void setEnergyKcal100g(Double energyKcal100g) {
        this.energyKcal100g = energyKcal100g;
    }

    public Double getSugars100g() {
        return sugars100g;
    }

    public void setSugars100g(Double sugars100g) {
        this.sugars100g = sugars100g;
    }

    public Double getFat100g() {
        return fat100g;
    }

    public void setFat100g(Double fat100g) {
        this.fat100g = fat100g;
    }

    public Double getProteins100g() {
        return proteins100g;
    }

    public void setProteins100g(Double proteins100g) {
        this.proteins100g = proteins100g;
    }

    public Double getFiber100g() {
        return fiber100g;
    }

    public void setFiber100g(Double fiber100g) {
        this.fiber100g = fiber100g;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String toRecommendationSnippet() {
        StringBuilder builder = new StringBuilder();
        builder.append(valueOrDash(productName));
        if (brands != null && !brands.isBlank()) {
            builder.append(" | Marque: ").append(brands.trim());
        }
        if (energyKcal100g != null) {
            builder.append(" | kcal/100g: ").append(trimNumber(energyKcal100g));
        }
        if (sugars100g != null) {
            builder.append(" | sucres/100g: ").append(trimNumber(sugars100g)).append(" g");
        }
        if (fat100g != null) {
            builder.append(" | lipides/100g: ").append(trimNumber(fat100g)).append(" g");
        }
        if (proteins100g != null) {
            builder.append(" | proteines/100g: ").append(trimNumber(proteins100g)).append(" g");
        }
        if (nutriScoreGrade != null && !nutriScoreGrade.isBlank()) {
            builder.append(" | Nutri-Score: ").append(nutriScoreGrade.toUpperCase());
        }
        return builder.toString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String trimNumber(Double value) {
        if (value == null) {
            return "-";
        }
        if (Math.rint(value) == value) {
            return String.valueOf(value.intValue());
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
