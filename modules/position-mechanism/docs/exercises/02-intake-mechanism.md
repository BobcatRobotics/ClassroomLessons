# Lesson: Building the Intake Mechanism with Command V3

## Objective

In this lesson, you will implement `Intake`, the high-level mechanism class that coordinates the intake arm's IO, command behavior, telemetry, and simulation updates.

Unlike `IntakeIOReal`, which communicates directly with the TalonFX and CANcoder, this class defines how the robot software uses the intake. It accepts an `IntakeIO` implementation, exposes unit-aware measurements, creates commands using WPILib 2027 Command V3, and records telemetry through AdvantageKit.

By the end of this lesson, you should understand how to:

- Implement the `Mechanism` interface.
- Inject an IO implementation through a constructor.
- Process and log inputs using AdvantageKit.
- Expose mechanism measurements using WPILib units.
- Create commands with `run()` and `coroutine.yield()`.
- Clamp position and duty-cycle requests to safe configured ranges.
- Use `Optional` to represent a goal that may not exist.
- Create triggers for mechanism position checks.
- Implement a low-priority holding command.
- Separate mechanism behavior from hardware implementation.

## Prerequisites

Before starting, you should be familiar with:

- The `IntakeIO` interface and `IntakeIOInputs`.
- The `IntakeIOReal` implementation.
- Java constructors, fields, methods, and lambdas.
- Java's `Optional` class.
- AdvantageKit logging.
- WPILib units, including `Angle` and `Voltage`.
- WPILib 2027 Command V3 concepts, including `Mechanism`, `Command`, `Trigger`, and coroutines.

## Step 1: Create the Intake Mechanism Class

Create the file:

`src/main/java/first/robot/mechanisms/Intake.java`

Start with the following skeleton:

```java
package first.robot.mechanisms;

import org.wpilib.command3.Mechanism;

public class Intake implements Mechanism {

  public Intake(IntakeIO io) {
    // TODO: Store the IO implementation.
  }
}
```

### Your task

1. Create the `Intake` class.
2. Implement `Mechanism`.
3. Add a constructor accepting `IntakeIO`.
4. Explain why this class should not construct a `TalonFX` directly.

### Explanation

The `Intake` class is responsible for defining mechanism-level behavior, not hardware-specific details.

It receives an `IntakeIO` implementation through its constructor. That implementation could be real hardware, simulation, or a test double.

This is called **dependency injection**. It allows the same mechanism logic to work with different IO implementations without changing the class itself.

For example, the robot container can construct the mechanism with a real implementation:

```java
Intake intake = new Intake(new IntakeIOReal());
```

A simulation or unit test can supply a different implementation.

## Step 2: Declare the IO and Input Fields

Add the following fields:

```java
private static final double LOOP_PERIOD_SECONDS = 0.020;

private final IntakeIO io;

private final IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();

private Angle goalAngle;
```

You will also need these imports:

```java
import org.littletonrobotics.junction.AutoLog;
import org.wpilib.units.measure.Angle;
```

The `AutoLog` import is not required in `Intake.java` if it is already imported and used only in `IntakeIO`. The generated `IntakeIOInputsAutoLogged` class must be available through your project's AdvantageKit annotation processing.

### Your task

- Declare the loop period.
- Store the injected IO implementation.
- Create the generated input object.
- Add a nullable `Angle` field for the active goal.

### Explanation

`LOOP_PERIOD_SECONDS` represents a 20-millisecond simulation timestep:

\[
T = 0.020 \text{ seconds}
\]

The `io` field provides access to the underlying hardware or simulation implementation.

The `inputs` object stores the latest measured values. It is reused rather than recreated on every periodic update.

The `goalAngle` field tracks the currently requested position. A value of `null` indicates that the mechanism does not currently have an active position goal tracked by this class.

The generated `IntakeIOInputsAutoLogged` class is created from the `@AutoLog` annotation on `IntakeIOInputs`.

## Step 3: Implement the Constructor

Complete the constructor:

```java
/**
 * Creates an intake mechanism.
 *
 * @param io real, simulated, or test IO implementation
 */
public Intake(IntakeIO io) {
  // TODO: Assign the provided IO implementation.
}
```

### Your task

Assign the constructor argument to the instance field.

### Explanation

The constructor should retain the supplied IO implementation so all subsequent mechanism operations use the same object.

It should not decide which hardware configuration to use. That decision belongs to the robot's composition or initialization code.

This separation makes the mechanism easier to test and reuse.

## Step 4: Implement Periodic Input Processing and Logging

Complete the following method:

```java
/** Updates and records the latest hardware inputs. */
public void periodic() {
  // TODO: Refresh the IO inputs.
  // TODO: Process inputs through AdvantageKit.
  // TODO: Log the active goal when present.
  // TODO: Log measured position and motor telemetry.
}
```

Your implementation should record:

- `Intake/GoalRadians`
- `Intake/AngleRadians`
- `Intake/VelocityRadiansPerSecond`
- `Intake/AppliedVolts`
- `Intake/CurrentAmps`
- `Intake/TemperatureCelsius`

### Your task

1. Call `io.updateInputs(inputs)`.
2. Pass the inputs to `Logger.processInputs(...)` using the key `"Intake"`.
3. Log the goal only when `goalAngle` is not `null`.
4. Record the measured position, velocity, voltage, current, and temperature.

### Explanation

The IO layer provides measurements; the mechanism class is responsible for making those measurements available to AdvantageKit.

The typical update flow is:

1. Read current inputs.
2. Process them through AdvantageKit.
3. Record additional mechanism-level outputs.

For example, a measured position is already represented in radians by the IO contract. It can be logged directly without converting it again.

The requested goal is a separate value from the measured position. Logging both makes it possible to compare the requested and actual mechanism states during testing.

The goal is conditional because the intake may be operating in open-loop voltage mode, where no position target is being tracked.

## Step 5: Implement Simulation Updates

Complete this method:

```java
/** Advances the IO simulation; call from the robot simulation periodic loop. */
public void simulationPeriodic() {
  // TODO: Advance simulation by the configured timestep.
  // TODO: Pass the current battery voltage.
}
```

Use:

```java
RobotController.getBatteryVoltage()
```

to obtain the current battery voltage, and pass it along with the loop period to `io.updateSim(...)`.

### Your task

Explain why simulation needs both a timestep and battery voltage.

### Explanation

A physics simulation needs to know how much simulated time to advance. The timestep tells it how long the mechanism should evolve during each update.

Battery voltage affects the motor's available output and can influence the simulated mechanism's acceleration and speed.

The simulation update should be called by the robot's simulation periodic loop, rather than assuming that every hardware IO implementation must simulate itself.

For real hardware, `updateSim()` can remain an empty default method.

## Step 6: Expose the Measured Angle

Complete the following getter:

```java
/** Returns the measured arm angle. */
public Angle getAngle() {
  // TODO: Return the measured position as an Angle.
}
```

Use the `Radians` unit factory.

### Your task

Return the current measured position wrapped in a WPILib `Angle`.

### Explanation

The IO interface stores the measurement as a primitive `double` in radians. The mechanism provides a unit-aware API to its callers.

This lets other code work with an `Angle` instead of repeatedly handling unqualified numeric values.

For example, the OpMode can compare the measured angle with a requested target using unit-aware operations.

Remember that the returned measurement reflects the most recent input update. It does not necessarily represent a newly refreshed sensor reading unless the periodic input update has run.

## Step 7: Expose Velocity and Voltage

Implement these getters:

```java
/** Returns the measured angular velocity in radians per second. */
public double getVelocityRadiansPerSecond() {
  // TODO: Return the measured angular velocity.
}

/** Returns the motor's applied voltage. */
public Voltage getVoltage() {
  // TODO: Return the measured voltage as a WPILib Voltage.
}
```

### Your task

- Return angular velocity using the units defined by the IO interface.
- Wrap the applied voltage using `Volts.of(...)`.

### Explanation

The velocity getter returns a primitive `double` because the IO interface already defines the field in radians per second.

The voltage getter returns a unit-aware `Voltage` object, making the physical quantity explicit.

These measurements can be used for dashboards, diagnostics, control logic, and testing.

## Step 8: Expose the Optional Position Goal

Complete this method:

```java
/** Returns the active goal, if position control is enabled. */
public Optional<Angle> getGoal() {
  // TODO: Return an Optional containing the goal, if present.
}
```

### Your task

Use `Optional.ofNullable(...)` to return the active goal.

### Explanation

The mechanism may or may not have a tracked position goal.

- If a goal exists, the method returns an `Optional` containing the `Angle`.
- If no goal exists, the method returns `Optional.empty()`.

Using `Optional` makes the possibility of a missing goal explicit in the method's return type.

For example:

```java
intake.getGoal().ifPresent(goal -> {
  // Use the goal here.
});
```

In this design, switching to direct duty-cycle control clears the tracked goal.

## Step 9: Implement Goal Clamping

Create the private helper:

```java
private void setGoalAngle(Angle angle) {
  // TODO: Convert the requested angle to radians.
  // TODO: Clamp it to the configured minimum and maximum.
  // TODO: Convert the clamped value back to an Angle.
  // TODO: Send the goal to the IO layer if it changed.
}
```

Use the configured limits:

- `IntakeConstants.Pivot.MIN_ANGLE`
- `IntakeConstants.Pivot.MAX_ANGLE`

### Your task

Clamp the requested angle between the minimum and maximum configured angles.

Then send the position request to the IO layer only when the clamped target differs from the previously tracked goal.

### Explanation

Clamping constrains a requested value to a defined interval:

\[
x_{\text{clamped}} = \max(x_{\min},\min(x_{\max},x))
\]

The intake uses this operation to prevent the mechanism API from requesting a target outside its configured angular range.

Because the constants are unit-aware angles, convert them to radians before comparing them with the requested value.

The `isEquivalent(...)` method provides a unit-aware comparison between angles. If the target has not meaningfully changed, the mechanism avoids resending the same position command.

**Important:** Software clamping is not a substitute for mechanical stops, controller-level limits, or safety interlocks. It limits the requested target but does not by itself prevent all unsafe motion.

## Step 10: Create the `setAngle()` Command

Complete this method:

```java
/** Commands the arm to a target angle and holds it there. */
public Command setAngle(Angle angle) {
  return run(coroutine -> {
    while (true) {
      // TODO: Apply the requested goal.
      // TODO: Yield to the command scheduler.
    }
  }).named("Intake Set Angle");
}
```

### Your task

1. Use `setGoalAngle(angle)` inside the loop.
2. Call `coroutine.yield()` after applying the goal.
3. Keep the command named `"Intake Set Angle"`.

### Explanation

The `run(...)` method creates a command whose behavior is defined by a coroutine.

The loop continuously checks the requested goal, while `coroutine.yield()` suspends the coroutine so the command framework can continue scheduling other work.

Because `setGoalAngle(...)` avoids resending an unchanged target, the loop does not need to issue a new position request every cycle.

The command can remain active while the scheduler continues running it, allowing the IO layer's position controller to keep working toward the target.

The command does not itself implement the motor feedback loop. That work belongs to the IO implementation and its configured controller.

## Step 11: Create the Open-Loop Duty-Cycle Command

Complete the following method:

```java
/** Commands the arm with an open-loop duty cycle between -1 and 1. */
public Command set(double dutyCycle) {
  // TODO: Clamp the duty cycle.

  return run(coroutine -> {
    // TODO: Clear the tracked position goal.

    while (true) {
      // TODO: Convert duty cycle to a voltage request.
      // TODO: Send the voltage request to the IO layer.
      // TODO: Yield to the scheduler.
    }
  }).named("Intake Set Duty Cycle");
}
```

### Your task

1. Clamp the input to the range `-1.0` through `1.0`.
2. Clear the active position goal when the command begins.
3. Multiply the clamped duty cycle by the current battery voltage.
4. Pass the resulting voltage to `io.setVoltage(...)`.
5. Yield after each iteration.

### Explanation

Duty cycle represents a fraction of the available output:

\[
V_{\text{requested}}=dV_{\text{battery}}
\]

where \(d\) is the duty-cycle request.

For example, a duty cycle of `0.3` with a battery voltage of 12 V corresponds to a requested output of 3.6 V.

The command clamps the duty cycle before using it, ensuring the requested fraction stays within the intended range.

The IO layer accepts voltage, so this command converts the higher-level duty-cycle request into a voltage request.

This implementation is open-loop with respect to mechanism position: it does not attempt to reach a target angle.

**Safety note:** The command repeatedly requests voltage while active. Confirm the intended behavior when the command ends, and ensure the controller bindings and mechanism safety logic stop or transition the motor appropriately.

## Step 12: Create a Position Proximity Trigger

Complete this method:

```java
/** Creates a trigger that is true when the arm is near the requested angle. */
public Trigger near(Angle angle, Angle tolerance) {
  // TODO: Create a trigger based on the measured angle.
}
```

### Your task

Return a trigger whose condition uses:

```java
getAngle().isNear(angle, tolerance)
```

### Explanation

A trigger represents a condition that can be used to activate or coordinate commands.

The `near(...)` method makes it easy to check whether the measured intake angle is sufficiently close to a target.

For example, the robot could use a proximity trigger to coordinate another action when the intake approaches a desired position.

The tolerance specifies how close the measured angle must be to the requested angle.

The trigger checks the measured angle, not merely the stored goal. A commanded target does not guarantee that the mechanism has reached it.

## Step 13: Create a Low-Priority Hold Command

Complete the following method:

```java
/** Holds an angle at the lowest command priority. */
public Command hold(Angle angle) {
  return run(coroutine -> {
    while (true) {
      // TODO: Maintain the requested angle.
      // TODO: Yield to the scheduler.
    }
  })
      // TODO: Apply the lowest command priority.
      .named("Intake Hold");
}
```

### Your task

1. Call `setGoalAngle(angle)` repeatedly.
2. Yield during each iteration.
3. Apply `Command.LOWEST_PRIORITY`.
4. Name the command `"Intake Hold"`.

### Explanation

A hold command continuously maintains a requested position goal while remaining active.

Assigning the lowest priority allows higher-priority commands to take precedence according to the Command V3 framework's priority rules.

This is useful when the intake should normally maintain a resting or holding angle but must still respond to explicit operator or autonomous commands.

The hold command relies on the underlying IO's position controller to maintain the target.

Verify the exact scheduling and preemption semantics in the WPILib 2027 Alpha version used by your project.

## Step 14: Implement `stop()`

Complete the stop method:

```java
/** Stops the arm and clears its active goal. */
public void stop() {
  // TODO: Clear the tracked goal.
  // TODO: Stop the IO implementation.
}
```

### Your task

Set the tracked goal to `null` and call `io.stop()`.

### Explanation

Clearing the goal indicates that the mechanism is no longer tracking a position request through this class.

Calling `io.stop()` delegates the physical stopping behavior to the IO implementation.

This separation lets the real implementation stop the TalonFX while a simulation implementation can stop its simulated motor output.

A stopped motor is not necessarily a mechanically locked arm. Gravity and inertia may still move the mechanism, so the physical design and safety strategy remain important.

## Step 15: Review the Complete Command Flow

The intake mechanism has two primary control paths.

**Position control**

1. The OpMode calls `intake.setAngle(...)`.
2. The command calls `setGoalAngle(...)`.
3. The target is clamped to the configured range.
4. The IO implementation receives the position request.
5. The TalonFX or simulation works toward the requested position.
6. `periodic()` updates and logs the latest measurements.

**Open-loop control**

1. The OpMode calls `intake.set(...)`.
2. The duty cycle is clamped to the valid range.
3. The command converts duty cycle to voltage using the battery voltage.
4. The IO implementation receives the voltage request.
5. `periodic()` records the resulting telemetry.

The distinction is important: the mechanism defines the operator-facing behavior, while the IO implementation translates that behavior into hardware-specific commands.

## Step 16: Verification Checklist

Before completing the lesson, verify the following:

- [ ] `Intake` implements `Mechanism`.
- [ ] The IO implementation is supplied through the constructor.
- [ ] Inputs use `IntakeIOInputsAutoLogged`.
- [ ] `periodic()` updates and processes AdvantageKit inputs.
- [ ] Goal and telemetry values are logged using consistent keys.
- [ ] Simulation updates use the configured timestep and battery voltage.
- [ ] Angle and voltage getters return the intended types.
- [ ] `getGoal()` correctly represents the absence of a goal.
- [ ] Position requests are clamped to the configured limits.
- [ ] Duty-cycle requests are clamped between `-1.0` and `1.0`.
- [ ] The position command uses `run()` and `coroutine.yield()`.
- [ ] The duty-cycle command clears the tracked position goal.
- [ ] The `near()` trigger checks the measured angle.
- [ ] The hold command uses the intended priority.
- [ ] `stop()` clears the goal and delegates stopping to IO.
- [ ] Command termination and motor stopping behavior are verified against the project's Command V3 implementation.

## Challenge Questions

1. Why does `Intake` depend on `IntakeIO` rather than `IntakeIOReal`?
2. What does `Logger.processInputs(...)` provide?
3. Why is the requested goal different from the measured position?
4. Why clamp a requested angle before passing it to the IO layer?
5. What is the purpose of `Optional<Angle>`?
6. How does `coroutine.yield()` affect command execution?
7. Why does the duty-cycle command multiply its request by battery voltage?
8. What is the difference between `setAngle()` and `set()`?
9. Why should a proximity trigger check the measured position rather than the goal?
10. How does a low-priority hold command interact with higher-priority commands?
11. What should happen to the motor output when a command is interrupted?
12. How would you test the position-clamping logic without connecting real hardware?

## Expected Outcome

After completing this lesson, you should have an `Intake` mechanism class that exposes a unit-aware API, coordinates IO commands, logs telemetry through AdvantageKit, supports simulation updates, and provides reusable Command V3 commands and triggers.

The key design principle is that **the mechanism translates robot behavior into IO requests while keeping hardware details, telemetry, and command scheduling responsibilities clearly separated**.
