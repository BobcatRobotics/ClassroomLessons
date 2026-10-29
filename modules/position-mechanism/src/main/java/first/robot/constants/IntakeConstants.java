package first.robot.constants;

import static org.wpilib.units.Units.Degrees;

import org.wpilib.units.measure.Angle;

public final class IntakeConstants {
  public final class Roller {
    /** CAN device ID for the Intake Roller motor. */
    public static final int CAN_ROLLER_ID = 2;
  }

  public final class Pivot {
    public static final int CAN_ID = 1;
    public static final Angle MIN_ANGLE = Degrees.of(0.0);
    public static final Angle MAX_ANGLE = Degrees.of(90.0);
    public static final int ENCODER_CAN_ID = 20;
    public static final double GEAR_RATIO = 60.0;
    public static final double KP = 150.0;
    public static final double KI = 0.0;
    public static final double KD = 30.0;
    public static final double STATOR_CURRENT_LIMIT_AMPS = 40.0;
    public static final double ENCODER_OFFSET_ROTATIONS = 0.0;
    public static final double ENCODER_RATIO = 1.0;
    public static final double TRAVEL_MIDDLE_ROTATIONS = 0.125;
  }
}
