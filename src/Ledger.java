import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
class Ledger implements Comparable<Ledger>
{
    @XmlElement(name = "CreatingProcessID")
    private int WorkingProcessID;

    public int getWorkingProcessID() {return WorkingProcessID;}

    public void setWorkingProcessID(int BID){this.WorkingProcessID = BID;}

    @XmlElement(name = "VerifiedBlockRecord", type = BlockRecord.class)
    public List<BlockRecord> VerifiedBlockRecords = new ArrayList<BlockRecord>();

    public Ledger()
    {
    }

    public Ledger(int WorkingProcessID)
    {
        this.WorkingProcessID = WorkingProcessID;
    }

    public Ledger(List<BlockRecord> VerifiedBlockRecords) {
        this.VerifiedBlockRecords = VerifiedBlockRecords;
    }

    public List<BlockRecord> getVerifiedBlockRecords() {
        return VerifiedBlockRecords;
    }

    public void setBooks(List<BlockRecord> VerifiedBlockRecords) {
        this.VerifiedBlockRecords = VerifiedBlockRecords;
    }

    public int size()
    {
        return VerifiedBlockRecords.size();
    }

    public void add(BlockRecord VerifiedBlockRecord) {
        this.VerifiedBlockRecords.add(VerifiedBlockRecord);
    }

    public BlockRecord GetLastverifiedBlock()
    {
        if(Blockchain.sharedVerifiedBlocks.size()>0)
        {
            return (BlockRecord)VerifiedBlockRecords.toArray()[Blockchain.sharedVerifiedBlocks.size()-1];
        }
        else return null;
    }

    public int compareTo(Ledger another)
    {
        if (VerifiedBlockRecords == null)
        {
            return -1;
        }
        if (another == null)
        {
            return 1;
        }
        return Integer.compare( VerifiedBlockRecords.size(),another.size());
    }

    public boolean contains(BlockRecord v)
    {
        return VerifiedBlockRecords.contains(v);
    }

    public String toXML()
    {
        //Return the XML format of the blochckain
        StringWriter sw = new StringWriter();
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(Ledger.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(this, sw);
        }
        catch (Exception e)
        {
            System.out.println("Error in creating xml");
            e.printStackTrace();
        }
        return sw.toString();
    }

    public boolean VerifyHashAndSignature()
    {
        //Verifies each blocks hash code signature and to see if its hashcode matches to its previous block's hash code
        boolean OK=false;
        BlockRecord previousBlock=null;
        for (BlockRecord verifiedBlock:VerifiedBlockRecords)
        {
            PublicKey PK = Blockchain.publicKeyChain.get(verifiedBlock.getVerificationProcessID());
            if (PK != null)
            {
                if (verifiedBlock.VerifySignedSHA256Hash(PK))
                {
                    if (verifiedBlock.VerifySHA256Hash(previousBlock))
                    {
                        OK = true;
                    }
                }
            }
            previousBlock=verifiedBlock;
        }
        return OK;
    }

    public boolean VerifyHashCodes()
    {
        //Verifies each blocks hash code to see if it matches to its previous block's hash code
        boolean OK=true;
        BlockRecord previousBlock=null;
        for (BlockRecord verifiedBlock:VerifiedBlockRecords)
        {
            if (!verifiedBlock.VerifySHA256Hash(previousBlock))
            {
                OK = false;
            }
            previousBlock=verifiedBlock;
        }
        return OK;
    }

    public boolean VerifyHashSignature()
    {
        //Verifies each blocks hash code signature
        boolean OK=true;
        for (BlockRecord verifiedBlock:VerifiedBlockRecords)
        {
            PublicKey PK = Blockchain.publicKeyChain.get(verifiedBlock.getVerificationProcessID());
            if (PK != null)
            {
                if (!verifiedBlock.VerifySignedSHA256Hash(PK))
                {
                        OK = false;
                }
            }
        }
        return OK;
    }

    public boolean VerifyBlockIDSignature()
    {
        //Verifies each blocks block ID
        boolean OK=true;
        for (BlockRecord verifiedBlock:VerifiedBlockRecords)
        {
            PublicKey PK = Blockchain.publicKeyChain.get(verifiedBlock.getVerificationProcessID());
            if (PK != null)
            {
                if (!verifiedBlock.VerifySignedBlockID(PK))
                {
                    OK = false;
                }
            }
        }
        return OK;
    }
}
