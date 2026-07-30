package cn.moze.catfight.client;

public interface FightRenderState {
    int catfight$getFightState();
    void catfight$setFightState(int state);
    boolean catfight$isFlat();
    void catfight$setFlat(boolean flat);
    int catfight$getProjectionYaw();
    void catfight$setProjectionYaw(int yaw);
    int catfight$getProjectionPitch();
    void catfight$setProjectionPitch(int pitch);
}
