



package java.awt.color;

import sun.java2d.cmm.Profile;
import sun.java2d.cmm.ProfileDeferralInfo;





public class ICC_ProfileGray
extends ICC_Profile {

    static final long serialVersionUID = -1124721290732002649L;


    ICC_ProfileGray(Profile p) {
        super(p);
    }


    ICC_ProfileGray(ProfileDeferralInfo pdi) {
        super(pdi);
    }



    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }



    public float getGamma() {
    float theGamma;

        theGamma = super.getGamma(ICC_Profile.icSigGrayTRCTag);
        return theGamma;
    }


    public short[] getTRC() {
    short[]    theTRC;

        theTRC = super.getTRC(ICC_Profile.icSigGrayTRCTag);
        return theTRC;
    }

}
