class Singleton{
    public:
        static Singleton& getInstance()
        {
            static Singleton instance;
            return instance;
        }
    
    private:
        Singleton()
        {

        }

        // this will not call copy constructor    
        Singleton(Singleton const&) = delete;

        Singleton& operator = (Singleton const&) = delete;

};

int main()
{
    Singleton& instance1 = Singleton::getInstance();
    Singleton& instance2 = Singleton::getInstance();
}