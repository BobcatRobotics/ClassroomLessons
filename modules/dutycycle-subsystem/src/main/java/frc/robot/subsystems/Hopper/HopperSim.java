package frc.robot.subsystems.Hopper;

import org.littletonrobotics.junction.Logger;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import frc.robot.Constants;

/**
 * Simulation implementation of the {@link HopperIO} interface.
 *
 * <p>The simulation models the physical behavior of the hopper motor using
 * WPILib's {@link DCMotorSim}. The motor is controlled using the same
 * duty-cycle command as the real TalonFX implementation.
 *
 * <p>The simulation flow is:
 *
 * <pre>
 * Duty Cycle
 *     ↓
 * Motor Voltage
 *     ↓
 * DCMotorSim
 *     ↓
 * Position / Velocity / Acceleration / Current
 *     ↓
 * HopperIOInputs
 * </pre>
 *
 * <p>The simulated physical state is also written back to the CTRE
 * {@link TalonFXSimState}, allowing code that reads from the TalonFX to
 * observe the simulated motor state.
 */
public class HopperSim implements HopperIO {

  /** Simulated TalonFX motor controller. */
  private final TalonFX hopperMotor;

  /** Simulation state associated with the TalonFX. */
  private final TalonFXSimState hopperMotorSim;

  /** WPILib model of the hopper motor and mechanism. */
  private final DCMotorSim motorSim;

  /** Kraken X60 FOC motor model used by the simulation. */
  private static final DCMotor MOTOR =
      DCMotor.getKrakenX60Foc(1);

  /** Moment of inertia of the simulated hopper mechanism. */
  private static final double MOMENT_OF_INERTIA = 0.035;

  /** Simulated motor duty-cycle output. */
  private double dutyCycle = 0.0;

  /**
   * Creates a new simulated hopper.
   */
  public HopperSim() {


  }

  /**
   * Updates the physical motor simulation and hopper inputs.
   *
   * <p>The duty-cycle output is converted into a motor voltage and applied to
   * the {@link DCMotorSim}. The resulting physical state is then reported
   * through {@link HopperIOInputs}.
   *
   * @param inputs object to populate with simulated motor measurements
   */
  @Override
  public void updateInputs(HopperIOInputs inputs) {

  }

  /**
   * Sets the simulated motor duty-cycle output.
   *
   * <p>The value is constrained to the valid range of -1.0 to +1.0.
   *
   * @param dutyCycle motor output from -1.0 to +1.0
   */
  @Override
  public void setDutyCycle(double dutyCycle) {


  }

  /**
   * Stops the simulated hopper motor.
   */
  @Override
  public void stop() {

  }

  /**
   * Performs periodic processing for the simulated hopper.
   */
  @Override
  public void periodic() {
    // Simulation is updated through updateInputs().
  }
}
