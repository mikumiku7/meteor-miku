package com.github.mikumiku.addon.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Rotation {
    float yaw;
    float pitch;
    int priority = 0;
    int waitTime = 100;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation(Vec3d vec3d) {
        MinecraftClient mc = MinecraftClient.getInstance();

        float[] angle = this.getRotation(mc.player.getPos().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0), vec3d);
        this.yaw = angle[0];
        this.pitch = angle[1];
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public int getPriority() {
        return this.priority;
    }

    public Rotation setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public Rotation setWaitTime(int waitTime) {
        this.waitTime = waitTime;
        return this;
    }

    private float[] getRotation(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }
}
