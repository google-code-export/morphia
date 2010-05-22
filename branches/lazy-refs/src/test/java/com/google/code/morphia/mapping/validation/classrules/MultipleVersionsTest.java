/**
 * 
 */
package com.google.code.morphia.mapping.validation.classrules;

import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.mapping.lazy.JUnit3TestBase;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.google.code.morphia.testutil.AssertedFailure;
import com.google.code.morphia.utils.AbstractMongoEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class MultipleVersionsTest extends JUnit3TestBase {
	
	public static class Fail1 extends AbstractMongoEntity {
		@Version
		long v1;
		@Version
		long v2;
	}

	public static class OK1 extends AbstractMongoEntity {
		@Version
		long v1;
	}

	public void testCheck() {
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(Fail1.class);
			}
		};
		morphia.map(OK1.class);
	}
	
}