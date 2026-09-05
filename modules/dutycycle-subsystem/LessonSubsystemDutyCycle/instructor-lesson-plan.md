# FRC Hopper Subsystem — Instructor Lesson Plan

## Overview

This lesson teaches students how to build a complete FRC hopper subsystem using an **IO-layer architecture**.

Students will start with a basic command-based robot and progressively build:

```text
RobotContainer
      |
      v
    Hopper
      |
      v
   HopperIO
    /    \
   /      \
HopperReal HopperSim
   |          |
 TalonFX    DCMotorSim
   |          |
Real Motor  Simulated Motor
    \        /
     \      /
  HopperIOInputs
       |
       v
 AdvantageKit
       |
       v
 AdvantageScope
```

The goal is not simply to make the hopper run.

Students should understand **why the code is separated into these different layers** and how the same subsystem can operate on both a real robot and in simulation.

---

# 1. Learning Objectives

By the end of this lesson, students should be able to:

### Robot Architecture

* Explain the purpose of `Robot.java`.
* Explain the purpose of `RobotContainer`.
* Explain how commands interact with subsystems.
* Explain how controller buttons trigger commands.

### Subsystems

* Explain what a WPILib `SubsystemBase` is.
* Explain why subsystem code should expose high-level robot actions.
* Explain why hardware-specific code should not be placed directly into commands.

### IO Architecture

* Explain what an IO interface is.
* Explain the difference between an interface and an implementation.
* Explain dependency injection.
* Explain why `HopperReal` and `HopperSim` both implement `HopperIO`.

### Real Hardware

* Create and configure a CTRE `TalonFX`.
* Configure motor inversion.
* Configure brake/coast behavior.
* Configure current limits.
* Read motor status signals.
* Convert CTRE units into useful robot units.

### Simulation

* Create a `DCMotorSim`.
* Model a Kraken X60 motor.
* Understand moment of inertia.
* Convert duty cycle into simulated voltage.
* Update a physics simulation.
* Connect the simulated motor to a simulated `TalonFX`.

### Logging

* Understand `HopperIOInputs`.
* Understand `@AutoLog`.
* Log subsystem inputs with AdvantageKit.
* Inspect subsystem behavior in AdvantageScope.

---

# 2. Prerequisites

Students should already understand:

* Java classes
* Java interfaces
* Java methods
* Java fields
* Constructors
* `if` statements
* `switch` statements
* basic object-oriented programming
* WPILib command-based programming
* Xbox controller bindings
* basic Git/GitHub usage

Students do **not** need to understand motor simulation before beginning this lesson.

---

# 3. Recommended Duration

| Section                     | Suggested Time |
| --------------------------- | -------------: |
| RobotContainer              |         20 min |
| Hopper subsystem            |         20 min |
| HopperIO                    |         25 min |
| HopperReal                  |         35 min |
| HopperSim                   |         40 min |
| Connecting everything       |         20 min |
| Simulation testing          |         30 min |
| AdvantageKit/AdvantageScope |         20 min |
| Real robot testing          |         30 min |
| Final challenge             |      30–60 min |

### Total

Approximately **4–5 hours**, depending on student experience.

This can be divided into multiple training sessions.

---

# 4. Teaching Philosophy

The most important part of this lesson is to avoid giving students the entire finished subsystem immediately.

Students should build the architecture progressively.

Use this progression:

```text
Starting Code
      ↓
One Change
      ↓
Explain Why
      ↓
Compile
      ↓
Test
      ↓
Next Change
```

After each major section, ask students to explain what the new code does **before moving on**.

---

# 5. Lesson 1 — Robot Architecture

## Goal

Students understand where `RobotContainer` fits into the command-based architecture.

### Concepts

Introduce:

```text
Robot
 |
 +-- RobotContainer
       |
       +-- Subsystems
       |
       +-- Commands
       |
       +-- Controller bindings
```

Explain that `Robot.java` is generally responsible for the robot lifecycle while `RobotContainer` describes the robot's structure.

---

## Instructor Discussion

Ask:

> Where should the code that controls the hopper motor live?

Students may suggest:

* `Robot.java`
* `RobotContainer`
* a command
* the hopper subsystem

Lead them toward:

> The subsystem should own the mechanism.

Then introduce the idea that even the subsystem will eventually be separated into hardware-specific IO code.

---

## Checkpoint

Students should be able to explain:

> RobotContainer connects operator input to robot actions, while the subsystem owns the mechanism.

---

# 6. Lesson 2 — Adding Hopper to RobotContainer

## Goal

Students add the hopper to the robot and create its hardware/simulation implementation.

Start with:

```java
private final CommandXboxController m_driverController =
    new CommandXboxController(
        OperatorConstants.kDriverControllerPort);
```

Then introduce:

```java
private final Hopper m_Hopper;
```

Explain that this is a reference to the hopper subsystem.

---

## Real vs Simulation

Introduce:

```java
switch (Constants.currentMode) {
  case REAL:
    m_Hopper = new Hopper(new HopperReal());
    break;

  case SIM:
    m_Hopper = new Hopper(new HopperSim());
    break;

  default:
    m_Hopper = new Hopper(new HopperIO() {});
    break;
}
```

Draw:

```text
                    Hopper
                       |
             +---------+---------+
             |                   |
         HopperReal          HopperSim
             |                   |
           TalonFX            DCMotorSim
```

---

## Important Teaching Point

Students should understand:

```java
new Hopper(new HopperReal())
```

The inner object is created first:

```java
new HopperReal()
```

Then passed into:

```java
new Hopper(...)
```

Explain this as **dependency injection**.

The `Hopper` does not need to know how the motor works.

It is simply given an object that knows how to operate the motor.

---

## Controller Binding

Add:

```java
m_driverController.a()
    .whileTrue(
        Commands.run(
            () -> m_Hopper.setDutyCycle(0.75)))
    .onFalse(
        Commands.runOnce(
            () -> m_Hopper.stop()));
```

Explain:

```text
A pressed
   ↓
setDutyCycle(0.75)
   ↓
Hopper
   ↓
HopperIO
   ↓
Real or Sim implementation
```

---

## Checkpoint

Ask:

> What happens when the A button is pressed?

Students should describe the complete path.

---

# 7. Lesson 3 — Understanding the Hopper Subsystem

## Goal

Students understand why `Hopper.java` exists separately from the motor implementation.

Introduce:

```java
public class Hopper extends SubsystemBase
```

Explain that the hopper is a WPILib subsystem.

Then:

```java
private final HopperIO io;
```

The subsystem contains an IO implementation.

---

## Constructor

```java
public Hopper(HopperIO io) {
    this.io = io;
}
```

Explain:

> The Hopper does not decide whether it is talking to a real motor or a simulated motor.

It receives an IO object.

---

## Periodic

Introduce:

```java
@Override
public void periodic() {
    io.periodic();
    io.updateInputs(inputs);
    Logger.processInputs("Hopper/inputs", inputs);
}
```

Explain the data flow:

```text
Hardware / Simulation
        ↓
   updateInputs()
        ↓
 HopperIOInputs
        ↓
 AdvantageKit
```

---

## Command API

Students should understand:

```java
public void setDutyCycle(double dutyCycle)
```

is the subsystem's public API.

Commands should not directly call:

```java
hopperMotor.set(...)
```

Instead:

```java
m_Hopper.setDutyCycle(...)
```

---

## Checkpoint

Ask:

> If we replaced the TalonFX with another motor controller, should RobotContainer need to change?

Expected answer:

> No.

---

# 8. Lesson 4 — Building HopperIO

## Goal

Students create the interface that both real hardware and simulation will implement.

Start with:

```java
public interface HopperIO {
```

Explain that an interface defines a contract.

Then:

```java
default void setDutyCycle(double dutyCycle) {}
```

Both implementations can provide their own behavior.

---

## Inputs

Introduce:

```java
@AutoLog
class HopperIOInputs {
```

Add each field individually.

### Velocity

```java
public double velocityOfHopperMotorRPS = 0.0;
```

Explain:

> Revolutions per second tells us how quickly the motor is spinning.

### Stator Current

```java
public double statorCurrentOfHopperAmps = 0.0;
```

### Torque Current

```java
public double torqueCurrentHopperAmps = 0.0;
```

### Voltage

```java
public double outputOfHopperVolts = 0.0;
```

### Acceleration

```java
public double accelerationOfHopper = 0.0;
```

### Position

```java
public double positionOfHopperMotorRotations = 0.0;
```

### Duty Cycle

```java
public double dutyCycleOutput = 0.0;
```

### Connection

```java
public boolean hopperConnected = false;
```

---

## Explain `@AutoLog`

Tell students:

> We want the same set of telemetry whether the robot is real or simulated.

`@AutoLog` allows AdvantageKit to generate:

```java
HopperIOInputsAutoLogged
```

---

## Data Flow

```text
HopperReal
     |
     | writes
     v
HopperIOInputs
     ^
     |
     | writes
     |
HopperSim
```

---

## Checkpoint

Ask:

> Why should `HopperReal` and `HopperSim` both populate the same input fields?

Expected answer:

> So the rest of the robot sees the same data regardless of whether it is running on real hardware or in simulation.

---

# 9. Lesson 5 — Building HopperReal

## Goal

Students connect the IO interface to an actual TalonFX.

---

## Step 1 — Create the Motor

```java
hopperMotor = new TalonFX(
    Constants.HopperConstants.hopperMotorId,
    new CANBus("rio"));
```

Explain:

* motor CAN ID
* CAN bus
* hardware object

---

## Step 2 — Create Configuration

```java
TalonFXConfiguration config =
    new TalonFXConfiguration();
```

Explain that configuration allows us to define how the motor controller behaves.

---

## Step 3 — Motor Direction

```java
config.MotorOutput.Inverted =
    Constants.HopperConstants.isInverted
        ? InvertedValue.Clockwise_Positive
        : InvertedValue.CounterClockwise_Positive;
```

Teach the ternary operator:

```text
condition ? trueValue : falseValue
```

---

## Step 4 — Neutral Mode

```java
config.MotorOutput.NeutralMode =
    Constants.HopperConstants.isCoast
        ? NeutralModeValue.Coast
        : NeutralModeValue.Brake;
```

Explain:

* Coast
* Brake

---

## Step 5 — Current Limits

Explain why current limits are important.

```java
config.CurrentLimits.StatorCurrentLimitEnable = true;
config.CurrentLimits.StatorCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

And:

```java
config.CurrentLimits.SupplyCurrentLimitEnable = true;
config.CurrentLimits.SupplyCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

Discuss the difference between motor-side and supply-side current.

---

## Step 6 — Apply Configuration

```java
hopperMotor.getConfigurator().apply(config);
```

Important concept:

> Creating a configuration object does not automatically configure the motor.

The configuration must be applied.

---

## Step 7 — Create Status Signals

```java
velocitySignal = hopperMotor.getVelocity();
statorCurrentSignal = hopperMotor.getStatorCurrent();
voltageSignal = hopperMotor.getMotorVoltage();
accelerationSignal = hopperMotor.getAcceleration();
```

Explain that these are references to telemetry provided by the TalonFX.

---

## Step 8 — Refresh Signals

```java
BaseStatusSignal.refreshAll(
    velocitySignal,
    statorCurrentSignal,
    voltageSignal,
    accelerationSignal);
```

Explain that the values should be refreshed before reading them.

---

## Step 9 — Populate Inputs

Students copy values into:

```java
HopperIOInputs
```

For example:

```java
inputs.velocityOfHopperMotorRPS =
    velocitySignal.getValue()
        .in(Rotation.per(Second));
```

Discuss units carefully.

---

## Important Unit Conversion Check

The following:

```java
.in(Rotation.per(Second))
```

already produces RPS.

Therefore:

```java
.in(Rotation.per(Second)) * 100
```

is **not** a unit conversion.

If a multiplier is intentionally used for calibration, it should be documented.

Otherwise it should be removed.

---

## Position

```java
inputs.positionOfHopperMotorRotations =
    hopperMotor.getPosition()
        .getValue()
        .in(Rotation);
```

---

## Connection Status

```java
inputs.hopperConnected =
    hopperMotor.isConnected();
```

Discuss why this is useful for diagnostics.

---

## Step 10 — Motor Control

```java
hopperMotor.set(dutyCycle);
```

This is intentionally hidden inside `HopperReal`.

Commands never interact with the TalonFX directly.

---

## Step 11 — Stop

```java
hopperMotor.stopMotor();
```

Explain why a dedicated `stop()` method is useful.

---

## Checkpoint

Ask students to trace:

```text
A button
 ↓
Hopper.setDutyCycle()
 ↓
HopperIO.setDutyCycle()
 ↓
HopperReal.setDutyCycle()
 ↓
TalonFX.set()
```

---

# 10. Lesson 6 — Building HopperSim

## Goal

Students build a physics-based simulated hopper.

Explain:

> Simulation should not simply return fake values.

Instead, we want the simulated motor to respond to inputs like a real mechanism.

---

# 11. Motor Model

Start with:

```java
private static final DCMotor MOTOR =
    DCMotor.getKrakenX60Foc(1);
```

Explain:

* motor type
* number of motors
* motor electrical characteristics

---

# 12. Moment of Inertia

```java
private static final double MOMENT_OF_INERTIA = 0.035;
```

Explain that moment of inertia describes how difficult it is to accelerate the rotating mechanism.

Higher inertia:

```text
Slower acceleration
```

Lower inertia:

```text
Faster acceleration
```

---

# 13. Create DCMotorSim

```java
motorSim = new DCMotorSim(
    LinearSystemId.createDCMotorSystem(
        MOTOR,
        MOMENT_OF_INERTIA,
        1),
    MOTOR);
```

Explain the three major pieces:

```text
DCMotor
   +
Moment of Inertia
   +
Gear Ratio
   =
Physics Model
```

---

# 14. Simulated TalonFX

Create:

```java
hopperMotor = new TalonFX(
    Constants.HopperConstants.hopperMotorId,
    new CANBus("rio"));
```

Then:

```java
hopperMotorSim =
    hopperMotor.getSimState();
```

Explain why we have both:

```text
DCMotorSim
```

and:

```text
TalonFXSimState
```

The `DCMotorSim` models the physics.

The `TalonFXSimState` represents the simulated state of the motor controller.

---

# 15. Duty Cycle → Voltage

Introduce:

```java
double appliedVoltage =
    MathUtil.clamp(
        dutyCycle * 12.0,
        -12.0,
        12.0);
```

Explain:

```text
100% = +12 V
50%  = +6 V
0%   = 0 V
-50% = -6 V
```

The clamp prevents the simulated voltage from exceeding the expected battery range.

---

# 16. Update Simulation

```java
motorSim.setInputVoltage(appliedVoltage);
motorSim.update(0.02);
```

Explain:

> FRC robot code normally runs in approximately 20 ms loops.

Therefore:

```java
0.02 seconds = 20 ms
```

---

# 17. Feed Simulation Back to TalonFX

```java
hopperMotorSim.setRawRotorPosition(
    motorSim.getAngularPosition());

hopperMotorSim.setRotorVelocity(
    motorSim.getAngularVelocity());

hopperMotorSim.setRotorAcceleration(
    motorSim.getAngularAcceleration());
```

Explain the direction:

```text
Duty Cycle
    ↓
Voltage
    ↓
DCMotorSim
    ↓
Position / Velocity / Acceleration
    ↓
TalonFXSimState
```

---

# 18. Populate HopperIOInputs

Students populate:

```java
inputs.positionOfHopperMotorRotations =
    motorSim.getAngularPositionRotations();
```

Velocity:

```java
inputs.velocityOfHopperMotorRPS =
    motorSim.getAngularVelocityRPM() / 60.0;
```

Acceleration:

```java
inputs.accelerationOfHopper =
    motorSim.getAngularAccelerationRadPerSecSq()
        / (2.0 * Math.PI);
```

Discuss unit conversions.

---

# 19. Current

```java
inputs.statorCurrentOfHopperAmps =
    Math.abs(motorSim.getCurrentDrawAmps());
```

For this simplified simulation:

```java
inputs.torqueCurrentHopperAmps =
    inputs.statorCurrentOfHopperAmps;
```

Explain that this is an approximation for the lesson and not necessarily a complete physical model of CTRE torque current.

---

# 20. Simulation Connection

Students should understand:

```text
RobotContainer
      ↓
Hopper
      ↓
HopperSim
      ↓
DCMotorSim
      ↓
Physical behavior
      ↓
HopperIOInputs
      ↓
AdvantageKit
```

---

# 21. Lesson 7 — Connecting Everything

At this point, have students stop coding and explain the entire architecture.

Draw:

```text
Controller
    |
    v
RobotContainer
    |
    v
  Hopper
    |
    v
 HopperIO
  /    \
 /      \
Real    Sim
 |       |
TalonFX DCMotorSim
 |       |
 \       /
  \     /
 Inputs
    |
    v
 Logger
```

---

## Instructor Questions

Ask:

### Question 1

> Does Hopper know whether it is running on real hardware?

Expected:

> No.

### Question 2

> Does HopperReal know about the controller?

Expected:

> No.

### Question 3

> Does HopperSim know about RobotContainer?

Expected:

> No.

### Question 4

> Where is the decision between real and simulation made?

Expected:

> RobotContainer.

---

# 22. Lesson 8 — Testing Simulation

## Goal

Students verify that the simulation actually behaves like a motor.

Run the robot in simulation.

Press the A button.

Expected behavior:

```text
A pressed
 ↓
75% duty cycle
 ↓
~9 V simulated input
 ↓
Motor accelerates
 ↓
Velocity increases
```

When the button is released:

```text
A released
 ↓
stop()
 ↓
Duty cycle = 0
 ↓
Motor stops receiving voltage
```

---

## AdvantageScope

Students should inspect:

```text
Hopper/inputs
```

and look for:

* velocity
* position
* acceleration
* voltage
* current
* duty cycle
* connection status

---

# 23. Simulation Investigation

Have students hold the A button and observe velocity.

Then release it.

Ask:

> Does velocity instantly become zero?

Students should observe that the simulated motor has physical behavior rather than simply switching from 75% to 0 RPM.

---

# 24. Experiment — Moment of Inertia

Change:

```java
MOMENT_OF_INERTIA = 0.035;
```

to a larger value.

For example:

```java
MOMENT_OF_INERTIA = 0.10;
```

Run the simulation again.

Ask:

> What changed?

Expected:

> The motor accelerates more slowly.

Then decrease the value.

Discuss why physical mechanism modeling matters.

---

# 25. Lesson 9 — AdvantageKit and AdvantageScope

## Goal

Students understand that telemetry is part of subsystem design.

Review:

```java
Logger.processInputs(
    "Hopper/inputs",
    inputs);
```

Explain that the subsystem is recording the state of the mechanism.

---

## Student Investigation

Have students identify:

| Signal         | Meaning                                   |
| -------------- | ----------------------------------------- |
| Velocity       | How fast the motor is rotating            |
| Position       | Motor rotation amount                     |
| Acceleration   | How quickly velocity changes              |
| Voltage        | Applied motor voltage                     |
| Stator Current | Motor-side current                        |
| Torque Current | Current associated with torque production |
| Duty Cycle     | Requested motor output                    |
| Connected      | Whether the motor controller is available |

---

# 26. Lesson 10 — Real Robot Testing

Only perform this section after simulation works.

Before enabling the motor:

* verify motor CAN ID
* verify inversion
* verify current limits
* verify mechanical safety
* verify robot is supported
* verify emergency stop
* verify correct battery voltage
* verify students are clear of moving mechanisms

---

## Test Sequence

### Test 1

Run with very small duty cycle.

```text
10%
```

Verify direction.

### Test 2

Test:

```text
25%
```

### Test 3

Test:

```text
50%
```

Only proceed if the mechanism behaves correctly.

---

# 27. Final Student Challenge

Students should extend the hopper.

Choose one or more:

### Challenge 1 — Add Temperature

Add:

```java
hopperTemperatureCelsius
```

to `HopperIOInputs`.

Implement it in:

* `HopperReal`
* `HopperSim`

---

### Challenge 2 — Add Reverse

Create a command that runs:

```text
-75%
```

---

### Challenge 3 — Add Speed Control

Create:

```java
setSpeed(double rps)
```

---

### Challenge 4 — Add Position Reset

Add:

```java
resetPosition()
```

---

### Challenge 5 — Add Deadband

Prevent very small commands from moving the motor.

---

### Challenge 6 — Tune the Simulation

Modify:

```java
MOMENT_OF_INERTIA
```

until the simulation behaves more like the physical hopper.

---

# 28. Assessment

## Level 1 — Beginner

Student can:

* identify each class
* explain what a subsystem is
* explain what a TalonFX is
* run the robot in simulation

---

## Level 2 — Developing

Student can:

* explain `HopperIO`
* explain `HopperReal`
* explain `HopperSim`
* add telemetry
* modify a controller binding

---

## Level 3 — Proficient

Student can:

* explain dependency injection
* explain why simulation and real hardware share an IO interface
* trace command execution through the entire architecture
* interpret AdvantageScope data
* make changes to both real and simulated implementations

---

## Level 4 — Advanced

Student can:

* create another subsystem using the same architecture
* create a realistic simulation
* add new IO inputs
* implement closed-loop control
* diagnose real-vs-simulation differences

---

# 29. Suggested Teaching Order

```text
1. Robot Architecture
        ↓
2. RobotContainer
        ↓
3. Hopper
        ↓
4. HopperIO
        ↓
5. HopperReal
        ↓
6. HopperSim
        ↓
7. Connect Everything
        ↓
8. Simulation Testing
        ↓
9. AdvantageKit / AdvantageScope
        ↓
10. Real Robot Testing
        ↓
11. Final Challenge
```

Do not skip directly to `HopperReal` or `HopperSim`.

Students should understand **why those classes exist** before implementing them.

---

# 30. Instructor Summary

The most important concept of this lesson is not the TalonFX code.

It is the architecture:

```text
What does the mechanism do?
        ↓
      Hopper
        ↓
How can the mechanism be controlled?
        ↓
     HopperIO
        ↓
How does the real robot do it?
        ↓
   HopperReal
        ↓
How does simulation do it?
        ↓
    HopperSim
```

This architecture allows the same subsystem to run with:

```text
REAL
```

or:

```text
SIM
```

without changing the commands that control it.

The same pattern can later be applied to:

* Intake
* Shooter
* Arm
* Elevator
* Climber
* Pivot
* Drivetrain
* Turret

The students should leave this lesson understanding that **good robot code separates the mechanism's behavior from the hardware used to implement that behavior**.
