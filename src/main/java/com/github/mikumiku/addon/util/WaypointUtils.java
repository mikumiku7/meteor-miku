package com.github.mikumiku.addon.util;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

public class WaypointUtils {

    public static void addToWaypoints(int x, int y, int z, String name) {
        WaypointSet waypointSet = getWaypointSet();
        if (waypointSet == null) return;

        // dont add waypoint that already exists
        if (getWaypointByCoordinate(x, z) != null) return;

        String waypointName = "翅";

        // set color based on total storage blocks
        int color = 10; // green
        //13 紫色

        Waypoint waypoint = new Waypoint(
            x,
            y,
            z,
            name,
            waypointName,
            color,
            0,
            false);

        waypointSet.add(waypoint);
    }

    public static Waypoint getWaypointByCoordinate(int x, int z) {
        WaypointSet waypointSet = getWaypointSet();
        if (waypointSet == null) return null;
        for (Waypoint waypoint : waypointSet.getWaypoints()) {
            if (waypoint.getX() == x && waypoint.getZ() == z) {
                return waypoint;
            }
        }
        return null;
    }


    public static WaypointSet getWaypointSet() {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return null;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return null;
        return currentWorld.getCurrentWaypointSet();
    }


}
