
package frc.robot.subsystems.Hopper;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotation;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;
import static edu.wpi.first.units.Units.Second;
import static edu.wpi.first.units.Units.Volts;

import org.littletonrobotics.junction.Logger;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.units.measure.AngularAcceleration;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.Constants;

/**
 * Real hardware implementation of the {@link HopperIO} interface.
 *
 * <p>This implementation uses a CTRE TalonFX to control the hopper motor
 * using open-loop duty-cycle control.
 */
public class HopperReal implements HopperIO {

  /** CTRE TalonFX controlling the hopper motor. */
  private final TalonFX hopperMotor;

  /** Motor velocity status signal. */
  private final StatusSignal<AngularVelocity> velocitySignal;

  /** Motor stator current status signal. */
  private final StatusSignal<Current> statorCurrentSignal;

  /** Motor output voltage status signal. */
  private final StatusSignal<Voltage> voltageSignal;

  /** Motor angular acceleration status signal. */
  private final StatusSignal<AngularAcceleration> accelerationSignal;

  /**
   * Creates and configures the real hopper motor.
   */
  public HopperReal() {
  }

  /**
   * Updates measurements from the TalonFX.
   *
   * @param inputs object to populate with current motor measurements
   */
  @Override
  public void updateInputs(HopperIOInputs inputs) {

    
  }

  /**
   * Sets the TalonFX duty-cycle output.
   *
   * @param dutyCycle motor output from -1.0 to +1.0
   */
  @Override
  public void setDutyCycle(double dutyCycle) {

  }

  /**
   * Stops the hopper motor.
   */
  @Override
  public void stop() {

  }

  /**
   * Performs periodic processing for the real hopper.
   */
  @Override
  public void periodic() {
    // No additional periodic processing required.
  }
}