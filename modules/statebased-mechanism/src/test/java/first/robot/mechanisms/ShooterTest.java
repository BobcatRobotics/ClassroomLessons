// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.mechanisms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.RPM;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wpilib.hardware.hal.HAL;

/**
 * Unit tests for the {@link Shooter} subsystem.
 *
 * <p>
 * These tests verify that the shooter correctly interacts with its I/O layer,
 * updates its inputs during periodic execution, and creates its expected
 * commands.
 *
 * <p>
 * A {@link FakeShooterIO} implementation is used to isolate the shooter from
 * real hardware, allowing the tests to run without physical motor controllers
 * or other shooter components.
 */
public class ShooterTest {

  /** The shooter subsystem instance under test. */
  Shooter shooter;

  /** Fake I/O implementation used to observe interactions with the shooter. */
  FakeShooterIO fakeIO;

  /**
   * Fake implementation of {@link ShooterIO} for unit testing.
   *
   * <p>
   * This implementation avoids hardware dependencies and tracks calls to
   * {@link #updateInputs(ShooterIOInputs)} so tests can verify that the shooter
   * periodically updates its input data.
   *
   * <p>
   * If {@link ShooterIO} gains additional abstract methods, this class must
   * implement them to remain compatible with the interface.
   */
  static class FakeShooterIO implements ShooterIO {

    /** Number of times {@link #updateInputs(ShooterIOInputs)} has been called. */
    int updateInputsCalls = 0;

    /**
     * Updates the supplied input structure and records the invocation.
     *
     * <p>
     * The fake implementation does not populate sensor values because these
     * tests focus on verifying interactions with the I/O layer.
     *
     * @param inputs the input structure to update
     */
    @Override
    public void updateInputs(ShooterIOInputs inputs) {
      updateInputsCalls++;
    }
  }

  /**
   * Initializes the HAL and creates a shooter backed by fake I/O.
   *
   * <p>
   * JUnit executes this method before each test, ensuring each test receives
   * a fresh shooter instance and an independent call counter.
   */
  @BeforeEach
  void setup() {
    assert HAL.initialize();
    fakeIO = new FakeShooterIO();
    shooter = new Shooter(fakeIO);
  }

  /**
   * Verifies that one periodic execution updates the I/O inputs exactly once.
   */
  @Test
  void periodicUpdatesInputsOnce() {
    shooter.periodic();
    assertEquals(1, fakeIO.updateInputsCalls);
  }

  /**
   * Verifies that every periodic execution updates the I/O inputs.
   *
   * <p>
   * Calling {@link Shooter#periodic()} three times should result in exactly
   * three calls to the fake I/O implementation.
   */
  @Test
  void periodicUpdatesInputsEveryCall() {
    shooter.periodic();
    shooter.periodic();
    shooter.periodic();
    assertEquals(3, fakeIO.updateInputsCalls);
  }

  /**
   * Verifies that the shooter can create its idle command.
   *
   * <p>
   * This test checks command creation without executing the command.
   */
  @Test
  void idleCommandIsCreated() {
    assertNotNull(shooter.idle());
  }

  /**
   * Verifies that the shooter can create its shooting command.
   *
   * <p>
   * The target distance supplier returns a distance of one meter.
   * This test checks command creation without executing the command or
   * validating its resulting flywheel speed.
   */
  @Test
  void shootCommandIsCreated() {
    assertNotNull(shooter.set(1.0));
    assertNotNull(shooter.setVelocity(RPM.of(4650.0)));
  }
}
