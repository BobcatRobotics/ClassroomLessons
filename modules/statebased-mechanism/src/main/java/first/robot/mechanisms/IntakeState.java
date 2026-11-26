package first.robot.mechanisms;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.RPM;

import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;

import first.robot.utilities.SubsystemState;

public enum IntakeState implements SubsystemState{
    IDLE(0.0, 0),
    FORWARDDUTYCYCLE(0.3, 0.0),
    FORWARD(RPM.of(2330.0), Degrees.of(45.0)),
    REVERSEDUTYCYCLE(-0.3, 0),
    REVERSE(RPM.of(-2330), Degrees.of(0));
    
    private final double rollerSpeed;
    private final double position;

    IntakeState(AngularVelocity rollerSpeed, Angle position) {
    }

    IntakeState(double rollerSpeed, Angle position) {
    }

    IntakeState(double rollerSpeed, double position) {
    }
    public double getPosition() {
        return position;
    }

    public double getRollerSpeed() {
        return rollerSpeed;
    }
}
