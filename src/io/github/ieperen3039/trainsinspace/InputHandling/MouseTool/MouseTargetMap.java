package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record MouseTargetMap(Vector3fc position) implements MouseTarget {
}
