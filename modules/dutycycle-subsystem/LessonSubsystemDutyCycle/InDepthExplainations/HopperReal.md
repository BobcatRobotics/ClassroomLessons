# HopperReal — Connecting the Real TalonFX

## Learning Objectives

By the end of this lesson, you should understand how to:

- Create a CTRE `TalonFX`.
- Configure a TalonFX motor controller.
- Configure motor inversion and neutral mode.
- Configure current limits.
- Retrieve status signals from the TalonFX.
- Update the `HopperIOInputs` object with real motor data.
- Command the motor using duty-cycle control.
- Stop the motor safely.
- Record values to AdvantageKit.

---

# Part 1 — Understanding `HopperReal`

`HopperReal` is the class that connects our generic `HopperIO` interface to the **actual hardware** on the robot.

The architecture looks like this:

```text
                 Hopper
                    |
                    v
                HopperIO
                    |
          +---------+---------+
          |                   |
          v                   v
      HopperReal          HopperSim
          |
          v
       TalonFX
          |
          v
      Real Motor
```

`Hopper` doesn't need to know whether it is controlling a real motor or a simulated motor.

`HopperReal` handles the hardware-specific CTRE code.

---

# Part 2 — Add the Required Imports

The starting file already imports the basic TalonFX classes:

```java
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.hardware.TalonFX;
```

We need additional classes for configuring the motor and reading its signals.

Add:

```java
import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotation;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;
import static edu.wpi.first.units.Units.Second;
import static edu.wpi.first.units.Units.Volts;

import org.littletonrobotics.junction.Logger;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import frc.robot.Constants;
```

## What do these imports do?

### Units

```java
import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotation;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;
import static edu.wpi.first.units.Units.Second;
import static edu.wpi.first.units.Units.Volts;
```

CTRE uses WPILib's units system.

These allow us to convert CTRE measurements into the units our inputs expect.

For example:

```java
velocitySignal.getValue().in(Rotation.per(Second))
```

means:

> Get the motor velocity and convert it to rotations per second.

---

### AdvantageKit Logger

```java
import org.littletonrobotics.junction.Logger;
```

This allows us to record values to AdvantageKit.

We will use it when commanding the motor:

```java
Logger.recordOutput(
    "Hopper/DutyCycleOutput",
    dutyCycle);
```

---

### Status signal utilities

```java
import com.ctre.phoenix6.BaseStatusSignal;
```

This allows us to refresh multiple TalonFX signals at the same time:

```java
BaseStatusSignal.refreshAll(
    velocitySignal,
    statorCurrentSignal,
    voltageSignal,
    accelerationSignal);
```

---

### TalonFX configuration

```java
import com.ctre.phoenix6.configs.TalonFXConfiguration;
```

This gives us an object that contains the TalonFX's configuration settings.

---

### Motor configuration values

```java
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
```

These are used to configure:

- Motor direction.
- Brake/coast behavior.

---

### Robot constants

```java
import frc.robot.Constants;
```

Instead of hardcoding values such as the motor CAN ID and current limit, we get them from `Constants`.

This makes the robot easier to configure.

---

# Part 3 — Create the TalonFX

The first thing we need to do is actually create the motor controller.

Inside the constructor, add:

```java
hopperMotor = new TalonFX(
    Constants.HopperConstants.hopperMotorId,
    new CANBus("rio"));
```

## What is happening here?

We are creating a new `TalonFX`.

The first argument:

```java
Constants.HopperConstants.hopperMotorId
```

is the CAN ID of the motor.

The second argument:

```java
new CANBus("rio")
```

specifies that the TalonFX is connected to the roboRIO CAN bus.

The result is stored in:

```java
hopperMotor
```

which is the field we already had:

```java
private final TalonFX hopperMotor;
```

---

# Part 4 — Create the Motor Configuration

Next, create a configuration object:

```java
TalonFXConfiguration config = new TalonFXConfiguration();
```

This object lets us configure the TalonFX before using it.

Think of it as a collection of settings:

```text
TalonFXConfiguration
        |
        +-- Motor Output
        |
        +-- Current Limits
        |
        +-- Other settings
```

---

# Part 5 — Configure Motor Inversion

Add:

```java
config.MotorOutput.Inverted =
    Constants.HopperConstants.isInverted
        ? InvertedValue.Clockwise_Positive
        : InvertedValue.CounterClockwise_Positive;
```

This uses the Java **ternary operator**.

It is equivalent to:

```java
if (Constants.HopperConstants.isInverted) {
    config.MotorOutput.Inverted =
        InvertedValue.Clockwise_Positive;
} else {
    config.MotorOutput.Inverted =
        InvertedValue.CounterClockwise_Positive;
}
```

The `?` means:

> If the condition is true, use this value.

The `:` means:

> Otherwise, use this value.

This lets us control motor direction from `Constants`.

---

# Part 6 — Configure Brake or Coast

Next, configure the motor's neutral mode:

```java
config.MotorOutput.NeutralMode =
    Constants.HopperConstants.isCoast
        ? NeutralModeValue.Coast
        : NeutralModeValue.Brake;
```

Again, this is a ternary expression.

If:

```java
Constants.HopperConstants.isCoast
```

is `true`, the motor uses:

```java
NeutralModeValue.Coast
```

Otherwise it uses:

```java
NeutralModeValue.Brake
```

## What is the difference?

### Coast

When the motor receives zero output, the motor is allowed to spin down naturally.

### Brake

When the motor receives zero output, the motor actively resists rotation.

For a mechanism like a hopper, the appropriate setting depends on the mechanical design.

---

# Part 7 — Configure the Stator Current Limit

Add:

```java
config.CurrentLimits.StatorCurrentLimitEnable = true;

config.CurrentLimits.StatorCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

The first line enables the stator current limit.

The second sets the maximum allowed stator current.

The value comes from:

```java
Constants.HopperConstants.hopperCurrentLimit
```

rather than being hardcoded.

## Why limit current?

Too much current can:

- Heat the motor.
- Stress the motor controller.
- Damage hardware.
- Indicate that the mechanism is jammed.

Current limits provide an additional layer of protection.

---

# Part 8 — Configure the Supply Current Limit

Add:

```java
config.CurrentLimits.SupplyCurrentLimitEnable = true;

config.CurrentLimits.SupplyCurrentLimit =
    Constants.HopperConstants.hopperCurrentLimit;
```

There are two different current measurements we are configuring:

### Stator current

Current flowing through the motor windings.

### Supply current

Current being supplied to the motor controller from the robot's electrical system.

They are related, but they are not the same measurement.

---

# Part 9 — Apply the Configuration

We have created the configuration, but the TalonFX does not automatically know about it.

We need to apply it:

```java
hopperMotor.getConfigurator().apply(config);
```

This sends our configuration to the TalonFX.

The sequence is:

```text
Create configuration
        ↓
Modify configuration
        ↓
Apply configuration
        ↓
TalonFX uses configuration
```

---

# Part 10 — Get the Motor Velocity Signal

After configuring the motor, retrieve its velocity signal:

```java
velocitySignal = hopperMotor.getVelocity();
```

This gives us a `StatusSignal<AngularVelocity>`.

The field was already declared in the starting file:

```java
private final StatusSignal<AngularVelocity> velocitySignal;
```

Now we connect that field to the TalonFX.

---

# Part 11 — Get the Current Signal

Retrieve the stator current:

```java
statorCurrentSignal = hopperMotor.getStatorCurrent();
```

This provides the motor's stator current.

The value will later be placed into:

```java
inputs.statorCurrentOfHopperAmps
```

---

# Part 12 — Get the Voltage Signal

Retrieve the motor voltage:

```java
voltageSignal = hopperMotor.getMotorVoltage();
```

This provides the voltage currently being applied to the motor.

---

# Part 13 — Get the Acceleration Signal

Retrieve the motor acceleration:

```java
accelerationSignal = hopperMotor.getAcceleration();
```

This provides the angular acceleration of the motor.

At this point, the constructor has connected all of our status signal fields to the TalonFX.

The complete section is:

```java
velocitySignal = hopperMotor.getVelocity();
statorCurrentSignal = hopperMotor.getStatorCurrent();
voltageSignal = hopperMotor.getMotorVoltage();
accelerationSignal = hopperMotor.getAcceleration();
```

---

# Part 14 — Refresh the Status Signals

Now we need to use those signals.

Inside `updateInputs()`, add:

```java
BaseStatusSignal.refreshAll(
    velocitySignal,
    statorCurrentSignal,
    voltageSignal,
    accelerationSignal);
```

This tells Phoenix to refresh the latest values from the TalonFX.

We then have access to the most recent measurements.

---

# Part 15 — Record Motor Velocity

Add:

```java
inputs.velocityOfHopperMotorRPS =
    velocitySignal.getValue().in(Rotation.per(Second)) * 100;
```

Let's break this down.

### Get the value

```java
velocitySignal.getValue()
```

gets the current velocity.

### Convert the units

```java
.in(Rotation.per(Second))
```

converts the value to rotations per second.

### Store the value

```java
inputs.velocityOfHopperMotorRPS = ...
```

puts the result into our `HopperIOInputs`.

---

## Important: Check the `* 100`

The solution contains:

```java
* 100
```

This is **not normally required** just to convert a TalonFX velocity to RPS.

If the goal is simply to report RPS, this would normally be:

```java
inputs.velocityOfHopperMotorRPS =
    velocitySignal.getValue().in(Rotation.per(Second));
```

If your robot intentionally uses the `* 100` scaling, document why. Otherwise, students should understand that this changes the actual measurement by a factor of 100.

---

# Part 16 — Record Stator Current

Add:

```java
inputs.statorCurrentOfHopperAmps =
    statorCurrentSignal.getValue().in(Amps);
```

The signal is converted into amps and stored in:

```java
statorCurrentOfHopperAmps
```

Now AdvantageKit can log the hopper's stator current.

---

# Part 17 — Record Motor Voltage

Add:

```java
inputs.outputOfHopperVolts =
    voltageSignal.getValue().in(Volts);
```

This gets the voltage being applied to the motor and converts it to volts.

---

# Part 18 — Record Motor Acceleration

Add:

```java
inputs.accelerationOfHopper =
    accelerationSignal.getValue()
        .in(RotationsPerSecondPerSecond);
```

This converts the TalonFX acceleration measurement into:

```text
rotations / second²
```

For example:

```text
10 rotations/s²
```

means that the motor's velocity is increasing by 10 rotations per second every second.

---

# Part 19 — Record Torque Current

The torque current was not given its own `StatusSignal` field.

Instead, we can retrieve it directly:

```java
inputs.torqueCurrentHopperAmps =
    hopperMotor.getTorqueCurrent()
        .getValue()
        .in(Amps);
```

This follows the chain:

```text
hopperMotor
     ↓
getTorqueCurrent()
     ↓
getValue()
     ↓
in(Amps)
     ↓
inputs.torqueCurrentHopperAmps
```

Torque current is useful because it gives us information about how much current is being used to produce motor torque.

---

# Part 20 — Record Motor Position

Get the TalonFX position:

```java
inputs.positionOfHopperMotorRotations =
    hopperMotor.getPosition()
        .getValue()
        .in(Rotation);
```

This gives the motor's position in rotations.

For example:

```text
0 rotations
10 rotations
25.5 rotations
```

The position can be useful for mechanisms that need to know how far they have moved.

---

# Part 21 — Record Duty Cycle

Add:

```java
inputs.dutyCycleOutput =
    hopperMotor.getDutyCycle().getValue();
```

This tells us what duty-cycle output the TalonFX is currently using.

A typical range is:

```text
-1.0 → full reverse
 0.0 → stopped
+1.0 → full forward
```

This is different from velocity.

For example:

```text
Duty cycle = 0.50
Velocity = 30 RPS
```

means the motor is being commanded at 50% output and is currently spinning at 30 RPS.

---

# Part 22 — Check Whether the Motor Is Connected

Finally:

```java
inputs.hopperConnected =
    hopperMotor.isConnected();
```

This tells the rest of the robot whether the TalonFX is communicating.

The value is a boolean:

```java
true
```

or:

```java
false
```

This is useful for diagnosing hardware problems.

---

# Part 23 — Understanding the Complete `updateInputs()`

At this point, the method should look like:

```java
@Override
public void updateInputs(HopperIOInputs inputs) {

  BaseStatusSignal.refreshAll(
      velocitySignal,
      statorCurrentSignal,
      voltageSignal,
      accelerationSignal);

  inputs.velocityOfHopperMotorRPS =
      velocitySignal.getValue().in(Rotation.per(Second));

  inputs.statorCurrentOfHopperAmps =
      statorCurrentSignal.getValue().in(Amps);

  inputs.outputOfHopperVolts =
      voltageSignal.getValue().in(Volts);

  inputs.accelerationOfHopper =
      accelerationSignal.getValue()
          .in(RotationsPerSecondPerSecond);

  inputs.torqueCurrentHopperAmps =
      hopperMotor.getTorqueCurrent()
          .getValue()
          .in(Amps);

  inputs.positionOfHopperMotorRotations =
      hopperMotor.getPosition()
          .getValue()
          .in(Rotation);

  inputs.dutyCycleOutput =
      hopperMotor.getDutyCycle().getValue();

  inputs.hopperConnected =
      hopperMotor.isConnected();
}
```

---

# Part 24 — Command the Motor

Now we need to actually make the motor move.

The starting method was empty:

```java
@Override
public void setDutyCycle(double dutyCycle) {

}
```

Replace it with:

```java
@Override
public void setDutyCycle(double dutyCycle) {
  hopperMotor.set(dutyCycle);

  Logger.recordOutput(
      "Hopper/DutyCycleOutput",
      dutyCycle);
}
```

---

# Part 25 — `hopperMotor.set()`

The important line is:

```java
hopperMotor.set(dutyCycle);
```

This sends the requested duty cycle to the TalonFX.

For example:

```java
hopperMotor.set(0.75);
```

requests approximately 75% output.

The `Hopper` subsystem is responsible for clamping the value:

```java
MathUtil.clamp(dutyCycle, -1.0, 1.0)
```

before it reaches this method.

That means the IO layer doesn't have to duplicate that logic.

---

# Part 26 — Log the Command

After commanding the motor, we record the requested value:

```java
Logger.recordOutput(
    "Hopper/DutyCycleOutput",
    dutyCycle);
```

This creates the AdvantageKit log entry:

```text
Hopper/DutyCycleOutput
```

This is useful when looking at the robot's behavior in AdvantageScope.

You can compare:

```text
Hopper/DutyCycleOutput
```

with:

```text
Hopper/inputs/velocityOfHopperMotorRPS
Hopper/inputs/statorCurrentOfHopperAmps
Hopper/inputs/outputOfHopperVolts
```

and see how the motor responds.

---

# Part 27 — Stop the Motor

The starting `stop()` method was empty:

```java
@Override
public void stop() {

}
```

Replace it with:

```java
@Override
public void stop() {
  hopperMotor.stopMotor();

  Logger.recordOutput(
      "Hopper/DutyCycleOutput",
      0.0);
}
```

The first line:

```java
hopperMotor.stopMotor();
```

tells the TalonFX to stop the motor.

Then we log:

```java
0.0
```

because the motor has been commanded to stop.

---

# Part 28 — Why Use `stopMotor()`?

You could theoretically do:

```java
hopperMotor.set(0.0);
```

but CTRE provides:

```java
hopperMotor.stopMotor();
```

specifically for stopping the motor.

It makes the intent very clear:

> Stop this motor.

---

# Part 29 — How Everything Connects

The completed data flow looks like this:

```text
                    TalonFX
                       |
        +--------------+--------------+
        |              |              |
     Velocity       Current        Voltage
        |              |              |
        +--------------+--------------+
                       |
                       v
                 HopperReal
                       |
                       v
                HopperIOInputs
                       |
                       v
                   Hopper
                       |
                       v
                 AdvantageKit
                       |
                       v
                 AdvantageScope
```

Commands flow in the opposite direction:

```text
Command
   |
   v
Hopper
   |
   v
HopperIO
   |
   v
HopperReal
   |
   v
TalonFX
   |
   v
Motor
```

This separation is one of the most important concepts in this architecture.

---

# Part 30 — Why Don't We Put TalonFX Code in `Hopper.java`?

You might wonder why we don't simply do this inside `Hopper`:

```java
TalonFX motor = new TalonFX(...);
```

The reason is that `Hopper` should not care about the hardware implementation.

Instead:

```text
Hopper
  |
  | talks to
  v
HopperIO
  |
  +---- HopperReal → TalonFX
  |
  +---- HopperSim  → Simulation
```

This means the same `Hopper` subsystem can work with both real hardware and simulation.

---

# Student Challenge

Try implementing `HopperReal` yourself using the starting file.

Complete these sections in order:

1. Create the `TalonFX`.
2. Create the `TalonFXConfiguration`.
3. Configure inversion.
4. Configure brake/coast.
5. Configure current limits.
6. Apply the configuration.
7. Connect the four `StatusSignal` fields.
8. Refresh the signals.
9. Populate every `HopperIOInputs` field.
10. Implement `setDutyCycle()`.
11. Implement `stop()`.

Try not to copy the solution immediately.

Use the following questions to guide yourself:

### Question 1

Where does the TalonFX CAN ID come from?

### Question 2

Why do we store the TalonFX in:

```java
private final TalonFX hopperMotor;
```

instead of creating it every time we want to move the motor?

### Question 3

What is the difference between:

```java
getStatorCurrent()
```

and:

```java
getTorqueCurrent()
```

### Question 4

Why do we call:

```java
BaseStatusSignal.refreshAll(...)
```

before reading the status signals?

### Question 5

Why do we convert values using:

```java
.in(Amps)
```

or:

```java
.in(Volts)
```

?

### Question 6

Why does `HopperReal` update `HopperIOInputs` instead of directly updating values in `Hopper`?

---

# Key Takeaways

The most important concept in this lesson is that **`HopperReal` is the bridge between the generic subsystem and the physical TalonFX hardware.**

The major responsibilities of `HopperReal` are:

```text
1. Create the TalonFX
        ↓
2. Configure the TalonFX
        ↓
3. Read hardware measurements
        ↓
4. Put measurements into HopperIOInputs
        ↓
5. Accept commands from HopperIO
        ↓
6. Send commands to the TalonFX
```

The overall architecture is:

```text
                 Robot Code
                     |
                     v
                  Hopper
                     |
                     v
                 HopperIO
                  /     \
                 /       \
                v         v
         HopperReal   HopperSim
             |             |
             v             v
          TalonFX      Simulation
             |
             v
          Motor
```

This architecture allows the exact same `Hopper` subsystem code to operate with either a **real TalonFX** or a **simulated motor**.