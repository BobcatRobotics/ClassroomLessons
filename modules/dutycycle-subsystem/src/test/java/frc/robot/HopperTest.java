import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import frc.robot.subsystems.Hopper.Hopper;
import frc.robot.subsystems.Hopper.HopperIO;

/**
 * Unit tests for the {@link Hopper} subsystem.
 *
 * <p>The tests use a fake {@link HopperIO} implementation so the subsystem
 * can be tested without requiring a TalonFX or physical hardware.
 */
class HopperTest {

  /** Fake IO implementation used by the tests. */
  private FakeHopperIO io;

  /** Hopper subsystem under test. */
  private Hopper hopper;

  /**
   * Creates a fresh hopper and fake IO implementation before each test.
   */
  @BeforeEach
  void setup() {
    io = new FakeHopperIO();
    hopper = new Hopper(io);
  }

  /**
   * Verifies that a duty-cycle command is passed to the IO layer.
   */
  @Test
  void setDutyCyclePassesCommandToIO() {

    hopper.setDutyCycle(0.5);

    assertEquals(0.5, io.dutyCycle, 0.001);
  }

  /**
   * Verifies that positive duty-cycle commands are accepted.
   */
  @Test
  void positiveDutyCycle() {

    hopper.setDutyCycle(1.0);

    assertEquals(1.0, io.dutyCycle, 0.001);
  }

  /**
   * Verifies that negative duty-cycle commands are accepted.
   */
  @Test
  void negativeDutyCycle() {

    hopper.setDutyCycle(-1.0);

    assertEquals(-1.0, io.dutyCycle, 0.001);
  }

  /**
   * Verifies that duty-cycle commands above the valid range are clamped.
   */
  @Test
  void dutyCycleIsClampedHigh() {

    hopper.setDutyCycle(2.0);

    assertEquals(1.0, io.dutyCycle, 0.001);
  }

  /**
   * Verifies that duty-cycle commands below the valid range are clamped.
   */
  @Test
  void dutyCycleIsClampedLow() {

    hopper.setDutyCycle(-2.0);

    assertEquals(-1.0, io.dutyCycle, 0.001);
  }

  /**
   * Verifies that the stop command is passed to the IO layer.
   */
  @Test
  void stopStopsMotor() {

    hopper.setDutyCycle(0.75);

    hopper.stop();

    assertEquals(0.0, io.dutyCycle, 0.001);
    assertTrue(io.stopCalled);
  }

  /**
   * Verifies that sensor values are exposed by the subsystem.
   */
  @Test
  void sensorValuesAreReturned() {

    io.inputs.velocityOfHopperMotorRPS = 50.0;
    io.inputs.positionOfHopperMotorRotations = 100.0;
    io.inputs.dutyCycleOutput = 0.5;
    io.inputs.hopperConnected = true;

    hopper.periodic();

    assertEquals(50.0, hopper.getVelocity(), 0.001);
    assertEquals(100.0, hopper.getPosition(), 0.001);
    assertEquals(0.5, hopper.getDutyCycle(), 0.001);
    assertTrue(hopper.getIsMotorConnected());
  }

  /**
   * Verifies that a disconnected motor is reported correctly.
   */
  @Test
  void motorDisconnected() {

    io.inputs.hopperConnected = false;

    hopper.periodic();

    assertFalse(hopper.getIsMotorConnected());
  }

  /**
   * Simple fake IO implementation used to test the Hopper subsystem.
   */
  private static class FakeHopperIO implements HopperIO {

    /** Fake hopper inputs. */
    private final HopperIOInputs inputs = new HopperIOInputs();

    /** Last commanded duty cycle. */
    private double dutyCycle = 0.0;

    /** Whether stop() was called. */
    private boolean stopCalled = false;

    /**
     * Provides fake sensor inputs to the subsystem.
     *
     * @param inputs object to populate
     */
    @Override
    public void updateInputs(HopperIOInputs inputs) {
      inputs.velocityOfHopperMotorRPS =
          this.inputs.velocityOfHopperMotorRPS;

      inputs.positionOfHopperMotorRotations =
          this.inputs.positionOfHopperMotorRotations;

      inputs.dutyCycleOutput =
          this.inputs.dutyCycleOutput;

      inputs.hopperConnected =
          this.inputs.hopperConnected;
    }

    /**
     * Stores the commanded duty cycle.
     *
     * @param dutyCycle commanded motor output
     */
    @Override
    public void setDutyCycle(double dutyCycle) {
      this.dutyCycle = dutyCycle;
      inputs.dutyCycleOutput = dutyCycle;
    }

    /**
     * Records that the motor was stopped.
     */
    @Override
    public void stop() {
      dutyCycle = 0.0;
      inputs.dutyCycleOutput = 0.0;
      stopCalled = true;
    }
  }
}
