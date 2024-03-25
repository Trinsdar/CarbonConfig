package carbonconfiglib.impl.entries;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import carbonconfiglib.api.ISuggestionProvider.Suggestion;
import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.config.ConfigEntry.BasicConfigEntry;
import carbonconfiglib.impl.entries.ColorValue.ColorWrapper;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.structure.IStructuredData;
import carbonconfiglib.utils.structure.IStructuredData.EntryDataType;
import carbonconfiglib.utils.structure.IStructuredData.SimpleData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import speiger.src.collections.objects.lists.ObjectArrayList;

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
public class ColorValue extends BasicConfigEntry<ColorWrapper>
{
	public ColorValue(String key, int defaultValue, String... comment) {
		super(key, new ColorWrapper(defaultValue), comment);
	}
	
	@Override
	protected ColorValue copy() {
		return new ColorValue(getKey(), get(), getComment());
	}
	
	public ColorValue addMCChatFormatSuggestions() {
		addSuggestionProvider(this::addMCColorPalette);
		return this;
	}
	
	private void addMCColorPalette(Consumer<Suggestion> result, Predicate<Suggestion> filter) {
		for(ChatFormatting formatting : ChatFormatting.values()) {
			if(!formatting.isColor()) continue;
			Suggestion value = Suggestion.namedTypeValue(Helpers.firstLetterUppercase(formatting.getName()), ColorWrapper.serializeRGB(formatting.getColor()), ColorWrapper.class);
			if(filter.test(value)) result.accept(value);
		}
	}
	
	public final ColorValue addSuggestions(int... values) {
		List<Suggestion> suggestions = new ObjectArrayList<>();
		for(int value : values) {
			suggestions.add(Suggestion.namedTypeValue(Long.toHexString(0xFF00000000L | value).substring(2), serializedValue(MultilinePolicy.DISABLED, new ColorWrapper(value)), ColorWrapper.class));
		}
		return this;
	}
	
	public final ColorValue addSuggestion(String name, int value) {
		return addSingleSuggestion(Suggestion.namedTypeValue(name, serializedValue(MultilinePolicy.DISABLED, new ColorWrapper(value)), ColorWrapper.class));
	}
	
	@Override
	public ParseResult<ColorWrapper> parseValue(String value) {
		ParseResult<Integer> result = ColorWrapper.parseInt(value);
		return result.hasError() ? result.onlyError() : ParseResult.success(new ColorWrapper(result.getValue()));
	}
	
	@Override
	public IStructuredData getDataType() {
		return SimpleData.variant(EntryDataType.INTEGER, ColorWrapper.class);
	}
	
	public int get() {
		return getValue().getColor();
	}
	
	public int getRGB() {
		return getValue().getColor() & 0xFFFFFF;
	}

	public int getRGBA() {
		return getValue().getColor() & 0xFFFFFFFF;
	}
	
	public TextColor getMCColor() {
		return TextColor.fromRgb(getRGB());
	}
	
	public Style getMCStyle() {
		return Style.EMPTY.withColor(getMCColor());
	}
	
	public String toHex() {
		return ColorWrapper.serialize(getValue().getColor());
	}
	
	public String toRGBHex() {
		return ColorWrapper.serializeRGB(getValue().getColor() & 0xFFFFFF);
	}
	
	public String toRGBAHex() {
		return ColorWrapper.serialize(getValue().getColor() & 0xFFFFFFFF);
	}
	
	protected String serializedValue(MultilinePolicy policy, ColorWrapper value) {
		return ColorWrapper.serialize(value.getColor());
	}
	
	@Override
	public char getPrefix() {
		return 'C';
	}
	
	@Override
	public String getLimitations() {
		return "";
	}
	
	@Override
	public void serialize(IWriteBuffer buffer) {
		buffer.writeInt(get());
	}
	
	@Override
	protected void deserializeValue(IReadBuffer buffer) {
		set(new ColorWrapper(buffer.readInt()));
	}
	
	public static ParseResult<ColorValue> parse(String key, String value, String... comment) {
		ParseResult<Integer> result = ColorWrapper.parseInt(value);
		if (result.hasError()) return result.withDefault(new ColorValue(key, 0, comment));
		return ParseResult.success(new ColorValue(key, result.getValue(), comment));
	}
	
	public static class ColorWrapper extends Number {
		private static final long serialVersionUID = -6737187197596158253L;
		int color;
		
		public ColorWrapper(int color) {
			this.color = color;
		}
		
		public int getColor() {
			return color;
		}
		
		public int intValue() { return color; }
		public long longValue() { return (long)color; }
		public float floatValue() { return (float)color; }
		public double doubleValue() { return (double)color; }
		
		public String serialize() {
			return serialize(color);
		}
		
		public static ParseResult<ColorWrapper> parse(String value) {
			try { return ParseResult.success(new ColorWrapper(Long.decode(value).intValue())); }
			catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Colour"); }
		}
		
		
		public static ParseResult<Integer> parseInt(String value) {
			try { return ParseResult.success(Long.decode(value).intValue()); }
			catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Colour"); }
		}
		
		public static String serializeRGB(long color) {
			return "0x"+(Long.toHexString(0xFF000000L | (color & 0xFFFFFFL)).substring(2));
		}
		
		public static String serialize(long color) {
			return "0x"+(Long.toHexString(0xFF00000000L | (color & 0xFFFFFFFFL)).substring(2));
		}
	}
}
