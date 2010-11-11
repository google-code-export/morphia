package com.google.code.morphia.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.code.morphia.logging.jdk.JDKLoggerFactory;

public class MorphiaLoggerFactory {
	private static LogrFactory loggerFactory = null;
	
	private static List<String> factories = Arrays.asList(JDKLoggerFactory.class.getName(),
	"com.google.code.morphia.logging.slf4j.SLF4JLoggerImplFactory");
	
	private static synchronized void init() {
		if (MorphiaLoggerFactory.loggerFactory == null) {
			chooseLoggerFactory();
		}
	}
	
	private static void chooseLoggerFactory() {
		Collections.reverse(MorphiaLoggerFactory.factories);
		for (String f : MorphiaLoggerFactory.factories) {
			MorphiaLoggerFactory.loggerFactory = newInstance(f);
            if (MorphiaLoggerFactory.loggerFactory != null) {
                loggerFactory.get(MorphiaLoggerFactory.class).info(
                        "LoggerImplFactory set to " + loggerFactory.getClass().getName());
                return;
            }
		}
		throw new IllegalStateException("Cannot instanciate any MorphiaLoggerFactory");
	}
	
	private static LogrFactory newInstance(String f) {
		try {
			Class c = Class.forName(f);
			return (LogrFactory) c.newInstance();
		} catch (Throwable ignore) {
		}
		return null;
	}
	
	public static final Logr get(Class<?> c) {
		init();
		return MorphiaLoggerFactory.loggerFactory.get(c);
	}

	/** Register a LoggerFactory; last one registered is used.**/
	public static void registerLogger(Class<? extends LogrFactory> factoryClass) {
		if (MorphiaLoggerFactory.loggerFactory == null)
			MorphiaLoggerFactory.factories.add(0,factoryClass.getName());
		else
			throw new IllegalStateException("LoggerImplFactory must be registered before logging is initialized.");
	}
}
