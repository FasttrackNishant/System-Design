#include <iostream>
using namespace std;

/*
 Single Responsibility Principle (SRP) states that a class should have only one reason to change.
 In this example, the UserInfo class is responsible for getting user information,
    while the logger class is responsible for logging errors.

This adheres to the SRP as each class has a single responsibility.

    


*/

class UserInfo
{
    public : 
    void getUserInfo() {
        cout << "Getting user information..." << endl;
    }

    // This Should be in a separate class to adhere to SRP
    // void logError() {
    //     cout << "Logging error..." << endl;
    // }
};

class logger
{
    public:
    void logError() {
        cout << "Logging error..." << endl;
    }
};

int main() {
    
    return 0;
}