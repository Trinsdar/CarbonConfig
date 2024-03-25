package carbonconfiglib.gui.impl.carbon;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import carbonconfiglib.api.IReloadMode;
import carbonconfiglib.api.ISuggestionProvider.Suggestion;
import carbonconfiglib.gui.api.DataType;
import carbonconfiglib.gui.api.ICompoundNode;
import carbonconfiglib.gui.api.IConfigNode;
import carbonconfiglib.gui.api.INode;
import carbonconfiglib.impl.ReloadMode;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.ParseResult;
import carbonconfiglib.utils.structure.IStructuredData;
import carbonconfiglib.utils.structure.IStructuredData.StructureType;
import carbonconfiglib.utils.structure.StructureCompound.CompoundData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;
import speiger.src.collections.utils.Stack;

public class CarbonCompound implements ICompoundNode, IValueActions
{
	IReloadMode mode;
	CompoundData data;
	Component name;
	Component tooltip;
	Function<String, ParseResult<Boolean>> isValid;
	Supplier<List<Suggestion>> suggestions;
	Consumer<String> saveAction;
	
	Map<String, String> validationTemp = Object2ObjectMap.builder().linkedMap();
	List<IValueActions> values = new ObjectArrayList<>();
	Stack<Map<String, String>> previous = new ObjectArrayList<>();
	Map<String, String> current = Object2ObjectMap.builder().linkedMap();
	Map<String, String> defaultValue = Object2ObjectMap.builder().linkedMap();
	
	public CarbonCompound(IReloadMode mode, CompoundData data, Component name, Component tooltip, String value, String defaultValue, Function<String, ParseResult<Boolean>> isValid, Supplier<List<Suggestion>> suggestions, Consumer<String> saveAction) {
		this.mode = mode;
		this.data = data;
		this.name = name;
		this.current.putAll(Helpers.splitArguments(Helpers.splitCompound(value), data.getKeys(), true));
		this.defaultValue.putAll(Helpers.splitArguments(Helpers.splitCompound(defaultValue), data.getKeys(), true));
		this.previous.push(Object2ObjectMap.builder().linkedMap(current));
		this.tooltip = tooltip;
		this.isValid = isValid;
		this.suggestions = suggestions;
		this.saveAction = saveAction;
		reload();
	}

	private void reload() {
		values.clear();
		Map<String, IStructuredData> structure = data.getFormat();
		for(Map.Entry<String, String> entry : current.entrySet()) {
			String key = entry.getKey();
			values.add(addEntry(Helpers.removeLayer(entry.getValue(), 0), Helpers.removeLayer(defaultValue.getOrDefault(key, ""), 0), structure.get(key), key));
		}
	}
	
	protected IValueActions addEntry(String value, String defaultValue, IStructuredData type, String key) {
		switch(type.getDataType()) {
			case COMPOUND: return new CarbonCompound(mode, type.asCompound(), IConfigNode.createLabel(key), createTooltip(key), value, defaultValue, T -> isValid(key, T), () -> data.getSuggestions(key, this::isSuggestionValid), T -> save(key, T));
			case LIST: return new CarbonArray(mode, type.asList(), IConfigNode.createLabel(key), createTooltip(key), value, defaultValue, T -> isValid(key, T), () -> data.getSuggestions(key, this::isSuggestionValid), T -> save(key, T));
			case SIMPLE: return new CarbonValue(mode, IConfigNode.createLabel(key), createTooltip(key), DataType.bySimple(type.asSimple()), data.isForcedSuggestion(key), () -> data.getSuggestions(key, this::isSuggestionValid), value, defaultValue, T -> isValid(key, T), T -> save(key, T));
			default: return null;
		}
	}
	
	private Map<String, String> getPrev() {
		return previous.top();
	}
	
	protected void save(String key, String value) {
		current.put(key, value);
	}
	
	protected boolean isSuggestionValid(String key, String value) {
		return isValid(key, value).getValue();
	}
	
	protected ParseResult<Boolean> isValid(String key, String value) {
		validationTemp.clear();
		validationTemp.putAll(current);
		validationTemp.put(key, value);
		return isValid.apply(Helpers.mergeCompound(current, false, 0));
	}
	
	private Component createTooltip(String key) {
		MutableComponent comp = Component.empty();
		comp.append(Component.literal(key).withStyle(ChatFormatting.YELLOW));
		String[] array = data.getComments(key);
		if(array != null && array.length > 0) {
			for(int i = 0;i<array.length;comp.append("\n").append(array[i++]).withStyle(ChatFormatting.GRAY));
		}
		return comp;
	}
	
	@Override
	public void save() { saveAction.accept(Helpers.mergeCompound(current, false, 0)); }
	@Override
	public boolean isDefault() { return Objects.equals(defaultValue, current); }
	@Override
	public boolean isChanged() { return !Objects.equals(getPrev(), current); }
	@Override
	public void setDefault() {
		current.clear();
		current.putAll(defaultValue);
		reload();
	}
	
	@Override
	public void setPrevious() {
		current.clear();
		current.putAll(getPrev());
		if(previous.size() > 1) previous.pop();
		reload();
	}
	
	@Override
	public void createTemp() {
		previous.push(Object2ObjectMap.builder().linkedMap(current));
		reload();
	}
	
	@Override
	public void apply() {
		if(previous.size() > 1) previous.pop();
		for(int i = 0,m=values.size();i<m;i++) {
			values.get(i).save();
		}
	}
	
	@Override
	public boolean isValid() {
		return isValid.apply(get()).getValue();
	}
	
	@Override
	public String get() { return Helpers.mergeCompound(current, false, 0); }
	
	@Override
	public void set(String value) {
		current.clear();
		current.putAll(Helpers.splitArguments(Helpers.splitCompound(value), data.getKeys(), true));
	}

	@Override
	public List<? extends INode> getValues() { return values; }
	@Override
	public StructureType getNodeType() { return StructureType.COMPOUND; }
	@Override
	public boolean requiresRestart() { return mode == ReloadMode.GAME; }
	@Override
	public boolean requiresReload() { return mode == ReloadMode.WORLD; }
	@Override
	public Component getName() { return name; }
	@Override
	public Component getTooltip() { return tooltip; }
}
