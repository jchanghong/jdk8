

package java.awt.font;


public interface OpenType {




  public final static int       TAG_CMAP        = 0x636d6170;


  public final static int       TAG_HEAD        = 0x68656164;


  public final static int       TAG_NAME        = 0x6e616d65;


  public final static int       TAG_GLYF        = 0x676c7966;


  public final static int       TAG_MAXP        = 0x6d617870;


  public final static int       TAG_PREP        = 0x70726570;


  public final static int       TAG_HMTX        = 0x686d7478;


  public final static int       TAG_KERN        = 0x6b65726e;


  public final static int       TAG_HDMX        = 0x68646d78;


  public final static int       TAG_LOCA        = 0x6c6f6361;


  public final static int       TAG_POST        = 0x706f7374;


  public final static int       TAG_OS2 = 0x4f532f32;


  public final static int       TAG_CVT = 0x63767420;


  public final static int       TAG_GASP        = 0x67617370;


  public final static int       TAG_VDMX        = 0x56444d58;


  public final static int       TAG_VMTX        = 0x766d7478;


  public final static int       TAG_VHEA        = 0x76686561;


  public final static int       TAG_HHEA        = 0x68686561;


  public final static int       TAG_TYP1        = 0x74797031;


  public final static int       TAG_BSLN        = 0x62736c6e;


  public final static int       TAG_GSUB        = 0x47535542;


  public final static int       TAG_DSIG        = 0x44534947;


  public final static int       TAG_FPGM        = 0x6670676d;


  public final static int       TAG_FVAR        = 0x66766172;


  public final static int       TAG_GVAR        = 0x67766172;


  public final static int       TAG_CFF = 0x43464620;


  public final static int       TAG_MMSD        = 0x4d4d5344;


  public final static int       TAG_MMFX        = 0x4d4d4658;


  public final static int       TAG_BASE        = 0x42415345;


  public final static int       TAG_GDEF        = 0x47444546;


  public final static int       TAG_GPOS        = 0x47504f53;


  public final static int       TAG_JSTF        = 0x4a535446;


  public final static int       TAG_EBDT        = 0x45424454;


  public final static int       TAG_EBLC        = 0x45424c43;


  public final static int       TAG_EBSC        = 0x45425343;


  public final static int       TAG_LTSH        = 0x4c545348;


  public final static int       TAG_PCLT        = 0x50434c54;


  public final static int       TAG_ACNT        = 0x61636e74;


  public final static int       TAG_AVAR        = 0x61766172;


  public final static int       TAG_BDAT        = 0x62646174;


  public final static int       TAG_BLOC        = 0x626c6f63;


  public final static int       TAG_CVAR        = 0x63766172;


  public final static int       TAG_FEAT        = 0x66656174;


  public final static int       TAG_FDSC        = 0x66647363;


  public final static int       TAG_FMTX        = 0x666d7478;


  public final static int       TAG_JUST        = 0x6a757374;


  public final static int       TAG_LCAR        = 0x6c636172;


  public final static int       TAG_MORT        = 0x6d6f7274;


  public final static int       TAG_OPBD        = 0x6d6f7274;


  public final static int       TAG_PROP        = 0x70726f70;


  public final static int       TAG_TRAK        = 0x7472616b;


  public int getVersion();


  public byte[] getFontTable(int sfntTag);


  public byte[] getFontTable(String strSfntTag);


  public byte[] getFontTable(int sfntTag, int offset, int count);


  public byte[] getFontTable(String strSfntTag, int offset, int count);


  public int getFontTableSize(int sfntTag);


  public int getFontTableSize(String strSfntTag);


}
