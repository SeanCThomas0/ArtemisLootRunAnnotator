/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.map.pois;

import com.wynntils.gui.render.Texture;
import com.wynntils.models.map.PoiLocation;
import com.wynntils.models.map.type.DisplayPriority;
import java.util.function.Supplier;

public class WaypointPoi extends DynamicIconPoi {
    private final PointerPoi pointer;

    public WaypointPoi(Supplier<PoiLocation> locationSupplier) {
        super(locationSupplier);
        pointer = new PointerPoi(locationSupplier);
    }

    public PointerPoi getPointerPoi() {
        return pointer;
    }

    @Override
    public Texture getIcon() {
        return Texture.WAYPOINT;
    }

    @Override
    public float getMinZoomForRender() {
        return -1f;
    }

    @Override
    public String getName() {
        return "Waypoint";
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.NORMAL;
    }
}