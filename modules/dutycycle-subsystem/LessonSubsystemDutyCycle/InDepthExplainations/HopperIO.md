# HopperIO — Adding Logged Inputs

## Learning Objectives

By the end of this lesson, you should be able to:

- Understand the purpose of the `HopperIO` interface.
- Understand what `HopperIOInputs` represents.
- Explain how AdvantageKit's `@AutoLog` annotation is used.
- Add sensor and status values to an IO input class.
- Understand the difference between `double` and `boolean`.
- Understand how input values flow from the hardware to the subsystem and AdvantageKit.
- Choose appropriate names and initial values for sensor data.

---

# Part 1 — Understanding the Starting Code

Our `HopperIO` interface provides the connection between the Hopper subsystem and the underlying hardware implementation.

The important section for this lesson is:

```java
@AutoLog
class HopperIOInputs {
}
```

Currently, the class is empty.

That means the Hopper has no place to store information about the motor.

We want to add information such as:

- Motor velocity
- Motor current
- Motor voltage
- Motor acceleration
- Motor position
- Motor output
- Motor connection status

---

# Part 2 — What Is `HopperIOInputs`?

Inside `HopperIO`, we have:

```java
class HopperIOInputs {
}
```

This class acts as a **container for sensor data**.

Think of it like a box that holds information about the Hopper.

```text
Hopper Motor
     |
     | Sensor information
     v
HopperIOInputs
     |
     +-- Velocity
     +-- Current
     +-- Voltage
     +-- Acceleration
     +-- Position
     +-- Duty Cycle
     +-- Connected
```

The IO implementation is responsible for filling this container with the latest values.

The Hopper subsystem can then read those values.

---

# Part 3 — Adding Motor Velocity

The first input we want to add is motor velocity.

Add the following inside `HopperIOInputs`:

```java
/** Hopper motor velocity in rotations per second (RPS). */
public double velocityOfHopperMotorRPS = 0.0;
```

Let's break this down.

## `public`

```java
public
```

This allows other classes to access this value.

Our `Hopper` subsystem needs to be able to read the value.

---

## `double`

```java
double
```

A `double` stores a decimal number.

Examples:

```text
0.0
10.5
25.75
-12.3
```

Motor velocity can contain decimal values, so `double` is appropriate.

---

## The variable name

```java
velocityOfHopperMotorRPS
```

The name describes what the value represents.

Breaking it apart:

```text
velocity
    of
Hopper Motor
    RPS
```

`RPS` means **rotations per second**.

For example:

```text
velocityOfHopperMotorRPS = 25.5
```

means the Hopper motor is rotating at 25.5 rotations per second.

---

## Initial value

```java
= 0.0;
```

When the object is first created, we assume the motor velocity is zero.

The hardware implementation will update this value later.

---

# Part 4 — Adding Stator Current

Next, add:

```java
/** Hopper motor stator current in amps. */
public double statorCurrentOfHopperAmps = 0.0;
```

This stores the motor's **stator current**.

Current is measured in amps.

For example:

```text
statorCurrentOfHopperAmps = 15.2
```

means the motor is currently reporting 15.2 amps of stator current.

Again, we use a `double` because current can contain decimal values.

---

# Part 5 — Adding Torque Current

Add:

```java
/** Hopper motor torque-producing current in amps. */
public double torqueCurrentHopperAmps = 0.0;
```

This stores the current associated with producing motor torque.

The value is measured in amps.

This can be useful when analyzing how hard the motor is working.

For example:

```text
torqueCurrentHopperAmps = 8.5
```

means the motor is using 8.5 amps of torque-producing current.

---

# Part 6 — Adding Output Voltage

Add:

```java
/** Hopper motor output voltage in volts. */
public double outputOfHopperVolts = 0.0;
```

This stores the voltage being applied to the motor.

The unit is **volts**.

For example:

```text
outputOfHopperVolts = 6.0
```

means the motor is receiving approximately 6 volts of output.

---

# Part 7 — Adding Acceleration

Add:

```java
/** Hopper motor angular acceleration in rotations per second squared. */
public double accelerationOfHopper = 0.0;
```

This stores the motor's angular acceleration.

The units are:

```text
rotations per second squared
```

or:

```text
rotations / second²
```

Acceleration tells us how quickly the motor's velocity is changing.

For example:

```text
Velocity = 10 RPS
Velocity = 20 RPS
```

The motor's velocity increased, meaning the motor experienced positive acceleration.

---

# Part 8 — Adding Motor Position

Add:

```java
/** Hopper motor position in rotations. */
public double positionOfHopperMotorRotations = 0.0;
```

This stores the motor's current position.

The units are **rotations**.

For example:

```text
positionOfHopperMotorRotations = 5.5
```

means the motor has moved 5.5 rotations from its reference position.

Unlike velocity, which tells us how fast the motor is moving, position tells us **where the motor is**.

---

# Part 9 — Adding Duty Cycle

Add:

```java
/** Current hopper motor duty-cycle output from -1.0 to +1.0. */
public double dutyCycleOutput = 0.0;
```

This stores the current motor output.

The expected range is:

```text
-1.0 ───────── 0.0 ───────── +1.0
 reverse       stop          forward
```

Examples:

| Value | Meaning |
|---:|---|
| `-1.0` | Full reverse |
| `-0.5` | 50% reverse |
| `0.0` | No output |
| `0.5` | 50% forward |
| `1.0` | Full forward |

This value is useful because we can log what output the motor controller is actually receiving.

---

# Part 10 — Adding Motor Connection Status

The final input we need is whether the motor controller is connected.

Add:

```java
/** Whether the hopper motor controller is connected. */
public boolean hopperConnected = false;
```

Notice that this is different from the previous variables.

Instead of:

```java
double
```

we use:

```java
boolean
```

A `boolean` can only have two values:

```text
true
false
```

So:

```java
hopperConnected = true;
```

means:

> The motor controller is connected.

While:

```java
hopperConnected = false;
```

means:

> The motor controller is not connected.

We initialize it to `false` because we don't want to assume the hardware is connected before the IO implementation has checked it.

---

# Part 11 — Why Are These Values Inside `HopperIOInputs`?

The completed input class now looks like:

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

These variables represent information **coming into the subsystem**.

This is why the class is called:

```java
HopperIOInputs
```

The IO implementation gathers information from the motor and puts it into this object.

---

# Part 12 — Understanding the Data Flow

The complete data flow looks like this:

```text
               Motor Controller
                      |
                      | Sensor Data
                      v
                 HopperReal
                      |
                      v
                   HopperIO
                      |
                      | updateInputs()
                      v
               HopperIOInputs
                      |
                      v
                   Hopper
                      |
                      v
             AdvantageKit Logger
```

For simulation, the process is similar:

```text
              Physics Simulation
                      |
                      v
                 HopperSim
                      |
                      v
                   HopperIO
                      |
                      v
               HopperIOInputs
                      |
                      v
                   Hopper
                      |
                      v
             AdvantageKit Logger
```

The important part is that the `Hopper` subsystem does not need to know where the data came from.

It simply receives an `HopperIOInputs` object.

---

# Part 13 — How `Hopper.java` Uses These Inputs

Remember the `Hopper` subsystem from the previous lesson.

It contains:

```java
private final HopperIOInputsAutoLogged inputs =
    new HopperIOInputsAutoLogged();
```

Then, inside `periodic()`:

```java
io.updateInputs(inputs);

Logger.processInputs("Hopper/inputs", inputs);
```

The IO implementation fills in the values.

For example:

```text
HopperReal
    |
    | velocity = 25 RPS
    | current = 10 A
    | voltage = 6 V
    v
HopperIOInputs
    |
    +-- velocityOfHopperMotorRPS = 25
    +-- statorCurrentOfHopperAmps = 10
    +-- outputOfHopperVolts = 6
```

AdvantageKit then logs those values.

---

# Part 14 — How the Getter Methods Use the Inputs

The `Hopper` subsystem provides methods such as:

```java
public double getVelocity() {
  return inputs.velocityOfHopperMotorRPS;
}
```

This means that another class can simply ask:

```java
m_Hopper.getVelocity();
```

instead of directly accessing the IO inputs.

The data flow is:

```text
Motor
  ↓
HopperReal
  ↓
HopperIOInputs
  ↓
Hopper
  ↓
getVelocity()
  ↓
Other Robot Code
```

This keeps the IO implementation hidden from the rest of the robot.

---

# Part 15 — Understanding `@AutoLog`

At the top of the input class we have:

```java
@AutoLog
class HopperIOInputs {
```

`@AutoLog` is an AdvantageKit annotation.

It tells AdvantageKit to generate an automatically logged version of this input class.

This generates:

```text
HopperIOInputsAutoLogged
```

which is why the Hopper subsystem can create:

```java
new HopperIOInputsAutoLogged();
```

The fields that we add to `HopperIOInputs` can then be automatically logged by AdvantageKit.

This is extremely useful when viewing robot data later in AdvantageScope.

---

# Part 16 — Why Initialize Everything?

You may notice that every numeric value starts with:

```java
= 0.0;
```

and the connection status starts with:

```java
= false;
```

For example:

```java
public double velocityOfHopperMotorRPS = 0.0;
public boolean hopperConnected = false;
```

Initializing values gives them a known starting state.

Instead of having an unknown value, we know:

```text
Velocity     → 0 RPS
Current      → 0 A
Voltage      → 0 V
Acceleration → 0 RPS²
Position     → 0 rotations
Duty Cycle   → 0
Connected    → false
```

The IO implementation can then replace these values with the actual measurements.

---

# Part 17 — Student Challenge

Try adding a new input to `HopperIOInputs`.

For example, suppose we want to track motor temperature.

Create a variable that stores:

> The Hopper motor temperature in degrees Celsius.

Consider:

- What type should it be?
- What should the variable be called?
- What should its starting value be?
- What unit should be documented?

Your result might look something like:

```java
/** Hopper motor temperature in degrees Celsius. */
public double motorTemperatureCelsius = 0.0;
```

---

# Student Questions

Before moving on, make sure you can answer these questions.

### Question 1

What is the purpose of `HopperIOInputs`?

---

### Question 2

Why do we use `double` for values such as velocity and voltage?

---

### Question 3

Why do we use `boolean` for `hopperConnected`?

---

### Question 4

What is the difference between:

```java
velocityOfHopperMotorRPS
```

and:

```java
positionOfHopperMotorRotations
```

?

---

### Question 5

Why are the values initialized to `0.0`?

---

### Question 6

What does this annotation do?

```java
@AutoLog
```

---

### Question 7

Where does the data in `HopperIOInputs` ultimately come from?

---

# Final Code

After completing this lesson, your `HopperIOInputs` class should look like:

```java
@AutoLog
class HopperIOInputs {

  /** Hopper motor velocity in rotations per second (RPS). */
  public double velocityOfHopperMotorRPS = 0.0;

  /** Hopper motor stator current in amps. */
  public double statorCurrentOfHopperAmps = 0.0;

  /** Hopper motor torque-producing current in amps. */
  public double torqueCurrentHopperAmps = 0.0;

  /** Hopper motor output voltage in volts. */
  public double outputOfHopperVolts = 0.0;

  /** Hopper motor angular acceleration in rotations per second squared. */
  public double accelerationOfHopper = 0.0;

  /** Hopper motor position in rotations. */
  public double positionOfHopperMotorRotations = 0.0;

  /** Current hopper motor duty-cycle output from -1.0 to +1.0. */
  public double dutyCycleOutput = 0.0;

  /** Whether the hopper motor controller is connected. */
  public boolean hopperConnected = false;
}
```

# Key Takeaways

The most important concepts from this lesson are:

1. **`HopperIOInputs` stores information coming from the Hopper hardware.**

2. **The IO implementation fills in the input values.**

3. **The Hopper subsystem reads those values.**

4. **AdvantageKit logs those values using `@AutoLog`.**

5. **`double` is used for numerical measurements.**

6. **`boolean` is used for true/false states.**

7. **Initializing values gives the system a known starting state.**

8. **The IO layer allows the same input structure to work with real hardware, simulation, and replay.**

The overall architecture is:

```text
             ┌─────────────────┐
             │ Real / Sim IO   │
             └────────┬────────┘
                      │
                      │ updateInputs()
                      ▼
             ┌─────────────────┐
             │ HopperIOInputs  │
             │                 │
             │ Velocity        │
             │ Current         │
             │ Voltage         │
             │ Acceleration    │
             │ Position        │
             │ Duty Cycle      │
             │ Connected       │
             └────────┬────────┘
                      │
                      ▼
                ┌──────────┐
                │  Hopper  │
                └────┬─────┘
                     │
                     ▼
               AdvantageKit
                  Logging
```