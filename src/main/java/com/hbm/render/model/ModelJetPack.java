package com.hbm.render.model;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelJetPack extends ModelBiped {

	ModelRenderer JetPack;

	public ModelJetPack() {
		textureWidth = 32;
		textureHeight = 32;

		JetPack = new ModelRenderer(this, 0, 0);
		JetPack.setRotationPoint(0F, 0F, 0F);

		ModelRenderer Pack = new ModelRenderer(this, 12, 10);
		Pack.addBox(-2F, 3F, 2F, 4, 6, 1);
		JetPack.addChild(Pack);

		ModelRenderer Tank1 = new ModelRenderer(this, 0, 0);
		Tank1.addBox(0.5F, 2F, 2.5F, 3, 8, 3);
		JetPack.addChild(Tank1);

		ModelRenderer Tank2 = new ModelRenderer(this, 0, 11);
		Tank2.addBox(-3.5F, 2F, 2.5F, 3, 8, 3);
		JetPack.addChild(Tank2);

		ModelRenderer Tip1 = new ModelRenderer(this, 0, 22);
		Tip1.addBox(1F, 1F, 3F, 2, 1, 2);
		JetPack.addChild(Tip1);

		ModelRenderer Tip2 = new ModelRenderer(this, 0, 25);
		Tip2.addBox(-3F, 1F, 3F, 2, 1, 2);
		JetPack.addChild(Tip2);

		ModelRenderer Duct1 = new ModelRenderer(this, 8, 22);
		Duct1.addBox(1F, 9.5F, 3F, 2, 1, 2);
		JetPack.addChild(Duct1);

		ModelRenderer Duct2 = new ModelRenderer(this, 8, 25);
		Duct2.addBox(-3F, 9.5F, 3F, 2, 1, 2);
		JetPack.addChild(Duct2);

		ModelRenderer Thruster1 = new ModelRenderer(this, 12, 0);
		Thruster1.addBox(0.5F, 10.5F, 2.5F, 3, 2, 3);
		JetPack.addChild(Thruster1);

		ModelRenderer Thruster2 = new ModelRenderer(this, 12, 5);
		Thruster2.addBox(-3.5F, 10.5F, 2.5F, 3, 2, 3);
		JetPack.addChild(Thruster2);
	}

	public void syncBodyRotation(ModelBiped source) {
		JetPack.rotateAngleX = source.bipedBody.rotateAngleX;
		JetPack.rotateAngleY = source.bipedBody.rotateAngleY;
		JetPack.rotateAngleZ = source.bipedBody.rotateAngleZ;
		JetPack.rotationPointX = source.bipedBody.rotationPointX;
		JetPack.rotationPointY = source.bipedBody.rotationPointY;
		JetPack.rotationPointZ = source.bipedBody.rotationPointZ;
		JetPack.offsetX = source.bipedBody.offsetX;
		JetPack.offsetY = source.bipedBody.offsetY;
		JetPack.offsetZ = source.bipedBody.offsetZ;

		if(source.isSneak) {
			JetPack.rotationPointY += 4.0F;
		}
	}

	public void renderJetpack(float scale) {
		JetPack.render(scale);
	}
}