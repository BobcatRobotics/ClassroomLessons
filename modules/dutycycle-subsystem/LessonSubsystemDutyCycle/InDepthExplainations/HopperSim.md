# HopperSim — Building a Simulated Motor

## Learning Objectives

By the end of this lesson, you should understand how to:

- Create a WPILib `DCMotorSim`.
- Model a Kraken X60 motor.
- Define the simulated mechanism's moment of inertia.
- Convert a duty-cycle command into motor voltage.
- Advance a physical simulation over time.
- Read position, velocity, acceleration, and current from a simulation.
- Write simulated values into `HopperIOInputs`.
- Connect a simulated motor to a `TalonFXSimState`.
- Log simulated commands with AdvantageKit.
- Understand how `HopperSim` can replace `HopperReal`.

---

# Part 1 — What Is `HopperSim`?

`HopperSim` is the simulation implementation of our `HopperIO` interface.

We already have:

```text
HopperIO
   |
   +---- HopperReal
   |        |
   |        +---- Real TalonFX
   |
   +---- HopperSim
            |
            +---- DCMotorSim
```

The important part is that `Hopper` does not need to know which implementation it is using.

The subsystem can simply call:

```java
io.setDutyCycle(0.75);
```

In real mode, that command eventually reaches a physical TalonFX.

In simulation mode, that command is converted into a simulated motor voltage.

---

# Part 2 — The Simulation Model

The simulation follows this process:

```text
                 Duty Cycle
                     |
                     v
              Motor Voltage
                     |
                     v
                DCMotorSim
                     |
          +----------+----------+
          |          |          |
          v          v          v
      Position    Velocity  Acceleration
          |
          v
    HopperIOInputs
```

The goal is to make the simulated motor behave similarly to a real motor.

---

# Part 3 — Create the DCMotorSim

The starting constructor is empty:

```java
public HopperSim() {


}
```

The first thing we need to do is create the motor simulation.

Add:

```java
motorSim = new DCMotorSim(
    LinearSystemId.createDCMotorSystem(
        MOTOR,
        MOMENT_OF_INERTIA,
        1),
    MOTOR);
```

This creates the WPILib `DCMotorSim`.

---

# Part 4 — Understanding `DCMotor`

We already have this motor model:

```java
private static final DCMotor MOTOR =
    DCMotor.getKrakenX60Foc(1);
```

This tells WPILib that our simulated motor is a:

**Kraken X60 FOC**

The `1` means that the simulation contains one motor.

The motor model gives WPILib information about the physical characteristics of the motor, such as:

- Torque.
- Speed.
- Electrical characteristics.
- Motor resistance.
- Motor constants.

This allows the simulation to behave more like the real motor.

---

# Part 5 — Understanding Moment of Inertia

We also have:

```java
private static final double MOMENT_OF_INERTIA = 0.035;
```

Moment of inertia describes how difficult it is to accelerate the mechanism rotationally.

You can think of it as rotational mass.

A mechanism with a larger moment of inertia takes more effort to accelerate.

For example:

```text
Small inertia
     ↓
Accelerates quickly

Large inertia
     ↓
Accelerates slowly
```

The value:

```java
0.035
```

is the model's assumed moment of inertia for the hopper mechanism.

---

# Part 6 — Understanding `LinearSystemId.createDCMotorSystem()`

Inside the simulation we have:

```java
LinearSystemId.createDCMotorSystem(
    MOTOR,
    MOMENT_OF_INERTIA,
    1)
```

This creates a mathematical model of the motor system.

The arguments are:

```text
MOTOR
    ↓
Motor characteristics

MOMENT_OF_INERTIA
    ↓
Mechanical inertia

1
    ↓
Gear reduction
```

The final `1` means that we are modeling a 1:1 gear ratio.

If the hopper had gearing, this value would need to represent the appropriate gearing.

---

# Part 7 — Create the Simulated TalonFX

The next addition is:

```java
hopperMotor = new TalonFX(
    Constants.HopperConstants.hopperMotorId,
    new CANBus("rio"));
```

This may seem strange.

Why are we creating a `TalonFX` if this is simulation?

The reason is that we want our simulation to behave like a TalonFX from the rest of the robot code's perspective.

We can then obtain its simulation state:

```java
hopperMotorSim = hopperMotor.getSimState();
```

This gives us access to the simulated TalonFX state.

---

# Part 8 — Get the TalonFX Simulation State

Add:

```java
hopperMotorSim = hopperMotor.getSimState();
```

We already declared:

```java
private final TalonFXSimState hopperMotorSim;
```

`TalonFXSimState` allows us to write simulated physical values back into the CTRE motor controller's simulated state.

For example:

```java
hopperMotorSim.setRawRotorPosition(...);
```

tells the simulated TalonFX where its rotor is.

---

# Part 9 — Understanding the Constructor

The completed constructor is:

```java
public HopperSim() {

  motorSim = new DCMotorSim(
      LinearSystemId.createDCMotorSystem(
          MOTOR,
          MOMENT_OF_INERTIA,
          1),
      MOTOR);

  hopperMotor = new TalonFX(
      Constants.HopperConstants.hopperMotorId,
      new CANBus("rio"));

  hopperMotorSim = hopperMotor.getSimState();
}
```

The constructor creates three important objects:

```text
HopperSim
   |
   +-- DCMotorSim
   |      ↓
   |   Physical model
   |
   +-- TalonFX
   |      ↓
   |   Simulated motor controller
   |
   +-- TalonFXSimState
          ↓
      Simulation state
```

---

# Part 10 — Converting Duty Cycle to Voltage

The next major step is updating the simulation.

Inside `updateInputs()`, add:

```java
double appliedVoltage =
    MathUtil.clamp(dutyCycle * 12.0, -12.0, 12.0);
```

The simulated motor is being controlled using duty cycle.

For example:

```text
Duty Cycle = 1.0
    ↓
12 volts

Duty Cycle = 0.5
    ↓
6 volts

Duty Cycle = -0.5
    ↓
-6 volts
```

The calculation is:

```text
Voltage = Duty Cycle × Battery Voltage
```

Here we are assuming a nominal 12 V supply.

---

# Part 11 — Why Use `MathUtil.clamp()`?

We use:

```java
MathUtil.clamp(
    dutyCycle * 12.0,
    -12.0,
    12.0);
```

This prevents the simulated voltage from going outside the expected range.

For example:

```text
Duty cycle = 2.0

2.0 × 12 = 24 V

clamp → 12 V
```

Similarly:

```text
Duty cycle = -2.0

-2.0 × 12 = -24 V

clamp → -12 V
```

The result is always between:

```text
-12 V and +12 V
```

---

# Part 12 — Apply the Voltage to the Simulation

Next:

```java
motorSim.setInputVoltage(appliedVoltage);
```

This tells the `DCMotorSim` how much voltage is being applied to the simulated motor.

The simulation can now calculate how the motor responds.

---

# Part 13 — Advance the Simulation

Next:

```java
motorSim.update(0.02);
```

This advances the simulation by:

```text
0.02 seconds
```

or:

```text
20 milliseconds
```

This matches the typical WPILib robot periodic loop.

Conceptually:

```text
Time 0.00
    ↓
Simulation update
    ↓
Time 0.02
    ↓
Simulation update
    ↓
Time 0.04
    ↓
Simulation update
```

Every time `updateInputs()` runs, the simulated motor moves forward in time.

---

# Part 14 — Write Position Back to the Simulated TalonFX

Now we take the physical state calculated by `DCMotorSim` and send it to the simulated TalonFX.

Add:

```java
hopperMotorSim.setRawRotorPosition(
    motorSim.getAngularPosition());
```

The `DCMotorSim` calculates the motor's angular position.

We then tell the simulated TalonFX:

> This is where your rotor currently is.

This creates the connection:

```text
DCMotorSim
    |
    | position
    v
TalonFXSimState
```

---

# Part 15 — Write Velocity Back to the TalonFX

Next:

```java
hopperMotorSim.setRotorVelocity(
    motorSim.getAngularVelocity());
```

This gives the simulated TalonFX the motor's current angular velocity.

The flow is:

```text
DCMotorSim
    ↓
Angular velocity
    ↓
TalonFXSimState
```

---

# Part 16 — Write Acceleration Back to the TalonFX

Next:

```java
hopperMotorSim.setRotorAcceleration(
    motorSim.getAngularAcceleration());
```

This updates the simulated TalonFX's rotor acceleration.

Now the simulated TalonFX has:

```text
Position
Velocity
Acceleration
```

that correspond to the physical simulation.

---

# Part 17 — Record Position in `HopperIOInputs`

Now we need to populate the same inputs that `HopperReal` populates.

Add:

```java
inputs.positionOfHopperMotorRotations =
    motorSim.getAngularPositionRotations();
```

The simulation gives us the position directly in rotations.

We store that value in:

```java
inputs.positionOfHopperMotorRotations
```

This is important because `Hopper` doesn't care whether this value came from:

```text
Real TalonFX
```

or:

```text
DCMotorSim
```

Both provide the same `HopperIOInputs` field.

---

# Part 18 — Record Velocity

Add:

```java
inputs.velocityOfHopperMotorRPS =
    motorSim.getAngularVelocityRPM() / 60.0;
```

The simulation provides velocity in RPM:

```text
Revolutions Per Minute
```

but our input field expects:

```text
Rotations Per Second
```

There are 60 seconds in a minute, so:

```text
RPS = RPM / 60
```

For example:

```text
600 RPM / 60 = 10 RPS
```

---

# Part 19 — Record Acceleration

The simulation provides angular acceleration in:

```text
radians / second²
```

but our input field expects:

```text
rotations / second²
```

So we use:

```java
inputs.accelerationOfHopper =
    motorSim.getAngularAccelerationRadPerSecSq()
        / (2.0 * Math.PI);
```

Why divide by `2π`?

One complete rotation is:

```text
2π radians
```

Therefore:

```text
rotations = radians / 2π
```

So:

```text
rad/s² ÷ 2π
    =
rotations/s²
```

---

# Part 20 — Record Motor Voltage

Add:

```java
inputs.outputOfHopperVolts =
    appliedVoltage;
```

We already calculated the voltage:

```java
double appliedVoltage =
    MathUtil.clamp(dutyCycle * 12.0, -12.0, 12.0);
```

Now we put that value into the inputs.

---

# Part 21 — Record Current

Add:

```java
inputs.statorCurrentOfHopperAmps =
    Math.abs(motorSim.getCurrentDrawAmps());
```

`DCMotorSim` gives us the simulated current draw.

We use:

```java
Math.abs(...)
```

so that the logged current is always positive.

Current magnitude is generally more useful for monitoring things such as motor load.

For example:

```text
-15 A → 15 A
+15 A → 15 A
```

---

# Part 22 — Record Torque Current

The simulation does not separately calculate a torque-current signal in this implementation.

Instead, we use the stator current:

```java
inputs.torqueCurrentHopperAmps =
    inputs.statorCurrentOfHopperAmps;
```

This means the simulation uses the same value for both fields.

This is an approximation for the simulation.

The real TalonFX can provide separate measurements.

---

# Part 23 — Record Duty Cycle

Add:

```java
inputs.dutyCycleOutput =
    dutyCycle;
```

This records the command currently being used by the simulation.

For example:

```text
dutyCycle = 0.75
```

results in:

```text
inputs.dutyCycleOutput = 0.75
```

---

# Part 24 — Report the Motor as Connected

Finally:

```java
inputs.hopperConnected = true;
```

Because this is a simulation, we can assume that the simulated motor controller is connected.

This allows code such as:

```java
if (hopper.getIsMotorConnected()) {
    // Motor is available
}
```

to behave normally during simulation.

---

# Part 25 — The Complete `updateInputs()`

At this point, the method should contain:

```java
@Override
public void updateInputs(HopperIOInputs inputs) {

  double appliedVoltage =
      MathUtil.clamp(dutyCycle * 12.0, -12.0, 12.0);

  motorSim.setInputVoltage(appliedVoltage);
  motorSim.update(0.02);

  /*
   * Feed the physical simulation state back into the TalonFX simulation.
   */
  hopperMotorSim.setRawRotorPosition(
      motorSim.getAngularPosition());

  hopperMotorSim.setRotorVelocity(
      motorSim.getAngularVelocity());

  hopperMotorSim.setRotorAcceleration(
      motorSim.getAngularAcceleration());

  inputs.positionOfHopperMotorRotations =
      motorSim.getAngularPositionRotations();

  inputs.velocityOfHopperMotorRPS =
      motorSim.getAngularVelocityRPM() / 60.0;

  inputs.accelerationOfHopper =
      motorSim.getAngularAccelerationRadPerSecSq()
          / (2.0 * Math.PI);

  inputs.outputOfHopperVolts =
      appliedVoltage;

  inputs.statorCurrentOfHopperAmps =
      Math.abs(motorSim.getCurrentDrawAmps());

  inputs.torqueCurrentHopperAmps =
      inputs.statorCurrentOfHopperAmps;

  inputs.dutyCycleOutput =
      dutyCycle;

  inputs.hopperConnected = true;
}
```

---

# Part 26 — Implement `setDutyCycle()`

The starting method is empty:

```java
@Override
public void setDutyCycle(double dutyCycle) {


}
```

Replace it with:

```java
@Override
public void setDutyCycle(double dutyCycle) {

  this.dutyCycle =
      MathUtil.clamp(dutyCycle, -1.0, 1.0);

  Logger.recordOutput(
      "Hopper/DutyCycleOutput",
      this.dutyCycle);
}
```

---

# Part 27 — Why Use `this.dutyCycle`?

We have a class variable:

```java
private double dutyCycle = 0.0;
```

and a method parameter:

```java
public void setDutyCycle(double dutyCycle)
```

Both have the same name.

We use:

```java
this.dutyCycle
```

to specifically refer to the class variable.

So:

```java
this.dutyCycle =
    MathUtil.clamp(dutyCycle, -1.0, 1.0);
```

means:

> Take the requested duty cycle, clamp it, and store it in the simulation's duty-cycle variable.

---

# Part 28 — Why Store the Duty Cycle?

The duty cycle needs to be stored because the simulation uses it later.

The sequence is:

```text
setDutyCycle(0.75)
        ↓
dutyCycle = 0.75
        ↓
updateInputs()
        ↓
0.75 × 12 V
        ↓
9 V
        ↓
DCMotorSim
```

The command does not directly move the simulated motor.

Instead, it changes the input that the simulation will use on its next update.

---

# Part 29 — Log the Simulated Command

We also record:

```java
Logger.recordOutput(
    "Hopper/DutyCycleOutput",
    this.dutyCycle);
```

This lets us see the simulated command in AdvantageKit logs.

That means real and simulated implementations can produce the same log:

```text
Hopper/DutyCycleOutput
```

This makes comparing real and simulated behavior much easier.

---

# Part 30 — Implement `stop()`

The starting method is empty:

```java
@Override
public void stop() {

}
```

Replace it with:

```java
@Override
public void stop() {

  dutyCycle = 0.0;

  Logger.recordOutput(
      "Hopper/DutyCycleOutput",
      0.0);
}
```

Setting:

```java
dutyCycle = 0.0;
```

means that the next simulation update will apply:

```text
0 × 12 V = 0 V
```

to the simulated motor.

---

# Part 31 — Understanding the Simulation Loop

The complete process is now:

```text
Command
   |
   v
setDutyCycle()
   |
   v
dutyCycle = 0.75
   |
   v
updateInputs()
   |
   v
0.75 × 12 V
   |
   v
9 volts
   |
   v
DCMotorSim
   |
   +------ Position
   |
   +------ Velocity
   |
   +------ Acceleration
   |
   +------ Current
   |
   v
HopperIOInputs
   |
   v
Hopper
   |
   v
AdvantageKit
```

---

# Part 32 — Why Use `DCMotorSim`?

Without a physical simulation, you might do something like:

```java
velocity += 1;
```

But that doesn't model an actual motor.

`DCMotorSim` uses a mathematical model that accounts for motor characteristics and mechanical properties.

This allows us to simulate things such as:

- Acceleration.
- Deceleration.
- Motor voltage.
- Motor current.
- Position.
- Velocity.
- Mechanical inertia.

This makes the simulation much more useful for testing robot code.

---

# Part 33 — Real vs Simulation

The most important thing is that both implementations satisfy the same interface.

### Real

```text
Hopper
  ↓
HopperIO
  ↓
HopperReal
  ↓
TalonFX
  ↓
Real Motor
```

### Simulation

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

But both produce:

```java
HopperIOInputs
```

Therefore `Hopper` can use either one.

---

# Part 34 — Why Simulate the TalonFX Too?

You may ask:

> If `DCMotorSim` already models the motor, why create a simulated TalonFX?

The answer is that some robot code may interact directly with the TalonFX.

By using:

```java
TalonFXSimState
```

we can make the simulated TalonFX reflect the physical state of our `DCMotorSim`.

For example:

```java
hopperMotorSim.setRotorVelocity(
    motorSim.getAngularVelocity());
```

means:

```text
Physical simulation
       ↓
   DCMotorSim
       ↓
TalonFXSimState
       ↓
Simulated TalonFX
```

This gives us a much closer representation of what the real hardware would look like.

---

# Student Challenge

Starting from the original empty `HopperSim`, try implementing the simulation yourself.

Complete these steps in order:

1. Create the `DCMotorSim`.
2. Create the simulated `TalonFX`.
3. Get its `TalonFXSimState`.
4. Convert duty cycle into voltage.
5. Apply voltage to `DCMotorSim`.
6. Advance the simulation by 20 ms.
7. Copy position into the TalonFX simulation.
8. Copy velocity into the TalonFX simulation.
9. Copy acceleration into the TalonFX simulation.
10. Populate every `HopperIOInputs` field.
11. Implement `setDutyCycle()`.
12. Implement `stop()`.

---

# Student Questions

Before moving on, make sure you can answer these.

### Question 1

Why does `HopperSim` contain a `TalonFX` if there isn't a real TalonFX?

### Question 2

What does this line do?

```java
motorSim.update(0.02);
```

### Question 3

Why do we multiply duty cycle by `12.0`?

### Question 4

Why do we clamp the resulting voltage between `-12.0` and `12.0`?

### Question 5

Why do we divide RPM by `60.0`?

### Question 6

Why do we divide radians by `2π` to get rotations?

### Question 7

What is the purpose of:

```java
hopperMotorSim.setRotorVelocity(...)
```

?

### Question 8

What would happen if `dutyCycle` was never stored in the class variable?

---

# Key Takeaways

`HopperSim` creates a **virtual version of the hopper motor**.

The core idea is:

```text
Duty Cycle
    ↓
Voltage
    ↓
DCMotorSim
    ↓
Physical Motor Behavior
    ↓
Position / Velocity / Acceleration / Current
    ↓
HopperIOInputs
```

At the same time, the simulated physical state is copied into:

```java
TalonFXSimState
```

so the simulated TalonFX reflects the simulated motor.

The biggest architectural takeaway is:

> **`HopperReal` and `HopperSim` implement the same `HopperIO` interface, so the rest of the robot doesn't need to know whether it is talking to a real motor or a simulation.**

That is what allows us to write robot code once and run it both on the real robot and in simulation.