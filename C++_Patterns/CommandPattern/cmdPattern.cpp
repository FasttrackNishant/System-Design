#include <iostream>
using namespace std;

class Command
{
public:
    virtual void execute() = 0;

    // destructor handle karlena
};

class BreakMechanism
{
public:
    void applyBreak()
    {
        cout << "Applying breaks" << endl;
    }
};

class AirMechanism
{
public:
    void lifeSuspension()
    {
        cout << "lift suspension" << endl;
    }
};

class AirSuspensionCommand : public Command
{
private:
    AirMechanism *airmechanism;

public:
    AirSuspensionCommand(AirMechanism *mechanism)
    {
        this->airmechanism = mechanism;
    }

    void execute()
    {
        this->airmechanism->lifeSuspension();
    }
};

class BreakCommand : public Command
{
private:
    BreakMechanism *mechanism;

public:
    BreakCommand(BreakMechanism *mechanism)
    {
        this->mechanism = mechanism;
    }

    void execute()
    {
        mechanism->applyBreak();
    }
};

class Panel
{
private:
    Command *commands[5];

public:
    Panel()
    {
    }

    void setCommand(int index, Command *cmd)
    {
        commands[index] = cmd;
    }

    void liftSuspension()
    {
        commands[0]->execute();
    }

    void applyBreaks()
    {
        commands[1]->execute();
    }
};

int main()
{

    BreakMechanism *breakMechanism = new BreakMechanism();
    AirMechanism *airMechanism = new AirMechanism();

    BreakCommand *breakCmd = new BreakCommand(breakMechanism);
    AirSuspensionCommand *airCmd = new AirSuspensionCommand(airMechanism);

    Panel panel;

    panel.setCommand(0,airCmd);
    panel.setCommand(1,breakCmd);

    panel.applyBreaks();
    panel.liftSuspension();
    
    return 0;
}