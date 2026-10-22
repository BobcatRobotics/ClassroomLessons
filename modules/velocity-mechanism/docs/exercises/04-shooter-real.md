# Exercise 4: Real and Simulated Shooter Hardware

## Objective

Understand how `ShooterIOReal` controls a TalonFX flywheel with Phoenix 6, and how `ShooterIOSim` simulates the same motor so the shooter can be tested without hardware.

You will learn how to:

- Create a TalonFX on a named CAN bus.
- Send `VelocityVoltage` and `VoltageOut` control requests.
- Read Phoenix 6 status signals into the IO input structure.
- Simulate a Kraken X60 flywheel with `FlywheelSim` and `TalonFXSimState`.
- Compare real and simulated implementations of one interface.

## Before you begin

Review:

- `ShooterConstants.java` for `CAN_ID`, `GEAR_RATIO`, `MOMENT_OF_INERTIA_KG_M2`, and `createMotorConfiguration()`.
- `ShooterIO.java` for the interface you are implementing.
- `Shooter.java` for how the IO methods are called.

## Part A: `ShooterIOReal`

### Task 1: Motor and requests

```java
private final TalonFX motor =
    new TalonFX(ShooterConstants.CAN_ID, new CANBus(CANPort.CAN_S0));
private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);
private final VoltageOut voltageRequest = new VoltageOut(0.0);
```

**Tasks:**

1. Which CAN ID and which bus does the motor use? Where is each value defined?
2. Why are the request objects stored as fields instead of created on every call?
3. What is the difference between `VelocityVoltage` and `VoltageOut`?

### Task 2: Constructor

```java
public ShooterIOReal() {
  motor.getConfigurator().apply(ShooterConstants.createMotorConfiguration());
  motor.setPosition(Rotations.of(0.0));
}
```

**Tasks:**

1. What is the difference between constructing a `TalonFX` and applying its configuration?
2. Open `createMotorConfiguration()`. List the settings that matter for this exercise (PID gains, feedforward, `SensorToMechanismRatio`, inversion, neutral mode).
3. Why does the constructor zero the position?

### Task 3: Reading inputs

```java
inputs.positionRotations = motor.getPosition().getValueAsDouble();
inputs.velocityRotationsPerSecond = motor.getVelocity().getValueAsDouble();
inputs.appliedVolts = motor.getMotorVoltage().getValueAsDouble();
inputs.statorCurrentAmps = motor.getStatorCurrent().getValueAsDouble();
inputs.temperatureCelsius = motor.getDeviceTemp().getValueAsDouble();
inputs.velocityControlEnabled = velocityControlEnabled;
inputs.goalVelocityRotationsPerSecond = goalVelocityRotationsPerSecond;
```

**Tasks:**

1. Phoenix 6 position and velocity default to rotations and rotations per second. Why does that match the field names in `ShooterIOInputs`?
2. The last two fields come from private variables, not from the motor. Which methods write them?
3. This implementation reads each signal individually with `getValueAsDouble()`. Phoenix 6 also provides `BaseStatusSignal.refreshAll(...)`. When is it worth refreshing several signals together?
4. What happens to `inputs` if the CAN bus is unplugged?

### Task 4: Control methods

```java
public void setVelocity(double rotationsPerSecond) {
  velocityControlEnabled = true;
  goalVelocityRotationsPerSecond = rotationsPerSecond;
  motor.setControl(velocityRequest.withVelocity(RotationsPerSecond.of(rotationsPerSecond)));
}

public void setVoltage(double volts) {
  velocityControlEnabled = false;
  motor.setControl(voltageRequest.withOutput(Volts.of(volts)));
}

public void stop() {
  velocityControlEnabled = false;
  motor.setControl(voltageRequest.withOutput(Volts.of(0.0)));
}
```

**Tasks:**

1. Why do `setVoltage(...)` and `stop()` set `velocityControlEnabled = false`?
2. `setControl(...)` is sent once by `Shooter.setVelocity(...)` and not every loop. Why does the motor keep running?
3. In `stop()`, why is zero volts requested rather than calling `motor.stopMotor()`? What is the difference in neutral behavior if `NeutralMode` is configured?

## Part B: `ShooterIOSim`

`ShooterIOSim` implements the same four methods and adds physics.

### Task 5: Simulation components

```java
private static final DCMotor DC_MOTOR = DCMotor.getKrakenX60(1);

private final FlywheelSim flywheelSim = new FlywheelSim(
    Models.flywheelFromPhysicalConstants(
        DC_MOTOR, ShooterConstants.MOMENT_OF_INERTIA_KG_M2, ShooterConstants.GEAR_RATIO),
    DC_MOTOR);
```

**Tasks:**

1. A real `TalonFX` object is still created here. What does it simulate, and what does `FlywheelSim` simulate?
2. Why is `setMotorType(MotorType.KrakenX60)` called on the sim state?

### Task 6: The simulation loop in `updateInputs`

Trace one call in order:

1. `setSupplyVoltage(...)` copies the robot battery voltage into the simulated TalonFX.
2. `flywheelSim.setInputVoltage(simState.getMotorVoltageMeasure().in(Volts))` applies the voltage the Talon would output (including the `VelocityVoltage` PID result) to the physics model.
3. `flywheelSim.update(0.020)` advances physics by one 20 ms loop.
4. Position is integrated from angular velocity, and acceleration is computed from the change in velocity.
5. `setRawRotorPosition`, `setRotorVelocity`, and `setRotorAcceleration` feed the result back into the Talon, with the gear ratio applied.
6. The input structure is populated.

**Tasks:**

1. Draw this as a feedback loop between the `TalonFX` sim state and `FlywheelSim`.
2. Why must rotor values be multiplied by `GEAR_RATIO` when fed to the Talon, while `inputs.*` use mechanism values?
3. `LOOP_PERIOD_SECONDS` is hardcoded to 0.020. What would break if the robot loop period changed?
4. Why does `inputs.statorCurrentAmps` come from `motor.getStatorCurrent()` instead of `flywheelSim`?

## Part C: Verify the integration

### In simulation

1. Run the simulator, hold **A**, and plot `Shooter/VelocityRPS` against the goal in AdvantageScope. The velocity should settle near 38.8 rot/s (2330 RPM).
2. Hold **B**. Compare the rise time and current draw to **A**.
3. Hold **X**, then **Y**. Verify the sign of `Shooter/AppliedVolts` flips.
4. Release all buttons and confirm applied volts goes to zero.

### On hardware

Only after simulation behaves correctly:

- [ ] Confirm CAN ID, bus, and motor direction.
- [ ] Secure the robot and clear the flywheel.
- [ ] Test idle first, then X (30% duty cycle), then the velocity bindings.
- [ ] Compare the real and simulated velocity curves. A large difference indicates that the sim constants or PID gains need tuning.

Never treat a successful build as proof that motor direction or gear ratio is correct.

## Verification checklist

- [ ] `ShooterIOReal` implements all four `ShooterIO` methods.
- [ ] `ShooterIOSim` implements all four methods and runs the physics update.
- [ ] Both populate every field of `ShooterIOInputs`.
- [ ] Velocity is reported in rotations per second in both.
- [ ] Velocity-control state and goal are tracked identically in both.
- [ ] The project compiles and passes applicable checks.

## Reflection questions

1. Why is the hardware implementation separate from the mechanism?
2. What is the difference between a sensor measurement and a control request?
3. Why is the same `ShooterIO` interface usable for hardware, simulation, and tests?
4. Why does the sim feed `FlywheelSim` output back into the TalonFX sim state?
5. How could you test that a reversed motor produces the expected sign in `appliedVolts` without risking hardware?

## Optional extension

Add a `Shooter`-level test that uses `ShooterIOSim`. Run 50 loops of `setVelocity(RPM.of(2330))` and assert that `getVelocity()` is within 5% of the goal, using the `near(...)` trigger or a direct check.
