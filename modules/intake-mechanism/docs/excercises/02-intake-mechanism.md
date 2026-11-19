# Lesson: Building the Intake Mechanism with Command V3

## Objective

In this lesson, you will implement `Intake`, the high-level mechanism class that coordinates the intake roller and pivot through the AdvantageKit IO layer.

Unlike `IntakeIOReal`, which communicates directly with the TalonFX motors and CANcoder, this class defines how the rest of the robot software uses the intake. It accepts an `IntakeIO` implementation, exposes unit-aware measurements, creates WPILib 2027 Command V3 commands, evaluates triggers, and records telemetry through AdvantageKit.

By the end of this lesson, you should understand how to:

- Implement the `Mechanism` interface.
- Inject an IO implementation through a constructor.
- Use the AdvantageKit-generated `IntakeIOInputsAutoLogged` class.
- Expose measured roller and pivot values using WPILib units.
- Track optional roller velocity and pivot angle goals.
- Create commands with `run()` and `coroutine.yield()`.
- Clamp pivot angle and duty-cycle requests.
- Convert duty-cycle requests into voltage requests.
- Create a trigger that checks measured roller velocity.
- Implement a low-priority pivot hold command.
- Separate mechanism behavior from hardware implementation.
- Record roller and pivot telemetry.

## Prerequisites

Before starting, you should be familiar with:

- The `IntakeIO` interface and `IntakeIOInputs`.
- The `IntakeIOReal` implementation.
- Java constructors, fields, methods, lambdas, and nullable references.
- Java's `Optional` class.
- AdvantageKit logging and `@AutoLog`.
- WPILib units, including `Angle`, `AngularVelocity`, `Voltage`, and `Current`.
- WPILib 2027 Command V3 concepts, including `Mechanism`, `Command`, `Trigger`, and coroutines.
- The configured pivot limits in `IntakeConstants`.

---

## Step 1: Create the Intake Mechanism Class

Create the file:

`src/main/java/first/robot/mechanisms/Intake.java`

Start with this skeleton:

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
3. Add a constructor that accepts an `IntakeIO`.
4. Explain why this class should not construct a `TalonFX` directly.

### Explanation

The `Intake` class is responsible for mechanism-level behavior, not hardware-specific details.

It receives an `IntakeIO` implementation through its constructor. That implementation could represent real hardware, simulation, or a test implementation. This is called **dependency injection**.

For example, robot initialization code can select the implementation:

```java
Intake intake = new Intake(new IntakeIOReal());
```

A simulation or test can supply a different implementation without changing the `Intake` class.

## Step 2: Declare the Loop Period, IO, and Logged Inputs

Add the following fields:

```java
private static final double LOOP_PERIOD_SECONDS = 0.020;

private final IntakeIO io;

private final IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();
```

### Your task

- Declare the 20 ms loop period.
- Store the injected IO implementation.
- Create the AdvantageKit-generated input object.

### Explanation

`LOOP_PERIOD_SECONDS` represents a nominal 20-millisecond robot loop:

\[
T = 0.020 \text{ seconds}
\]

In the supplied class, this constant is not currently used. It may be intended for future simulation stepping, but `simulationPeriodic()` is empty and does not call an IO simulation method.

The `inputs` object stores the latest measured values. It is reused on every update rather than recreated each time.

The generated `IntakeIOInputsAutoLogged` class comes from the `@AutoLog` annotation on `IntakeIO.IntakeIOInputs`. The generated class must be available through the project's AdvantageKit annotation processing.

## Step 3: Track the Active Goals

Add these fields:

```java
private AngularVelocity rollerGoalVelocity = null;
private Angle pivotGoalAngle = null;
```

### Your task

1. Track the requested roller velocity.
2. Track the requested pivot angle.
3. Understand why each field can be `null`.

### Explanation

The two goals represent different control modes:

- `rollerGoalVelocity` stores the requested roller velocity while velocity control is being used.
- `pivotGoalAngle` stores the requested pivot angle while position control is being used.

A `null` value means this mechanism class is not currently tracking a goal for that mechanism axis. The public getters use `Optional` to make that possibility explicit.

These fields are software-side goal tracking. The measured values remain in `inputs` and should not be confused with the requested goals.

## Step 4: Create the Roller Mechanism Visualization

Add the `Mechanism2d` fields:

```java
private final Mechanism2d rollerMechanism2d = new Mechanism2d(1, 1);

private final MechanismLigament2d rollerSpokeLigament =
    rollerMechanism2d.getRoot("Axle", 0.5, 0.5)
        .append(new MechanismLigament2d("Flywheel", 0.4, 0.0));
```

### Your task

- Create a `Mechanism2d` drawing.
- Add an axle root at the center.
- Attach a ligament to represent the flywheel.

### Explanation

The drawing is a 1-by-1 mechanism canvas. The root named `"Axle"` is placed at `(0.5, 0.5)`. A ligament named `"Flywheel"` is attached to it.

The ligament angle is updated later in `periodic()` using the measured roller position.

**Review note:** The supplied `Logger.recordOutput` statement for the `Mechanism2d` object is commented out. Therefore, this class updates the visualization object but does not currently publish it through that statement. To see it in a telemetry viewer, publish the object using the logging approach supported by the project's AdvantageKit version.

## Step 5: Implement the Constructor

Complete the constructor:

```java
/**
 * Creates a Intake with the specified IO implementation.
 *
 * @param io real or simulated Intake IO
 */
public Intake(IntakeIO io) {
  this.io = io;
}
```

### Your task

Assign the constructor argument to the instance field.

### Explanation

The constructor retains the supplied IO implementation so all mechanism operations use the same object.

It should not decide which hardware configuration to use. That decision belongs to the robot's initialization or composition code. This keeps the mechanism reusable and easier to test.

## Step 6: Expose Measured Roller Velocity and Position

Implement the roller getters:

```java
/** Returns the measured flywheel velocity. */
public AngularVelocity getVelocity() {
  return RotationsPerSecond.of(inputs.rollerVelocityRotationsPerSecond);
}

/** Returns the measured flywheel position. */
public Angle getPosition() {
  return Rotation.of(inputs.rollerPositionRotations);
}
```

### Your task

- Return roller velocity as an `AngularVelocity`.
- Return roller position as an `Angle`.
- Use the units that match the corresponding `IntakeIOInputs` fields.

### Explanation

The IO layer stores roller velocity as rotations per second and roller position as rotations. The mechanism wraps those primitive values in WPILib measure types.

These getters return the most recently sampled values in `inputs`. They do not request a new sensor update themselves; `periodic()` refreshes the inputs.

## Step 7: Expose Optional Goals

Implement both goal getters:

```java
/** Returns the requested velocity when velocity control is active. */
public Optional<AngularVelocity> getGoalVelocity() {
  return Optional.ofNullable(rollerGoalVelocity);
}

/** Returns the active goal, if position control is enabled. */
public Optional<Angle> getGoalPosition() {
  return Optional.ofNullable(pivotGoalAngle);
}
```

### Your task

1. Use `Optional.ofNullable(...)` for each goal.
2. Return the roller goal as an `Optional<AngularVelocity>`.
3. Return the pivot goal as an `Optional<Angle>`.

### Explanation

A goal may not exist while the corresponding axis is being driven open-loop or stopped.

- If a goal is present, the `Optional` contains the requested value.
- If the field is `null`, the method returns `Optional.empty()`.

Example:

```java
intake.getGoalVelocity().ifPresent(goal -> {
  // Use the requested roller velocity.
});

intake.getGoalPosition().ifPresent(goal -> {
  // Use the requested pivot angle.
});
```

This design distinguishes the requested target from the measured state.

## Step 8: Expose Combined Voltage and Current

Implement the getters:

```java
/** Returns the measured motor voltage. */
public Voltage getVoltage() {
  return Volts.of(inputs.rollerAppliedVolts)
      .plus(Volts.of(inputs.pivotAppliedVolts));
}

/** Returns the measured motor stator current. */
public Current getCurrent() {
  return Amps.of(inputs.rollerStatorCurrentAmps)
      .plus(Amps.of(inputs.pivotCurrentAmps));
}
```

### Your task

- Wrap each motor's measured voltage in a `Voltage`.
- Add the two voltage measures.
- Wrap each motor's measured current in a `Current`.
- Add the two current measures.

### Explanation

The IO inputs contain separate values for the roller and pivot. These getters return the arithmetic sum of the two reported values as a unit-aware measure.

**Important interpretation:** The sum is an aggregate reported by this class. It is not the voltage of a single electrical node or necessarily the total current drawn from the battery. In particular, adding motor terminal voltages does not produce a physically meaningful shared supply voltage in all operating conditions. Use the individual IO fields when diagnosing each motor.

## Step 9: Implement Pivot Goal Clamping

Complete the private helper:

```java
private void setGoalAngle(Angle angle) {
  double requestedRadians = angle.in(Radians);

  double clampedRadians = Math.max(
      IntakeConstants.Pivot.MIN_ANGLE.in(Radians),
      Math.min(
          IntakeConstants.Pivot.MAX_ANGLE.in(Radians),
          requestedRadians));

  Angle clamped = Radians.of(clampedRadians);

  if (pivotGoalAngle == null || !clamped.isEquivalent(pivotGoalAngle)) {
    io.setPosition(clampedRadians);
    pivotGoalAngle = clamped;
  }
}
```

### Your task

1. Convert the requested angle to radians.
2. Clamp it between `IntakeConstants.Pivot.MIN_ANGLE` and `IntakeConstants.Pivot.MAX_ANGLE`.
3. Convert the clamped value back to an `Angle`.
4. Send a new IO position request only if the goal has changed.
5. Store the clamped goal.

### Explanation

Clamping constrains a value to a defined interval:

\[
x_{\text{clamped}} = \max(x_{\min}, \min(x_{\max}, x))
\]

The configured limits are unit-aware angles, so they are converted to radians before comparing them with the requested value.

`isEquivalent(...)` compares the new target with the previously stored target. If the goal has not changed, the class avoids sending the same position request repeatedly.

**Safety note:** Software clamping is not a substitute for mechanical stops, controller-level limits, sensor validation, or other safety interlocks.

## Step 10: Create the Roller Velocity Command

Complete the command:

```java
/**
 * Runs closed-loop velocity control until interrupted.
 *
 * @param velocity target flywheel velocity
 * @return command that maintains the requested velocity
 */
public Command setVelocity(AngularVelocity velocity) {
  return run(coroutine -> {
    while (true) {
      if (rollerGoalVelocity == null
          || !velocity.isEquivalent(rollerGoalVelocity)) {
        io.setVelocity(velocity.in(RotationsPerSecond));
      }

      rollerGoalVelocity = velocity;
      coroutine.yield();
    }
  }).named("Intake Set Velocity");
}
```

### Your task

1. Create a command using `run(...)`.
2. Compare the requested velocity with the tracked goal.
3. Send a new velocity request when no goal exists or the requested value changes.
4. Store the requested velocity.
5. Yield to the command scheduler on every loop iteration.
6. Name the command `"Intake Set Velocity"`.

### Explanation

`run(...)` creates a Command V3 command whose behavior is expressed as a coroutine.

The command checks whether the requested velocity differs from the tracked goal. It only calls `io.setVelocity(...)` when the target needs to be updated. The goal is then stored and the coroutine yields.

The IO implementation is responsible for translating the velocity request into hardware-specific control. The mechanism command does not implement the motor's feedback controller.

## Step 11: Create the Pivot Angle Command

Complete the command:

```java
/** Commands the arm to a target angle and holds it there. */
public Command setAngle(Angle angle) {
  return run(coroutine -> {
    while (true) {
      setGoalAngle(angle);
      coroutine.yield();
    }
  }).named("Intake Set Angle");
}
```

### Your task

1. Create a command using `run(...)`.
2. Call `setGoalAngle(angle)` inside the loop.
3. Yield after applying the goal.
4. Name the command `"Intake Set Angle"`.

### Explanation

The command remains active and repeatedly asks the helper to maintain the requested goal. Since `setGoalAngle(...)` only sends a new IO request when the target changes, the repeated call does not continuously resend an unchanged position request.

The IO implementation and motor controller are responsible for moving the pivot to the requested position and holding it there.

## Step 12: Create the Roller Duty-Cycle Command

Complete the roller open-loop command:

```java
/**
 * Drives the flywheel using an open-loop duty cycle.
 *
 * @param dutyCycle output from -1.0 to 1.0
 * @return command that maintains the requested output
 */
public Command setRollerDutyCycle(double dutyCycle) {
  return run(coroutine -> {
    rollerGoalVelocity = null;

    while (true) {
      io.setVoltage(
          0,
          RobotController.getMeasureBatteryVoltage()
              .in(Volts) * Math.max(-1.0, Math.min(1.0, dutyCycle)));

      coroutine.yield();
    }
  }).named("Intake Set Duty Cycle");
}
```

### Your task

1. Clear the tracked roller velocity goal when the command starts.
2. Clamp the duty cycle to `-1.0` through `1.0`.
3. Read the current battery voltage.
4. Multiply the clamped duty cycle by battery voltage.
5. Call `io.setVoltage(...)` with zero pivot voltage and the calculated roller voltage.
6. Yield on each iteration.

### Explanation

A duty-cycle request represents a fraction of the available voltage:

\[
V_{\text{requested}} = d \cdot V_{\text{battery}}
\]

For example, a duty cycle of `0.25` with a 12 V battery corresponds to a requested roller voltage of 3 V.

The `IntakeIO.setVoltage(...)` method takes the pivot voltage first and roller voltage second. This command passes `0` for the pivot and applies the calculated voltage to the roller.

**Review note:** The code clamps the duty cycle inside the loop. This is correct but repeats the same calculation every scheduler iteration. You could clamp it once before creating the command and capture the clamped value, if that better fits your project's coding conventions.

## Step 13: Create the Pivot Duty-Cycle Command

Complete the pivot open-loop command:

```java
/** Commands the arm with an open-loop duty cycle between -1 and 1. */
public Command setPivotDutyCycle(double dutyCycle) {
  double clampedDutyCycle = Math.max(-1.0, Math.min(1.0, dutyCycle));

  return run(coroutine -> {
    pivotGoalAngle = null;

    while (true) {
      io.setVoltage(
          RobotController.getBatteryVoltage() * clampedDutyCycle,
          0);
      coroutine.yield();
    }
  }).named("Intake Set Duty Cycle");
}
```

### Your task

1. Clamp the duty cycle before creating the command.
2. Clear the tracked pivot angle goal when the command starts.
3. Multiply the clamped duty cycle by the current battery voltage.
4. Pass the result as pivot voltage and zero as roller voltage.
5. Yield on each iteration.

### Explanation

This command drives only the pivot. The first argument to `io.setVoltage(...)` is pivot voltage; the second is roller voltage.

The command clears `pivotGoalAngle` because the pivot is no longer tracking a closed-loop position target through this mechanism class.

**Review note:** Both duty-cycle commands currently use the name `"Intake Set Duty Cycle"`. Consider giving them distinct names, such as `"Intake Roller Set Duty Cycle"` and `"Intake Pivot Set Duty Cycle"`, to make command traces and debugging easier.

## Step 14: Create a Roller Velocity Proximity Trigger

Complete the trigger:

```java
/**
 * Creates a trigger that becomes active near a target velocity.
 *
 * @param velocity target velocity
 * @param tolerance acceptable velocity error
 * @return velocity tolerance trigger
 */
public Trigger near(
    AngularVelocity velocity, AngularVelocity tolerance) {
  return new Trigger(() -> getVelocity().isNear(velocity, tolerance));
}
```

### Your task

Return a trigger that checks whether the measured roller velocity is within the supplied tolerance of the target velocity.

### Explanation

A `Trigger` represents a condition that can be used to coordinate commands.

This trigger uses `getVelocity()`, which reads the latest measured roller velocity, and compares it to the requested target with `isNear(...)`.

The trigger checks the measured state, not the stored goal. A commanded velocity does not guarantee that the roller has reached that velocity.

## Step 15: Create a Low-Priority Pivot Hold Command

Complete the hold command:

```java
/** Holds an angle at the lowest command priority. */
public Command hold(Angle angle) {
  return run(coroutine -> {
    while (true) {
      setGoalAngle(angle);
      coroutine.yield();
    }
  })
      .withPriority(Command.LOWEST_PRIORITY)
      .named("Intake Hold");
}
```

### Your task

1. Call `setGoalAngle(angle)` continuously.
2. Yield during each iteration.
3. Apply `Command.LOWEST_PRIORITY`.
4. Name the command `"Intake Hold"`.

### Explanation

The hold command continuously maintains a pivot target while remaining active.

Assigning the lowest priority allows higher-priority commands to take precedence according to the Command V3 framework's priority rules. This is useful when the intake should normally maintain a resting angle but still respond to explicit operator or autonomous commands.

Verify the exact priority and preemption semantics against the WPILib 2027 Alpha version used by your project.

## Step 16: Create the Idle Command

Complete the idle command:

```java
/**
 * Coasts or brakes at zero applied voltage at the lowest command priority.
 *
 * @return idle command
 */
public Command idle() {
  return run(coroutine -> {
    rollerGoalVelocity = null;

    while (true) {
      io.stop();
      coroutine.yield();
    }
  })
      .withPriority(Command.LOWEST_PRIORITY)
      .named("Intake Idle");
}
```

### Your task

1. Clear the tracked roller velocity goal when the command starts.
2. Call `io.stop()` while the command is active.
3. Yield each iteration.
4. Set the command to the lowest priority.
5. Name it `"Intake Idle"`.

### Explanation

The idle command repeatedly delegates the stop behavior to the IO implementation. It is assigned the lowest priority so higher-priority commands can take precedence under the Command V3 priority rules.

**Review note:** The supplied implementation clears `rollerGoalVelocity` but does not clear `pivotGoalAngle`. It also calls `io.stop()` for both axes. Consider whether idle should clear both tracked goals to keep the software goal state consistent with the hardware stop request.

## Step 17: Implement Periodic Input Processing and Telemetry

The `periodic()` method refreshes the IO inputs, processes them through AdvantageKit, updates the roller visualization angle, and records additional outputs.

The supplied implementation:

```java
public void periodic() {
  io.updateInputs(inputs);
  Logger.processInputs("Intake", inputs);

  rollerSpokeLigament.setAngle(
      inputs.rollerPositionRotations * 360.0 % 360.0);

  // Logger.recordOutput("Intake/Mechanism", rollerMechanism2d);

  Logger.recordOutput(
      "Intake/Roller/VelocityRPS",
      inputs.rollerVelocityRotationsPerSecond);
  Logger.recordOutput(
      "Intake/Roller/AppliedVolts",
      inputs.rollerAppliedVolts);
  Logger.recordOutput(
      "Intake/Roller/StatorCurrentAmps",
      inputs.rollerStatorCurrentAmps);

  if (rollerGoalVelocity != null) {
    Telemetry.log("Intake/Roller/GoalVelocity", rollerGoalVelocity);
  }

  Logger.recordOutput("Intake/AngleRadians", inputs.pivotPositionRadians);
  Logger.recordOutput(
      "Intake/VelocityRadiansPerSecond",
      inputs.pivotVelocityRadiansPerSecond);
  Logger.recordOutput("Intake/AppliedVolts", inputs.pivotAppliedVolts);
  Logger.recordOutput("Intake/CurrentAmps", inputs.pivotCurrentAmps);
  Logger.recordOutput(
      "Intake/TemperatureCelsius",
      inputs.pivotTemperatureCelsius);

  if (pivotGoalAngle != null) {
    Telemetry.log("Intake/Pivot/GoalAngle", pivotGoalAngle);
  }
}
```

### Your task

1. Call `io.updateInputs(inputs)`.
2. Process the inputs using `Logger.processInputs("Intake", inputs)`.
3. Update the roller ligament angle from the measured roller position.
4. Record the roller velocity, voltage, and stator current.
5. Log the roller goal only when it exists.
6. Record the pivot angle, velocity, voltage, current, and temperature.
7. Log the pivot goal only when it exists.

### Explanation

The normal update flow is:

1. Read the latest sensor and motor data from the IO implementation.
2. Process those inputs through AdvantageKit.
3. Update any mechanism visualization.
4. Record additional mechanism-level outputs and active goals.

The roller visualization converts measured rotations to degrees:

\[
\theta_{\text{degrees}} = (\text{position rotations} \times 360) \bmod 360
\]

The measured values and requested goals are logged separately. This makes it possible to compare what the mechanism was asked to do with what it actually measured.

`Logger.processInputs(...)` handles the structured IO input record. The additional `Logger.recordOutput(...)` and `Telemetry.log(...)` calls publish selected values under explicit keys.

**Review note:** The roller and pivot output keys use different naming patterns. For example, roller voltage is recorded under `"Intake/Roller/AppliedVolts"`, while pivot voltage is recorded under `"Intake/AppliedVolts"`. Consider using a consistent `"Intake/Pivot/..."` prefix for pivot telemetry to make the AdvantageScope hierarchy easier to navigate.

## Step 18: Implement Simulation Periodic

The supplied class contains:

```java
/** Advances the IO simulation; call from the robot simulation periodic loop. */
public void simulationPeriodic() {
}
```

### Your task

Review whether the simulation implementation needs an update call and, if so, add the appropriate method to your `IntakeIO` contract and simulation implementation.

### Explanation

The method is currently empty. It does not advance a simulation model, pass a timestep, or provide battery voltage.

The current `IntakeIO` interface supplied for this lesson does not declare an `updateSim(...)` method. Do not call such a method unless you deliberately add it to the interface and implement it in the relevant IO classes.

If simulation stepping is handled elsewhere in the project, document that ownership so the mechanism simulation is not accidentally advanced zero times or multiple times per loop.

## Step 19: Implement the Mechanism Stop Method

Complete the stop method:

```java
/** Stops the pivot and roller and clears its active goal. */
public void stop() {
  rollerGoalVelocity = null;
  pivotGoalAngle = null;
  io.stop();
}
```

### Your task

1. Clear the tracked roller velocity goal.
2. Clear the tracked pivot angle goal.
3. Delegate the stop request to `io.stop()`.

### Explanation

Clearing both goals indicates that this mechanism class is no longer tracking either a velocity target or a position target.

Calling `io.stop()` delegates the actual motor stopping behavior to the IO implementation. The real implementation can send the appropriate Phoenix 6 requests, while a simulation implementation can stop its simulated motor output.

A stopped motor is not necessarily a mechanically locked pivot. Gravity and inertia may still move the mechanism, so the physical design and safety strategy remain important.

## Step 20: Review the Complete Command Flow

The intake has separate control paths for its roller and pivot.

### Roller closed-loop velocity control

1. The OpMode schedules `intake.setVelocity(...)`.
2. The command checks whether the requested velocity differs from the tracked goal.
3. The command sends the target to `io.setVelocity(...)` when needed.
4. The IO implementation applies the appropriate motor-controller request.
5. `periodic()` refreshes and logs the measured roller state.

### Roller open-loop control

1. The OpMode schedules `intake.setRollerDutyCycle(...)`.
2. The command clears the tracked velocity goal.
3. The duty cycle is clamped to the range `-1.0` through `1.0`.
4. The command multiplies the duty cycle by battery voltage.
5. The resulting voltage is sent to the roller through `io.setVoltage(0, rollerVolts)`.

### Pivot closed-loop position control

1. The OpMode schedules `intake.setAngle(...)`.
2. The command calls `setGoalAngle(...)`.
3. The requested angle is clamped to the configured pivot limits.
4. The IO implementation receives the position request.
5. The motor controller works toward the requested angle.
6. `periodic()` refreshes and logs the measured pivot state.

### Pivot open-loop control

1. The OpMode schedules `intake.setPivotDutyCycle(...)`.
2. The command clears the tracked pivot goal.
3. The duty cycle is clamped to the range `-1.0` through `1.0`.
4. The command multiplies the duty cycle by battery voltage.
5. The resulting voltage is sent to the pivot through `io.setVoltage(pivotVolts, 0)`.

The mechanism class defines the robot-facing behavior. The IO implementation translates those requests into hardware-specific operations.

## Step 21: Verification Checklist

Before completing the lesson, verify the following:

- [ ] `Intake` implements `Mechanism`.
- [ ] The IO implementation is supplied through the constructor.
- [ ] Inputs use `IntakeIOInputsAutoLogged`.
- [ ] Roller and pivot goals are tracked separately.
- [ ] `Mechanism2d` is updated using measured roller position.
- [ ] `getVelocity()` returns roller velocity as an `AngularVelocity`.
- [ ] `getPosition()` returns roller position as an `Angle`.
- [ ] Both goal getters use `Optional`.
- [ ] Voltage and current getters return unit-aware measures.
- [ ] Pivot goals are clamped to the configured limits.
- [ ] Roller velocity control uses `run()` and `coroutine.yield()`.
- [ ] Pivot position control uses `run()` and `coroutine.yield()`.
- [ ] Both duty-cycle requests are clamped between `-1.0` and `1.0`.
- [ ] Roller duty cycle controls only the roller.
- [ ] Pivot duty cycle controls only the pivot.
- [ ] The velocity trigger checks measured roller velocity.
- [ ] The hold and idle commands use the intended lowest priority.
- [ ] `periodic()` refreshes and processes AdvantageKit inputs.
- [ ] Roller and pivot telemetry are recorded.
- [ ] `simulationPeriodic()` has a clearly defined responsibility.
- [ ] `stop()` clears both tracked goals and calls `io.stop()`.
- [ ] Command interruption and motor output behavior are verified against the project's Command V3 implementation.

## Code Review Items in the Supplied Implementation

These are observations about the supplied Java file, not changes silently applied by this lesson.

1. **Unused imports and constant:** `Degrees` and `LOOP_PERIOD_SECONDS` are not used in the shown class. Remove them or use them if they are needed by the intended implementation.
2. **Combined voltage getter:** Adding roller and pivot applied voltages is not generally a meaningful total supply voltage. Prefer exposing the two motor voltages separately, or clearly document why the sum is useful.
3. **Combined current getter:** The sum of the two reported motor currents can be useful as an aggregate, but it should not be confused with a direct battery-current measurement.
4. **Command names:** Both open-loop commands are named `"Intake Set Duty Cycle"`. Distinct names would make logs and debugging clearer.
5. **Idle goal tracking:** `idle()` clears the roller goal but not the pivot goal, even though `io.stop()` stops both axes. Consider clearing both.
6. **Visualization publishing:** The `Mechanism2d` output call is commented out, so the visualization is not published by this method as written.
7. **Simulation update:** `simulationPeriodic()` is empty, and the current `IntakeIO` interface does not include a simulation-update method. Decide where simulation stepping belongs.
8. **Command termination:** The open-loop commands repeatedly send voltage while active. Verify what happens when they are interrupted and ensure the command bindings or a follow-up command put the mechanism in a safe state.
9. **Goal state after other commands:** `setVelocity()` tracks the roller goal, while `setRollerDutyCycle()` clears it. `setAngle()` tracks the pivot goal, while `setPivotDutyCycle()` clears it. Verify that all command transitions and stop/idle paths keep those software-side goals consistent with actual control mode.

## Challenge Questions

1. Why does `Intake` depend on `IntakeIO` rather than `IntakeIOReal`?
2. What does `Logger.processInputs(...)` provide?
3. Why are measured values stored separately from requested goals?
4. Why does the pivot helper clamp angles before sending them to IO?
5. Why do the goal getters return `Optional` values?
6. What does `coroutine.yield()` do in a Command V3 coroutine?
7. Why does a duty-cycle command multiply its request by battery voltage?
8. What is the difference between `setVelocity()` and `setRollerDutyCycle()`?
9. What is the difference between `setAngle()` and `setPivotDutyCycle()`?
10. Why should the `near(...)` trigger check measured velocity instead of the requested goal?
11. What is the purpose of assigning the hold and idle commands the lowest priority?
12. Why might adding two motor voltages not represent a meaningful electrical measurement?
13. Where should simulation stepping occur if `IntakeIO` does not expose an update method?
14. What should happen to motor output when an open-loop command is interrupted?
15. How would you unit test pivot angle clamping without connecting real hardware?

## Expected Outcome

After completing this lesson, you should have an `Intake` mechanism class that exposes unit-aware roller and pivot measurements, tracks their goals independently, coordinates real or simulated IO, creates reusable Command V3 commands and triggers, and records telemetry through AdvantageKit.

The key design principle is that **the mechanism translates robot behavior into IO requests while keeping hardware details, command behavior, measured state, and telemetry clearly separated.**
