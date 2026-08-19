package com.hbm.sound;

import net.minecraft.entity.Entity;

public class AudioWrapper {

	static {
		try {
			if (AudioWrapper.class.isAnnotationPresent(
					(Class<? extends java.lang.annotation.Annotation>) Class.forName("net.minecraftforge.fml.relauncher.SideOnly")
			)) {
				throw new IllegalStateException(
						"CRITICAL ERROR: AudioWrapper class MUST NOT be annotated with @SideOnly! " +
								"This is a common class. Restricting it will cause NoClassDefFoundError on the opposite side."
				);
			}
		} catch (ClassNotFoundException _) {
		}
	}

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