import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;


class PublicKeyWorker extends Thread
{
    //worker threads to handle all the work needs to be done
    private Socket sock;

    PublicKeyWorker (Socket s) { // constructor
        this.sock = s;
    }

    public void run()
    {
        BufferedReader in;
        try
        {
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String fromProceses;
            fromProceses = in.readLine();
            String[] words = fromProceses.split(" ");
            byte[] publickeydecoded = Base64.getDecoder().decode(words[1]);
            KeyFactory keyf = KeyFactory.getInstance("RSA"); //decode the public key
            Blockchain.publicKeyChain.put(Integer.parseInt(words[0]), keyf.generatePublic(new X509EncodedKeySpec(publickeydecoded))); //add the publick key to the keychain
            System.out.println("Process " + Blockchain.processID + " is received public key from process " + words[0]);
            Blockchain.AppendLog("Process " + Blockchain.processID + " is received public key from process " + words[0]);
            sock.close();
        }
        catch (Exception e)
        {
            System.out.println("Extracting public key error");
            System.out.println(e);
        }
    }
}


public class PublicKeyServer implements Runnable
{
    //new thread to handle public key ports
    private int PublicKeyPort;

    PublicKeyServer(int PublicKeyPort)
    {
        this.PublicKeyPort = PublicKeyPort;
    }

    public void run()
    {
        System.out.println("Process " + Blockchain.processID + " is listening at Port: " + PublicKeyPort+" for public keys");
        Blockchain.WriteLog("Process " + Blockchain.processID + " is listening at Port: " + PublicKeyPort+" for public keys");
        int q_len = 6;
        Socket sock;
        try{
            ServerSocket servsock = new ServerSocket(PublicKeyPort, q_len);
            while (true) {
                sock = servsock.accept(); //accept connection from client admin
                new PublicKeyWorker(sock).start();
            }
        }
        catch (IOException ioe)
        {
            System.out.println(ioe);
        }
    }
}
