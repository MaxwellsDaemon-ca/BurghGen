package com.chancema.burghgen.generation.road;

import java.awt.Point;
import java.util.Objects;

/**
 * Represents a node in the road network, such as the town center, a gate, or a district hub.
 * These are connected via road paths during generation.
 */
public class RoadNode {

    /**
     * The type of node this represents.
     */
    public enum NodeType {
        TOWN_CENTER,
        GATE,
        DISTRICT_CENTER,
        LANDMARK,
        RANDOM
    }

    private final int x;
    private final int y;
    private final NodeType type;

    /**
     * Constructs a RoadNode with the given position and type.
     *
     * @param x    X coordinate on the map grid
     * @param y    Y coordinate on the map grid
     * @param type Type of the node (e.g., TOWN_CENTER, GATE)
     */
    public RoadNode(int x, int y, NodeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * Convenience method for creating a RoadNode from a Point.
     */
    public RoadNode(Point point, NodeType type) {
        this(point.x, point.y, type);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public NodeType getType() {
        return type;
    }

    /**
     * Returns this node’s position as a Point object.
     */
    public Point toPoint() {
        return new Point(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoadNode)) return false;
        RoadNode node = (RoadNode) o;
        return x == node.x && y == node.y && type == node.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, type);
    }

    @Override
    public String toString() {
        return String.format("RoadNode[%s at (%d,%d)]", type, x, y);
    }
}
