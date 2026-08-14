package com.hbm.blocks.machine.dummy;

import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.HashMap;
import java.util.Map;

public class DummyProperties {

    public enum PortType { NONE, ITEM_ENERGY, FLUID, PORT_NEW }

    public String name;
    public String blockRegistryName;
    public String portRegistryName;
    public int guiId = -1;
    public Block dropBlock;
    public PortType portType = PortType.NONE;
    public boolean isDoor = false;
    public boolean isRadResistant = false;
    public boolean hasVariantToggle = false;
    public float hardness = 5.0F;
    public float resistance = 10.0F;
    public AxisAlignedBB boundingBox = Block.FULL_BLOCK_AABB;

    private final Map<Class<?>, Object> extensions = new HashMap<>();

    public DummyProperties(String name, Block dropBlock) {
        this.name = name;
        this.dropBlock = dropBlock;
        this.blockRegistryName = "dummy_block_" + name;
        this.portRegistryName = "dummy_port_" + name;
    }

    public DummyProperties(String name, Block dropBlock, float hardness, float resistance) {
        this(name, dropBlock);
        this.hardness = hardness;
        this.resistance = resistance;
    }

    public DummyProperties setRegistryNames(String blockName, String portName) {
        this.blockRegistryName = blockName;
        this.portRegistryName = portName;
        return this;
    }

    public DummyProperties setGui(int guiId) { this.guiId = guiId; return this; }
    public DummyProperties setPort(PortType type) { this.portType = type; return this; }
    public DummyProperties asDoor() { this.isDoor = true; return this; }
    public DummyProperties asRadResistant() { this.isRadResistant = true; return this; }
    public DummyProperties withVariantToggle() { this.hasVariantToggle = true; return this; }

    public DummyProperties setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.boundingBox = new AxisAlignedBB(minX / 16F, minY / 16F, minZ / 16F, maxX / 16F, maxY / 16F, maxZ / 16F);
        return this;
    }

    public <T> DummyProperties withExtension(Class<T> key, T value) {
        extensions.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> key) {
        return (T) extensions.get(key);
    }

    public <T> boolean hasExtension(Class<T> key) {
        return extensions.containsKey(key);
    }
}