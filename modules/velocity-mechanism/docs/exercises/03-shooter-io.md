# Exercise 3: The Shooter I/O Interface

## Objective

Understand how a Java interface separates mechanism logic from hardware-specific code, and how `@AutoLog` turns an input class into AdvantageKit log data.

You will learn how to:

- Read and write Java interfaces with default methods.
- Describe sensor data in an `@AutoLog` input class.
- Explain how real, simulated, and test implementations share one interface.

## Background

`ShooterIO.java` defines everything `Shooter` is allowed to know about the hardware. It has one nested data class and four methods, and no hardware dependencies:

```java
public interface ShooterIO {
  @AutoLog
  class ShooterIOInputs { ... }

  default void updateInputs(ShooterIOInputs inputs) {}
  default void setVelocity(double rotationsPerSecond) {}
  default void setVoltage(double volts) {}
  default void stop() {}
}
```

Before changing anything, find where `Shooter.java` calls each method.

## Task 1: The interface contract

**Tasks:**

1. Why is `ShooterIO` an interface instead of a concrete class?
2. Fill in this table by reading `Shooter.java`:

   | `ShooterIO` method | Called from | Purpose |
   |---|---|---|
   | `updateInputs` | | |
   | `setVelocity` | | |
   | `setVoltage` | | |
   | `stop` | | |

3. Identify the two implementations in the project and which `Robot` selects.

Do not add imports for CTRE, WPILib simulation, or other hardware libraries to this file. The only import is AdvantageKit's `@AutoLog`.

## Task 2: The input structure

`ShooterIOInputs` has seven fields, all public and initialized to a default value:

| Field | Type | Units | Meaning |
|---|---|---|---|
| `positionRotations` | `double` | rotations | Flywheel position |
| `velocityRotationsPerSecond` | `double` | rot/s | Measured velocity |
| `appliedVolts` | `double` | V | Voltage applied to the motor |
| `statorCurrentAmps` | `double` | A | Motor stator current |
| `temperatureCelsius` | `double` | °C | Motor temperature |
| `velocityControlEnabled` | `boolean` | n/a | Whether closed-loop velocity control is active |
| `goalVelocityRotationsPerSecond` | `double` | rot/s | The last requested velocity |

**Tasks:**

1. Which implementation populates each field? Compare `ShooterIOReal.updateInputs(...)` and `ShooterIOSim.updateInputs(...)`. Which fields come from the TalonFX and which come from `FlywheelSim`?
2. `velocityControlEnabled` and `goalVelocityRotationsPerSecond` are not sensor measurements. Where are they stored, and why does the IO layer keep them?
3. Why are the fields initialized to `0.0` and `false`?
4. Which of these fields does `Shooter` read for its getters (`getVelocity()`, `getVoltage()`, `getCurrent()`)? Which does it only log?

Do not rename fields without updating every implementation, `Shooter`, and any log-analysis tools that read the key names.

## Task 3: `@AutoLog`

AdvantageKit's annotation processor generates `ShooterIOInputsAutoLogged`, which extends `ShooterIOInputs` and adds the methods that write fields to and read them from the log.

**Tasks:**

1. Find where `ShooterIOInputsAutoLogged` is used in `Shooter.java`.
2. Why should you never write this class by hand?
3. The generated class does not exist until the project is built. Run a build and locate the generated file.
4. Add a field, for example `public double supplyCurrentAmps = 0.0;`, rebuild, and confirm it appears in the log inputs without any other change to `Shooter`.

## Task 4: Default methods

All four methods have empty default bodies.

**Tasks:**

1. How does a default method differ from an abstract method?
2. Why is an empty body a reasonable default for `updateInputs`, `setVelocity`, `setVoltage`, and `stop`? What does `Shooter` see if a test implementation overrides none of them?
3. Which methods does each concrete implementation override? Do `ShooterIOReal` and `ShooterIOSim` override all four?
4. Create a minimal fake implementation inside a test:

   ```java
   class FakeShooterIO implements ShooterIO {
     double lastVoltage;
     @Override public void setVoltage(double volts) { lastVoltage = volts; }
   }
   ```
   Use it to verify that `Shooter.set(0.5)` requests approximately half the battery voltage.

## Task 5: Velocity versus voltage

The interface has two control requests:

- `setVelocity(double rotationsPerSecond)`: closed-loop target. The motor controller runs the PID loop.
- `setVoltage(double volts)`: open-loop voltage. There is no feedback.

**Tasks:**

1. Which unit does each method expect, and where does `Shooter` convert into it?
2. `stop()` is separate from `setVoltage(0.0)`. In both current implementations they do the same thing. When might a real implementation want them to differ, such as neutral coast versus brake?

## Verification checklist

- [ ] The input structure documents the units of each field.
- [ ] `ShooterIO` has no hardware-specific imports.
- [ ] `ShooterIOReal` and `ShooterIOSim` both implement all four methods.
- [ ] `ShooterIOInputsAutoLogged` is generated by the build.
- [ ] A fake implementation can drive `Shooter` in a unit test.

## Reflection questions

1. What problem does hardware abstraction solve?
2. Why does `updateInputs(...)` receive an object instead of returning values?
3. What is the difference between sensor feedback and a control request?
4. What happens if an implementation never updates its input fields?
5. Why does the IO layer use primitive `double` values while `Shooter` exposes unit types?
