package com.hbm.core;

/**
 * Copied from MekanismCoreTransformer in Mek: CE Unofficial
 * @author sddsd2332
 */
public final class ObfSafeName {
    public final String mcp, srg;

    public ObfSafeName(String mcp, String srg) {
        this.mcp = mcp;
        this.srg = srg;
    }

    public String getName() {
        return HbmCorePlugin.runtimeDeobfEnabled() ? srg : mcp;
    }

    public boolean matches(String name) {
        return mcp.equals(name) || srg.equals(name);
    }
}
