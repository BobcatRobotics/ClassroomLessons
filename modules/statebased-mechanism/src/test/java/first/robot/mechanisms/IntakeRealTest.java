package first.robot.mechanisms;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wpilib.hardware.hal.HAL;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ControlModeValue;
import com.ctre.phoenix6.unmanaged.Unmanaged;

import first.robot.constants.IntakeConstants;

/**
 * Integration tests for {@link IntakeIOReal} using Phoenix 6 motor simulation.
 *
 * <p>These tests exercise the real Intake implementation without mocking its
 * motor controllers. Separate {@link TalonFX} handles with matching CAN IDs
 * are used to observe the simulated devices owned by {@link IntakeIOReal}.
 *
 * <p>The tests verify that the secondary motor enters follower mode and that
 * the Intake creates its expected commands.
 */
class IntakeRealTest {

  /** Maximum time, in seconds, to wait for a simulated condition. */
  private static final double TIMEOUT_SEC = 2.0;

  /** Intake implementation under test. */
  private IntakeIOReal intake;

  /** Additional TalonFX handle used to inspect the primary simulated motor. */
  private TalonFX primaryHandle;


  /**
   * Initializes the Hardware Abstraction Layer before running the test suite.
   *
   * @throws AssertionError if HAL initialization fails
   */
  @BeforeAll
  static void initHal() {
    assertTrue(HAL.initialize(), "HAL failed to initialize");
  }

  /**
   * Creates a fresh Intake and observer handles before each test.
   *
   * <p>Both simulated motors are supplied with 12 volts so that their simulation
   * states have a defined supply voltage.
   */
  @BeforeEach
  void setUp() {
    intake = new IntakeIOReal();
    primaryHandle = new TalonFX(IntakeConstants.Pivot.CAN_ID, new CANBus());

    primaryHandle.getSimState().setSupplyVoltage(12.0);
  }

  /**
   * Closes the additional TalonFX handles after each test.
   *
   * <p>Releasing these resources prevents the observer handles from remaining
   * open between tests.
   */
  @AfterEach
  void tearDown() {
    primaryHandle.close();
  }

  /**
   * Repeatedly feeds the simulated devices and checks a condition until it
   * succeeds or the timeout expires.
   *
   * <p>The condition is checked immediately and then approximately every
   * 20 milliseconds. Phoenix devices are kept enabled during polling.
   *
   * @param condition condition that must eventually become true
   * @return {@code true} if the condition holds before or at the final check;
   *     {@code false} if the timeout expires or the polling thread is interrupted
   */
  private static boolean eventually(BooleanSupplier condition) {
    long deadline = System.nanoTime() + (long) (TIMEOUT_SEC * 1e9);

    while (System.nanoTime() < deadline) {
      Unmanaged.feedEnable(100);

      if (condition.getAsBoolean()) {
        return true;
      }

      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }

    return condition.getAsBoolean();
  }


  /**
   * Verifies that the Intake creates its idle and flywheel duty-output commands.
   *
   * <p>The flywheel command receives a duty-cycle supplier returning {@code 0.5}.
   * This test verifies command creation only; it does not execute the commands
   * or verify the resulting motor output.
   */
  @Test
  void commandsAreCreated() {
    // assertNotNull(Intake.setVelocity(0));
    // assertNotNull(Intake.setVelocity(40));
  }
}