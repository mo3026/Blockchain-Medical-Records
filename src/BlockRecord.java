import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.security.*;
import java.util.*;

@XmlRootElement
class BlockRecord implements Comparable<BlockRecord>
{
    private String Fname;
    private String Lname;
    private String DOB;
    private String SSNum;
    private String Diagnosis;
    private String Treatment;
    private String Medicine;


    public String getFFname()
    {
        return Fname;
    }

    @XmlElement
    public void setFFname(String FN)
    {
        this.Fname = FN;
    }

    public String getFLname()
    {
        return Lname;
    }

    @XmlElement
    public void setFLname(String LN)
    {
        this.Lname = LN;
    }

    public String getFDOB()
    {
        return DOB;
    }

    @XmlElement
    public void setFDOB(String DOB)
    {
        this.DOB = DOB;
    }

    public String getFSSNum()
    {
        return SSNum;
    }

    @XmlElement
    public void setFSSNum(String SS)
    {
        this.SSNum = SS;
    }

    public String getDiagnosis()
    {
        return Diagnosis;
    }

    @XmlElement
    public void setDiagnosis(String D)
    {
        this.Diagnosis = D;
    }

    public String getTreatment()
    {
        return Treatment;
    }

    @XmlElement
    public void setTreatment(String D)
    {
        this.Treatment = D;
    }

    public String getMedicine()
    {
        return Medicine;
    }

    @XmlElement
    public void setMedicine(String D)
    {
        this.Medicine = D;
    }


    private UUID BlockID;
    private String SignedBlockID;
    private int CreatingProcessID;
    private Date CreationTimeStamp;

    private String PreviousHash;
    public String SHA256Hash;
    private String SignedSHA256Hash;
    private int VerificationProcessID;
    public Date VerificationTimeStamp;
    private String Seed;
    private int BlockNumber;


    public UUID getBlockID()
    {
        return BlockID;
    }

    @XmlElement
    public void setBlockID(UUID BID)
    {
        this.BlockID = BID;
    }

    public String getSignedBlockID()
    {
        return SignedBlockID;
    }

    @XmlElement
    public void setSignedBlockID(String signed)
    {
        this.SignedBlockID = signed;
    }

    public int getCreatingProcessID()
    {
        return CreatingProcessID;
    }

    @XmlElement
    public void setCreatingProcessID(int CP)
    {
        this.CreatingProcessID = CP;
    }


    public Date getCreationTimeStamp()
    {
        return CreationTimeStamp;
    }

    @XmlElement
    public void setCreationTimeStamp(Date timeStamp)
    {
        this.CreationTimeStamp = timeStamp;
    }

    public int getVerificationProcessID()
    {
        return VerificationProcessID;
    }

    @XmlElement
    public void setVerificationProcessID(int VID)
    {
        this.VerificationProcessID = VID;
    }

    public Date getVerifiedTimeStamp()
    {
        return VerificationTimeStamp;
    }

    @XmlElement
    public void setVerifiedTimeStamp(Date t)
    {
        this.VerificationTimeStamp = t;
    }


    public String getPreviousHash()
    {
        return PreviousHash;
    }

    @XmlElement
    public void setPreviousHash(String h)
    {
        this.PreviousHash = h;
    }

    public String getSHA256HASH()
    {
        return SHA256Hash;
    }

    @XmlElement
    public void setSHA256HASH(String SH)
    {
        this.SHA256Hash = SH;
    }

    public String getSignedSHA256Hash()
    {
        return SignedSHA256Hash;
    }

    @XmlElement
    public void setSignedSHA256Hash(String signed)
    {
        this.SignedSHA256Hash = signed;
    }

    public String getSeed()
    {
        return Seed;
    }

    @XmlElement
    public void setSeed(String s)
    {
        this.Seed = s;
    }

    public int getBlockNumber()
    {
        return BlockNumber;
    }

    @XmlElement
    public void setBlockNumber(int BlockNumber)
    {
        this.BlockNumber = BlockNumber;
    }


    @Override
    public int hashCode()
    {
        return BlockID.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        BlockRecord other = (BlockRecord) obj;
        if (BlockID.equals(other.BlockID))
            return true;
        return false;
    }

    @Override
    public int compareTo(BlockRecord another)
    {
        //It compares each block when they are contained in a sorted list
        if (CreationTimeStamp == null)
        {
            return -1;
        }
        if (another == null)
        {
            return 1;
        }
        return this.CreationTimeStamp.compareTo(another.CreationTimeStamp);
    }

    public void SignBlockID(PrivateKey key)
    {
        //Signs the block ID
        try
        {
            byte[] signedBlockID = signData(BlockID.toString().getBytes(), key);
            SignedBlockID = Base64.getEncoder().encodeToString(signedBlockID);
        }
        catch (Exception e)
        {
            System.out.println("Error in creating signature");
            e.printStackTrace();
        }
    }

    public void SignSHA256Hash(PrivateKey key)
    {
        try
        {
            byte[] signedHashCode = signData(SHA256Hash.getBytes(), Blockchain.privateKey);
            SignedSHA256Hash = Base64.getEncoder().encodeToString(signedHashCode);
        }
        catch (Exception e)
        {
            System.out.println("Error in creating signature");
            e.printStackTrace();
        }
    }

    byte[] signData(byte[] data, PrivateKey key) throws Exception
    {
        //Signs the data
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(key);
        signer.update(data);
        return (signer.sign());
    }

    public boolean VerifySignedBlockID(PublicKey key)
    {
        //Verifies the block id signature using public key with block id
        byte[] idSignature = Base64.getDecoder().decode(SignedBlockID);
        try
        {
            if (verifySig(BlockID.toString().getBytes(), key, idSignature))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            System.out.println("Verification error");
            e.printStackTrace();
        }
        return false;
    }

    public boolean VerifySignedSHA256Hash(PublicKey key)
    {
        //Verifies the hash code signature using public key with the verification process
        byte[] idSignature = Base64.getDecoder().decode(SignedSHA256Hash);
        try
        {
            if (verifySig(SHA256Hash.getBytes(), key, idSignature))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            System.out.println("Verification error");
            e.printStackTrace();
        }
        return false;
    }

    static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception
    {
        //Verifiy the signature
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initVerify(key);
        signer.update(data);
        return (signer.verify(sig));
    }

    public String toXML()
    {
        //Return the XML format of the block
        StringWriter sw = new StringWriter();
        try
        {
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class); //set up the marshalling process
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(this, sw); //marshall the block to xml string
        }
        catch (Exception e)
        {
            System.out.println("Error in creating xml");
            e.printStackTrace();
        }
        return sw.toString();
    }

    public String GenerateStringContent()
    {
        //Creates a string representing the block
        StringBuilder sw = new StringBuilder();
        try
        {
            sw.append(Fname);
            sw.append(Lname);
            sw.append(DOB);
            sw.append(SSNum);
            sw.append(Diagnosis);
            sw.append(Treatment);
            sw.append(Medicine);
            sw.append(BlockID);
            sw.append(CreatingProcessID);
            sw.append(CreationTimeStamp);
            sw.append(VerificationProcessID);
            sw.append(Seed);
        }
        catch (Exception e)
        {
            System.out.println("Error in creating xml");
            e.printStackTrace();
        }
        return sw.toString();
    }

    public boolean VerifySHA256Hash(BlockRecord PreviousBlock)
    {
        //Verifies the hash code with the content of the block
        try
        {
            String preivousHash;
            if(PreviousBlock!=null)
            {
                preivousHash=PreviousBlock.getSHA256HASH();
            }
            else
            {
                preivousHash = generateDummyHash("");
            }


            if (SHA256Hash.equals(GenerateHash(preivousHash+GenerateStringContent())))
            {
                return true;
            }
            else return false;
        }
        catch (Exception e)
        {
            System.out.println("Verification error");
            e.printStackTrace();
        }
        return false;
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
}
