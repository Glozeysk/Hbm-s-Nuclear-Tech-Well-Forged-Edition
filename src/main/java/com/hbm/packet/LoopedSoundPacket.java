package com.hbm.packet;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.sound.SoundLoopAssembler;
import com.hbm.sound.SoundLoopBroadcaster;
import com.hbm.sound.SoundLoopCentrifuge;
import com.hbm.sound.SoundLoopChemplant;
import com.hbm.sound.SoundLoopTurbofan;
import com.hbm.sound.SoundLoopFel;
import com.hbm.tileentity.machine.TileEntityBroadcaster;
import com.hbm.tileentity.machine.TileEntityMachineAssembler;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineChemplant;
import com.hbm.tileentity.machine.TileEntityMachineChemical;
import com.hbm.tileentity.machine.TileEntityMachineChemfac;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.hbm.tileentity.machine.TileEntityFEL;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LoopedSoundPacket implements IMessage {

	int x;
	int y;
	int z;

	public LoopedSoundPacket() {
	}

	public LoopedSoundPacket(BlockPos pos) {
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public LoopedSoundPacket(int xPos, int yPos, int zPos) {
		x = xPos;
		y = yPos;
		z = zPos;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
	}

	public static class Handler implements IMessageHandler<LoopedSoundPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(LoopedSoundPacket m, MessageContext ctx) {

			Minecraft.getMinecraft().addScheduledTask(() -> {
				try {
					World world = Minecraft.getMinecraft().world;
					if (world == null) return;

					BlockPos pos = new BlockPos(m.x, m.y, m.z);

					if (!world.isBlockLoaded(pos)) return;

					TileEntity te = world.getTileEntity(pos);
					if (te == null) return;

					BlockPos tePos = te.getPos();
					if (tePos == null) return;

					if (te instanceof TileEntityMachineChemplant) {
						boolean flag = true;
						if (SoundLoopChemplant.list != null) {
							for (int i = 0; i < SoundLoopChemplant.list.size(); i++) {
								if (SoundLoopChemplant.list.get(i) != null &&
										SoundLoopChemplant.list.get(i).getTE() == te &&
										!SoundLoopChemplant.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineChemplant) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopChemplant(HBMSoundHandler.chemplantOperate, te));
						}
					}

					if (te instanceof TileEntityMachineChemical) {
						boolean flag = true;
						if (SoundLoopChemplant.list != null) {
							for (int i = 0; i < SoundLoopChemplant.list.size(); i++) {
								if (SoundLoopChemplant.list.get(i) != null &&
										SoundLoopChemplant.list.get(i).getTE() == te &&
										!SoundLoopChemplant.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineChemical) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopChemplant(HBMSoundHandler.chemplantOperate, te));
						}
					}

					if (te instanceof TileEntityMachineChemfac) {
						boolean flag = true;
						if (SoundLoopChemplant.list != null) {
							for (int i = 0; i < SoundLoopChemplant.list.size(); i++) {
								if (SoundLoopChemplant.list.get(i) != null &&
										SoundLoopChemplant.list.get(i).getTE() == te &&
										!SoundLoopChemplant.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineChemfac) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopChemplant(HBMSoundHandler.chemplantOperate, te));
						}
					}

					if (te instanceof TileEntityFEL) {
						boolean flag = true;
						if (SoundLoopFel.list != null) {
							for (int i = 0; i < SoundLoopFel.list.size(); i++) {
								if (SoundLoopFel.list.get(i) != null &&
										SoundLoopFel.list.get(i).getTE() == te &&
										!SoundLoopFel.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityFEL) te).isOn) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopFel(HBMSoundHandler.fel, te));
						}
					}

					if (te instanceof TileEntityMachineMiningLaser) {
						boolean flag = true;
						if (SoundLoopFel.list != null) {
							for (int i = 0; i < SoundLoopFel.list.size(); i++) {
								if (SoundLoopFel.list.get(i) != null &&
										SoundLoopFel.list.get(i).getTE() == te &&
										!SoundLoopFel.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineMiningLaser) te).isOn) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopFel(HBMSoundHandler.fel, te));
						}
					}

					if (te instanceof TileEntityMachineAssembler) {
						boolean flag = true;
						if (SoundLoopAssembler.list != null) {
							for (int i = 0; i < SoundLoopAssembler.list.size(); i++) {
								if (SoundLoopAssembler.list.get(i) != null &&
										SoundLoopAssembler.list.get(i).getTE() == te &&
										!SoundLoopAssembler.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineAssembler) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopAssembler(HBMSoundHandler.assemblerOperate, te));
						}
					}

					if (te instanceof TileEntityMachineTurbofan) {
						boolean flag = true;
						if (SoundLoopTurbofan.list != null) {
							for (int i = 0; i < SoundLoopTurbofan.list.size(); i++) {
								if (SoundLoopTurbofan.list.get(i) != null &&
										SoundLoopTurbofan.list.get(i).getTE() == te &&
										!SoundLoopTurbofan.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineTurbofan) te).isRunning) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopTurbofan(HBMSoundHandler.turbofanOperate, te));
						}
					}

					if (te instanceof TileEntityBroadcaster) {
						boolean flag = true;
						if (SoundLoopBroadcaster.list != null) {
							for (int i = 0; i < SoundLoopBroadcaster.list.size(); i++) {
								if (SoundLoopBroadcaster.list.get(i) != null &&
										SoundLoopBroadcaster.list.get(i).getTE() == te &&
										!SoundLoopBroadcaster.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						int j = tePos.getX() + tePos.getY() + tePos.getZ();
						int rand = Math.abs(j) % 3 + 1;
						SoundEvent sound;
						switch (rand) {
							case 1:
								sound = HBMSoundHandler.broadcast1;
								break;
							case 2:
								sound = HBMSoundHandler.broadcast2;
								break;
							case 3:
								sound = HBMSoundHandler.broadcast3;
								break;
							default:
								sound = HBMSoundHandler.broadcast1;
								break;
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopBroadcaster(sound, te));
						}
					}

					if (te instanceof TileEntityMachineCentrifuge) {
						boolean flag = true;
						if (SoundLoopCentrifuge.list != null) {
							for (int i = 0; i < SoundLoopCentrifuge.list.size(); i++) {
								if (SoundLoopCentrifuge.list.get(i) != null &&
										SoundLoopCentrifuge.list.get(i).getTE() == te &&
										!SoundLoopCentrifuge.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineCentrifuge) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopCentrifuge(HBMSoundHandler.centrifugeOperate, te));
						}
					}

					if (te instanceof TileEntityMachineGasCent) {
						boolean flag = true;
						if (SoundLoopCentrifuge.list != null) {
							for (int i = 0; i < SoundLoopCentrifuge.list.size(); i++) {
								if (SoundLoopCentrifuge.list.get(i) != null &&
										SoundLoopCentrifuge.list.get(i).getTE() == te &&
										!SoundLoopCentrifuge.list.get(i).isDonePlaying()) {
									flag = false;
									break;
								}
							}
						}

						if (flag && te.getWorld() != null && te.getWorld().isRemote &&
								((TileEntityMachineGasCent) te).isProgressing) {
							Minecraft.getMinecraft().getSoundHandler().playSound(
									new SoundLoopCentrifuge(HBMSoundHandler.centrifugeOperate, te));
						}
					}
				} catch (Exception e) {
				}
			});

			return null;
		}
	}
}