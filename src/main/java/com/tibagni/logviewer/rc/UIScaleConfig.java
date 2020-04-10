package com.tibagni.logviewer.rc;

import com.tibagni.logviewer.util.scaling.UIScaleUtils;

public class UIScaleConfig implements Config<Integer> {
    public static final int SCALE_OFF = -1;
    private final int scaleValue;

    UIScaleConfig(String value) {
        scaleValue = parseValue(value);
    }

    private int parseValue(String value) {
        if ("auto".equalsIgnoreCase(value)) {
            return UIScaleUtils.getSystemScalingCalculator().calculateScaling();
        } else {
            try {
                int parsedValue = Integer.parseInt(value);
                if (parsedValue > 0) {
                    return parsedValue;
                }
            } catch (NumberFormatException nfe) { }
        }

        return SCALE_OFF;
    }

    @Override
    public Integer getConfigValue() {
        return scaleValue;
    }
}
