
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

        /** Measured rotational velocity of the primary flywheel, in rotations per second. */
        public double velocityOfPrimaryRotPerSecond = 0.0;

        /** Measured rotational velocity of the secondary flywheel, in rotations per second. */
        public double velocityOfSecondaryRotPerSecond = 0.0;
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
    default Command idle() {
        // TODO 3: Determine the appropriate default behavior for this interface.
        return null;
    }

    /**
     * Creates a command that controls the flywheel using a duty-cycle output.
     *
     * @param speed supplier for the requested flywheel duty-cycle output
     * @return a command that applies the requested duty-cycle output
     */
    default Command runFlywheelDutyOutput(Supplier<Double> speed) {
        // TODO 4: Determine the appropriate default behavior for this interface.
        return null;
    }
}
