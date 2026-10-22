# Shooter Mechanism Lab

In this lab you will work through a complete shooter flywheel implementation built with **WPILib Command V3** (`org.wpilib.command3`), **AdvantageKit**, and **CTRE Phoenix 6**. The shooter supports closed-loop velocity control and open-loop duty-cycle control, and runs on real hardware or in simulation.

The lab has four exercises. Each one covers a different layer of the software, from operator input down to the motor controller.

## Learning Objectives

By completing this lab, you will learn how to:

- Bind controller buttons to commands with `whileTrue(...)`.
- Write a mechanism with Command V3 coroutine commands (`run(...)`, `coroutine.yield()`, `named(...)`, `withPriority(...)`).
- Use WPILib unit types (`AngularVelocity`, `RPM`, `RotationsPerSecond`, `Volts`).
- Separate hardware-independent logic from hardware-specific code using a Java interface with default methods.
- Log inputs and outputs with AdvantageKit (`@AutoLog`, `Logger.processInputs`, `Logger.recordOutput`).
- Control a TalonFX with `VelocityVoltage` and `VoltageOut` requests.
- Simulate a Kraken X60 flywheel with `FlywheelSim` and `TalonFXSimState`.

## Project Files

| File | Package | Role |
|---|---|---|
| `DefaultTeleop.java` | `first.robot.opmodes.teleop` | Binds controller buttons to shooter commands |
| `Shooter.java` | `first.robot.mechanisms` | Mechanism: commands, trigger, telemetry |
| `ShooterIO.java` | `first.robot.mechanisms` | Hardware abstraction and `@AutoLog` input structure |
| `ShooterIOReal.java` | `first.robot.mechanisms` | TalonFX hardware implementation |
| `ShooterIOSim.java` | `first.robot.mechanisms` | Simulated TalonFX + `FlywheelSim` implementation |

Supporting classes that are not part of the exercises: `Robot`, `ShooterConstants` (CAN ID, gear ratio, moment of inertia, `createMotorConfiguration()`), and the generated `ShooterIOInputsAutoLogged`.

## Exercise Sequence

Complete the exercises in order. Each builds on the one before.

### Exercise 1: Default Teleop
**File:** `DefaultTeleop.java`
**Focus:** Controller bindings and command scheduling.
Four face buttons are bound to shooter commands. Two run closed-loop velocity targets, and two run open-loop duty cycles in opposite directions.
**Instructions:** [Exercise 1](docs/exercises/01-default-teleop.md)

### Exercise 2: Shooter Mechanism
**File:** `Shooter.java`
**Focus:** Mechanism commands, goal tracking, triggers, and telemetry.
The shooter exposes `setVelocity(...)`, `set(...)`, `idle()`, and `near(...)`, and logs through AdvantageKit and WPILib `Telemetry` in `periodic()`.
**Instructions:** [Exercise 2](docs/exercises/02-shooter-mechanism.md)

### Exercise 3: Shooter I/O Interface
**File:** `ShooterIO.java`
**Focus:** Interfaces, default methods, and `@AutoLog` inputs.
The interface defines seven measured inputs and four methods (`updateInputs`, `setVelocity`, `setVoltage`, `stop`).
**Instructions:** [Exercise 3](docs/exercises/03-shooter-io.md)

### Exercise 4: Real and Simulated Hardware
**Files:** `ShooterIOReal.java`, `ShooterIOSim.java`
**Focus:** Phoenix 6 control requests, signal reading, and physics simulation.
**Instructions:** [Exercise 4](docs/exercises/04-shooter-real.md)

---

## How the Pieces Fit Together

```text
              Operator Input
                    |
                    v
          +--------------------+
          |   DefaultTeleop    |   whileTrue(...) bindings
          +--------------------+
                    |
                    | shooter.setVelocity(...) / shooter.set(...)
                    v
          +--------------------+
          |      Shooter       |   commands, goal tracking,
          |    (Mechanism)     |   near() trigger, telemetry
          +--------------------+
                    |
                    | io.setVelocity / setVoltage / stop / updateInputs
                    v
          +--------------------+
          |     ShooterIO      |   interface + ShooterIOInputs
          +--------------------+
              ^           ^
              |           |
   +----------------+ +----------------+
   | ShooterIOReal  | | ShooterIOSim   |
   | TalonFX        | | TalonFX sim +  |
   |                | | FlywheelSim    |
   +----------------+ +----------------+
```

### Execution Flow

When the operator holds the bottom (A) face button:

1. `DefaultTeleop` has bound `faceDown()` to `shooter.setVelocity(RPM.of(2330.0))` with `whileTrue(...)`, so the command is scheduled while the button is held.
2. The command loops every cycle. The first time through (or whenever the requested velocity changes), it calls `io.setVelocity(...)` with the target in rotations per second. It then records the goal and yields.
3. `ShooterIOReal` sends a `VelocityVoltage` request to the TalonFX, which runs the velocity PID on the motor controller. `ShooterIOSim` sends the same request to a simulated TalonFX.
4. Every robot loop, `Shooter.periodic()` calls `io.updateInputs(inputs)`, passes the result to `Logger.processInputs("Shooter", inputs)`, and records output values.
5. When the button is released, the command ends. The lowest-priority `idle()` command then takes over and calls `io.stop()`.

## Getting Started

Before beginning, make sure you have:

- The project cloned and its dependencies resolved (WPILib 2027 `org.wpilib.*` packages, AdvantageKit, Phoenix 6).
- The supporting classes `Robot` and `ShooterConstants` available.
- Familiarity with Java classes, interfaces, lambdas, and `static import`.

### Recommended Workflow

1. Read the exercise instructions before opening the code.
2. Read the corresponding Java file and trace each method.
3. Complete the tasks and answer the questions.
4. Build the project and run applicable tests.
5. Commit before moving to the next exercise.

## Important Implementation Notes

### Velocity control versus duty-cycle control

- `setVelocity(AngularVelocity)` is **closed loop**. The TalonFX uses the `VelocityVoltage` request and the gains from `ShooterConstants.createMotorConfiguration()`.
- `set(double)` is **open loop**. It converts a duty cycle in the range -1.0 to 1.0 into a voltage by multiplying by the current battery voltage, then sends a `VoltageOut` request. Duty cycles outside the range are clamped.

### Goal tracking

`Shooter` keeps a nullable `goalVelocity`. It is non-null only while velocity control is active, which is why `getGoalVelocity()` returns an `Optional`. `set(...)` and `idle()` clear it.

### Unit consistency

The mechanism works in `AngularVelocity`. The IO layer works in primitive `double` values in **rotations per second**. The conversion happens in `Shooter.setVelocity(...)` through `velocity.in(RotationsPerSecond)`.

### Hardware safety

`ShooterIOReal` drives a physical motor. Before testing on hardware:

- Verify the CAN ID and the CAN bus (`CANPort.CAN_S0`).
- Verify motor direction, because `set(-0.3)` and negative velocities spin the flywheel in reverse.
- Secure the robot and clear the flywheel.
- Start with the low-speed and low-duty-cycle bindings.
- Keep the approved disable procedure within reach.

Simulation (`ShooterIOSim`) is the safest place to test first.

## Verification Checklist

At the end of the lab, verify that:

- [ ] Holding A runs the flywheel at 2330 RPM, and holding B runs it at 4650 RPM.
- [ ] Holding X drives +30% duty cycle, and holding Y drives -30%.
- [ ] Releasing every button returns the shooter to idle (`io.stop()`).
- [ ] `Shooter/GoalVelocity` is logged only while velocity control is active.
- [ ] `ShooterIOInputsAutoLogged` is generated and `Shooter` inputs appear in the log.
- [ ] `ShooterIOReal` and `ShooterIOSim` both override every method of `ShooterIO`.
- [ ] In simulation, measured velocity converges on the goal velocity.
- [ ] The project compiles and passes applicable tests and checks.

## Completion Criteria

You have completed the lab when all four exercises are done, the project builds, and you can explain how a button press becomes a `VelocityVoltage` or `VoltageOut` request, and how the measured result flows back into the log.
