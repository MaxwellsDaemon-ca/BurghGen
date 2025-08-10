package com.chancema.burghgen.util;

import com.chancema.burghgen.model.RoadStyle;

public class RoadTileLookup {
    public static int getTileIdForStyle(RoadStyle style) {
        switch (style) {
            case FIELD_TAN:
                return 571; 
            case COBBLE_LIGHTGRAY:
                return 563; 
            case COBBLE_GRAY:
                return 112; 
            default:
                throw new IllegalArgumentException("Unknown road style: " + style);
        }
    }
}
