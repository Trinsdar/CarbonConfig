package carbonconfiglib.gui.config;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import carbonconfiglib.gui.api.IArrayNode;
import carbonconfiglib.gui.api.IConfigNode;
import carbonconfiglib.gui.api.IValueNode;
import carbonconfiglib.gui.widgets.CarbonHoverIconButton;
import carbonconfiglib.gui.widgets.CarbonHoverIconButton.IconInfo;
import carbonconfiglib.gui.widgets.CarbonIconButton;
import carbonconfiglib.gui.widgets.GuiUtils;
import carbonconfiglib.gui.widgets.IOwnable;
import carbonconfiglib.gui.widgets.Icon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

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
public class ConfigElement extends Element
{
	private static final Component DELETE = Component.translatable("gui.carbonconfig.delete");
	private static final Component REVERT = Component.translatable("gui.carbonconfig.revert");
	private static final Component DEFAULT = Component.translatable("gui.carbonconfig.default");
	private static final Component RELOAD = Component.translatable("gui.carbonconfig.reload").withStyle(ChatFormatting.YELLOW);
	private static final Component RESTART = Component.translatable("gui.carbonconfig.restart").withStyle(ChatFormatting.YELLOW);
	protected List<GuiEventListener> listeners = new ObjectArrayList<>();
	protected List<Map.Entry<AbstractWidget, AlignOffset>> mappedListeners = new ObjectArrayList<>();
	protected IConfigNode node;
	protected IValueNode value;
	protected IArrayNode array;
	
	protected CarbonIconButton setReset;
	protected CarbonIconButton setDefault;
	protected CarbonHoverIconButton moveDown;
	protected CarbonHoverIconButton moveUp;
	
	public ConfigElement(IConfigNode node) {
		super(node.getName());
		this.node = node;
		this.value = node.asValue();
	}
	
	public ConfigElement(IConfigNode node, IValueNode value) {
		super(node.getName());
		this.node = node;
		this.value = value;
	}
	
	public ConfigElement(IConfigNode node, IArrayNode array) {
		super(node.getName());
		this.node = node;
		this.array = array;
	}
	
	public ConfigElement(IConfigNode node, IArrayNode array, int index) {
		super(node.getName());
		this.node = node;
		this.array = array;
		this.value = array.asValue(index);
	}
	
	protected <T extends AbstractWidget> T addChild(T element) {
		return addChild(element, GuiAlign.RIGHT, 0);
	}
	
	protected <T extends AbstractWidget> T addChild(T element, int xOffset) {
		return addChild(element, GuiAlign.RIGHT, xOffset);
	}
	
	protected <T extends AbstractWidget> T addChild(T element, GuiAlign align, int xOffset) {
		listeners.add(element);
		mappedListeners.add(new AbstractMap.SimpleEntry<>(element, new AlignOffset(align, -xOffset)));
		if(owner != null && element instanceof IOwnable) {
			((IOwnable)element).setOwner(owner);
		}
		return element;
	}
	
	@Override
	public void init() {
		super.init();
		if(createResetButtons(value)) {
			if(isArray()) {
				setReset = addChild(new CarbonIconButton(0, 0, 18, 18, Icon.DELETE, Component.empty(), this::onDeleted).setIconOnly(), -31);
				setReset.active = isReset();
				moveDown = new CarbonHoverIconButton(0, 0, 15, 8, new IconInfo(0, -3, 16, 16), Icon.MOVE_DOWN, Icon.MOVE_DOWN_HOVERED, this::onMoveDown);
				listeners.add(moveDown);
				moveUp = new CarbonHoverIconButton(0, 0, 15, 8, new IconInfo(0, -3, 16, 16), Icon.MOVE_UP, Icon.MOVE_UP_HOVERED, this::onMoveUp);
				listeners.add(moveUp);
			}
			else {
				setReset = addChild(new CarbonIconButton(0, 0, 18, 18, Icon.REVERT, Component.empty(), this::onReset).setIconOnly(), -21);
				setDefault = addChild(new CarbonIconButton(0, 0, 18, 18, Icon.SET_DEFAULT, Component.empty(), this::onDefault).setIconOnly(), -40);
				setReset.active = isReset();
				setDefault.active = !isDefault();
			}
		}
		if(owner != null) {
			for(GuiEventListener entry : listeners) {
				if(entry instanceof IOwnable) {
					((IOwnable)entry).setOwner(owner);
				}
			}
		}
	}
	
	@Override
	public void tick() {
		super.tick();
		if(setReset != null) {
			setReset.active = isReset();
		}
		if(setDefault != null) {
			setDefault.active = !isDefault();
		}
	}
	
	@Override
	public void render(PoseStack poseStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks) {
		if(renderName() && !isArray()) {
			renderName(poseStack, left, top+6, isChanged(), width-115);
			if(!isCompound()) {
				if(node.requiresReload()) {
					GuiUtils.drawTextureRegion(poseStack, left-16, top+(height/2)-6, 12, 12, Icon.RELOAD, 16, 16);
					if(mouseX >= left-16 && mouseX <= left-4 && mouseY >= top && mouseY <= top+height && owner.isInsideList(mouseX, mouseY)) {
						owner.addTooltips(RELOAD);
					}
				}
				if(node.requiresRestart()) {
					GuiUtils.drawTextureRegion(poseStack, left-16, top+(height/2)-6, 12, 12, Icon.RESTART, 16, 16);
					if(mouseX >= left-16 && mouseX <= left-4 && mouseY >= top && mouseY <= top+height && owner.isInsideList(mouseX, mouseY)) {
						owner.addTooltips(RESTART);
					}
				}
			}
		}
		int maxX = Integer.MAX_VALUE;
		if(renderChildren()) {
			if(isArray()) {
				moveUp.x = left + width - 16;
				moveUp.y = top;
				moveUp.visible = canMoveUp();
				moveUp.render(poseStack, mouseX, mouseY, partialTicks);
				moveDown.x = left + width - 16;
				moveDown.y = top + 10;
				moveDown.visible = canMoveDown();
				moveDown.render(poseStack, mouseX, mouseY, partialTicks);
				if(moveDown.visible || moveUp.visible) {
					left -= 8;
				}
			}
			for(Map.Entry<AbstractWidget, AlignOffset> entry : mappedListeners) {
				AbstractWidget widget = entry.getKey();
				AlignOffset offset = entry.getValue();
				widget.x = offset.align.align(left, width, widget.getWidth()) + offset.offset;
				widget.y = top;
				widget.render(poseStack, mouseX, mouseY, partialTicks);
				maxX = Math.min(maxX, widget.x);
			}
		}
		maxX = getMaxX(maxX);
		if(isArray()) {
			Component comp = Component.literal(indexOf()+":");
			renderText(poseStack, comp, maxX-15-font.width(comp), top+6, -1);
		}
		if(mouseY >= top && mouseY <= top + height && mouseX >= left && mouseX <= maxX-2 && owner.isInsideList(mouseX, mouseY)) {
			owner.addTooltips(node.getTooltip());
		}
		if(isArray()) {
			if(setReset.isHoveredOrFocused() && owner.isInsideList(mouseX, mouseY)) {
				owner.addTooltips(DELETE);
			}
		}
		else {
			if(setReset.isHoveredOrFocused() && owner.isInsideList(mouseX, mouseY)) {
				owner.addTooltips(REVERT);
			}
			if(setDefault.isHoveredOrFocused() && owner.isInsideList(mouseX, mouseY)) {
				owner.addTooltips(DEFAULT);
			}
		}
	}
	
	protected int getMaxX(int prevMaxX) {
		return prevMaxX;
	}
	
	protected boolean isArray() {
		return array != null;
	}
	
	protected boolean isCompound() {
		return value != null && value.isCompoundNode();
	}
	
	protected void onMoveDown(CarbonHoverIconButton button) {
		if(!isArray()) return;
		array.moveDown(indexOf());
		owner.updateInformation();
	}
	
	protected void onMoveUp(CarbonHoverIconButton button) {
		if(!isArray()) return;
		array.moveUp(indexOf());
		owner.updateInformation();		
	}
	
	protected boolean canMoveUp() {
		return indexOf() > 0;
	}
	
	protected boolean canMoveDown() {
		return indexOf() < array.size() - 1;
	}
	
	protected boolean renderName() {
		return true;
	}
	
	protected boolean renderChildren() {
		return true;
	}
	
	protected boolean createResetButtons(IValueNode value) {
		return value != null;
	}
	
	protected int indexOf() {
		return array.indexOf(value);
	}
	
	protected boolean isReset() {
		return isArray() || value.isChanged();
	}
	
	public boolean isChanged() {
		return value.isChanged();
	}
	
	public boolean isDefault() {
		return value.isDefault();
	}
	
	protected void onDeleted(CarbonIconButton button) {
		if(!isArray()) return;
		owner.removeEntry(this);
	}
	
	protected void onReset(CarbonIconButton button) {
		value.setPrevious();
		updateValues();
	}
	
	protected void onDefault(CarbonIconButton button) {
		if(value == null) return;
		value.setDefault();
		updateValues();
	}
	
	@Override
	public void updateValues() {
	}
	
	@Override
	public List<? extends GuiEventListener> children() {
		return listeners;
	}
	
	public static class AlignOffset {
		GuiAlign align;
		int offset;
		
		public AlignOffset(GuiAlign align, int offset) {
			this.align = align;
			this.offset = offset;
		}
	}
	
	public static enum GuiAlign {
		LEFT,
		CENTER,
		RIGHT;
		
		public int align(int left, int bounds, int width) {
			switch(this) {
				case LEFT: return left;
				case CENTER: return left + (bounds/2) - (width / 2);
				case RIGHT: return left + bounds - width - 42;
				default: return left;
			}
		}
	}
}
