/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import java.util.HashMap;
import java.util.Map;

import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.MappingException;
import com.google.code.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class MapOfValuesConverter extends TypeConverter {
	
	private final ConverterChain chain;
	
	public MapOfValuesConverter(ConverterChain chain) {
		this.chain = chain;
	}
	
	@Override
	boolean canHandle(Class c, MappedField optionalExtraInfo) {
		return ReflectionUtils.isMap(c);
	}
	
	@Override
	Object decode(Class targetClass, Object fromDBObject, MappedField f) throws MappingException {
		Map<Object, Object> map = (Map<Object, Object>) fromDBObject;
		Map values = (Map) ReflectionUtils.newInstance(f.getCTor(), HashMap.class);
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object objKey = chain.decode(f.getMapKeyType(), entry.getKey());
			values.put(objKey, chain.decode(f.getSubType(), entry.getValue()));
		}
		return values;
	}
	
	@Override
	Object encode(Object value, MappedField f) {
		if (value == null)
			return null;
		
		Map<Object, Object> map = (Map<Object, Object>) value;
		if ((map != null) && (map.size() > 0)) {
			Map mapForDb = new HashMap();
			for (Map.Entry<Object, Object> entry : map.entrySet()) {
				String strKey = chain.encode(entry.getKey()).toString();
				mapForDb.put(strKey, chain.encode(entry.getValue()));
			}
			return mapForDb;
		}
		return null;
	}
	
}