package carbonconfiglib.impl.entries;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import carbonconfiglib.CarbonConfig;
import carbonconfiglib.api.buffer.IReadBuffer;
import carbonconfiglib.api.buffer.IWriteBuffer;
import carbonconfiglib.config.ConfigEntry.CollectionConfigEntry;
import carbonconfiglib.config.ConfigEntry.IArrayConfig;
import carbonconfiglib.config.ConfigSection;
import carbonconfiglib.utils.Helpers;
import carbonconfiglib.utils.IEntryDataType;
import carbonconfiglib.utils.IEntryDataType.SimpleDataType;
import carbonconfiglib.utils.MultilinePolicy;
import carbonconfiglib.utils.ParseResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

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
public class RegistryKeyValue extends CollectionConfigEntry<ResourceLocation, Set<ResourceLocation>> implements IArrayConfig
{
	ForgeRegistry<?> registry;
	Class<?> clz;
	Predicate<ResourceLocation> filter;
	
	public RegistryKeyValue(String key, IForgeRegistry<?> registry, Class<?> clz, Set<ResourceLocation> defaultValue, Predicate<ResourceLocation> filter, String... comment) {
		super(key, defaultValue, comment);
		this.registry = (ForgeRegistry<?>)registry;
		this.clz = clz;
		this.filter = filter;
	}
	
	public static <E> Builder<E> builder(String key, Class<E> clz) {
		return new Builder<>(key, clz);
	}
	
	private void loadSuggestions() {
		for(ResourceLocation entry : registry.getKeys()) {
			if(filter == null || filter.test(entry)) addSuggestion(entry.toString(), entry, clz);
		}
	}
	
	@Override
	protected RegistryKeyValue copy() {
		return new RegistryKeyValue(getKey(), registry, clz, getDefault(), filter, getComment());
	}
	
	@Override
	protected String serializedValue(MultilinePolicy policy, Set<ResourceLocation> value) {
		String[] result = new String[value.size()];
		int i = 0;
		for(ResourceLocation entry : value) {
			result[i] = entry.toString();
		}
		return serializeArray(policy, result);
	}
	
	@Override
	public ParseResult<Set<ResourceLocation>> parseValue(String value) {
		String[] values = Helpers.splitArray(value, ",");
		Set<ResourceLocation> result = new ObjectLinkedOpenHashSet<>();
		for(int i = 0,m=values.length;i<m;i++) {
			ResourceLocation location = ResourceLocation.tryParse(values[i]);
			if(location == null || (filter != null && !filter.test(location))) continue;
			result.add(location);
		}
		return ParseResult.success(result);
	}
	
	@Override
	public ParseResult<Boolean> canSet(Set<ResourceLocation> value) {
		ParseResult<Boolean> result = super.canSet(value);
		if(result.hasError()) return result;
		for(ResourceLocation entry : value) {
			if(!registry.containsKey(entry)) return ParseResult.partial(false, NoSuchElementException::new, "Value ["+entry+"] doesn't exist in the registry");
			if(filter != null && !filter.test(entry)) return ParseResult.partial(false, IllegalArgumentException::new, "Value ["+entry+"] isn't allowed");
		}
		return ParseResult.success(true);
	}
	
	@Override
	public List<String> getEntries() {
		List<String> result = new ObjectArrayList<>();
		for(ResourceLocation entry : getValue()) {
			result.add(entry.toString());
		}
		return result;
	}
	
	@Override
	public List<String> getDefaults() {
		List<String> result = new ObjectArrayList<>();
		for(ResourceLocation entry : getDefault()) {
			result.add(entry.toString());
		}
		return result;
	}
	
	@Override
	public ParseResult<Boolean> canSetArray(List<String> entries) {
		if(entries == null) return ParseResult.partial(false, NullPointerException::new, "Value isn't allowed to be null");
		for(int i = 0,m=entries.size();i<m;i++) {
			ResourceLocation result = ResourceLocation.tryParse(entries.get(i));
			if(result == null || !registry.containsKey(result)) return ParseResult.partial(false, NoSuchElementException::new, "Value ["+entries.get(i)+"] doesn't exist in the registry");
			if(filter != null && !filter.test(result)) return ParseResult.partial(false, IllegalArgumentException::new, "Value ["+entries.get(i)+"] isn't allowed");
		}
		return ParseResult.success(true);
	}
	
	@Override
	public void setArray(List<String> entries) {
		StringJoiner joiner = new StringJoiner(",");
		for(String s : entries) {
			joiner.add(s);
		}
		deserializeValue(joiner.toString());
	}
	
	@Override
	public IEntryDataType getDataType() {
		return SimpleDataType.ofVariant(clz);
	}
	
	@Override
	public char getPrefix() {
		return 'K';
	}
	
	@Override
	public String getLimitations() {
		return "";
	}
	
	@Override
	public void serialize(IWriteBuffer buffer) {
		Set<ResourceLocation> value = getValue();
		buffer.writeVarInt(value.size());
		for(ResourceLocation entry : value) {
			buffer.writeString(entry.toString());
		}
	}
	
	@Override
	protected void deserializeValue(IReadBuffer buffer) {
		Set<ResourceLocation> result = new ObjectLinkedOpenHashSet<>();
		int size = buffer.readVarInt();
		for(int i = 0;i<size;i++) {
			ResourceLocation entry = ResourceLocation.tryParse(buffer.readString());
			if(entry != null) {
				result.add(entry);
			}
		}
	}
	
	@Override
	protected Set<ResourceLocation> create(ResourceLocation value) {
		return ObjectSets.singleton(value);
	}
	
	public static class Builder<E> {
		Class<E> clz;
		String key;
		Set<E> unparsedValues = new ObjectLinkedOpenHashSet<>();
		Set<ResourceLocation> values = new ObjectLinkedOpenHashSet<>();
		Predicate<ResourceLocation> filter;
		String[] comments;
		
		private Builder(String key, Class<E> clz) {
			this.key = key;
			this.clz = clz;
		}
		
		@SuppressWarnings("unchecked")
		public Builder<E> addDirectDefault(E... elements) {
			unparsedValues.addAll(ObjectArrayList.wrap(elements));
			return this;
		}
		
		public Builder<E> addDirectDefaults(Collection<E> elements) {
			unparsedValues.addAll(elements);
			return this;
		}
		
		public Builder<E> addDefault(ResourceLocation... elements) {
			values.addAll(ObjectArrayList.wrap(elements));
			return this;
		}
		
		public Builder<E> addDefaults(Collection<ResourceLocation> elements) {
			values.addAll(elements);
			return this;
		}
		
		public Builder<E> withFilter(Predicate<ResourceLocation> filter) {
			this.filter = filter;
			return this;
		}
		
		public Builder<E> withComment(String... comments) {
			this.comments = comments;
			return this;
		}
		
		private void parseValues(IForgeRegistry<E> registry) {
			for(E entry : unparsedValues) {
				ResourceLocation location = registry.getKey(entry);
				if(location != null) values.add(location);
			}
			unparsedValues.clear();
		}
		
		public RegistryKeyValue build(IForgeRegistry<E> registry) {
			parseValues(registry);
			RegistryKeyValue result = new RegistryKeyValue(key, registry, clz, values, filter, comments);
			CarbonConfig.runAfterRegistries(result::loadSuggestions);
			return result;
		}
		
		public RegistryKeyValue build(IForgeRegistry<E> registry, ConfigSection section) {
			parseValues(registry);
			RegistryKeyValue result = new RegistryKeyValue(key, registry, clz, values, filter, comments);
			CarbonConfig.runAfterRegistries(result::loadSuggestions);
			section.add(result);
			return result;
		}
	}

}
