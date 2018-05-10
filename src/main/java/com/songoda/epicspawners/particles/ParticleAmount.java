package com.songoda.epicspawners.particles;

public enum ParticleAmount {

    LIGHT(8, 2, 1),
    NORMAL(15, 5, 3),
    EXCESSIVE(21, 9, 7),
    MAD(30, 13, 15);

    private int spawnerSpawn;
    private int entitySpawn;
    private int effect;

    ParticleAmount(int spawnerSpawn, int entitySpawn, int effect) {
        this.spawnerSpawn = spawnerSpawn;
        this.entitySpawn = entitySpawn;
        this.effect = effect;
    }

    public int getSpawnerSpawn() {
        return spawnerSpawn;
    }

    public int getEntitySpawn() {
        return entitySpawn;
    }

    public int getEffect() {
        return effect;
    }
}
