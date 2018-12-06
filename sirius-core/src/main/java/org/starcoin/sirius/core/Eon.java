package org.starcoin.sirius.core;

public final class Eon {

    public int id;
    public Epoch epoch;

    public Eon(int id, Epoch epoch) {
        this.id = id;
        this.epoch = epoch;
    }

    public Eon() {
    }

    public enum Epoch {
        FIRST,
        SECOND,
        THIRD,
        LAST;
    }

    public static Eon calculateEon(int blockHeight, int blocksPerEon) {
        return new Eon(blockHeight / blocksPerEon,
                Epoch.values()[(blockHeight % blocksPerEon) / (blocksPerEon / 4)]);
    }
}
