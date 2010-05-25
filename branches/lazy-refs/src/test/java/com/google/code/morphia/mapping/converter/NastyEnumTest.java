/**
 * 
 */
package com.google.code.morphia.mapping.converter;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class NastyEnumTest extends TestBase {
	public enum NastyEnum {
		A {
			@Override
			public String toString() {
				return "Never use toString for other purposes than debugging";
			}
		},
		B {
			public String toString() {
				return "Never use toString for other purposes than debugging ";
			}
		}
	}
	
	public static class NastyEnumEntity extends AbstractMongoEntity {
		NastyEnum e1 = NastyEnum.A;
		NastyEnum e2 = NastyEnum.B;
		NastyEnum e3 = null;
	}
	
	@Test
	public void testNastyEnumPerisistence() throws Exception {
		NastyEnumEntity n = new NastyEnumEntity();
		ds.save(n);
		n = ds.get(n);
		Assert.assertSame(NastyEnum.A, n.e1);
		Assert.assertSame(NastyEnum.B, n.e2);
		Assert.assertNull(n.e3);
	}
}
