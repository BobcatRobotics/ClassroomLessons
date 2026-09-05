# Hopper Subsystem — Understanding the IO Layer

## Learning Objectives

By the end of this lesson, you should be able to:

- Understand what a WPILib subsystem is.
- Understand how `SubsystemBase` is used.
- Explain the purpose of the `HopperIO` interface.
- Understand how the IO layer separates hardware from robot logic.
- Understand how AdvantageKit receives and logs subsystem inputs.
- Understand constructors and dependency injection.
- Use methods to control and read information from a subsystem.
- Understand the difference between real hardware and simulation implementations.

---

# Part 1 — What Is a Subsystem?

The Hopper is a mechanism on our robot that needs to be controlled and monitored.

In command-based programming, mechanisms are represented using **subsystems**.

Our Hopper is defined as:

```java
public class Hopper extends SubsystemBase {
```

The important part is:

```java
extends SubsystemBase
```

`SubsystemBase` is provided by WPILib and gives our class the functionality expected from a command-based subsystem.

A subsystem represents a physical or logical part of the robot.

Examples include:

- Drivetrain
- Intake
- Shooter
- Elevator
- Arm
- Hopper

Our Hopper is therefore a subsystem that is responsible for controlling and monitoring the Hopper mechanism.

---

# Part 2 — The IO Layer

The next important piece is:

```java
private final HopperIO io;
```

This variable stores the **hardware implementation** that the Hopper will use.

The important idea is that `Hopper` does not directly control a TalonFX.

Instead, it communicates through the `HopperIO` interface.

The architecture looks like this:

```text
             Hopper Subsystem
                    |
                    v
                HopperIO
               /        \
              /          \
             v            v
        HopperReal     HopperSim
             |            |
             v            v
       Real Hardware   Simulation
```

This allows us to change the underlying implementation without changing the Hopper subsystem.

For example, the following code can remain exactly the same:

```java
m_Hopper.setDutyCycle(0.75);
```

Whether the robot is running on:

- The real robot
- Simulation
- Log replay

the Hopper subsystem can provide the same interface.

---

# Part 3 — Declaring the IO Object

Inside the Hopper class we have:

```java
private final HopperIO io;
```

Let's break this line down.

### `private`

```java
private
```

Only the `Hopper` class can directly access this variable.

Other classes cannot directly manipulate the IO implementation.

This helps keep the internal implementation of the subsystem hidden.

---

### `final`

```java
final
```

The variable can only be assigned once.

Once the Hopper has been given its IO implementation, we don't replace it with another implementation.

---

### `HopperIO`

```java
HopperIO
```

This is the type of the variable.

`HopperIO` is an interface that defines what the Hopper's hardware implementation needs to provide.

For example, it may contain methods such as:

```java
setDutyCycle()
stop()
updateInputs()
periodic()
simulationPeriodic()
```

---

### `io`

```java
io
```

This is the name of the variable.

Whenever the Hopper needs to communicate with its implementation, it uses:

```java
io
```

For example:

```java
io.setDutyCycle(...);
```

---

# Part 4 — Logged Inputs

The Hopper also contains:

```java
private final HopperIOInputsAutoLogged inputs =
    new HopperIOInputsAutoLogged();
```

This object stores information coming **from the Hopper**.

Examples of information include:

- Motor velocity
- Motor position
- Duty-cycle output
- Motor connection status

The IO implementation updates these values.

The Hopper then uses them to provide information to the rest of the robot.

The data flow looks like:

```text
             Motor / Simulation
                    |
                    v
                HopperIO
                    |
                    v
              updateInputs()
                    |
                    v
                 inputs
                    |
                    v
             AdvantageKit
```

---

# Part 5 — The Constructor

The Hopper constructor is:

```java
public Hopper(HopperIO io) {
  this.io = io;
}
```

This is one of the most important parts of the subsystem.

The constructor receives a `HopperIO` object.

That object is then stored in the subsystem.

---

## What does `this.io` mean?

There are two variables named `io` here:

```java
public Hopper(HopperIO io) {
  this.io = io;
}
```

The `io` on the right side:

```java
io
```

is the parameter passed into the constructor.

The `this.io` on the left side:

```java
this.io
```

refers to the variable belonging to the Hopper object.

So this:

```java
this.io = io;
```

means:

> Store the IO implementation that was passed into the constructor as the Hopper's IO implementation.

---

# Part 6 — Creating the Hopper

This constructor allows us to create different versions of the Hopper.

For example:

```java
new Hopper(new HopperReal());
```

creates a Hopper using the real hardware implementation.

While:

```java
new Hopper(new HopperSim());
```

creates a Hopper using the simulation implementation.

The Hopper itself doesn't need to change.

Only the IO implementation changes.

```text
                   Hopper
                     |
                 HopperIO
                /        \
               /          \
              v            v
         HopperReal     HopperSim
              |             |
              v             v
        Real hardware    Simulation
```

This technique is commonly called **dependency injection**.

Instead of the Hopper creating its own hardware implementation, we give the Hopper the implementation it should use.

---

# Part 7 — The `periodic()` Method

Our Hopper contains:

```java
@Override
public void periodic() {
  io.periodic();
  io.updateInputs(inputs);

  Logger.processInputs("Hopper/inputs", inputs);
}
```

The `periodic()` method is automatically called by the WPILib command scheduler.

This happens repeatedly while the robot is running.

---

## `@Override`

```java
@Override
```

This tells Java that we are replacing a method provided by the parent class, `SubsystemBase`.

In this case, we are providing our own implementation of:

```java
periodic()
```

---

## `io.periodic()`

```java
io.periodic();
```

This tells the IO implementation to perform any periodic work it needs to do.

The Hopper doesn't need to know whether this is:

```text
HopperReal
```

or:

```text
HopperSim
```

It simply calls:

```java
io.periodic();
```

---

## `io.updateInputs(inputs)`

```java
io.updateInputs(inputs);
```

This asks the IO implementation to update the sensor information.

For example:

```text
Motor
  |
  | velocity
  | position
  | current
  | voltage
  v
HopperIO
  |
  v
inputs
```

---

## `Logger.processInputs()`

```java
Logger.processInputs("Hopper/inputs", inputs);
```

This sends the inputs to AdvantageKit for logging.

The string:

```java
"Hopper/inputs"
```

is the logging path.

The input values are stored under that path.

This allows us to inspect the Hopper's data later using tools such as AdvantageScope.

---

# Part 8 — Setting the Motor Speed

The Hopper provides this method:

```java
public void setDutyCycle(double dutyCycle) {
  io.setDutyCycle(
      MathUtil.clamp(dutyCycle, -1.0, 1.0));
}
```

This is the method that other parts of the robot use to control the Hopper.

For example:

```java
m_Hopper.setDutyCycle(0.75);
```

---

## The `double` parameter

```java
double dutyCycle
```

The method accepts a decimal number.

For example:

```java
0.5
```

or:

```java
0.75
```

The value represents the requested motor output.

---

# Part 9 — `MathUtil.clamp()`

The Hopper uses:

```java
MathUtil.clamp(dutyCycle, -1.0, 1.0)
```

This prevents the requested value from going outside the valid range.

The valid range is:

```text
-1.0 ───────── 0.0 ───────── +1.0
 reverse       stop          forward
```

For example:

| Requested Value | Result |
|---:|---:|
| `0.5` | `0.5` |
| `0.75` | `0.75` |
| `1.0` | `1.0` |
| `1.5` | `1.0` |
| `-0.5` | `-0.5` |
| `-2.0` | `-1.0` |

This protects the IO implementation from receiving an invalid duty-cycle value.

---

# Part 10 — Reading Hopper Information

The Hopper also provides several methods for retrieving information.

## Motor Duty Cycle

```java
public double getDutyCycle() {
  return inputs.dutyCycleOutput;
}
```

This returns the current motor duty-cycle output.

Other robot code can simply call:

```java
m_Hopper.getDutyCycle();
```

instead of directly accessing the `inputs` object.

---

## Motor Velocity

```java
public double getVelocity() {
  return inputs.velocityOfHopperMotorRPS;
}
```

This returns the motor velocity in rotations per second.

For example:

```java
double velocity = m_Hopper.getVelocity();
```

---

## Motor Position

```java
public double getPosition() {
  return inputs.positionOfHopperMotorRotations;
}
```

This returns the motor position in rotations.

---

## Motor Connection

```java
public boolean getIsMotorConnected() {
  return inputs.hopperConnected;
}
```

This returns either:

```text
true
```

or:

```text
false
```

depending on whether the motor controller is connected.

---

# Part 11 — Stopping the Hopper

The Hopper provides a simple method:

```java
public void stop() {
  io.stop();
}
```

Other robot code can now simply call:

```java
m_Hopper.stop();
```

The Hopper then tells its IO implementation to stop the motor.

The rest of the robot does not need to know how the motor is stopped.

---

# Part 12 — Simulation

The Hopper also has:

```java
@Override
public void simulationPeriodic() {
  io.simulationPeriodic();
}
```

`simulationPeriodic()` is called periodically while the robot is running in simulation.

Instead of putting simulation-specific code directly inside the Hopper, we pass the call to:

```java
io.simulationPeriodic();
```

This means `HopperSim` can contain the physics simulation while the main `Hopper` class remains unchanged.

The architecture becomes:

```text
                  Hopper
                    |
                    v
                 HopperIO
                /        \
               /          \
              v            v
         HopperReal     HopperSim
              |             |
              v             v
        Real TalonFX     Physics Model
```

---

# Part 13 — Why Use This Architecture?

Without an IO layer, we might have code like:

```java
public class Hopper extends SubsystemBase {

  private TalonFX motor = new TalonFX(10);

}
```

Now the Hopper is directly tied to a TalonFX.

That creates problems when we want to:

- Run simulation
- Replay logs
- Unit test the subsystem
- Change motor controllers
- Test code without hardware

With the IO layer, the Hopper only knows about:

```java
HopperIO
```

The implementation can then change.

```text
                 Hopper
                    |
                 HopperIO
              /      |      \
             /       |       \
            v        v        v
       HopperReal HopperSim Replay
            |        |
            v        v
         Hardware  Physics
```

This makes the code much easier to maintain and test.

---

# Student Questions

Before moving on, make sure you can answer these questions.

### Question 1

What is the purpose of:

```java
extends SubsystemBase
```

?

---

### Question 2

Why does the Hopper contain:

```java
private final HopperIO io;
```

instead of directly creating a TalonFX?

---

### Question 3

What does this line do?

```java
this.io = io;
```

---

### Question 4

What is the difference between:

```java
Hopper
```

and:

```java
HopperIO
```

?

---

### Question 5

Why can we use the same code:

```java
m_Hopper.setDutyCycle(0.75);
```

both on the real robot and in simulation?

---

### Question 6

What would happen if we called:

```java
m_Hopper.setDutyCycle(2.0);
```

?

---

### Question 7

What is the purpose of:

```java
Logger.processInputs("Hopper/inputs", inputs);
```

?

---

# Challenge

Without looking at the `Hopper` class, explain what you think happens when this code runs:

```java
m_Hopper.setDutyCycle(0.75);
```

Try to trace the entire process:

```text
RobotContainer
      ↓
    Hopper
      ↓
   HopperIO
      ↓
   HopperReal
      ↓
    TalonFX
```

Then explain how the process would be different when running in simulation:

```text
RobotContainer
      ↓
    Hopper
      ↓
   HopperIO
      ↓
   HopperSim
      ↓
 Physics Simulation
```

---

# Key Takeaways

The most important concepts from this lesson are:

### 1. Subsystems represent robot mechanisms

```java
public class Hopper extends SubsystemBase
```

The Hopper is responsible for controlling and monitoring the hopper mechanism.

### 2. The IO layer separates the subsystem from hardware

```java
private final HopperIO io;
```

The Hopper doesn't need to know how the hardware works.

### 3. The constructor provides the implementation

```java
public Hopper(HopperIO io) {
  this.io = io;
}
```

This allows us to provide different implementations.

### 4. Real and simulation implementations can be swapped

```java
new Hopper(new HopperReal());
```

versus:

```java
new Hopper(new HopperSim());
```

### 5. The subsystem provides a simple API

Other robot code can use:

```java
m_Hopper.setDutyCycle(0.75);
m_Hopper.stop();
m_Hopper.getVelocity();
m_Hopper.getPosition();
```

without needing to know anything about the underlying hardware.

### 6. AdvantageKit logs the subsystem inputs

```java
Logger.processInputs("Hopper/inputs", inputs);
```

This allows us to inspect what the Hopper is doing during operation and replay.