#include <iostream>
using namespace std;

class Command
{
    public:
        virtual void execute() = 0;

    //destructor handle karlena
};

class BreakMechanism{
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

class AirSuspensionCommand : public  Command
{
    private:
        AirMechanism *  airmechanism;

    public:
        AirSuspensionCommand(AirMechanism * mechanism)
        {
            this->airmechanism = mechanism;
        }
};

class BreakCommand : public  Command
{
    private:
        BreakMechanism * mechanism;

    public:
        BreakCommand(BreakMechanism * mechanism)
        {
            this->mechanism = mechanism;
        }
};

class Panel 
{
private:
    Command * commands[5];

    public:

    Panel(){

    }

    void setCommand (int index , Command * cmd )
    {
            
    }
}

int main() {
    
    return 0;
}