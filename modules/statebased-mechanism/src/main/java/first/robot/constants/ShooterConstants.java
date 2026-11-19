
package first.robot.constants;

import static org.wpilib.units.Units.Amps;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.NeutralModeValue;

/** Configuration constants for the shooter flywheel. */
public final class ShooterConstants {
  /** CAN device ID for the shooter motor. */
  public static final int CAN_ID = 1;

  /** Motor rotations per flywheel rotation. */
  public static final double GEAR_RATIO = 1.0;

  /** Flywheel moment of inertia in kg m^2. */
  public static final double MOMENT_OF_INERTIA_KG_M2 = 0.0025;

  /** Whether the motor uses brake neutral mode. */
  public static final boolean BRAKE_MODE = true;

  /** Maximum stator current in amps. */
  public static final double STATOR_CURRENT_LIMIT_AMPS = 40.0;

  /** Velocity proportional gain. */
  public static final double KP = 5.0;

  /** Velocity integral gain. */
  public static final double KI = 0.0;

  /** Velocity derivative gain. */
  public static final double KD = 0.0;

  private ShooterConstants() {
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
