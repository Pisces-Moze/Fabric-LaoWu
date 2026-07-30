package cn.moze.catfight;

public interface CatFightAccess {
    int catfight$getState();
    void catfight$setState(int state);
    boolean catfight$isFlat();
    void catfight$setFlat(boolean flat);
    int catfight$getFlatPose();
    void catfight$setFlatPose(int pose);
    int catfight$getRestoreClicks();
    void catfight$setRestoreClicks(int clicks);
    int catfight$getProjectionYaw();
    void catfight$setProjectionYaw(int yaw);
    int catfight$getProjectionPitch();
    void catfight$setProjectionPitch(int pitch);
}
