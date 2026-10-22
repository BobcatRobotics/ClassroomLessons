// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.lookup;

import static org.wpilib.units.Units.Value;

import org.wpilib.math.interpolation.Interpolator;
import org.wpilib.math.interpolation.InverseInterpolator;
import org.wpilib.units.Measure;
import org.wpilib.units.Unit;
import org.wpilib.units.measure.Dimensionless;

/** Utility class for working with unit-based interpolating tree maps. */
public final class LookupTables {
  private LookupTables() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  /**
   * Creates an interpolator operating on Measure objects.
   *
   * @param <U> The type of the unit for the measure
   * @param <M> The type of the measure
   * @return The interpolator for the measure
   */
  @SuppressWarnings("unchecked")
  public static <U extends Unit, M extends Measure<U>> Interpolator<M> unitInterpolator() {
    return (startValue, endValue, t) ->
        (M) endValue.minus(startValue).times(Math.clamp(t, 0, 1)).plus(startValue);
  }

  /**
   * Creates an inverse interpolator operating on Measure objects.
   *
   * @param <U> The type of the unit for the measure
   * @param <M> The type of the measure
   * @return The inverse interpolator for the measure
   */
  public static <U extends Unit, M extends Measure<U>>
      InverseInterpolator<M> inverseUnitInterpolator() {
    return (startValue, endValue, q) -> {
      Measure<U> totalRange = endValue.minus(startValue);
      if (totalRange.baseUnitMagnitude() <= 0) {
        return 0;
      }

      Measure<U> queryToStart = q.minus(startValue);
      if (queryToStart.baseUnitMagnitude() <= 0) {
        return 0;
      }

      return ((Dimensionless) queryToStart.div(totalRange)).in(Value);
    };
  }

  /**
 * Creates an interpolator operating on Double values.
 *
 * @return the interpolator for Double values
 */
public static Interpolator<Double> doubleInterpolator() {
  return (startValue, endValue, t) ->
      startValue + (endValue - startValue) * Math.clamp(t, 0.0, 1.0);
}

/**
 * Creates an inverse interpolator operating on Double values.
 *
 * @return the inverse interpolator for Double values
 */
public static InverseInterpolator<Double> inverseDoubleInterpolator() {
  return (startValue, endValue, query) -> {
    double range = endValue - startValue;

    if (range <= 0.0) {
      return 0.0;
    }

    return Math.clamp((query - startValue) / range, 0.0, 1.0);
  };
}
}
