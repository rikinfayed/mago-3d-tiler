package com.gaia3d.basic.halfEdgeStructure;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class HalfEdgeMesh {
    @Setter
    @Getter
    private List<HalfEdgePrimitive> primitives = new ArrayList<>();

    public void doTrianglesReduction()
    {
        for (HalfEdgePrimitive primitive : primitives)
        {
            primitive.doTrianglesReduction();
        }
    }

    public void deleteObjects()
    {
        for (HalfEdgePrimitive primitive : primitives)
        {
            primitive.deleteObjects();
        }
        primitives.clear();
    }

    public void checkSandClockFaces()
    {
        for (HalfEdgePrimitive primitive : primitives)
        {
            primitive.checkSandClockFaces();
        }
    }
}
