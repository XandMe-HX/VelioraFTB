package id.velioragardens.velioraftb.model;

import java.util.UUID;

public class PlayerSkillData {
    private final UUID uuid;
    private long veinExpire;
    private long treeExpire;
    private long farmerExpire;

    public PlayerSkillData(UUID uuid) {
        this.uuid = uuid;
        this.veinExpire = 0;
        this.treeExpire = 0;
        this.farmerExpire = 0;
    }

    public PlayerSkillData(UUID uuid, long veinExpire, long treeExpire, long farmerExpire) {
        this.uuid = uuid;
        this.veinExpire = veinExpire;
        this.treeExpire = treeExpire;
        this.farmerExpire = farmerExpire;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getVeinExpire() {
        return veinExpire;
    }

    public void setVeinExpire(long veinExpire) {
        this.veinExpire = veinExpire;
    }

    public long getTreeExpire() {
        return treeExpire;
    }

    public void setTreeExpire(long treeExpire) {
        this.treeExpire = treeExpire;
    }

    public long getFarmerExpire() {
        return farmerExpire;
    }

    public void setFarmerExpire(long farmerExpire) {
        this.farmerExpire = farmerExpire;
    }

    public boolean isVeinActive() {
        return veinExpire > System.currentTimeMillis();
    }

    public boolean isTreeActive() {
        return treeExpire > System.currentTimeMillis();
    }

    public boolean isFarmerActive() {
        return farmerExpire > System.currentTimeMillis();
    }
}
