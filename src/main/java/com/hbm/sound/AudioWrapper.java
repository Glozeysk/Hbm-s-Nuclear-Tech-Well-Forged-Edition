package com.hbm.sound;

import net.minecraft.entity.Entity;

/**
 * Common-side handle for a looped sound.
 * <p>
 * Neither this class nor fields of this type may be annotated with {@code @SideOnly(Side.CLIENT)}.
 * Forge's {@code SideTransformer} removes such classes and fields from the dedicated server, while
 * common tile entity code (for example {@code onChunkUnload} and {@code invalidate}) still references
 * them, which causes {@code NoSuchFieldError}/{@code NoClassDefFoundError} on the server.
 * <p>
 * This cannot be checked in a static initializer: by the time a class is initialized, Forge has already
 * stripped the annotation (or refused to load the class). The build-time check in {@code dependencies.gradle}
 * enforces this instead.
 */
public class AudioWrapper {

	public void setKeepAlive(int keepAlive) {}

	public void keepAlive() {}

	public void updatePosition(float x, float y, float z) {}

	public void attachTo(Entity e) {}

	public void updateVolume(float volume) {}

	public void updateRange(float range) {}

	public void updatePitch(float pitch) {}

	public float getVolume() {
		return 0F;
	}

	public float getRange() {
		return 0F;
	}

	public float getPitch() {
		return 0F;
	}

	public void setDoesRepeat(boolean repeats) {}

	public void startSound() {}

	public void stopSound() {}

	public boolean isPlaying() {
		return false;
	}
}