package com.chancema.burghgen.model;

public enum MapSize {
    SMALL, MEDIUM, LARGE;

    public static MapSize fromDimensions(int width, int height) {
        int area = width * height;
        if (area <= 64 * 64) return SMALL;
        if (area <= 128 * 128) return MEDIUM;
        return LARGE;
    }
}

