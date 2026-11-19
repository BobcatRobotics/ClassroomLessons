package first.robot.constants;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Degrees;

import org.wpilib.units.measure.Angle;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

public final class IntakeConstants {
  public final class Roller {
    /** CAN device ID for the Intake Roller motor. */
    public static final int CAN_ROLLER_ID = 2;
    public static final int ENCODER_CAN_ID = 20;
    public static final double GEAR_RATIO = 60.0;
    public static final double KP = 150.0;
    public static final double KI = 0.0;
    public static final double KD = 30.0;
    public static final double STATOR_CURRENT_LIMIT_AMPS = 40.0;
    public static final double ENCODER_OFFSET_ROTATIONS = 0.0;
    public static final double ENCODER_RATIO = 1.0;
    public static final boolean BRAKE_MODE = false;
    /** Flywheel moment of inertia in kg m^2. */
    public static final double MOMENT_OF_INERTIA_KG_M2 = 0.0025;

    private Roller() {
      throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates the common TalonFX configuration.
     *
     * @return configured TalonFX settings
     */
    public static TalonFXConfiguration createMotorConfiguration() {
      TalonFXConfiguration config = new TalonFXConfiguration();

      config.Feedback.withSensorToMechanismRatio(GEAR_RATIO);
      config.Slot0.withKP(KP).withKI(KI).withKD(KD);
      config.Slot0.withKS(0.0).withKV(0.0).withKA(0.0);
      config.MotorOutput.withNeutralMode(
          BRAKE_MODE ? NeutralModeValue.Brake : NeutralModeValue.Coast);
      config.CurrentLimits
          .withStatorCurrentLimit(Amps.of(STATOR_CURRENT_LIMIT_AMPS))
          .withStatorCurrentLimitEnable(true);

      return config;
    }
  }

  public final class Pivot {
    public static final int CAN_ID = 1;
    public static final Angle MIN_ANGLE = Degrees.of(0.0);
    public static final Angle MAX_ANGLE = Degrees.of(90.0);
    public static final int ENCODER_CAN_ID = 20;
  public static final double GEAR_RATIO = 60.0;
  public static final double ARM_LENGTH_METERS = 1.0;
  public static final double ARM_MASS_KG = 3.0;
  public static final double MIN_ANGLE_RADIANS = 0.0;
  public static final double MAX_ANGLE_RADIANS = Math.PI / 2.0;
  public static final double STARTING_ANGLE_RADIANS = 0.0;
    public static final double KP = 150.0;
    public static final double KI = 0.0;
    public static final double KD = 30.0;
    public static final double STATOR_CURRENT_LIMIT_AMPS = 40.0;
    public static final double ENCODER_OFFSET_ROTATIONS = 0.0;
    public static final double ENCODER_RATIO = 1.0;
    public static final double TRAVEL_MIDDLE_ROTATIONS = 0.125;
    public static final boolean BRAKE_MODE = true;

    private Pivot() {
      throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates the common TalonFX configuration.
     *
     * @return configured TalonFX settings
     */
    public static TalonFXConfiguration createMotorConfiguration() {
      TalonFXConfiguration config = new TalonFXConfiguration();

      config.Feedback.withSensorToMechanismRatio(IntakeConstants.Pivot.GEAR_RATIO);
      config.Slot0.withKP(IntakeConstants.Pivot.KP).withKI(IntakeConstants.Pivot.KI).withKD(IntakeConstants.Pivot.KD);
      config.Slot0.withGravityType(GravityTypeValue.Arm_Cosine).withKG(0.0);
      config.Slot0.withKS(0.0).withKV(0.0).withKA(0.0);

      config.MotorOutput.withNeutralMode(NeutralModeValue.Brake);
      config.CurrentLimits
          .withStatorCurrentLimit(Amps.of(IntakeConstants.Pivot.STATOR_CURRENT_LIMIT_AMPS))
          .withStatorCurrentLimitEnable(true);
          
      return config;
    }
  }

}
