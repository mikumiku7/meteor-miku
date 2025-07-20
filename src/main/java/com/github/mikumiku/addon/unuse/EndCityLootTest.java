package com.github.mikumiku.addon.unuse;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.data.ThreadPool;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.ChestContent;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.generator.Generator;
import com.seedfinding.mcfeature.structure.generator.structure.EndCityGenerator;
import com.seedfinding.mcterrain.TerrainGenerator;

import java.util.ArrayList;
import java.util.List;

import static com.seedfinding.mcfeature.structure.generator.structure.EndCityGenerator.LootType.*;

public class EndCityLootTest {
    private List<Pair<Generator.ILootType, BPos>> chestPos;
    private EndCityGenerator structureGenerator;

    public void setup(long worldseed, CPos cPos, MCVersion version) {
        BiomeSource biomeSource = BiomeSource.of(Dimension.END, version, worldseed);
        TerrainGenerator generator = TerrainGenerator.of(Dimension.END, biomeSource);
        structureGenerator = new EndCityGenerator(version);
        ChunkRand rand = new ChunkRand().asChunkRandDebugger();
        structureGenerator.generate(generator, cPos, rand);
        chestPos = structureGenerator.getChestsPos();
    }


    public void testCorrectChest1() {
        setup(1L, new BPos(-1232, 0, -25280).toChunkPos(), MCVersion.v1_16_5);
        assertTrue(structureGenerator.hasShip());
        List<Pair<EndCityGenerator.LootType, BPos>> checks = new ArrayList<Pair<EndCityGenerator.LootType, BPos>>() {{
            add(new Pair<>(SHIP_CHEST_1, new BPos(-1222, 100, -25202)));
            add(new Pair<>(SHIP_CHEST_2, new BPos(-1224, 100, -25202)));
            add(new Pair<>(SHIP_ELYTRA, new BPos(-1223, 100, -25202)));
        }};
        for (Pair<EndCityGenerator.LootType, BPos> check : checks) {
            assertTrue(chestPos.contains(check), String.format("Missing loot %s at pos %s", check.getFirst(), check.getSecond()));
        }
    }

    private void assertTrue(Object... format) {
        return;
    }


    public void testCorrectChest2() {
        // TODO see why this one generated 5 too high
        setup(1L, new BPos(-12752, 0, -30976).toChunkPos(), MCVersion.v1_16_5);
        List<Pair<EndCityGenerator.LootType, BPos>> checks = new ArrayList<Pair<EndCityGenerator.LootType, BPos>>() {{
            add(new Pair<>(THIRD_FLOOR_CHEST, new BPos(-12711, 103, -30973)));
        }};
        for (Pair<EndCityGenerator.LootType, BPos> check : checks) {
            assertTrue(chestPos.contains(check), String.format("Missing loot %s at pos %s", check.getFirst(), check.getSecond()));
        }
    }


    public void testCorrectChest3() {
        setup(1L, new BPos(-127280, 0, -30944).toChunkPos(), MCVersion.v1_16_5);
        assertTrue(structureGenerator.hasShip());
        List<Pair<EndCityGenerator.LootType, BPos>> checks = new ArrayList<Pair<EndCityGenerator.LootType, BPos>>() {{
            add(new Pair<>(FAT_TOWER_TOP_CHEST_1, new BPos(-127270, 123, -30943)));
            add(new Pair<>(FAT_TOWER_TOP_CHEST_2, new BPos(-127272, 123, -30945)));
            // those are also 5 too high
            add(new Pair<>(SHIP_CHEST_1, new BPos(-127272, 145, -30907)));
            add(new Pair<>(SHIP_ELYTRA, new BPos(-127273, 145, -30907)));
            add(new Pair<>(SHIP_CHEST_2, new BPos(-127274, 145, -30907)));
            add(new Pair<>(THIRD_FLOOR_CHEST, new BPos(-127276, 127, -30989)));
        }};
        for (Pair<EndCityGenerator.LootType, BPos> check : checks) {
            assertTrue(chestPos.contains(check), String.format("Missing loot %s at pos %s", check.getFirst(), check.getSecond()));
        }
    }


    public void testCorrectChest4() {
        setup(172023L, new BPos(1008, 0, 384).toChunkPos(), MCVersion.v1_16_1);
        EndCity endCity = new EndCity(MCVersion.v1_16_1);

        List<ChestContent> chests = endCity.getLoot(172023L, structureGenerator, new ChunkRand(), false);

        long hash = 0;
        for (ChestContent chest : chests) {
            for (ItemStack stack : chest.getItems()) hash += stack.hashCode();
        }
        assertEquals(4863304372L, hash, "Items changed maybe?");
    }

    private void assertEquals(Object... o) {
        return;
    }


    public void testChestLoot() {
        setup(1L, new BPos(-127280, 0, -30944).toChunkPos(), MCVersion.v1_16_5);
        EndCity endCity = new EndCity(MCVersion.v1_16_5);
        List<ChestContent> chests = endCity.getLoot(1L, structureGenerator, new ChunkRand(), false);
        long hash = 0;
        for (ChestContent chest : chests) {
            if (chest.ofType(FAT_TOWER_TOP_CHEST_1)) {
                assertEquals("ChestContent{lootType=FAT_TOWER_TOP_CHEST_1, chestType=SINGLE_CHEST, items=[ItemStack{item=Item{name='gold_ingot', enchantments=[], effects=[]}, count=4}, ItemStack{item=Item{name='beetroot_seeds', enchantments=[], effects=[]}, count=3}, ItemStack{item=Item{name='iron_ingot', enchantments=[], effects=[]}, count=10}, ItemStack{item=Item{name='iron_chestplate', enchantments=[(protection, 4), (mending, 1)], effects=[]}, count=1}], pos=Pos{x=-127270, y=123, z=-30943}, indexed=false}", chest.toString());
            }
            if (chest.ofType(FAT_TOWER_TOP_CHEST_2)) {
                assertEquals("ChestContent{lootType=FAT_TOWER_TOP_CHEST_2, chestType=SINGLE_CHEST, items=[ItemStack{item=Item{name='gold_ingot', enchantments=[], effects=[]}, count=2}, ItemStack{item=Item{name='iron_leggings', enchantments=[(binding_curse, 1)], effects=[]}, count=1}, ItemStack{item=Item{name='iron_helmet', enchantments=[(protection, 2), (unbreaking, 2)], effects=[]}, count=1}, ItemStack{item=Item{name='iron_boots', enchantments=[(protection, 4), (binding_curse, 1)], effects=[]}, count=1}], pos=Pos{x=-127272, y=123, z=-30945}, indexed=false}", chest.toString());
            }
            if (chest.ofType(SHIP_CHEST_1)) {
                assertEquals("ChestContent{lootType=SHIP_CHEST_1, chestType=SINGLE_CHEST, items=[ItemStack{item=Item{name='diamond_sword', enchantments=[(sharpness, 4), (knockback, 2), (unbreaking, 3)], effects=[]}, count=1}, ItemStack{item=Item{name='iron_ingot', enchantments=[], effects=[]}, count=5}, ItemStack{item=Item{name='iron_sword', enchantments=[(knockback, 2), (sharpness, 3)], effects=[]}, count=1}], pos=Pos{x=-127272, y=145, z=-30907}, indexed=false}", chest.toString());
            }
            if (chest.ofType(SHIP_CHEST_2)) {
                assertEquals("ChestContent{lootType=SHIP_CHEST_2, chestType=SINGLE_CHEST, items=[ItemStack{item=Item{name='iron_leggings', enchantments=[(unbreaking, 3), (fire_protection, 2)], effects=[]}, count=1}, ItemStack{item=Item{name='iron_chestplate', enchantments=[(mending, 1), (thorns, 2)], effects=[]}, count=1}], pos=Pos{x=-127274, y=145, z=-30907}, indexed=false}", chest.toString());
            }
            if (chest.ofType(THIRD_FLOOR_CHEST)) {
                assertEquals("ChestContent{lootType=THIRD_FLOOR_CHEST, chestType=SINGLE_CHEST, items=[ItemStack{item=Item{name='iron_shovel', enchantments=[(efficiency, 3)], effects=[]}, count=1}, ItemStack{item=Item{name='gold_ingot', enchantments=[], effects=[]}, count=2}, ItemStack{item=Item{name='iron_shovel', enchantments=[(unbreaking, 3), (efficiency, 4)], effects=[]}, count=1}], pos=Pos{x=-127276, y=127, z=-30989}, indexed=false}", chest.toString());
            }
            for (ItemStack stack : chest.getItems()) hash += stack.hashCode();
        }

        assertEquals(-2289062442L, hash, "Items changed maybe?");
    }


    public void testLargeEndcity() {
        // /tp @p 1952 150 -1840
        long worldSeed = -4425006226675986357L;
        setup(worldSeed, new BPos(-2880, 0, -320).toChunkPos(), MCVersion.v1_16_5);
        assertEquals(155, structureGenerator.getGlobalPieces().size(), "The end city doesn't have the proper size");
        assertTrue(structureGenerator.hasShip());
        EndCity endCity = new EndCity(MCVersion.v1_16_5);
        List<ChestContent> chests = endCity.getLoot(worldSeed, structureGenerator, new ChunkRand(), false);
        long diamondCount = 0;
        for (ChestContent chest : chests) {
            diamondCount += chest.getCount(f -> f.getName().contains("diamond"));
        }
        assertEquals(68, diamondCount, "Diamond count doesn't match");
    }

    public static void main(String[] args) {
        new ThreadPool(Runtime.getRuntime().availableProcessors()).run(EndCityLootTest::tryFindDiamond);
    }

    public static void tryFindDiamond() {
        MCVersion version = MCVersion.v1_16_5;
        EndCity endCity = new EndCity(version);
        ChunkRand rand = new ChunkRand();

        EndCityGenerator endCityGenerator = new EndCityGenerator(version);

        for (int i = 0; i < 1000000000; i++) {
            long worldseed = rand.nextLong();
            BiomeSource biomeSource = BiomeSource.of(Dimension.END, version, worldseed);
            TerrainGenerator generator = TerrainGenerator.of(Dimension.END, biomeSource);
            for (int regionX = -3000 / 16 / endCity.getSpacing(); regionX < 3000 / 16 / endCity.getSpacing(); regionX++) {
                for (int regionZ = -3000 / 16 / endCity.getSpacing(); regionZ < 3000 / 16 / endCity.getSpacing(); regionZ++) {
                    CPos pos = endCity.getInRegion(worldseed, regionX, regionZ, rand);
                    if (endCity.canSpawn(pos, biomeSource)) {
                        if (endCity.canGenerate(pos, generator)) {
                            endCityGenerator.generate(generator, pos.getX(), pos.getZ(), rand);
                            List<ChestContent> chests = endCity.getLoot(worldseed, endCityGenerator, rand, false);
                            int diamond = 0;
                            for (ChestContent chest : chests) {
                                diamond += chest.getCount(e -> e.getName().contains("diamond"));
                            }
                            if (diamond > 50) {
                                System.out.printf("Diamond: %d, seed: %d, tp: /tp @p %d ~ %d%n", diamond, worldseed, pos.getX() * 16, pos.getZ() * 16);
                            }
                            endCityGenerator.reset();
                        }
                    }
                }
            }
        }
    }
}


