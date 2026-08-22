package com.hbm.core;

import net.minecraft.launchwrapper.IClassTransformer;

public final class HbmCoreTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) return basicClass;

        // a class is transformed at most once
        return switch (transformedName) {
//            case FMLNetworkTransformer.TARGET_DISPATCHER ->
//                    FMLNetworkTransformer.transformNetworkDispatcher(name, transformedName, basicClass);
//            case FMLNetworkTransformer.TARGET_PACKET ->
//                    FMLNetworkTransformer.transformFMLProxyPacket(name, transformedName, basicClass);
            default -> basicClass;
        };
    }
}
