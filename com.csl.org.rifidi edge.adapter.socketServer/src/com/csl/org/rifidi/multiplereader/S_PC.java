package com.csl.org.rifidi.multiplereader;

/**
 * Protocol Control(must be 2 Bytes)
 * @author gene.yeung
 */
public class S_PC{
     /**
     * Protocol Control Value(one word)
     */
    public char pc;

    /**
     * Custom constructor
     * @param pc pc in string format, must be 4 hex numbers
     */
    public S_PC(String pc)
    {
        this.pc = CSLibrary.Text.Hex.ToChar(pc);
    }
    /**
     * Custom constructor
     * @param pc pc in char format
     */
    public S_PC(char pc)
    {
        this.pc = pc;
    }

    /**
     * Set PC value in char
     * @param value
     */
    public void SetPC(char value)
    {
        pc = value;
    }
    /**
     * Get PC value in char
     * @return
     */
    public char GetPC()
    {
        return pc;
    }

    /**
     * return PC in string format
     * @return
     */
    public String ToString()
    {
        return Integer.toHexString(pc);
    }
    /**
     * Get 16bit EPC Length from current PC value
     * @return
     */
    public int EPCLength()
    {
        return (int)(pc >> 11 & 0x1F);
    }
    /**
     * User Memory Indicator, true if user memory contains data.
     * Notes: Not all tags support this function
     * @return
     */
    public boolean UMI()
    {
        return ((pc >> 10 & 0x1) == 0x1);
    }
    /**
     * An XPC_W1 Indicator, true if XPC_W1 is non-zero value
     * Notes: Not all tags support this function
     * @return
     */
    public boolean XI()
    {
        return ((pc >> 9 & 0x1) == 0x1);
    }
}
