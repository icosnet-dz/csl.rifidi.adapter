package com.csl.org.rifidi.multiplereader;

/**
 * Electronic Product Code
 * @author gene.yeung
 */
public class S_EPC {
    /**
     * EPC
     */
    public char[] m_epc;

    /**
     * Custom constructor
     * @param epc epc in string format, must be smaller than or equal to 62 hex numbers
     */
    public S_EPC(String epc)
    {
        m_epc = CSLibrary.Text.Hex.ToCharArray(epc);
        
    }
    /**
     * Custom constructor
     * @param epc epc in char array format, must be smaller than or equal to 31
     */
    public S_EPC(char[] epc)
    {
        m_epc = (char[])epc.clone();
    }
    /**
     * Custom constructor
     * @param epc epc in char array format, must be smaller than or equal to 31
     * @param count number of char copy to local
     */
    public S_EPC(char[] epc, int count)
    {
        m_epc = new char[count];
        System.arraycopy(epc, 0, m_epc, 0, count);
    }
    /**
     * Custom constructor
     * @param epc epc in byte array format, must be smaller than or equal to 62 bytes
     */
    public S_EPC(byte[] epc)
    {
        m_epc = CSLibrary.Text.Hex.ToCharArray(epc);
    }

    /**
     * Set epc
     * @param value epc in char array format
     */
    public void SetEPC(char[] value)
    {
        m_epc = (char[])value.clone();
    }
    /**
     * Get epc
     * @return value epc in char array format
     */
    public char[] GetEPC()
    {
        return m_epc;
    }

    /**
     * Get epc in byte array format
     * @return epc in byte array format
     */
    public byte[] ToBytes()
    {
        return CSLibrary.Text.Hex.ToBytes(m_epc);
    }

    /**
     * get length of EPC
     * @return total char length of EPC
     */
    public int Length()
    {
        return (m_epc == null || m_epc.length == 0) ? 0 : m_epc.length;
    }

    /**
     * Convert epc to string format
     * @return EPC in string format
     */
    public String ToString()
    {
        return CSLibrary.Text.Hex.ToString(m_epc);
    }
}
