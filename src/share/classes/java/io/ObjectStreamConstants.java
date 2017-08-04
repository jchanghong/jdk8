package java.io;
public interface ObjectStreamConstants {
    final static short STREAM_MAGIC = (short)0xaced;
    final static short STREAM_VERSION = 5;
    final static byte TC_BASE = 0x70;
    final static byte TC_NULL =         (byte)0x70;
    final static byte TC_REFERENCE =    (byte)0x71;
    final static byte TC_CLASSDESC =    (byte)0x72;
    final static byte TC_OBJECT =       (byte)0x73;
    final static byte TC_STRING =       (byte)0x74;
    final static byte TC_ARRAY =        (byte)0x75;
    final static byte TC_CLASS =        (byte)0x76;
    final static byte TC_BLOCKDATA =    (byte)0x77;
    final static byte TC_ENDBLOCKDATA = (byte)0x78;
    final static byte TC_RESET =        (byte)0x79;
    final static byte TC_BLOCKDATALONG= (byte)0x7A;
    final static byte TC_EXCEPTION =    (byte)0x7B;
    final static byte TC_LONGSTRING =   (byte)0x7C;
    final static byte TC_PROXYCLASSDESC =       (byte)0x7D;
    final static byte TC_ENUM =         (byte)0x7E;
    final static byte TC_MAX =          (byte)0x7E;
    final static int baseWireHandle = 0x7e0000;
    final static byte SC_WRITE_METHOD = 0x01;
    final static byte SC_BLOCK_DATA = 0x08;
    final static byte SC_SERIALIZABLE = 0x02;
    final static byte SC_EXTERNALIZABLE = 0x04;
    final static byte SC_ENUM = 0x10;
    final static SerializablePermission SUBSTITUTION_PERMISSION =
                           new SerializablePermission("enableSubstitution");
    final static SerializablePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
                    new SerializablePermission("enableSubclassImplementation");
    public final static int PROTOCOL_VERSION_1 = 1;
    public final static int PROTOCOL_VERSION_2 = 2;
}
