package carbonconfiglib.gui.widgets;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraftforge.client.gui.ScreenUtils;

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
public class CarbonIconButton extends AbstractButton
{
	Consumer<CarbonIconButton> listener;
	Icon icon;
	boolean iconOnly = false;
	
	public CarbonIconButton(int x, int y, int width, int height, Icon icon, Component name, Consumer<CarbonIconButton> listener) {
		super(x, y, width, height, name);
		this.listener = listener;
		this.icon = icon;
	}
	
	public CarbonIconButton setIconOnly() {
		iconOnly = true;
		return this;
	}
	
	@Override
	public void renderButton(PoseStack stack, int mouseX, int mouseY, float p_93679_) {
        int k = this.getYImage(this.isHoveredOrFocused());
        ScreenUtils.blitWithBorder(stack, WIDGETS_LOCATION, x, y, 0, 46 + k * 20, this.width, this.height, 200, 20, 2, 3, 2, 2, this.getBlitOffset());
        if(iconOnly) {
    		int j = getFGColor();
            RenderSystem.setShaderColor(((j >> 16) & 0xFF) / 255F, ((j >> 8) & 0xFF) / 255F, (j & 0xFF) / 255F, 1F);
    		GuiUtils.drawTextureRegion(stack, x + (width / 2) - 5.5F, y+height/2-5.5F, 11, 11, icon, 16, 16);
    		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        	return;
        }
        
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		FormattedText text = GuiUtils.ellipsize(getMessage(), width - 21, font);
		int width = font.width(text) + 21;
		float minX = x + 4 + (this.width / 2) - (width / 2);
		int j = getFGColor();
        RenderSystem.setShaderColor(((j >> 16) & 0xFF) / 255F, ((j >> 8) & 0xFF) / 255F, (j & 0xFF) / 255F, 1F);
		GuiUtils.drawTextureRegion(stack, minX, y+(height-8)/2, 11, 11, icon, 16, 16);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		font.draw(stack, Language.getInstance().getVisualOrder(text), minX+15, y+(height-8)/2, j);
	}
	
	@Override
	public void onPress() {
		if(listener == null) return;
		listener.accept(this);
	}
	
	@Override
	public void updateNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
