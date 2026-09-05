# FRC Hopper Subsystem

## Building a Real + Simulated FRC Subsystem

In this lesson, you will build a complete hopper subsystem using an architecture that supports both:

* a **real FRC robot**
* a **simulated robot**

You will build the subsystem one piece at a time.

By the end, you will have:

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
```

---

# Learning Objectives

By the end of this lesson, you should be able to:

* explain what a subsystem is
* explain what `RobotContainer` does
* explain an IO interface
* explain dependency injection
* control a TalonFX
* read TalonFX telemetry
* create a simulated motor
* understand basic motor physics
* log subsystem data with AdvantageKit
* inspect robot data in AdvantageScope

---

## In Depth Explanations
## [RobotContainer In Depth](./InDepthExplainations/RobotContainer.md)
## [Hopper In Depth](./InDepthExplainations/Hopper.md)
## [HopperIO In Depth](./InDepthExplainations/HopperIO.md)
## [HopperReal In Depth](./InDepthExplainations/HopperIO.md)
## [HopperSim In Depth](./InDepthExplainations/HopperSim.md)

---

# Part 1 — RobotContainer



## What is RobotContainer?

`RobotContainer` is where we define the structure of our robot.

It contains things such as:

* subsystems
* commands
* controller bindings
* autonomous commands

For this lesson, we want to add our hopper to `RobotContainer`.

---

## Step 1 — Import the Hopper Classes

Add:

```java
import frc.robot.subsystems.Hopper.Hopper;
import frc.robot.subsystems.Hopper.HopperIO;
import frc.robot.subsystems.Hopper.HopperReal;
import frc.robot.subsystems.Hopper.HopperSim;
```

We need four classes because each has a different job.

| Class        | Purpose                      |
| ------------ | ---------------------------- |
| `Hopper`     | Main subsystem               |
| `HopperIO`   | Defines the IO interface     |
| `HopperReal` | Controls the real motor      |
| `HopperSim`  | Controls the simulated motor |

---

# Part 2 — Create the Hopper

Inside `RobotContainer`, add:

```java
private final Hopper m_Hopper;
```

This creates a reference to our hopper subsystem.

Notice that we are **not** creating the hopper yet.

We are only declaring that the robot will have one.

---

# Part 3 — Select Real or Simulation

Inside the constructor, add:

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

This is one of the most important pieces of the architecture.

---

## What Does This Do?

If we are running on the real robot:

```text
Constants.currentMode
        ↓
       REAL
        ↓
   HopperReal
        ↓
      TalonFX
```

If we are running simulation:

```text
Constants.currentMode
        ↓
       SIM
        ↓
    HopperSim
        ↓
    DCMotorSim
```

---

# Understanding This Line

Look at:

```java
m_Hopper = new Hopper(new HopperReal());
```

There are actually two objects being created.

First:

```java
new HopperReal()
```

creates the hardware implementation.

Then:

```java
new Hopper(...)
```

creates the hopper subsystem and gives it the `HopperReal`.

In other words:

```text
HopperReal
    ↓
given to
    ↓
Hopper
```

This is called **dependency injection**.

The `Hopper` does not have to create its own motor hardware.

Instead, we give it an object that knows how to communicate with the motor.

---

# Why Is This Useful?

Because we can replace:

```java
new HopperReal()
```

with:

```java
new HopperSim()
```

without changing the rest of the subsystem.

That means our commands can work in both environments.

---

# Part 4 — Control the Hopper

Now let's connect the Xbox controller.

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

This means:

```text
A button pressed
       ↓
Run hopper at 75%
       ↓
A button released
       ↓
Stop hopper
```

---

# Understanding `whileTrue`

This:

```java
.whileTrue(...)
```

means the command runs while the button is held.

So:

```java
m_driverController.a()
```

detects the A button.

Then:

```java
.whileTrue(...)
```

runs the command while A is pressed.

---

# Understanding `setDutyCycle`

We call:

```java
m_Hopper.setDutyCycle(0.75)
```

A duty cycle of:

```text
1.0  = 100%
0.75 = 75%
0.50 = 50%
0.00 = 0%
-1.0 = -100%
```

---

# Checkpoint

Before continuing, explain this sequence:

```text
A button
   ↓
RobotContainer
   ↓
Hopper.setDutyCycle()
```

What should happen next?

---

# Part 5 — Understanding Hopper.java

The `Hopper` class is our subsystem.

It extends:

```java
SubsystemBase
```

```java
public class Hopper extends SubsystemBase
```

This allows WPILib to treat it as a subsystem.

---

# The IO Object

Inside `Hopper` we have:

```java
private final HopperIO io;
```

This is the object that actually communicates with the motor implementation.

Notice that the type is:

```java
HopperIO
```

not:

```java
HopperReal
```

and not:

```java
HopperSim
```

That's intentional.

The Hopper only cares that it has something that implements `HopperIO`.

---

# The Constructor

```java
public Hopper(HopperIO io) {
    this.io = io;
}
```

This takes the IO implementation and stores it.

So when we write:

```java
new Hopper(new HopperReal())
```

the `HopperReal` is passed into this constructor.

---

# Periodic

The hopper periodically updates its inputs:

```java
@Override
public void periodic() {
    io.periodic();
    io.updateInputs(inputs);
    Logger.processInputs("Hopper/inputs", inputs);
}
```

The important part is:

```java
io.updateInputs(inputs);
```

The IO implementation provides information about the motor.

That information gets stored in:

```java
HopperIOInputs
```

Then AdvantageKit records it.

---

# The Data Flow

```text
TalonFX / DCMotorSim
        ↓
   HopperReal / HopperSim
        ↓
     updateInputs()
        ↓
   HopperIOInputs
        ↓
     AdvantageKit
```

---

# The Hopper API

The hopper exposes:

```java
public void setDutyCycle(double dutyCycle)
```

This is how the rest of the robot asks the hopper to move.

It does **not** directly expose the TalonFX.

That is important.

Commands should say:

```java
m_Hopper.setDutyCycle(0.75);
```

rather than:

```java
hopperMotor.set(0.75);
```

The command should not need to know what motor controller is being used.

---

# Clamping the Output

Inside `setDutyCycle()`:

```java
io.setDutyCycle(
    MathUtil.clamp(dutyCycle, -1.0, 1.0));
```

This makes sure the value stays between:

```text
-1.0
```

and:

```text
1.0
```

For example:

```text
1.5  → 1.0
0.5  → 0.5
-2.0 → -1.0
```

---

# Checkpoint

Why do we clamp the duty cycle?

Think about what could happen if someone accidentally called:

```java
setDutyCycle(50);
```

---

# Part 6 — Building HopperIO

Now we need to define the IO interface.

Start with:

```java
public interface HopperIO {
```

An interface defines what an implementation should be able to do.

Our implementations will be:

```text
HopperReal
HopperSim
```

Both will implement:

```java
HopperIO
```

---

# HopperIOInputs

Inside the interface, create:

```java
@AutoLog
class HopperIOInputs {
}
```

This class will contain the information we want to know about the hopper.

---

# Add Velocity

```java
public double velocityOfHopperMotorRPS = 0.0;
```

`RPS` means:

> Revolutions Per Second

For example:

```text
10 RPS
```

means the motor rotates ten times every second.

---

# Add Stator Current

```java
public double statorCurrentOfHopperAmps = 0.0;
```

This tells us how much current the motor is drawing on the motor side.

---

# Add Torque Current

```java
public double torqueCurrentHopperAmps = 0.0;
```

This gives us information related to motor torque production.

---

# Add Voltage

```java
public double outputOfHopperVolts = 0.0;
```

This tells us the voltage being applied to the motor.

---

# Add Acceleration

```java
public double accelerationOfHopper = 0.0;
```

This tells us how quickly the motor's velocity is changing.

---

# Add Position

```java
public double positionOfHopperMotorRotations = 0.0;
```

This tells us how many rotations the motor has made.

---

# Add Duty Cycle

```java
public double dutyCycleOutput = 0.0;
```

This tells us the requested motor output.

---

# Add Connection Status

```java
public boolean hopperConnected = false;
```

This tells us whether the motor controller is connected.

---

# Complete Inputs Class

Your class should now look like:

```java
@AutoLog
class HopperIOInputs {

  public double velocityOfHopperMotorRPS = 0.0;
  public double statorCurrentOfHopperAmps = 0.0;
  public double torqueCurrentHopperAmps = 0.0;
  public double outputOfHopperVolts = 0.0;
  public double accelerationOfHopper = 0.0;
  public double positionOfHopperMotorRotations = 0.0;
  public double dutyCycleOutput = 0.0;
  public boolean hopperConnected = false;
}
```

---

# What Does `@AutoLog` Do?

AdvantageKit uses:

```java
@AutoLog
```

to generate a logging version of this class.

This gives us:

```text
HopperIOInputs
        ↓
HopperIOInputsAutoLogged
        ↓
AdvantageKit
```

This allows us to easily record the subsystem's state.

---

# Part 7 — Building HopperReal

Now we're ready to connect the hopper to the actual TalonFX.

---

# Create the TalonFX

Add:

```java
hopperMotor = new TalonFX(
    Constants.HopperConstants.hopperMotorId,
    new CANBus("rio"));
```

This creates the motor controller.

The CAN ID comes from:

```java
Constants.HopperConstants.hopperMotorId
```

This is better than hard-coding the ID.

---

# Create the Configuration

```java
TalonFXConfiguration config =
    new TalonFXConfiguration();
```

We can use this configuration object to define how the TalonFX behaves.

---

# Motor Inversion

```java
config.MotorOutput.Inverted =
    Constants.HopperConstants.isInverted
        ? InvertedValue.Clockwise_Positive
        : InvertedValue.CounterClockwise_Positive;
```

This uses the Java ternary operator.

The general structure is:

```java
condition ? valueIfTrue : valueIfFalse
```

So:

```java
condition
    ?
clockwise
    :
counter-clockwise
```

---

# Neutral Mode

Next:

```java
config.MotorOutput.NeutralMode =
    Constants.HopperConstants.isCoast
        ? NeutralModeValue.Coast
        : NeutralModeValue.Brake;
```

The two choices are:

```text
Coast
Brake
```

### Coast

The motor is allowed to spin freely.

### Brake

The motor attempts to resist motion when output is removed.

---

# Current Limits

Add the stator current limit:

```java
config.CurrentLimits.StatorCurrentLimitEnable = true;

config.CurrentLimits.StatorCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

Then supply current:

```java
config.CurrentLimits.SupplyCurrentLimitEnable = true;

config.CurrentLimits.SupplyCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

Current limits help protect the motor and electrical system.

---

# Apply the Configuration

Creating the configuration isn't enough.

We need:

```java
hopperMotor.getConfigurator().apply(config);
```

This sends the configuration to the TalonFX.

---

# Read Motor Data

Create status signals:

```java
velocitySignal = hopperMotor.getVelocity();

statorCurrentSignal =
    hopperMotor.getStatorCurrent();

voltageSignal =
    hopperMotor.getMotorVoltage();

accelerationSignal =
    hopperMotor.getAcceleration();
```

These provide information about the motor.

---

# Refresh the Signals

Before reading the values:

```java
BaseStatusSignal.refreshAll(
    velocitySignal,
    statorCurrentSignal,
    voltageSignal,
    accelerationSignal);
```

This refreshes the signals from the TalonFX.

---

# Store Velocity

```java
inputs.velocityOfHopperMotorRPS =
    velocitySignal.getValue()
        .in(Rotation.per(Second));
```

We explicitly request:

```text
Rotations / Second
```

so the value is in RPS.

### Important

Do not assume multiplying a value by an arbitrary number converts its units.

For example:

```java
.in(Rotation.per(Second)) * 100
```

does **not** convert something into RPS.

The `.in(Rotation.per(Second))` call already performs the unit conversion.

---

# Store Current

```java
inputs.statorCurrentOfHopperAmps =
    statorCurrentSignal.getValue()
        .in(Amps);
```

---

# Store Voltage

```java
inputs.outputOfHopperVolts =
    voltageSignal.getValue()
        .in(Volts);
```

---

# Store Acceleration

```java
inputs.accelerationOfHopper =
    accelerationSignal.getValue()
        .in(RotationsPerSecondPerSecond);
```

---

# Store Torque Current

```java
inputs.torqueCurrentHopperAmps =
    hopperMotor.getTorqueCurrent()
        .getValue()
        .in(Amps);
```

---

# Store Position

```java
inputs.positionOfHopperMotorRotations =
    hopperMotor.getPosition()
        .getValue()
        .in(Rotation);
```

---

# Store Duty Cycle

```java
inputs.dutyCycleOutput =
    hopperMotor.getDutyCycle().getValue();
```

---

# Store Connection Status

```java
inputs.hopperConnected =
    hopperMotor.isConnected();
```

---

# Controlling the Motor

Now implement:

```java
@Override
public void setDutyCycle(double dutyCycle) {
    hopperMotor.set(dutyCycle);

    Logger.recordOutput(
        "Hopper/DutyCycleOutput",
        dutyCycle);
}
```

Notice that the command never calls:

```java
hopperMotor.set()
```

directly.

Instead:

```text
Command
   ↓
Hopper
   ↓
HopperIO
   ↓
HopperReal
   ↓
TalonFX
```

---

# Stopping the Motor

Implement:

```java
@Override
public void stop() {
    hopperMotor.stopMotor();

    Logger.recordOutput(
        "Hopper/DutyCycleOutput",
        0.0);
}
```

---

# Part 8 — Building HopperSim

Now we're going to create a simulated version of the hopper.

The goal is to make simulation behave like a physical motor rather than just returning fake values.

---

# Define the Motor

```java
private static final DCMotor MOTOR =
    DCMotor.getKrakenX60Foc(1);
```

This tells WPILib that we are simulating one Kraken X60 FOC motor.

---

# Define Moment of Inertia

```java
private static final double MOMENT_OF_INERTIA = 0.035;
```

Moment of inertia describes how resistant something is to rotational acceleration.

Imagine two wheels:

```text
Small/light wheel
      ↓
Easy to accelerate

Large/heavy wheel
      ↓
Harder to accelerate
```

The second wheel has a larger effective moment of inertia.

---

# Create the Simulation

```java
motorSim = new DCMotorSim(
    LinearSystemId.createDCMotorSystem(
        MOTOR,
        MOMENT_OF_INERTIA,
        1),
    MOTOR);
```

We are creating a physics model using:

```text
Motor
+
Moment of Inertia
+
Gear Ratio
```

---

# Create a Simulated TalonFX

We still create a TalonFX:

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

This does not create a real motor.

Instead, it gives us access to the simulated state of the TalonFX.

---

# The Two Simulation Layers

We now have:

```text
DCMotorSim
```

which models the physics.

And:

```text
TalonFXSimState
```

which models the motor controller's state.

Think of it as:

```text
DCMotorSim
   |
   | physical behavior
   v
TalonFXSimState
   |
   | simulated TalonFX data
   v
HopperIOInputs
```

---

# Duty Cycle → Voltage

Inside `updateInputs()`:

```java
double appliedVoltage =
    MathUtil.clamp(
        dutyCycle * 12.0,
        -12.0,
        12.0);
```

Why multiply by 12?

Because we are approximating a 12 V battery.

For example:

```text
100% → 12 V
75%  → 9 V
50%  → 6 V
25%  → 3 V
0%   → 0 V
```

---

# Update the Motor

```java
motorSim.setInputVoltage(appliedVoltage);
motorSim.update(0.02);
```

The:

```java
0.02
```

represents:

```text
20 milliseconds
```

or:

```text
0.02 seconds
```

This matches the typical FRC robot loop timing.

---

# Update the Simulated TalonFX

Now copy the physics state into the simulated motor controller.

Position:

```java
hopperMotorSim.setRawRotorPosition(
    motorSim.getAngularPosition());
```

Velocity:

```java
hopperMotorSim.setRotorVelocity(
    motorSim.getAngularVelocity());
```

Acceleration:

```java
hopperMotorSim.setRotorAcceleration(
    motorSim.getAngularAcceleration());
```

The data flow is now:

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

# Store Position

```java
inputs.positionOfHopperMotorRotations =
    motorSim.getAngularPositionRotations();
```

This is already in rotations.

---

# Store Velocity

The simulation provides RPM:

```java
motorSim.getAngularVelocityRPM()
```

but our IO interface expects RPS.

There are:

```text
60 seconds per minute
```

so:

```java
inputs.velocityOfHopperMotorRPS =
    motorSim.getAngularVelocityRPM() / 60.0;
```

---

# Store Acceleration

The simulation gives:

```text
radians / second²
```

Our input expects:

```text
rotations / second²
```

One rotation is:

```text
2π radians
```

Therefore:

```java
inputs.accelerationOfHopper =
    motorSim.getAngularAccelerationRadPerSecSq()
        / (2.0 * Math.PI);
```

---

# Store Voltage

```java
inputs.outputOfHopperVolts =
    appliedVoltage;
```

---

# Store Current

```java
inputs.statorCurrentOfHopperAmps =
    Math.abs(
        motorSim.getCurrentDrawAmps());
```

---

# Store Torque Current

For this simplified simulation:

```java
inputs.torqueCurrentHopperAmps =
    inputs.statorCurrentOfHopperAmps;
```

This is an approximation.

A more advanced simulation could model these values independently.

---

# Store Duty Cycle

```java
inputs.dutyCycleOutput =
    dutyCycle;
```

---

# Connection Status

The simulated motor should appear connected:

```java
inputs.hopperConnected = true;
```

---

# Set Duty Cycle

The simulation still needs to remember what duty cycle the command requested.

```java
this.dutyCycle =
    MathUtil.clamp(
        dutyCycle,
        -1.0,
        1.0);
```

Then log it:

```java
Logger.recordOutput(
    "Hopper/DutyCycleOutput",
    this.dutyCycle);
```

---

# Stop the Simulation

```java
@Override
public void stop() {

    dutyCycle = 0.0;

    Logger.recordOutput(
        "Hopper/DutyCycleOutput",
        0.0);
}
```

The next simulation update will therefore apply:

```text
0 V
```

to the motor model.

---

# Part 9 — The Complete Architecture

You have now built the entire system.

The complete flow is:

```text
                  Xbox Controller
                         |
                         v
                  RobotContainer
                         |
                         v
                      Hopper
                         |
                         v
                     HopperIO
                    /        \
                   /          \
                  v            v
            HopperReal      HopperSim
                 |               |
                 v               v
              TalonFX        DCMotorSim
                 |               |
                 v               v
            Real Motor       Simulated Motor
                  \             /
                   \           /
                    v         v
                   HopperIOInputs
                         |
                         v
                    AdvantageKit
                         |
                         v
                    AdvantageScope
```

---

# Part 10 — Testing Simulation

Start the robot in simulation.

Press the A button.

You should be commanding:

```text
75% duty cycle
```

which is approximately:

```text
9 V
```

in our simplified simulation.

---

# What Should Happen?

When you press A:

```text
A pressed
    ↓
75% duty cycle
    ↓
9 V
    ↓
Motor accelerates
    ↓
Velocity increases
```

When you release A:

```text
A released
    ↓
stop()
    ↓
0% duty cycle
    ↓
0 V
    ↓
Motor stops accelerating
```

---

# Part 11 — Inspect AdvantageKit Data

Open AdvantageScope.

Look for:

```text
Hopper/inputs
```

You should be able to find values such as:

```text
velocityOfHopperMotorRPS
statorCurrentOfHopperAmps
torqueCurrentHopperAmps
outputOfHopperVolts
accelerationOfHopper
positionOfHopperMotorRotations
dutyCycleOutput
hopperConnected
```

---

# Questions to Investigate

While running the simulation, answer these questions.

### Question 1

What happens to velocity while holding A?

### Question 2

What happens to acceleration?

### Question 3

What happens to position?

### Question 4

What happens to voltage when A is released?

### Question 5

Does the motor's velocity immediately become zero?

Why or why not?

---

# Part 12 — Experiment With Moment of Inertia

Find:

```java
private static final double MOMENT_OF_INERTIA = 0.035;
```

Change it to:

```java
private static final double MOMENT_OF_INERTIA = 0.10;
```

Run the simulation again.

Observe the motor.

---

## What Changed?

A larger moment of inertia makes the mechanism harder to accelerate.

Try:

```java
0.01
```

Then:

```java
0.10
```

Then:

```java
0.50
```

Compare the results.

---

# Part 13 — Real Robot vs Simulation

The same command:

```java
m_Hopper.setDutyCycle(0.75);
```

works in both environments.

## Real Robot

```text
Hopper
  ↓
HopperIO
  ↓
HopperReal
  ↓
TalonFX
  ↓
Physical Motor
```

## Simulation

```text
Hopper
  ↓
HopperIO
  ↓
HopperSim
  ↓
DCMotorSim
  ↓
Simulated Motor
```

The command doesn't care which one is being used.

That is the main benefit of the architecture.

---

# Part 14 — Student Challenge

Choose at least one challenge.

---

## Challenge 1 — Add Motor Temperature

Add:

```java
public double hopperTemperatureCelsius = 0.0;
```

to `HopperIOInputs`.

Then implement it in:

* `HopperReal`
* `HopperSim`

---

## Challenge 2 — Add Reverse Control

Create a controller binding that runs the hopper backward.

For example:

```text
75% forward
```

and:

```text
75% reverse
```

---

## Challenge 3 — Add a Speed Getter

Create a method in `Hopper`:

```java
public double getVelocity()
```

that returns the motor's current velocity.

---

## Challenge 4 — Add Position Reset

Create:

```java
resetPosition()
```

and make it work in both:

```text
HopperReal
HopperSim
```

---

## Challenge 5 — Add a Deadband

Modify the hopper so very small commands do not move the motor.

For example:

```text
0.00 → 0.00
0.02 → 0.00
0.05 → 0.00
0.10 → 0.10
```

---

## Challenge 6 — Improve the Simulation

Try to make the simulation behave more like the actual hopper.

Experiment with:

* moment of inertia
* gear ratio
* motor count
* current draw

---

# Part 15 — Architecture Questions

Before finishing, make sure you can answer these.

### 1. What is the purpose of `RobotContainer`?

---

### 2. What is the purpose of `Hopper`?

---

### 3. What is the purpose of `HopperIO`?

---

### 4. What is the difference between `HopperReal` and `HopperSim`?

---

### 5. Why doesn't `Hopper` directly create a `TalonFX`?

---

### 6. Why do `HopperReal` and `HopperSim` use the same `HopperIOInputs`?

---

### 7. Where is the decision made between real and simulated hardware?

---

### 8. What does `@AutoLog` do?

---

### 9. Why do we convert RPM to RPS?

---

### 10. Why does the simulation use `0.02` when updating?

---

# Final Takeaway

The most important thing to understand is the separation between **what the subsystem does** and **how the hardware implements it**.

```text
             WHAT?
              |
              v
           Hopper
              |
              v
          HopperIO
              |
       +------+------+
       |             |
       v             v
     REAL            SIM
       |             |
       v             v
 HopperReal      HopperSim
       |             |
       v             v
    TalonFX       DCMotorSim
```

The rest of the robot can simply say:

```java
m_Hopper.setDutyCycle(0.75);
```

It does not need to know whether that command is controlling:

```text
a real TalonFX
```

or:

```text
a simulated motor
```

That is the power of the IO-layer architecture.

---

# Where This Architecture Goes Next

Once you understand this pattern, you can use it for almost every subsystem on an FRC robot:

```text
Hopper
Intake
Shooter
Arm
Elevator
Climber
Pivot
Turret
Drivetrain
```

For each subsystem, you can separate:

```text
Subsystem behavior
        ↓
IO interface
        ↓
Real hardware
        +
Simulation
```

This allows you to write robot code that is easier to:

* test
* simulate
* debug
* maintain
* reuse
* expand
