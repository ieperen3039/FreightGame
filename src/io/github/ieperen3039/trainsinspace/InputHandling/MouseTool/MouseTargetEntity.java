package io.github.ieperen3039.trainsinspace.InputHandling.MouseTool;

import io.github.ieperen3039.ngn.InputHandling.MouseTools.MouseTarget;
import io.github.ieperen3039.trainsinspace.Entities.GameEntity;

public record MouseTargetEntity(GameEntity target) implements MouseTarget {
}
