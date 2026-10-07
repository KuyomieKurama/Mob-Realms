package dev.mobrealms.core;

/** Proper names, not UI text. A persisted world sequence makes allocation collision-free. */
public final class ResidentNames {
    private ResidentNames() {}
    private static final String[] GIVEN={"Ari","Mira","Finn","Noor","Taro","Lina","Kai","Nora","Emil","Yara","Jona","Runa","Eren","Sora","Ilya","Vela"};
    private static final String[] STEM={"Val","Mor","Kel","Tal","Fen","Dor","Sil","Vor","Nar","Bel","Tor","Lum","Rav","Mer","Cal","Zel"};
    private static final String[] END={"dorin","varis","lune","mar","ren","dell","vane","rin","nora","wick","mere","thor","riel","las","wyn","dan"};
    public static String fromSequence(long sequence) {
        if(sequence<1)throw new IllegalArgumentException("name sequence");
        long value=sequence-1,cycle=value/4096;
        String name=GIVEN[(int)(value%16)]+" "+STEM[(int)(value/16%16)]+END[(int)(value/256%16)];
        return cycle==0?name:name+" "+(cycle+1);
    }
}
