import javax.xml.bind.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class unverifiedWorker extends Thread
{
    //unverified worker thread that handles the delivery of unverified blocks
    private Socket sock;

    unverifiedWorker (Socket s) { // constructor
        this.sock = s;
    }

    public void run() {
        BufferedReader in;
        try
        {
            Blockchain.UserInterface.InputGiven=true;
            Blockchain.TotalReceivedUnverifiedRecords++;
            //Gets the whole block of data from the socket
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String xmlString;
            StringBuilder sbuilder = new StringBuilder();
            while ((xmlString = in.readLine()) != null)
            {
                sbuilder.append(xmlString);
            }

            //Unmarshall received message to an unverified blockrecord
            JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(sbuilder.toString());
            BlockRecord blockRecord = (BlockRecord) jaxbUnmarshaller.unmarshal(reader);
            //Adds the unverified blockrecord in the list of unverified block queue
            Blockchain.sharedUnverifiedblocks.add(blockRecord);
            sock.close();
        }
        catch (Exception e)
        {
            System.out.println("Extracting unverified block error");
            e.printStackTrace();
        }
    }
}


public class UnverifiedBlockServer implements Runnable
{
    //new thread to handle unverified ports
    private int UnverifiedBlockPort;

    UnverifiedBlockServer(int UnverifiedBlockPort)
    {
        this.UnverifiedBlockPort = UnverifiedBlockPort;
    }

    public void run()
    {
        System.out.println("Process " + Blockchain.processID + " is listening at Port: " + UnverifiedBlockPort+" for unverified blocks");
        Blockchain.AppendLog("Process " + Blockchain.processID + " is listening at Port: " + UnverifiedBlockPort+" for unverified blocks");
        int q_len = 6;
        Socket sock;
        try{
            ServerSocket servsock = new ServerSocket(UnverifiedBlockPort, q_len);
            while (true) {
                sock = servsock.accept(); //accept connections from peers
                new unverifiedWorker(sock).start();
            }
        }
        catch (IOException ioe)
        {
            System.out.println(ioe);
        }
    }
}
