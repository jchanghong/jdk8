package java.util.zip;
class ZStreamRef {
    private volatile long address;
    ZStreamRef (long address) {
        this.address = address;
    }
    long address() {
        return address;
    }
    void clear() {
        address = 0;
    }
}
