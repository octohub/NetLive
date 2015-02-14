package com.richardlucasapps.netlive;

public class WidgetSettings {

    private String measurementUnit;
    private boolean displayActiveApp;
    private boolean displayTotalValue;

    public WidgetSettings(String measurementUnit, boolean displayActiveApp,
                          boolean displayTotalValue) {

        this.measurementUnit = measurementUnit;
        this.displayActiveApp = displayActiveApp;
        this.displayTotalValue = displayTotalValue;

    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public boolean isDisplayActiveApp() {
        return displayActiveApp;
    }

    public boolean isDisplayTotalValue(){
        return displayTotalValue;
    }
}
