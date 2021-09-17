import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Date;
import javax.xml.bind.*;

public class Worker implements Runnable
{
    public void run()
    {
            doWork();
    }

    public static BlockRecord unverifiedBlockRecord=null;
    public static boolean KeepWorking=true;
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private void doWork()
    {
        //solve the puzzle for each waiting unverified block
        while (true)
        {
            try
            {
                if (Blockchain.AtomicEligiblity.compareAndSet(true, true))
                {
                    KeepWorking = true;
                    unverifiedBlockRecord = Blockchain.sharedUnverifiedblocks.take();
                    if (Blockchain.sharedVerifiedBlocks.contains(unverifiedBlockRecord))
                    {
                        continue;
                    }
                    if (!KeepWorking)
                    {
                        //if the puzzle is already solved
                        continue;
                    }
                    PublicKey PK = Blockchain.publicKeyChain.get(unverifiedBlockRecord.getCreatingProcessID());
                    if (PK != null)
                    {
                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }

                        //get the last hash from the verified queue
                        BlockRecord lastVerifiedBlock = Blockchain.sharedVerifiedBlocks.GetLastverifiedBlock();
                        //make a dummy hash if the block is the first
                        String preivousHash = "";
                        if (lastVerifiedBlock != null)
                        {
                            preivousHash = lastVerifiedBlock.getSHA256HASH();

                        } else
                        {
                            preivousHash = generateDummyHash("");
                        }

                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }

                        unverifiedBlockRecord.setVerificationProcessID(Blockchain.processID);
                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }
                        String newHashCodeString = "";
                        int workNumber = 0;
                        String randString;
                        String hash = "";
                        //this is what the puzzle is
                        for (int i = 1; i < 500000000; i++)
                        {
                            //check to see if the puzzle is already solved
                            if (KeepWorking)
                            {
                                unverifiedBlockRecord.setVerifiedTimeStamp(new Date());
                                randString = randomAlphaNumeric(8);
                                unverifiedBlockRecord.setSeed(randString);
                                hash = GenerateHash(preivousHash + unverifiedBlockRecord.GenerateStringContent());
                                workNumber = Integer.parseInt(hash.substring(0, 6), 32);
                                if (workNumber < 10)
                                {
                                    break;
                                }
                            } else
                            {
                                break;
                            }
                        }
                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }
                        unverifiedBlockRecord.setSHA256HASH(hash);
                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }
                        unverifiedBlockRecord.SignSHA256Hash(Blockchain.privateKey);

                        if (!KeepWorking)
                        {
                            //if the puzzle is already solved
                            continue;
                        }

                        //Locks the blockchain to prevent no threads to tamper with it when a solved blockchain is going to get added to it
                        if(Blockchain.Lock.compareAndSet(true,false))
                        {
                            if (KeepWorking)
                            {
                                if (!Blockchain.sharedVerifiedBlocks.contains(unverifiedBlockRecord))
                                {
                                    //Assignes a clock number one more that the last block number added to the blockchain
                                    unverifiedBlockRecord.setBlockNumber(Blockchain.sharedVerifiedBlocks.size() + 1);
                                    Blockchain.sharedVerifiedBlocks.add(unverifiedBlockRecord);
                                    MulticastBlockChain(); //multicast the blockchain
                                    System.out.println("\tProcess " + Blockchain.processID + " solves a puzzle and adds the new verified blockrecord to its ledger with block number of " + unverifiedBlockRecord.getBlockNumber());
                                    Blockchain.AppendLog("\tProcess " + Blockchain.processID + " solves a puzzle and adds the new verified blockrecord to its ledger with block number of " + unverifiedBlockRecord.getBlockNumber());

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
                    unverifiedBlockRecord = null;
                }
            } catch (InterruptedException e)
            {
            } catch (Exception e)
            {
                System.out.println("Solving the puzzle error");
                e.printStackTrace();
            }
        }
    }

    public String GenerateHash(String t)
    {
        //Generates Sha256 hash code string
        MessageDigest md = null;
        String stringOut = "";
        try
        {
            md = MessageDigest.getInstance("SHA-256");

            byte byteData[] = md.digest(t.getBytes("UTF-8"));
            stringOut=DatatypeConverter.printHexBinary(byteData);
        }
        catch(Exception e)
        {
            System.out.println("Generating Sha256hash error");
            e.printStackTrace();
        }
        return stringOut;
    }

    public static String randomAlphaNumeric(int count)
    {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0)
        {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    private String generateDummyHash(String x)
    {
        //Generates Sha256 hash code string
        MessageDigest md = null;
        String stringOut = "";
        try
        {
            md = MessageDigest.getInstance("SHA-256");
            byte byteData[] = md.digest(x.getBytes("UTF-8"));
            stringOut = DatatypeConverter.printHexBinary(byteData);
            return stringOut;
        }
        catch (NoSuchAlgorithmException e)
        {
            System.out.println("Generating Sha256hash error");
            e.printStackTrace();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return stringOut.toString();
    }

    private static void MulticastBlockChain()
    {
        //Multicast a string to verifying ports
        Socket sock;
        PrintStream toServer;
        String serverName = "localhost";
        int[] verifiedPorts = {4930, 4931,4932};
        try
        {
            for (int i = 0; i < verifiedPorts.length; i++)
            {
                sock = new Socket(serverName, verifiedPorts[i]);
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println(Blockchain.sharedVerifiedBlocks.toXML());
                toServer.flush();
                sock.close();
            }
        }
        catch (IOException x)
        {
            System.out.println("Multcasting socket error.");
            x.printStackTrace();
        }
    }
}
