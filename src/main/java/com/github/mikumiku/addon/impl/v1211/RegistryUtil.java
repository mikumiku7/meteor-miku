package com.github.mikumiku.addon.impl.v1211;

import com.github.mikumiku.addon.util.Ore;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.OrePlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistryUtil implements com.github.mikumiku.addon.util.RegistryUtil {
    @Override
    public Map<RegistryKey<Biome>, List<Ore>> getRegistry(Dimension dimension) {

        RegistryWrapper.WrapperLookup registry = BuiltinRegistries.createWrapperLookup();
        RegistryWrapper.Impl<PlacedFeature> features = registry.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE);
        var reg = registry.getWrapperOrThrow(RegistryKeys.WORLD_PRESET).getOrThrow(WorldPresets.DEFAULT).value().createDimensionsRegistryHolder().dimensions();

        DimensionOptions dim = switch (dimension) {
            case Overworld -> reg.get(DimensionOptions.OVERWORLD);
            case Nether -> reg.get(DimensionOptions.NETHER);
            case End -> reg.get(DimensionOptions.END);
        };
        ChunkGenerator generator = dim.chunkGenerator();
        var biomes = generator.getBiomeSource().getBiomes();
        var biomes1 = biomes.stream().toList();

        List<PlacedFeatureIndexer.IndexedFeatures> indexer = PlacedFeatureIndexer.collectIndexedFeatures(
            biomes1, biomeEntry -> biomeEntry.value().getGenerationSettings().getFeatures(), true
        );


        Map<PlacedFeature, Ore> featureToOre = new HashMap<>();
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_COAL_LOWER, 6, coal, new Color(47, 44, 54), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_COAL_UPPER, 6, coal, new Color(47, 44, 54), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_IRON_MIDDLE, 6, iron, new Color(236, 173, 119), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_IRON_SMALL, 6, iron, new Color(236, 173, 119), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_IRON_UPPER, 6, iron, new Color(236, 173, 119), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_GOLD, 6, gold, new Color(247, 229, 30), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_GOLD_LOWER, 6, gold, new Color(247, 229, 30), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_GOLD_EXTRA, 6, gold, new Color(247, 229, 30), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_GOLD_NETHER, 7, gold, new Color(247, 229, 30), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_GOLD_DELTAS, 7, gold, new Color(247, 229, 30), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_REDSTONE, 6, redstone, new Color(245, 7, 23), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_REDSTONE_LOWER, 6, redstone, new Color(245, 7, 23), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_DIAMOND, 6, diamond, new Color(33, 244, 255), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_DIAMOND_BURIED, 6, diamond, new Color(33, 244, 255), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_DIAMOND_LARGE, 6, diamond, new Color(33, 244, 255), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_DIAMOND_MEDIUM, 6, diamond, new Color(33, 244, 255), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_LAPIS, 6, lapis, new Color(8, 26, 189), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_LAPIS_BURIED, 6, lapis, new Color(8, 26, 189), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_COPPER, 6, copper, new Color(239, 151, 0), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_COPPER_LARGE, 6, copper, new Color(239, 151, 0), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_EMERALD, 6, emerald, new Color(27, 209, 45), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_QUARTZ_NETHER, 7, quartz, new Color(205, 205, 205), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_QUARTZ_DELTAS, 7, quartz, new Color(205, 205, 205), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_DEBRIS_SMALL, 7, debris, new Color(209, 27, 245), generator);
        registerOre(featureToOre, indexer, features, OrePlacedFeatures.ORE_ANCIENT_DEBRIS_LARGE, 7, debris, new Color(209, 27, 245), generator);


        Map<RegistryKey<Biome>, List<Ore>> biomeOreMap = new HashMap<>();

        biomes1.forEach(biome -> {
            biomeOreMap.put(biome.getKey().get(), new ArrayList<>());
            biome.value().getGenerationSettings().getFeatures().stream()
                .flatMap(RegistryEntryList::stream)
                .map(RegistryEntry::value)
                .filter(featureToOre::containsKey)
                .forEach(feature -> {
                    biomeOreMap.get(biome.getKey().get()).add(featureToOre.get(feature));
                });
        });
        return biomeOreMap;
    }

    private static void registerOre(
        Map<PlacedFeature, Ore> map,
        List<PlacedFeatureIndexer.IndexedFeatures> indexer,
        RegistryWrapper.Impl<PlacedFeature> oreRegistry,
        RegistryKey<PlacedFeature> oreKey,
        int genStep,
        Setting<Boolean> active,
        Color color,
        ChunkGenerator generator
    ) {
        var orePlacement = oreRegistry.getOrThrow(oreKey).value();
        int index = indexer.get(genStep).indexMapping().applyAsInt(orePlacement);
        Ore ore = new Ore(orePlacement, genStep, index, active, color, generator);
        map.put(orePlacement, ore);
    }
}
