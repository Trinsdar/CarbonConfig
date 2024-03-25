package carbonconfiglib.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import carbonconfiglib.gui.api.ISuggestionRenderer;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Copyright 2023 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class SuggestionRenderers
{
	public static class ItemEntry implements ISuggestionRenderer {
		@Override
		public Component renderSuggestion(GuiGraphics stack, String value, int x, int y) {
			ResourceLocation id = ResourceLocation.tryParse(value);
			if(id == null) return null;
			Item item = BuiltInRegistries.ITEM.get(id);
			if(item == Items.AIR || item == null) return null;
			ItemStack itemStack = new ItemStack(item);
			stack.renderItem(itemStack, x, y);
			return itemStack.getHoverName().copy().withStyle(ChatFormatting.YELLOW).append("\n").append(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));			
		}
	}
	
	public static class FluidEntry implements ISuggestionRenderer {
		@Override
		public Component renderSuggestion(GuiGraphics stack, String value, int x, int y) {
			ResourceLocation id = ResourceLocation.tryParse(value);
			if(id == null) return null;
			Fluid fluid = BuiltInRegistries.FLUID.get(id);
			if(fluid == Fluids.EMPTY) return null;
			TextureAtlasSprite sprite = getSprite(fluid);
			if(sprite == null) return null;
			RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
			int color = FluidRenderHandlerRegistry.INSTANCE.get(fluid).getFluidColor(null, null, fluid.defaultFluidState());
			RenderSystem.setShaderColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
			stack.blit(x, y, 0, 18, 18, sprite);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			return getDescription(fluid).withStyle(ChatFormatting.YELLOW).append("\n").append(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
		}
		
		private MutableComponent getDescription(Fluid fluid) {
			return Component.translatable(Util.makeDescriptionId("fluid", BuiltInRegistries.FLUID.getKey(fluid)));
		}
		
		private TextureAtlasSprite getSprite(Fluid fluid) {
			return FluidRenderHandlerRegistry.INSTANCE.get(fluid).getFluidSprites(null, null, fluid.defaultFluidState())[0];
		}
	}
	
	public static class EnchantmentEntry implements ISuggestionRenderer {
		@Override
		public Component renderSuggestion(GuiGraphics stack, String value, int x, int y) {
			ResourceLocation id = ResourceLocation.tryParse(value);
			if(id == null) return null;
			Enchantment ench = BuiltInRegistries.ENCHANTMENT.get(id);
			if(ench == null) return null;
			stack.renderItem(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, ench.getMinLevel())), x, y);
			return ench.getFullname(ench.getMinLevel()).copy().withStyle(ChatFormatting.YELLOW).append("\n").append(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
		}
	}
	
	public static class PotionEntry implements ISuggestionRenderer {
		@Override
		public Component renderSuggestion(GuiGraphics stack, String value, int x, int y) {
			ResourceLocation id = ResourceLocation.tryParse(value);
			if(id == null) return null;
			MobEffect potion = BuiltInRegistries.MOB_EFFECT.get(id);
			if(potion == null) return null;
			ItemStack item = new ItemStack(Items.POTION);
			PotionUtils.setCustomEffects(item, ObjectLists.singleton(new MobEffectInstance(potion)));
			item.addTagElement("CustomPotionColor", IntTag.valueOf(potion.getColor()));
			stack.renderItem(item, x, y);
			return potion.getDisplayName().copy().withStyle(ChatFormatting.YELLOW).append("\n").append(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
		}
	}
	
	public static class ColorEntry implements ISuggestionRenderer {
		@Override
		public Component renderSuggestion(GuiGraphics stack, String value, int x, int y) {
			try {
				GuiComponent.fill(stack, x+1, y-1, x+18, y+17, 0xFFA0A0A0);
				GuiComponent.fill(stack, x+2, y, x+17, y+16, Long.decode(value).intValue() | 0xFF000000);
			}
			catch(Exception e) {
			}
			return null;
		}
	}
}
