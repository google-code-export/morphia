/**
 * 
 */
package com.google.code.morphia.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.code.morphia.DatastoreImpl;
import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.mapping.cache.EntityCache;
import com.google.code.morphia.mapping.lazy.LazyFeatureDependencies;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReference;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceList;
import com.google.code.morphia.mapping.lazy.proxy.ProxiedEntityReferenceMap;
import com.google.code.morphia.mapping.lazy.proxy.ProxyHelper;
import com.google.code.morphia.utils.ReflectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

@SuppressWarnings({ "unchecked", "rawtypes" })
class ReferenceMapper implements CustomMapper {
	public static final Logr log = MorphiaLoggerFactory.get(ReferenceMapper.class);
	
	public void toDBObject(Object entity, MappedField mf, BasicDBObject dbObject, Map<Object, DBObject> involvedObjects, Mapper mapr) {
		String name = mf.getNameToStore();
		
		Object fieldValue = mf.getFieldValue(entity);
		
		if (fieldValue == null && !mapr.getOptions().storeNulls)
			return;
		
		if (mf.isMap()) {
			writeMap(mf, dbObject, name, fieldValue, mapr);
		} else if (mf.isMultipleValues()) {
			writeCollection(mf, dbObject, name, fieldValue, mapr);
		} else {
			writeSingle(dbObject, name, fieldValue, mapr);
		}
		
	}
	
	private void writeSingle(BasicDBObject dbObject, String name, Object fieldValue, Mapper mapr) {
		DBRef dbrefFromKey = getKey(fieldValue, mapr).toRef(mapr);
		dbObject.put(name, dbrefFromKey);
	}
	
	private void writeCollection(MappedField mf, BasicDBObject dbObject, String name, Object fieldValue, Mapper mapr) {
		if (fieldValue != null) {
			List values = new ArrayList();
			
			if (ProxyHelper.isProxy(fieldValue) && ProxyHelper.isUnFetched(fieldValue)) {
				ProxiedEntityReferenceList p = (ProxiedEntityReferenceList) fieldValue;
				List<Key<?>> getKeysAsList = p.__getKeysAsList();
				for (Key<?> key : getKeysAsList) {
					values.add(key.toRef(mapr));
				}
			} else {
				
				if (mf.getType().isArray()) {
					for (Object o : (Object[]) fieldValue) {
						values.add(getKey(o, mapr).toRef(mapr));
					}
				} else {
					for (Object o : (Iterable) fieldValue) {
						values.add(getKey(o, mapr).toRef(mapr));
					}
				}
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}
	
	private void writeMap(final MappedField mf, final BasicDBObject dbObject, String name, Object fieldValue,
			Mapper mapr) {
		Map<Object, Object> map = (Map<Object, Object>) fieldValue;
		if ((map != null)) {
			Map values = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
			
			if (ProxyHelper.isProxy(map) && ProxyHelper.isUnFetched(map)) {
				ProxiedEntityReferenceMap proxy = (ProxiedEntityReferenceMap) map;
				
				Map<String, Key<?>> refMap = proxy.__getReferenceMap();
				for (Map.Entry<String, Key<?>> entry : refMap.entrySet()) {
					String strKey = entry.getKey();
					values.put(strKey, entry.getValue().toRef(mapr));
				}
			} else {
				for (Map.Entry<Object, Object> entry : map.entrySet()) {
					String strKey = mapr.converters.encode(entry.getKey()).toString();
					values.put(strKey, getKey(entry.getValue(), mapr).toRef(mapr));
				}
			}
			if (values.size() > 0 || mapr.getOptions().storeEmpties) {
				dbObject.put(name, values);
			}
		}
	}
	
	private Key<?> getKey(final Object entity, Mapper mapr) {
		try {
			if (entity instanceof ProxiedEntityReference) {
				ProxiedEntityReference proxy = (ProxiedEntityReference) entity;
				return proxy.__getKey();
			}
			MappedClass mappedClass = mapr.getMappedClass(entity);
			Object id = mappedClass.getIdField().get(entity);
			if (id == null)
				throw new MappingException("@Id field cannot be null!");
			Key key = new Key(mappedClass.getCollectionName(), id);
			return key;
		} catch (IllegalAccessException iae) {
			throw new RuntimeException(iae);
		}
	}
	
	/**
	 * @deprecated use void fromDBObject(final DBObject dbObject, final
	 *             MappedField mf, final Object entity, EntityCache cache)
	 *             instead.
	 */
	@Deprecated
	void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, Mapper mapr) {
		fromDBObject(dbObject, mf, entity, mapr.createEntityCache(), mapr);
	}

	public void fromDBObject(final DBObject dbObject, final MappedField mf, final Object entity, EntityCache cache, Mapper mapr) {
		Class fieldType = mf.getType();
		
		Reference refAnn = mf.getAnnotation(Reference.class);
		if (mf.isMap()) {
			readMap(dbObject, mf, entity, refAnn, cache, mapr);
		} else if (mf.isMultipleValues()) {
			readCollection(dbObject, mf, entity, refAnn, cache, mapr);
		} else {
			readSingle(dbObject, mf, entity, fieldType, refAnn, cache, mapr);
		}
		
	}
	
	private void readSingle(final DBObject dbObject, final MappedField mf, final Object entity, Class fieldType,
			Reference refAnn, EntityCache cache, Mapper mapr) {
		Class referenceObjClass = fieldType;

		DBRef dbRef = (DBRef) mf.getDbObjectValue(dbObject);
		if (dbRef != null) {
			
			Object resolvedObject = null;
			if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
				if (exists(referenceObjClass, dbRef, cache, mapr)) {
					resolvedObject = createOrReuseProxy(referenceObjClass, dbRef, cache, mapr);
				} else {
					if (!refAnn.ignoreMissing()) {
						throw new MappingException("The reference(" + dbRef.toString() + ") could not be fetched for "
								+ mf.getFullName());
					}
				}
			} else {
				resolvedObject = resolveObject(dbRef, referenceObjClass, refAnn.ignoreMissing(), mf, cache, mapr);
			}
			
			mf.setFieldValue(entity, resolvedObject);
			
		}
	}
	
	private void readCollection(final DBObject dbObject, final MappedField mf, final Object entity, Reference refAnn,
			EntityCache cache, Mapper mapr) {
		// multiple references in a List
		Class referenceObjClass = mf.getSubType();
		Collection references = (Collection) ReflectionUtils.newInstance(mf.getCTor(), (!mf.isSet()) ? ArrayList.class
				: HashSet.class);
		
		if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
			Object dbVal = mf.getDbObjectValue(dbObject);
			if (dbVal != null) {
				references = mapr.proxyFactory.createListProxy(references, referenceObjClass, refAnn.ignoreMissing(),
						mapr.datastoreProvider);
				ProxiedEntityReferenceList referencesAsProxy = (ProxiedEntityReferenceList) references;
				
				if (dbVal instanceof List) {
					List<DBRef> refList = (List) dbVal;
					DatastoreImpl dsi = (DatastoreImpl) mapr.datastoreProvider.get();
					List<Key<Object>> keys = dsi.getKeysByRefs(refList);
					
					if (keys.size() != refList.size()) {
						String msg = "Some of the references could not be fetched for " + mf.getFullName() + ". "
								+ refList + " != " + keys;
						if (!refAnn.ignoreMissing())
							throw new MappingException(msg);
						else
							log.warning(msg);
					}
					
					referencesAsProxy.__addAll(keys);
				} else {
					DBRef dbRef = (DBRef) dbVal;
					if (!exists(mf.getSubType(), dbRef, cache, mapr)) {
						String msg = "The reference(" + dbRef.toString() + ") could not be fetched for "
								+ mf.getFullName();
						if (!refAnn.ignoreMissing())
							throw new MappingException(msg);
						else
							log.warning(msg);
					} else {
						referencesAsProxy.__add(new Key(dbRef));
					}
				}
			}
		} else {
			
			Object dbVal = mf.getDbObjectValue(dbObject);
			if (dbVal != null) {
				if (dbVal instanceof List) {
					List refList = (List) dbVal;
					for (Object dbRefObj : refList) {
						DBRef dbRef = (DBRef) dbRefObj;
						
						Key k = new Key(dbRef);
						Object cachedEntity = cache.getEntity(k);
						if (cachedEntity != null) {
							references.add(cachedEntity);
						} else {
							BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
							
							if (refDbObject == null) {
								if (!refAnn.ignoreMissing()) {
									throw new MappingException("The reference(" + dbRef.toString()
											+ ") could not be fetched for " + mf.getFullName());
								}
							} else {
								Object refObj = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
								refObj = mapr.fromDb(refDbObject, refObj, cache);
								references.add(refObj);
								cache.putEntity(k, refObj);
							}
						}
					}
				} else {
					DBRef dbRef = (DBRef) dbVal;
					Key k = new Key(dbRef);
					Object cachedEntity = cache.getEntity(k);
					if (cachedEntity != null) {
						references.add(cachedEntity);
					} else {
						BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
						if (refDbObject == null) {
							if (!refAnn.ignoreMissing()) {
								throw new MappingException("The reference(" + dbRef.toString()
										+ ") could not be fetched for " + mf.getFullName());
							}
						} else {
							Object newEntity = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
							newEntity = mapr.fromDb(refDbObject, newEntity, cache);
							references.add(newEntity);
						}
					}
				}
			}
		}
		
		if (mf.getType().isArray()) {
			Object[] array = ReflectionUtils.convertToArray(mf.getSubType(), ReflectionUtils.iterToList(references));
			mf.setFieldValue(entity, array);
		} else {
			mf.setFieldValue(entity, references);
		}
	}
	
	boolean exists(Class c, final DBRef dbRef, EntityCache cache, Mapper mapr) {
		Key key = new Key(dbRef);
		Boolean cached = cache.exists(key);
		if (cached != null)
			return cached;

		DatastoreImpl dsi = (DatastoreImpl) mapr.datastoreProvider.get();

		DBCollection dbColl = dsi.getCollection(c);
		if (!dbColl.getName().equals(dbRef.getRef()))
			log.warning("Class " + c.getName() + " is stored in the '" + dbColl.getName()
					+ "' collection but a reference was found for this type to another collection, '" + dbRef.getRef()
					+ "'. The reference will be loaded using the class anyway. " + dbRef);
		boolean exists = (dsi.find(dbRef.getRef(), c).disableValidation().filter("_id", dbRef.getId()).asKeyList()
				.size() == 1);
		cache.notifyExists(key, exists);
		return exists;
	}
	
	Object resolveObject(final DBRef dbRef, final Class referenceObjClass, final boolean ignoreMissing,
			final MappedField mf, EntityCache cache, Mapper mapr) {
		
		Key key = new Key(referenceObjClass, dbRef.getId());
		// key.updateKind(mapper);
		
		Object cached = cache.getEntity(key);
		if (cached != null)
			return cached;
		
		BasicDBObject refDbObject = (BasicDBObject) dbRef.fetch();
		
		if (refDbObject != null) {
			Object refObj = ReflectionUtils.createInstance(referenceObjClass, refDbObject);
			refObj = mapr.fromDb(refDbObject, refObj, cache);
			cache.putEntity(key, refObj);
			return refObj;
		}
		
		if (!ignoreMissing) {
			throw new MappingException("The reference(" + dbRef.toString() + ") could not be fetched for "
					+ mf.getFullName());
		} else {
			return null;
		}
	}
	
	private void readMap(final DBObject dbObject, final MappedField mf, final Object entity, final Reference refAnn,
			EntityCache cache, Mapper mapr) {
		Class referenceObjClass = mf.getSubType();
		Map map = (Map) ReflectionUtils.newInstance(mf.getCTor(), HashMap.class);
		
		BasicDBObject dbVal = (BasicDBObject) mf.getDbObjectValue(dbObject);
		if (dbVal != null) {
			if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
				// replace map by proxy to it.
				map = mapr.proxyFactory.createMapProxy(map, referenceObjClass, refAnn.ignoreMissing(),
						mapr.datastoreProvider);
			}
			for (Map.Entry<String, ?> entry : dbVal.entrySet()) {
				DBRef dbRef = (DBRef) entry.getValue();
				
				if (refAnn.lazy() && LazyFeatureDependencies.assertDependencyFullFilled()) {
					ProxiedEntityReferenceMap proxiedMap = (ProxiedEntityReferenceMap) map;
					proxiedMap.__put(entry.getKey(), new Key(dbRef));
				} else {
					Object resolvedObject = resolveObject(dbRef, referenceObjClass, refAnn.ignoreMissing(), mf, cache, mapr);
					map.put(entry.getKey(), resolvedObject);
				}
			}
		}
		mf.setFieldValue(entity, map);
	}
	
	private Object createOrReuseProxy(final Class referenceObjClass, final DBRef dbRef, EntityCache cache, Mapper mapr) {
		Key key = new Key(dbRef);
		Object proxyAlreadyCreated = cache.getProxy(key);
		if (proxyAlreadyCreated != null) {
			return proxyAlreadyCreated;
		}
		Object newProxy = mapr.proxyFactory.createProxy(referenceObjClass, key, mapr.datastoreProvider);
		cache.putProxy(key, newProxy);
		return newProxy;
	}
}