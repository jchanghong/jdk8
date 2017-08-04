

package java.awt.im;

import java.awt.Rectangle;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;



public interface InputMethodRequests {


    Rectangle getTextLocation(TextHitInfo offset);


    TextHitInfo getLocationOffset(int x, int y);


    int getInsertPositionOffset();


    AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex,
                                                 Attribute[] attributes);


    int getCommittedTextLength();


    AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes);


    AttributedCharacterIterator getSelectedText(Attribute[] attributes);
}
