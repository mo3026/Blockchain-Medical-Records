import javax.xml.bind.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Blockchain
{
    public static HashMap<Integer,PublicKey> publicKeyChain = new HashMap<Integer, PublicKey>(); // list of processes with their keys
    public static PriorityBlockingQueue<BlockRecord> sharedUnverifiedblocks  = new PriorityBlockingQueue<BlockRecord>(); // list of the unverified blocks
    public static Ledger sharedVerifiedBlocks; //list of the verified blocks
    public static int processID=0;

    public static PrivateKey privateKey; // private key of the process
    public static PublicKey publicKey; // public key of the this process

    public static Thread WorkingThread=new Thread(new Worker());

    public static int TotalReceivedUnverifiedRecords=0; // total Received UnverifiedRecords;

    public static UserInterface UserInterface;

    public static AtomicBoolean AtomicEligiblity=new AtomicBoolean(true);

    public static AtomicBoolean Lock = new AtomicBoolean(true); // to lock the blockchain from unappropriate changes

    public static void main(String[] args)
    {
        int publicKeyPort = 0; // public Key port
        int UnverifiedBlockPort = 0; // unverifiedblock port
        int verifiedBlockPort = 0;// verifiedblock port

        if (args.length > 0)
        {
            processID =Integer.parseInt( args[0]);
        }

        sharedVerifiedBlocks = new Ledger(processID);

        publicKeyPort = 4710 + processID;
        UnverifiedBlockPort = 4820 + processID;
        verifiedBlockPort = 4930 + processID;


        createAndSetKeys();
        PublicKeyServer pl = new PublicKeyServer(publicKeyPort);
        //Starting new thread to handle public key ports,
        Thread p = new Thread(pl);
        p.start();
        MulticastPK();
        System.out.println("Keypair is generated and sent to all pairs");
        AppendLog("Keypair is generated and sent to all pairs");

        UnverifiedBlockServer ul = new UnverifiedBlockServer(UnverifiedBlockPort);
        //Starting new thread to handle unverfied block ports,
        Thread u = new Thread(ul);
        u.start();

        UserInterface = new UserInterface();
        UserInterface.InputGiven=true;

        VerifiedBlockServer vl = new VerifiedBlockServer(verifiedBlockPort);
        //Starting new thread to handle verified ports
        Thread v = new Thread(vl);
        v.start();

        readSetandSendInputfile();
        WorkingThread.start(); //start consuming the queued up unverified blocks and solving each puzzle
    }

    //This is for creating the log file
    static void WriteLog(String statement)
    {
        if(processID==0)
        {
            try (FileWriter fw = new FileWriter("BlockchainLog.txt"))
            {
                fw.write(statement + "\n");
                fw.flush();
                fw.close();
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    //This is for appending to the log file
    public static void AppendLog(String statement)
    {
        if(processID==0)
        {
            try(FileWriter fw = new FileWriter("BlockchainLog.txt", true))
            {
                fw.write(statement + "\n");
                fw.flush();
                fw.close();
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    //Marshal the blockchain to xml to be written in file
    public static void WriteBlockChain()
    {
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(Ledger.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            StringWriter sw = new StringWriter();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(sharedVerifiedBlocks, sw);
            String fullblock = sw.toString(); //multicast the string to every peer
            WriteString(fullblock);
        }
        catch (JAXBException e)
        {
            System.out.println("Multicastint blockchain error");
            e.printStackTrace();
        }
    }

    //This is for creating the xml file
    static void WriteString(String statement)
    {
        try(FileWriter fw=new FileWriter("BlockchainLedger.xml"))
        {
            fw.write(statement+"\n");
            fw.flush();
            fw.close();
        }catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    private static void createAndSetKeys()
    {
        //create the public and private keys
        KeyPair keyPair = null;
        try {
            keyPair = generateKeyPair(999); //use the key pair generator to create the keys
        }
        catch (Exception e)
        {
            System.out.println("Error generating feypair");
            e.printStackTrace();
        }
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
    }

    static KeyPair generateKeyPair(long seed) throws Exception
    {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        rng.setSeed(seed);
        keyGenerator.initialize(1024, rng);
        return (keyGenerator.generateKeyPair());
    }

    private static void MulticastPK()
    {
        //Multicast the public key
        byte[] pkBytes = publicKey.getEncoded(); // encode and turn it to string
        String publicKeyString = Base64.getEncoder().encodeToString(pkBytes);
        MultiCastStringToPublicKeyPorts(processID+" "+publicKeyString);
    }

    private static void MultiCastStringToPublicKeyPorts(String s)
    {
        //Multicast all strings to public key ports
        Socket sock;
        PrintStream toServer;
        String serverName = "localhost";
        int[] unverifiedPorts = {4710, 4711, 4712};
        try {
                for (int i = 0; i < unverifiedPorts.length; i++){
                    sock = new Socket(serverName, unverifiedPorts[i]);
                    toServer = new PrintStream(sock.getOutputStream());
                    toServer.println(s);
                    toServer.flush();
                    sock.close();
                }
        }
        catch (IOException x)
        {
            System.out.println("Error in multicating");
            x.printStackTrace();
        }
    }

    private static void readSetandSendInputfile()
    {
        //Read the file and multicast the xml string to all processes
        String FILENAME;
        switch(processID){
            case 1: FILENAME = "BlockInput1.txt"; break;
            case 2: FILENAME = "BlockInput2.txt"; break;
            default: FILENAME= "BlockInput0.txt"; break;
        }
        readSetandSendInputfile(FILENAME);
    }

    public static void readSetandSendInputfile(String FILENAME)
    {
        //Read the file and multicast the xml string to all processes
        System.out.println("process " + processID + " is reading file: " + FILENAME);
        try {
            BufferedReader br = new BufferedReader(new FileReader(FILENAME));
            String InputLineStr;
            while ((InputLineStr = br.readLine()) != null) {
                BlockRecord blockRecord=new BlockRecord();

                blockRecord.setCreatingProcessID(processID);

                //Create block ID
                UUID idA = UUID.randomUUID();
                blockRecord.setBlockID(idA);
                blockRecord.SignBlockID(privateKey);

                String[] tokens = InputLineStr.split(" "); //tokenize the input

                //Put the necessary info into the block
                blockRecord.setFFname(tokens[0]);
                blockRecord.setFLname(tokens[1]);
                blockRecord.setFDOB(tokens[2]);
                blockRecord.setFSSNum(tokens[3]);
                blockRecord.setDiagnosis(tokens[4]);
                blockRecord.setTreatment(tokens[5]);
                blockRecord.setMedicine(tokens[6]);

                blockRecord.setCreationTimeStamp(new Date());

                MultiCastStringToUnverifiedPorts(blockRecord.toXML()); //multicast the string to everyone
            }
        }
        catch (Exception e)
        {
            System.out.println("Error in reading the file");
            e.printStackTrace();
        }
    }

    private static void MultiCastStringToUnverifiedPorts(String s)
    {
        //Multicast a string to unverified ports
        Socket sock;
        PrintStream toServer;
        String serverName = "localhost";
        int[] unverifiedPorts = {4820, 4821, 4822};
        try {
            for (int i = 0; i < unverifiedPorts.length; i++){
                sock = new Socket(serverName, unverifiedPorts[i]);
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println(s);
                toServer.flush();
                sock.close();
            }
        }
        catch (IOException x)
        {
            System.out.println("Error in multicasting");
            x.printStackTrace();
        }
    }
}
