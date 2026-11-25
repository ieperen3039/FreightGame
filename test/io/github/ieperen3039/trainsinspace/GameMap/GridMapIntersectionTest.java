package io.github.ieperen3039.trainsinspace.GameMap;

import org.joml.Vector3f;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class GridMapIntersectionTest {
    private static final float DELTA = 1e-4f;
    private HeightMap map;

    @Before
    public void setUp() throws Exception {
        map = new HeightMap();

        // Five vertices per axis, spaced 0.5 apart: map spans [0, 2]².
        setField("heightmap", new float[5][5]);
        setField("xSize", 5);
        setField("ySize", 5);
    }

    // circumvent mesh generation using reflection
    private void setField(String name, Object value) throws Exception {
        Field field = HeightMap.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(map, value);
    }

    private void assertHit(Vector3f origin, Vector3f direction,
                           float expectedT, Vector3f expectedPoint) {
        Float t = map.gridMapIntersection(origin, direction);

        assertNotNull("Expected a map intersection", t);
        assertEquals(expectedT, t, DELTA);

        Vector3f point = new Vector3f(direction).mul(t).add(origin);
        assertEquals("point.x", expectedPoint.x, point.x, DELTA);
        assertEquals("point.y", expectedPoint.y, point.y, DELTA);
        assertEquals("point.z", expectedPoint.z, point.z, DELTA);
    }

    @Test
    public void rayIsNotTracedBeyondDirectionLength() {
        assertNull(map.gridMapIntersection(
                new Vector3f(0f, -0.5f, 2f),
                new Vector3f(0f, 0.5f, -1f)));
    }

    @Test
    public void angledRayStartingAboveMap() {
        assertHit(new Vector3f(0.5f, 0.25f, 3f),
                new Vector3f(1f, 0, -4f),
                0.75f, new Vector3f(1.25f, 0.25f, 0f));
    }

    @Test
    public void rayEntersFromOutsideMap() {
        assertHit(new Vector3f(-0.5f, 0.25f, 3f),
                new Vector3f(2f, 0f, -4f),
                0.75f, new Vector3f(1.0f, 0.25f, 0f));
    }

//    @Test
//    public void rayHitsOnOuterBoundary() {
//        assertHit(new Vector3f(2.5f, 0.25f, 0.5f),
//                new Vector3f(-1f, 0f, -1f),
//                0.5f, new Vector3f(2f, 0.25f, 0f));
//    }

    @Test
    public void verticalRayOutsideMapMisses() {
        assertNull(map.gridMapIntersection(
                new Vector3f(2.1f, 0.25f, 1f),
                new Vector3f(0f, 0f, -2f)));
    }

    @Test
    public void rayPointingAwayFromMapMisses() {
        assertNull(map.gridMapIntersection(
                new Vector3f(-0.5f, 0.25f, 1f),
                new Vector3f(-1f, 0f, -2f)));
    }
}