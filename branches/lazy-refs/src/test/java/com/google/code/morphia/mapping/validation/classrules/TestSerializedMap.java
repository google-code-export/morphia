/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Serialized;
import com.google.code.morphia.utils.AbstractEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * 
 */
public class TestSerializedMap extends TestBase {
	
	public static class Map1 extends AbstractEntity {
		@Serialized(disableCompression = false)
		Map<Integer, Foo> shouldBeOk = new HashMap();
		
	}
	
	public static class Map2 extends AbstractEntity {
		@Serialized(disableCompression = true)
		Map<Integer, Foo> shouldBeOk = new HashMap();
		
	}
	
	public static class Foo implements Serializable {
		
		final String id;
		
		public Foo(String id) {
			this.id = id;
		}
	}
	
	@Test
	public void testSerialization() throws Exception {
		Map1 map1 = new Map1();
		map1.shouldBeOk.put(3, new Foo("peter"));
		map1.shouldBeOk.put(27, new Foo("paul"));
		
		ds.save(map1);
		map1 = ds.get(map1);
		
		Assert.assertEquals("peter", map1.shouldBeOk.get(3).id);
		Assert.assertEquals("paul", map1.shouldBeOk.get(27).id);
		
	}
	
	@Test
	public void testSerialization2() throws Exception {
		Map2 map2 = new Map2();
		map2.shouldBeOk.put(3, new Foo("peter"));
		map2.shouldBeOk.put(27, new Foo("paul"));
		
		ds.save(map2);
		map2 = ds.get(map2);
		
		Assert.assertEquals("peter", map2.shouldBeOk.get(3).id);
		Assert.assertEquals("paul", map2.shouldBeOk.get(27).id);
		
	}
}
