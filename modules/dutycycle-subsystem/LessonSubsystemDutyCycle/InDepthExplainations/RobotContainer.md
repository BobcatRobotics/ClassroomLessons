# RobotContainer — Adding a Subsystem

## Learning Objectives

By the end of this lesson, you should be able to:

- Import a subsystem into `RobotContainer`.
- Declare a subsystem as a class variable.
- Understand the difference between declaring and creating an object.
- Understand how the Hopper IO layer works.
- Select the appropriate IO implementation based on the robot's current mode.
- Understand how `new` is used to create Java objects.

---

# Part 1 — Adding the Hopper

Our starting `RobotContainer.java` already contains the Xbox controller, but it does not know anything about our Hopper subsystem.

Before we can use the Hopper, we need to import the classes that we will need.

## Step 1 — Add the Hopper imports

At the top of `RobotContainer.java`, add:

```java
import frc.robot.subsystems.Hopper.Hopper;
import frc.robot.subsystems.Hopper.HopperIO;
import frc.robot.subsystems.Hopper.HopperReal;
import frc.robot.subsystems.Hopper.HopperSim;
```

### What does each import do?

| Import | Purpose |
|---|---|
| `Hopper` | The main subsystem that the rest of the robot interacts with |
| `HopperIO` | Defines the interface between the subsystem and the implemented Real/Sim |
| `HopperReal` | Provides the implementation for the real robot hardware |
| `HopperSim` | Provides the implementation for robot simulation |

The important idea is that `RobotContainer` does not need to know how the Hopper's motor actually works.

Instead, the structure looks like this:

```text
RobotContainer
      |
      v
   Hopper
      |
      v
   HopperIO
   /      \
  /        \
Real      Sim
```

The `Hopper` subsystem provides the interface that the rest of our robot code uses.

The IO implementation handles the actual interaction with either:

- Real hardware
- Simulation
- Replay

This allows the rest of our robot code to work without caring which environment the robot is currently running in.

---

# Part 2 — Declaring the Hopper

Now that we have imported the Hopper classes, we need to create a variable that will hold our Hopper subsystem.

Inside the `RobotContainer` class, add:

```java
private final Hopper m_Hopper;
```

For example:

```java
public class RobotContainer {

  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.kDriverControllerPort);

  private final Hopper m_Hopper;

  public RobotContainer() {
    // ...
  }
}
```

## Breaking down the line

Let's look at each part:

```java
private final Hopper m_Hopper;
```

### `private`

```java
private
```

`private` means that this variable can only be directly accessed from within the `RobotContainer` class.

Other classes cannot directly access `m_Hopper`.

This helps us control how different parts of our robot interact with each other.

---

### `final`

```java
final
```

`final` means that once `m_Hopper` has been assigned a Hopper object, we cannot assign a different object to it later.

For example, this would not be allowed:

```java
m_Hopper = new Hopper(new HopperReal());

m_Hopper = new Hopper(new HopperSim());
```

The variable can only be assigned once.

---

### `Hopper`

```java
Hopper
```

This is the **type** of the variable.

It tells Java:

> `m_Hopper` will contain a `Hopper` object.

This is similar to:

```java
int speed;
```

where `int` is the type.

Or:

```java
Command command;
```

where `Command` is the type.

---

### `m_Hopper`

```java
m_Hopper
```

This is the name of our variable.

We will use this variable later when we want to interact with the Hopper.

For example:

```java
m_Hopper.stop();
```

or:

```java
m_Hopper.setDutyCycle(0.75);
```

---

## Notice the semicolon

```java
private final Hopper m_Hopper;
                             ^
```

The semicolon tells Java that this statement is complete.

---

# Part 3 — Choosing the Correct Hopper

At this point, we have **declared** our Hopper variable, but we haven't actually created a Hopper object.

We need to decide which version of the Hopper to use.

Our robot can run in several different modes:

```text
REAL
SIM
REPLAY
```

We want the Hopper to behave differently depending on which mode the robot is running in.

Inside the `RobotContainer` constructor, add:

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

---

## What is a `switch`?

A `switch` allows us to execute different code depending on the value of a variable.

In our case, we're checking:

```java
Constants.currentMode
```

We then determine which mode the robot is currently running in.

Conceptually:

```text
             currentMode
                  |
        ┌─────────┼─────────┐
        |         |         |
       REAL      SIM      REPLAY
        |         |         |
        v         v         v
     Hopper     Hopper    Hopper
      Real       Sim       IO
```

---

## REAL Mode

When the robot is running on the actual robot, we use:

```java
case REAL:
  m_Hopper = new Hopper(new HopperReal());
  break;
```

`HopperReal` contains the code needed to communicate with the actual robot hardware.

The structure is:

```text
RobotContainer
      |
      v
   Hopper
      |
      v
 HopperReal
      |
      v
Real Motor Hardware
```

This means that when the robot is on the real robot, calling:

```java
m_Hopper.setDutyCycle(0.75);
```

eventually results in a command being sent to the real motor.

---

## SIM Mode

When running the robot in simulation, we don't want to try to communicate with a real motor.

Instead, we use:

```java
case SIM:
  m_Hopper = new Hopper(new HopperSim());
  break;
```

The structure becomes:

```text
RobotContainer
      |
      v
   Hopper
      |
      v
  HopperSim
      |
      v
Physics Simulation
```

This allows us to test our robot code without having the physical robot connected.

---

## REPLAY Mode

The `default` case handles modes where we don't want the Hopper to communicate with hardware.

We use:

```java
default:
  m_Hopper = new Hopper(new HopperIO() {});
  break;
```

`HopperIO` is the interface used by the Hopper subsystem to communicate with its implementation.

Here, we create an empty implementation.

This effectively tells the Hopper:

> There is no physical or simulated hardware that you need to control.

This is useful when replaying AdvantageKit logs.

---

## Why use different implementations?

The important advantage of this architecture is that the rest of our robot code doesn't need to care about the current mode.

For example, we can write:

```java
m_Hopper.setDutyCycle(0.75);
```

The command is the same whether we are running:

- On the real robot
- In simulation
- During a replay

The underlying implementation changes.

```text
                  m_Hopper
                     |
                     v
                  Hopper
                     |
                  HopperIO
                /    |    \
               /     |     \
              v      v      v
           Real     Sim    Replay
```

This is one of the major benefits of using an IO layer.

---

# Part 4 — Understanding `new`

Take a look at this line:

```java
m_Hopper = new Hopper(new HopperReal());
```

It may look like one operation, but there are actually **two objects being created**.

Let's start with the inside:

```java
new HopperReal()
```

This creates a new `HopperReal` object.

That object is then passed into:

```java
new Hopper(...)
```

which creates the actual `Hopper` subsystem.

So:

```java
m_Hopper = new Hopper(new HopperReal());
```

can be thought of as:

```text
Create HopperReal
      |
      v
Give HopperReal to Hopper
      |
      v
Create Hopper
      |
      v
Store Hopper in m_Hopper
```

---

## Understanding `new`

In Java, the `new` keyword is used to create an object.

For example:

```java
new HopperReal()
```

means:

> Create a new object whose type is `HopperReal`.

Similarly:

```java
new Hopper(...)
```

means:

> Create a new object whose type is `Hopper`.

---

## Nested `new` statements

The following code:

```java
m_Hopper = new Hopper(new HopperReal());
```

contains two `new` statements:

```java
new Hopper(
    new HopperReal()
)
```

The inner object is created first:

```java
new HopperReal()
```

Then that object is passed into the Hopper:

```java
new Hopper(hopperRealObject)
```

Finally, the resulting Hopper is stored in:

```java
m_Hopper
```

---

# Putting It All Together

After completing Parts 1–4, your `RobotContainer` should contain:

```java
import frc.robot.subsystems.Hopper.Hopper;
import frc.robot.subsystems.Hopper.HopperIO;
import frc.robot.subsystems.Hopper.HopperReal;
import frc.robot.subsystems.Hopper.HopperSim;
```

and:

```java
private final Hopper m_Hopper;
```

and the constructor should select the appropriate implementation:

```java
public RobotContainer() {

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

  configureBindings();
}
```

At this point, `RobotContainer` knows about the Hopper and has created the appropriate implementation for the current robot mode.

We are now ready to actually **control the Hopper with the Xbox controller**.

That will be the next part of the lesson.