# Exercise 2: The Shooter Mechanism

## Objective

Understand and verify a shooter mechanism that runs a flywheel in closed-loop velocity mode or open-loop duty-cycle mode, using Command V3 coroutines and AdvantageKit logging.

You will learn how to:

- Implement `Mechanism` and create commands with `run(...)`.
- Write looping coroutine commands with `coroutine.yield()`.
- Track a goal across commands without resending redundant requests.
- Name commands and set command priority.
- Expose measurements through getters and a `Trigger`.
- Log inputs and outputs with AdvantageKit and WPILib `Telemetry`.

## Before you begin

Read these files first:

- `ShooterIO.java` for the methods and input fields the mechanism uses.
- `ShooterIOReal.java` for what each IO call does on hardware.
- `DefaultTeleop.java` for how the commands are invoked.

## Task 1: Fields and constructor

```java
public class Shooter implements Mechanism {
  private final ShooterIO io;
  private final ShooterIOInputsAutoLogged inputs = new ShooterIOInputsAutoLogged();
  private AngularVelocity goalVelocity = null;
  ...
  public Shooter(ShooterIO io) { this.io = io; }
}
```

**Requirements and questions:**

1. The IO implementation is passed in, not created inside `Shooter`. Why does that allow `Robot` to choose real or simulated hardware?
2. What does `implements Mechanism` give the class? (Look at where `run(...)` comes from.)
3. `goalVelocity` is `null` when velocity control is not active. Why is `null` used instead of `0 RPM`?
4. The constructor does not register a default command. Find where `idle()` gets scheduled in the project. What is the role of `Command.LOWEST_PRIORITY` (Task 5) in that design?

## Task 2: Measurement getters

| Method | Source field | Type returned |
|---|---|---|
| `getVelocity()` | `inputs.velocityRotationsPerSecond` | `AngularVelocity` |
| `getGoalVelocity()` | `goalVelocity` | `Optional<AngularVelocity>` |
| `getVoltage()` | `inputs.appliedVolts` | `Voltage` |
| `getCurrent()` | `inputs.statorCurrentAmps` | `Current` |

**Tasks:**

1. Each getter wraps a primitive `double` from the input structure into a unit type. Explain why.
2. `getVoltage()` and `getCurrent()` use fully qualified names (`org.wpilib.units.measure.Voltage`). Replace them with imports and static imports (`Amps`) so they match the rest of the file.
3. The unused `Degrees` import can be removed.

## Task 3: Closed-loop velocity command

```java
public Command setVelocity(AngularVelocity velocity) {
  return run(coroutine -> {
    while (true) {
      if (goalVelocity == null || !velocity.isEquivalent(goalVelocity)) {
        io.setVelocity(velocity.in(RotationsPerSecond));
      }
      goalVelocity = velocity;
      coroutine.yield();
    }
  }).named("Shooter Set Velocity");
}
```

**Tasks:**

1. Explain what `coroutine.yield()` does and why the loop never terminates on its own.
2. The `io.setVelocity(...)` call is skipped when the goal has not changed. Why is it unnecessary to resend the same request every 20 ms? (Hint: TalonFX control requests persist.)
3. When does the condition `goalVelocity == null` become true? Which commands set it to `null`?
4. Why is `goalVelocity` assigned after the IO call?
5. `isEquivalent(...)` compares two unit measures. What would go wrong if you used `==` on two `AngularVelocity` objects?

## Task 4: Open-loop duty-cycle command

```java
public Command set(double dutyCycle) {
  return run(coroutine -> {
    goalVelocity = null;
    while (true) {
      io.setVoltage(
          RobotController.getMeasureBatteryVoltage().in(Volts)
              * Math.max(-1.0, Math.min(1.0, dutyCycle)));
      coroutine.yield();
    }
  }).named("Shooter Set Duty Cycle");
}
```

**Tasks:**

1. Describe the conversion from duty cycle to voltage. What happens when `dutyCycle` is 1.5?
2. Why is the battery voltage read inside the loop instead of once?
3. Why is `goalVelocity = null` set before the loop?
4. How does this differ from the `DutyCycleOut` request used in older versions of this lab? (Look at `ShooterIOReal.setVoltage(...)`.)

## Task 5: Idle command and priority

```java
public Command idle() {
  return run(coroutine -> {
    goalVelocity = null;
    while (true) { io.stop(); coroutine.yield(); }
  })
      .withPriority(Command.LOWEST_PRIORITY)
      .named("Shooter Idle");
}
```

**Tasks:**

1. Why does `idle()` use `LOWEST_PRIORITY`? What happens when the driver presses a button while idle is running?
2. What does `io.stop()` do in `ShooterIOReal`? Does it brake or coast?
3. Why is it acceptable for idle to call `io.stop()` every loop even though `setVelocity` avoids redundant calls?

## Task 6: The `near(...)` trigger

```java
public Trigger near(AngularVelocity velocity, AngularVelocity tolerance) {
  return new Trigger(() -> getVelocity().isNear(velocity, tolerance));
}
```

**Tasks:**

1. Describe what the returned `Trigger` does and when it is evaluated.
2. Write an example binding that starts a feeder command only while the shooter is within 50 RPM of 4650 RPM:
   ```java
   robot.shooter.near(RPM.of(4650), RPM.of(50)).whileTrue(/* feeder command */);
   ```
3. Why is the trigger built on the measured velocity and not on `goalVelocity`?

## Task 7: `periodic()` and telemetry

```java
public void periodic() {
  io.updateInputs(inputs);
  Logger.processInputs("Shooter", inputs);

  spokeLigament.setAngle(inputs.positionRotations * 360.0 % 360.0);

  //Logger.recordOutput("Shooter/Mechanism", mechanism2d);
  Logger.recordOutput("Shooter/VelocityRPS", inputs.velocityRotationsPerSecond);
  Logger.recordOutput("Shooter/AppliedVolts", inputs.appliedVolts);
  Logger.recordOutput("Shooter/StatorCurrentAmps", inputs.statorCurrentAmps);

  if (goalVelocity != null) {
    Telemetry.log("Shooter/GoalVelocity", goalVelocity);
  }
}
```

**Tasks:**

1. Why must `updateInputs` run before `processInputs`?
2. `processInputs` already logs every field, including velocity, voltage, and current. Why might the extra `recordOutput` calls still be useful? What is the cost of duplicating them?
3. The `Mechanism2d` visual is updated every loop but its `recordOutput` is commented out. Enable it, run in simulation, and confirm the spoke rotates. Does `Mechanism2d` need to be logged every loop?
4. Why is `Telemetry.log("Shooter/GoalVelocity", ...)` guarded by a null check?
5. `simulationPeriodic()` is empty. Why do the simulation physics live in `ShooterIOSim.updateInputs(...)` instead?

## Verification checklist

- [ ] `setVelocity(...)` sends the IO request only when the goal changes.
- [ ] `set(...)` clamps its input and uses the battery voltage.
- [ ] `idle()` uses lowest priority and clears the goal.
- [ ] All three commands have names (`Shooter Set Velocity`, `Shooter Set Duty Cycle`, `Shooter Idle`).
- [ ] `periodic()` updates inputs before logging.
- [ ] `near(...)` evaluates measured velocity.
- [ ] Unused imports are removed and the project compiles.

## Reflection questions

1. What is the relationship between a command and a mechanism?
2. What problem does the goal-change check in `setVelocity` solve, and what bug could it introduce if `set(...)` forgot to clear `goalVelocity`?
3. Why does the mechanism call `ShooterIO` methods rather than TalonFX methods directly?
4. How does AdvantageKit help diagnose problems after a match?
5. When would you choose velocity control over duty-cycle control?

## Optional extension

Add a `stopped()` trigger that is active when the measured velocity is within 5 RPM of zero, and use it in a unit test with a fake `ShooterIO` implementation.
