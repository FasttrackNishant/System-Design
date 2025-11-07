
public  class  Task implements  Runnable
{
    private  String TaskName;

    public Task(String taskName)
    {this.TaskName = taskName;}

    @Override
    public  void run()
    {
        for(int i = 0  ;i < 5 ; i++)
        {
            System.out.println(TaskName + " - Step " + i);

            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException ex)
            {
                System.out.println(TaskName + "interpted");
            }
        }
    }
}