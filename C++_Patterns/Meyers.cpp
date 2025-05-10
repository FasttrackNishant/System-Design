#include <iostream>
using namespace std;

class SingleTon
{
public:
    static SingleTon &getInstance(){
        static SingleTon instance; // gurantee to be destroyed and instantiated
        return instance;
        
    }
    private:
    SingleTon()
    {
        cout << "Creating  object" << endl;
    }

    SingleTon (SingleTon const&) = delete; //copy constructor deleted 
    SingleTon &operator=(SingleTon const &) = delete; // equal operator deleted
};

int main()
{
    SingleTon& singleton1 = SingleTon::getInstance();
    cout<< &singleton1<<endl;
    SingleTon& singleton2 = SingleTon::getInstance();

    return 0;
}