import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.*;


public class UserInterface extends Thread
{
    boolean loop=true;
    public Boolean InputGiven=false; // Checks if the user interface has to be restarted

    public void run()
    {
        try {
            while (loop)
            {
                System.out.println("\nEnter a command:" +
                        "\nC) Shows a tally of which process has verified each block." +
                        "\nR <filename>) Reads a file of records to create new data." +
                        "\nV hcs) Verifies each blocks hash code with their previous inthe blockchain and report errors if any." +
                        "\nV id) Verifies each block's ID with its signature and report errors if any." +
                        "\nV hc) Verifies each block's hash code with its signature and report errors if any." +
                        "\nL) Shows the records in the blockchain." +
                        "\nquit) Quits the application.");

                //Creates a task to read from the input
                final FutureTask<String> readLine = new FutureTask<String>(
                        new Callable<String>()
                        {
                            @Override
                            public String call() throws Exception
                            {
                                InputStreamReader isr = new InputStreamReader(System.in);
                                BufferedReader br = new BufferedReader(isr);
                                return br.readLine();
                            }
                        }
                );

                //Adds the task to the threadpools queue
                Executor executor = Executors.newCachedThreadPool();
                executor.execute(readLine);

                //Reserves a thread to execute it
                Thread MainThread = new Thread(() ->
                {
                    try
                    {
                        String input = readLine.get();
                        if (input != null)
                        {
                            String[] tokens = input.split(" ");

                            //checks the first word in the instruction
                            switch (tokens[0])
                            {
                                case "C":
                                {
                                    HashMap<Integer, Integer> table = new HashMap<Integer, Integer>(); // hashmap to stire block IDs and their number of solved puzzles
                                    for (BlockRecord b : Blockchain.sharedVerifiedBlocks.VerifiedBlockRecords)
                                    {
                                        //Iterates through the blockchain to sea each block is solved by who
                                        int ID = b.getVerificationProcessID();
                                        if (table.containsKey(ID))
                                        {
                                            int s = table.get(ID);
                                            table.put(ID, s + 1);
                                        } else
                                        {
                                            table.put(ID, 1);
                                        }
                                    }
                                    //Prints the result
                                    Iterator it = table.entrySet().iterator();
                                    while (it.hasNext())
                                    {
                                        Map.Entry pair = (Map.Entry) it.next();
                                        System.out.println("\tProcess " + pair.getKey() + " verified " + pair.getValue() + " blocks");
                                        it.remove();
                                    }
                                    break;
                                }
                                case "R":
                                {
                                    loop = false;
                                    //Reads a file name containing medical information to add them to the blockchain
                                    Blockchain.readSetandSendInputfile(tokens[1]);
                                    if (Blockchain.processID == 0) Blockchain.WriteBlockChain();
                                    break;
                                }
                                case "V":
                                {
                                    //checks the second word in the instruction
                                    switch (tokens[1])
                                    {
                                        case "hcs":
                                        {
                                            boolean b= Blockchain.sharedVerifiedBlocks.VerifyHashCodes();
                                            System.out.println("Hash Code verification result: "+b);
                                            break;
                                        }
                                        case "id":
                                        {
                                            boolean b= Blockchain.sharedVerifiedBlocks.VerifyBlockIDSignature();
                                            System.out.println("Block ID signature verification result: "+b);
                                            break;
                                        }
                                        case "hc":
                                        {
                                            boolean b= Blockchain.sharedVerifiedBlocks.VerifyHashSignature();
                                            System.out.println("Hash Code signature verification result: "+b);
                                            break;
                                        }
                                    }
                                    break;
                                }
                                case "L":
                                {
                                    for (Object o : Blockchain.sharedVerifiedBlocks.VerifiedBlockRecords)
                                    {
                                        //Iterates the blockchain and prints the information contained in each block
                                        BlockRecord b = (BlockRecord) o;
                                        System.out.println(b.getBlockNumber() + ". " + b.getCreationTimeStamp() + " " + b.getFFname() + " " + b.getFLname() + " " + b.getFDOB() + " " + b.getFSSNum() + " " + b.getDiagnosis() + " " + b.getTreatment() + " " + b.getMedicine());
                                    }
                                    break;
                                }
                                case "quit":
                                {
                                    loop = false; // to prevent the while loop from repeating
                                    System.out.println("Blockchain quits.");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e)
                    {
                        throw new RuntimeException("InterruptedException caught in lambda", e);
                    }
                });
                //It stars the reading thread
                MainThread.start();
                //It waits till it finishes with its loop
                MainThread.join();
            }
        }
        catch (Exception e)
        {

        }
    }
}
