package cn.moze.catfight.bukkit;

final class ModelIds {
    static final int BASE = 910000;
    static final int VARIANT_STRIDE = 100;
    static final int HEAD = 1;
    static final int JAW = 2;
    static final int BODY_1 = 3;
    static final int BODY_2 = 4;
    static final int BODY_3 = 5;
    static final int BODY_4 = 6;
    static final int TAIL_1 = 7;
    static final int TAIL_2 = 8;
    static final int FRONT_LEFT = 9;
    static final int FRONT_RIGHT = 10;
    static final int HIND_LEFT = 11;
    static final int HIND_RIGHT = 12;
    static final int PAPER_BASE = 20;

    private ModelIds() { }

    static int piece(int variant, int piece) {
        return BASE + variant * VARIANT_STRIDE + piece;
    }

    static int paper(int variant, int direction) {
        return BASE + variant * VARIANT_STRIDE + PAPER_BASE + Math.floorMod(direction, 16);
    }
}
