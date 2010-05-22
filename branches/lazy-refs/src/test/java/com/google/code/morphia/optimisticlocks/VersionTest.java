/**
 * 
 */
package com.google.code.morphia.optimisticlocks;

import java.util.ConcurrentModificationException;

import com.google.code.morphia.annotations.PreSave;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;
import com.mongodb.DBObject;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionTest extends JUnit3TestBase {
	

	public static class ALongPrimitive extends AbstractMongoEntity {
		@Version
		long hubba;
		
		String text;
	}
	
	public static class ALong extends AbstractMongoEntity {
		@Version
		Long v;
		
		String text;
	}

	public void testVersions() throws Exception {
		ALongPrimitive a = new ALongPrimitive();
		assertEquals(0, a.hubba);
		ds.save(a);
		assertTrue(a.hubba > 0);
		long version1 = a.hubba;
		
		ds.save(a);
		assertTrue(a.hubba > 0);
		long version2 = a.hubba;
		
		assertFalse(version1 == version2);
	}
	
	public void testConcurrentModDetection() throws Exception {
		morphia.map(ALongPrimitive.class);

		ALongPrimitive a = new ALongPrimitive();
		assertEquals(0, a.hubba);
		ds.save(a);
		final ALongPrimitive a1 = a;
		
		ALongPrimitive a2 = ds.get(a);
		ds.save(a2);
		

		new AssertedFailure(ConcurrentModificationException.class) {
			public void thisMustFail() throws Throwable {
				ds.save(a1);
			}
		};
	}

	public void testConcurrentModDetectionLong() throws Exception {
		ALong a = new ALong();
		assertEquals(null, (Long) a.v);
		ds.save(a);
		final ALong a1 = a;
		
		ALong a2 = ds.get(a);
		ds.save(a2);
		
		new AssertedFailure(ConcurrentModificationException.class) {
			public void thisMustFail() throws Throwable {
				ds.save(a1);
			}
		};
	}

}
