import javax.xml.bind.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;


class verifiedWorker extends Thread
{
    //verified worker threads to handle delivery of new blockchains
    private Socket sock;

    verifiedWorker (Socket s) { // constructor
        this.sock = s;
    }

    public void run() {
        BufferedReader in = null;
        try
        {
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String xmlString;
            StringBuilder sbuilder = new StringBuilder();
            while ((xmlString = in.readLine()) != null)
            {
                sbuilder.append(xmlString);
            }

            //Unmarshall received message to a blockchain
            JAXBContext jaxbContext = JAXBContext.newInstance(Ledger.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(sbuilder.toString());
            Ledger blockRecords = (Ledger) jaxbUnmarshaller.unmarshal(reader);

            //Verifies the blockchains against its contents
            if (blockRecords.VerifyHashAndSignature())
            {
                //Locks the blockchain to prevent no threads to tamper with it when a solved blockchain is going to get added to it
                if(Blockchain.Lock.compareAndSet(true,false))
                {
                    if (Blockchain.sharedVerifiedBlocks.size() < blockRecords.size())
                    {
                        boolean contained = false;
                        //To see if our blockchain records is contained in it while is not tampered
                        if (Blockchain.sharedVerifiedBlocks.VerifiedBlockRecords.size() > 0)
                        {
                            if (Blockchain.sharedVerifiedBlocks.VerifiedBlockRecords.get(0).getBlockID().equals(blockRecords.VerifiedBlockRecords.get(0).getBlockID()))
                            {
                                contained = true;
                            }
                        } else contained = true;

                        if (contained)
                        {
                            //Assign the new blockchain if its larger than the current and it has met the necessary requirements
                            if (blockRecords.contains(Worker.unverifiedBlockRecord))
                            {
                                //If the worker is working on it a block which is already solved stop it so it would start a new work
                                Blockchain.AtomicEligiblity.set(false);
                                Worker.KeepWorking = false;

                                System.out.println("Stop the worker from solving an already puzzle");
                                Blockchain.AppendLog("Stop the worker from solving an already puzzle");

                                //Assign the old and smaller blockchain to the one that is most recent among the peers
                                Blockchain.sharedVerifiedBlocks = blockRecords;
                                System.out.println("Assign the currently old blockchain to process " + blockRecords.getWorkingProcessID() + "s blockchain");
                                Blockchain.AppendLog("Assign the currently old blockchain to process " + blockRecords.getWorkingProcessID() + "s blockchain");

                                Worker.KeepWorking = true;
                                Blockchain.AtomicEligiblity.set(true);
                            } else
                            {
                                //Assign the old and smaller blockchain to the one that is most recent among the peers
                                Blockchain.sharedVerifiedBlocks = blockRecords;
                                System.out.println("Assign the currently old blockchain to process " + blockRecords.getWorkingProcessID() + "s blockchain");
                                Blockchain.AppendLog("Assign the currently old blockchain to process " + blockRecords.getWorkingProcessID() + "s blockchain");
                            }

                            //Saves the blockchain to BlockchainLedger.xml
                            if (Blockchain.processID == 0) Blockchain.WriteBlockChain();

                            //Check to see if all the received unverified blocks have been solved
                            if (Blockchain.sharedVerifiedBlocks.size() == Blockchain.TotalReceivedUnverifiedRecords)
                            {
                                //Check to see if the user interface has to be restarted
                                if (Blockchain.UserInterface.InputGiven)
                                {
                                    //Starts the user interface from beginning
                                    Blockchain.UserInterface = new UserInterface();
                                    Blockchain.UserInterface.start();
                                }
                            }
                        }
                    }
                    Blockchain.Lock.set(true);
                }
            }
            sock.close();
        }
        catch (Exception x)
        {
            System.out.println("Verified Server read error");
            x.printStackTrace();
        }
    }
}

public class VerifiedBlockServer implements Runnable
{
    //new thread to handle verified ports
    private int BlockChainPort;

    VerifiedBlockServer(int BlockChainPort)
    {
        this.BlockChainPort = BlockChainPort;
    }

    public void run(){
        System.out.println("Process " + Blockchain.processID + " is listening at Port: " + BlockChainPort+" for new blockchains");
        Blockchain.AppendLog("Process " + Blockchain.processID + " is listening at Port: " + BlockChainPort+" for new blockchains");
        int q_len = 6;
        Socket sock;
        try{
            ServerSocket servsock = new ServerSocket(BlockChainPort, q_len);
            while (true) {
                sock = servsock.accept(); //accept connections from peers
                new verifiedWorker(sock).start();
            }
        }catch (IOException ioe) {System.out.println(ioe);}
    }
}
