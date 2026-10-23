
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.mechanisms;

import java.util.function.Supplier;

import org.littletonrobotics.junction.AutoLog;
import org.wpilib.command3.Command;

/**
 * Defines the hardware abstraction interface for the shooter mechanism.
 *
 * <p>This interface separates shooter control logic from its hardware
 * implementation, allowing the shooter to use real hardware, simulation,
 * or fake implementations for unit testing.
 */
public interface ShooterIO {

    /**
     * Contains sensor measurements reported by the shooter I/O implementation.
     *
     * <p>The {@link AutoLog} annotation generates an AdvantageKit-compatible
     * subclass that supports automatic input logging.
     */
    @AutoLog
    class ShooterIOInputs {
    /** Flywheel position in rotations. */
    public double positionRotations = 0.0;

    /** Flywheel velocity in rotations per second. */
    public double velocityRotationsPerSecond = 0.0;

    /** Applied motor voltage in volts. */
    public double appliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double statorCurrentAmps = 0.0;

    /** Motor temperature in degrees Celsius. */
    public double temperatureCelsius = 0.0;

    /** Whether closed-loop velocity control is active. */
    public boolean velocityControlEnabled = false;

    /** Requested flywheel velocity in rotations per second. */
    public double goalVelocityRotationsPerSecond = 0.0;
    }

    /**
     * Updates the input measurements obtained from the shooter hardware or simulation.
     *
     * @param inputs input structure to populate with the latest shooter measurements
     */
    default void updateInputs(ShooterIOInputs inputs) {
        // TODO 1: Update the primary flywheel measurement.
        // TODO 2: Update the secondary flywheel measurement.
    }

    /**
     * Creates a command that places the shooter in its idle state.
     *
     * @return the command that places the shooter in its idle state
     */
    default void stop() {
        // TODO 3: Determine the appropriate default behavior for this interface.
        return null;
    }

    /**
     * Creates a command that controls the flywheel using a duty-cycle output.
     *
     * @param speed supplier for the requested flywheel duty-cycle output
     * @return a command that applies the requested duty-cycle output
     */
    default void setVoltage(double volts) {
        // TODO 4: Determine the appropriate default behavior for this interface.
        return null;
    }
}
