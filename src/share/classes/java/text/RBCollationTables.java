



package java.text;

import java.util.Vector;
import sun.text.UCompactIntArray;
import sun.text.IntHashtable;


final class RBCollationTables {
    //===========================================================================================
    //  The following diagram shows the data structure of the RBCollationTables object.
    //  Suppose we have the rule, where 'o-umlaut' is the unicode char 0x00F6.
    //  "a, A < b, B < c, C, ch, cH, Ch, CH < d, D ... < o, O; 'o-umlaut'/E, 'O-umlaut'/E ...".
    //  What the rule says is, sorts 'ch'ligatures and 'c' only with tertiary difference and
    //  sorts 'o-umlaut' as if it's always expanded with 'e'.
    //
    // mapping table                     contracting list           expanding list
    // (contains all unicode char
    //  entries)                   ___    ____________       _________________________
    //  ________                +>|_*_|->|'c' |v('c') |  +>|v('o')|v('umlaut')|v('e')|
    // |_\u0001_|-> v('\u0001') | |_:_|  |------------|  | |-------------------------|
    // |_\u0002_|-> v('\u0002') | |_:_|  |'ch'|v('ch')|  | |             :           |
    // |____:___|               | |_:_|  |------------|  | |-------------------------|
    // |____:___|               |        |'cH'|v('cH')|  | |             :           |
    // |__'a'___|-> v('a')      |        |------------|  | |-------------------------|
    // |__'b'___|-> v('b')      |        |'Ch'|v('Ch')|  | |             :           |
    // |____:___|               |        |------------|  | |-------------------------|
    // |____:___|               |        |'CH'|v('CH')|  | |             :           |
    // |___'c'__|----------------         ------------   | |-------------------------|
    // |____:___|                                        | |             :           |
    // |o-umlaut|----------------------------------------  |_________________________|
    // |____:___|
    //
    // Noted by Helena Shih on 6/23/97
    //============================================================================================

    public RBCollationTables(String rules, int decmp) throws ParseException {
        this.rules = rules;

        RBTableBuilder builder = new RBTableBuilder(new BuildAPI());
        builder.build(rules, decmp); // this object is filled in through
                                            // the BuildAPI object
    }

    final class BuildAPI {

        private BuildAPI() {
        }


        void fillInTables(boolean f2ary,
                          boolean swap,
                          UCompactIntArray map,
                          Vector<Vector<EntryPair>> cTbl,
                          Vector<int[]> eTbl,
                          IntHashtable cFlgs,
                          short mso,
                          short mto) {
            frenchSec = f2ary;
            seAsianSwapping = swap;
            mapping = map;
            contractTable = cTbl;
            expandTable = eTbl;
            contractFlags = cFlgs;
            maxSecOrder = mso;
            maxTerOrder = mto;
        }
    }


    public String getRules()
    {
        return rules;
    }

    public boolean isFrenchSec() {
        return frenchSec;
    }

    public boolean isSEAsianSwapping() {
        return seAsianSwapping;
    }

    // ==============================================================
    // internal (for use by CollationElementIterator)
    // ==============================================================


    Vector<EntryPair> getContractValues(int ch)
    {
        int index = mapping.elementAt(ch);
        return getContractValuesImpl(index - CONTRACTCHARINDEX);
    }

    //get contract values from contractTable by index
    private Vector<EntryPair> getContractValuesImpl(int index)
    {
        if (index >= 0)
        {
            return contractTable.elementAt(index);
        }
        else // not found
        {
            return null;
        }
    }


    boolean usedInContractSeq(int c) {
        return contractFlags.get(c) == 1;
    }


    int getMaxExpansion(int order) {
        int result = 1;

        if (expandTable != null) {
            // Right now this does a linear search through the entire
            // expansion table.  If a collator had a large number of expansions,
            // this could cause a performance problem, but in practise that
            // rarely happens
            for (int i = 0; i < expandTable.size(); i++) {
                int[] valueList = expandTable.elementAt(i);
                int length = valueList.length;

                if (length > result && valueList[length-1] == order) {
                    result = length;
                }
            }
        }

        return result;
    }


    final int[] getExpandValueList(int idx) {
        return expandTable.elementAt(idx - EXPANDCHARINDEX);
    }


    int getUnicodeOrder(int ch) {
        return mapping.elementAt(ch);
    }

    short getMaxSecOrder() {
        return maxSecOrder;
    }

    short getMaxTerOrder() {
        return maxTerOrder;
    }


    //shemran/Note: this is used for secondary order value reverse, no
    //              need to consider supplementary pair.
    static void reverse (StringBuffer result, int from, int to)
    {
        int i = from;
        char swap;

        int j = to - 1;
        while (i < j) {
            swap =  result.charAt(i);
            result.setCharAt(i, result.charAt(j));
            result.setCharAt(j, swap);
            i++;
            j--;
        }
    }

    final static int getEntry(Vector<EntryPair> list, String name, boolean fwd) {
        for (int i = 0; i < list.size(); i++) {
            EntryPair pair = list.elementAt(i);
            if (pair.fwd == fwd && pair.entryName.equals(name)) {
                return i;
            }
        }
        return UNMAPPED;
    }

    // ==============================================================
    // constants
    // ==============================================================
    //sherman/Todo: is the value big enough?????
    final static int EXPANDCHARINDEX = 0x7E000000; // Expand index follows
    final static int CONTRACTCHARINDEX = 0x7F000000;  // contract indexes follow
    final static int UNMAPPED = 0xFFFFFFFF;

    final static int PRIMARYORDERMASK = 0xffff0000;
    final static int SECONDARYORDERMASK = 0x0000ff00;
    final static int TERTIARYORDERMASK = 0x000000ff;
    final static int PRIMARYDIFFERENCEONLY = 0xffff0000;
    final static int SECONDARYDIFFERENCEONLY = 0xffffff00;
    final static int PRIMARYORDERSHIFT = 16;
    final static int SECONDARYORDERSHIFT = 8;

    // ==============================================================
    // instance variables
    // ==============================================================
    private String rules = null;
    private boolean frenchSec = false;
    private boolean seAsianSwapping = false;

    private UCompactIntArray mapping = null;
    private Vector<Vector<EntryPair>> contractTable = null;
    private Vector<int[]> expandTable = null;
    private IntHashtable contractFlags = null;

    private short maxSecOrder = 0;
    private short maxTerOrder = 0;
}
