package com.hbm.lib.internal;

import com.hbm.core.HbmCorePlugin;

/**
 * Use sun.misc.Unsafe on Java 8, jdk.internal.misc.Unsafe on Java 9+.
 *
 * @author mlbv
 */
public final class UnsafeHolder {
    public static final AbstractUnsafe U = AbstractUnsafe.getUnsafe();

    public static final long IA_BASE;

    static {
        try {
            IA_BASE = U.arrayBaseOffset(int[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int IA_SHIFT;

    static {
        try {
            IA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(int[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long JA_BASE;

    static {
        try {
            JA_BASE = U.arrayBaseOffset(long[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int JA_SHIFT;

    static {
        try {
            JA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(long[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long BA_BASE;

    static {
        try {
            BA_BASE = U.arrayBaseOffset(byte[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int BA_SHIFT;

    static {
        try {
            BA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(byte[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long ZA_BASE;

    static {
        try {
            ZA_BASE = U.arrayBaseOffset(boolean[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int ZA_SHIFT;

    static {
        try {
            ZA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(boolean[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long SA_BASE;

    static {
        try {
            SA_BASE = U.arrayBaseOffset(short[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int SA_SHIFT;

    static {
        try {
            SA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(short[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long CA_BASE;

    static {
        try {
            CA_BASE = U.arrayBaseOffset(char[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int CA_SHIFT;

    static {
        try {
            CA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(char[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long FA_BASE;

    static {
        try {
            FA_BASE = U.arrayBaseOffset(float[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int FA_SHIFT;

    static {
        try {
            FA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(float[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long DA_BASE;

    static {
        try {
            DA_BASE = U.arrayBaseOffset(double[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int DA_SHIFT;

    static {
        try {
            DA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(double[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final long RA_BASE;

    static {
        try {
            RA_BASE = U.arrayBaseOffset(Object[].class);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static final int RA_SHIFT;

    static {
        try {
            RA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(Object[].class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private UnsafeHolder() {
    }

    public static long offInt(int i) {
        return ((long) i << IA_SHIFT) + IA_BASE;
    }

    public static long offLong(int i) {
        return ((long) i << JA_SHIFT) + JA_BASE;
    }

    public static long offByte(int i) {
        return ((long) i << BA_SHIFT) + BA_BASE;
    }

    public static long offBoolean(int i) {
        return ((long) i << ZA_SHIFT) + ZA_BASE;
    }

    public static long offShort(int i) {
        return ((long) i << SA_SHIFT) + SA_BASE;
    }

    public static long offChar(int i) {
        return ((long) i << CA_SHIFT) + CA_BASE;
    }

    public static long offFloat(int i) {
        return ((long) i << FA_SHIFT) + FA_BASE;
    }

    public static long offDouble(int i) {
        return ((long) i << DA_SHIFT) + DA_BASE;
    }

    public static long offReference(int i) {
        return ((long) i << RA_SHIFT) + RA_BASE;
    }

    public static Object staticFieldBase(Class<?> clz, String fieldName) {
        try {
            return U.staticFieldBase(clz.getDeclaredField(fieldName));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long staticfieldOffset(Class<?> clz, String fieldName) {
        try {
            return U.staticFieldOffset(clz.getDeclaredField(fieldName));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long fieldOffset(Class<?> clz, String fieldName) {
        try {
            return U.objectFieldOffset(clz.getDeclaredField(fieldName));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T allocateInstance(Class<? extends T> clz) {
        try {
            //noinspection unchecked
            return (T) U.allocateInstance(clz);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long fieldOffset(Class<?> clz, String mcp, String srg) {
        try {
            return U.objectFieldOffset(clz.getDeclaredField(HbmCorePlugin.chooseName(mcp, srg)));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
