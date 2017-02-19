package es.auroralabs.tools.glifo.mappers.base;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.auroralabs.tools.glifo.entities.base.ModelEntity;
import es.auroralabs.tools.glifo.model.base.ModelData;


/**
 * Created by franciscojosesotoportillo on 10/1/17.
 */

public class BasePropertiesMapper<E extends ModelEntity, M extends ModelData> {

    private static String LOG_TAG = BasePropertiesMapper.class.getName();
    private boolean isDebug = false;

    Class<E> entityClass;
    Class<M> modelClass;

    Map<String, String> entityMapper = new HashMap<>();
    Map<String, String> modelMapper = new HashMap<>();

    static List<BasePropertiesMapper.MapperComponent> mapperList = new ArrayList<>();
    static List<String> mapperListInitiated = new ArrayList<>();

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public BasePropertiesMapper(Class<E> entityClass, Class<M> modelClass) {
        this.entityClass = entityClass;
        this.modelClass = modelClass;

        MapperComponent dumbMapper = new MapperComponent(this.getClass().getName(), entityClass.getName(), modelClass.getName(), null);


        Method[] allModelMethods = this.modelClass.getDeclaredMethods();
        Method[] allEntityMethods = this.entityClass.getDeclaredMethods();

        Method[] allSuperModelMethods = null;
        if (this.modelClass.getSuperclass() != null && this.modelClass.getSuperclass() != Object.class) {
            allSuperModelMethods = this.modelClass.getSuperclass().getDeclaredMethods();
        }
        Method[] allSuperEntityMethods = null;
        if (this.entityClass.getSuperclass() != null && this.entityClass.getSuperclass() != Object.class) {
            allSuperEntityMethods = this.entityClass.getSuperclass().getDeclaredMethods();
        }

        List<String> listPublicEntityMethods = new ArrayList<>();
        List<String> listPublicModelMethods = new ArrayList<>();

        for (Method method : allModelMethods) {
            if (Modifier.isPublic(method.getModifiers())) {
                listPublicModelMethods.add(method.getName());
            }
        }

        for (Method method : allEntityMethods) {
            if (Modifier.isPublic(method.getModifiers())) {
                listPublicEntityMethods.add(method.getName());
            }
        }

        if (allSuperModelMethods != null) {
            for (Method method : allSuperModelMethods) {
                if (Modifier.isPublic(method.getModifiers())) {
                    listPublicModelMethods.add(method.getName());
                }
            }
        }

        if (allSuperEntityMethods != null) {
            for (Method method : allSuperEntityMethods) {
                if (Modifier.isPublic(method.getModifiers())) {
                    listPublicEntityMethods.add(method.getName());
                }
            }
        }

        for (String methodName : listPublicEntityMethods) {
            if (methodName.startsWith("get") || methodName.startsWith("is")) {
                try {
                    Method getterEntityMethod;
                    try {
                        getterEntityMethod = this.entityClass.getMethod(methodName);
                    } catch (NoSuchMethodException e) {
                        getterEntityMethod = this.entityClass.getSuperclass().getMethod(methodName);
                    }
                    Class clazz = getterEntityMethod.getReturnType();
                    if (Collection.class.isAssignableFrom(clazz)) {
                        Type typeGeneric = getterEntityMethod.getGenericReturnType();
                        if (typeGeneric instanceof ParameterizedType) {
                            ParameterizedType paramType = (ParameterizedType) typeGeneric;
                            Type[] argTypes = paramType.getActualTypeArguments();
                            if (argTypes.length > 0) {
                                String entityName = cleanTypeName(argTypes[0].toString());
                                String modelName = modelNameFromEntity(entityName);
                                String mapperName = mapperNameFromEntityName(entityName);
                                try {
                                    Class mapperClass = Class.forName(mapperName);
                                    if (!mapperListInitiated.contains(mapperClass.getName())) {
                                        mapperListInitiated.add(mapperClass.getName());
                                        mapperClass.newInstance();
                                    }

                                } catch (ClassNotFoundException ce) {

                                } catch (InstantiationException ie) {

                                } catch (IllegalAccessException iae) {

                                }
                            }

                        }
                    } else {
                        if (isAEntityModel(clazz)) {
                            String entityName = clazz.getName();
                            String modelName = modelNameFromEntity(entityName);
                            String mapperName = mapperNameFromEntityName(clazz.getName());
                            try {
                                Class mapperClass = Class.forName(mapperName);
                                if (!mapperListInitiated.contains(mapperClass.getName())) {
                                    mapperListInitiated.add(mapperClass.getName());
                                    mapperClass.newInstance();
                                }
                            } catch (ClassNotFoundException ce) {

                            } catch (InstantiationException ie) {

                            } catch (IllegalAccessException iae) {

                            }

                        }
                    }
                } catch (IllegalArgumentException e) {

                } catch (NoSuchMethodException e) {

                }

                String setterName = methodName.replace(methodName.startsWith("get") ? "get" : "is", "set");
                if (listPublicModelMethods.contains(setterName)) {
                    entityMapper.put(methodName, setterName);
                }
            } else if (methodName.startsWith("set")) {
                String getterName = methodName.replace("set", "get");
                if (listPublicModelMethods.contains(getterName)) {
                    modelMapper.put(getterName, methodName);
                }
            }
        }

        if (!mapperList.contains(dumbMapper)) {
            MapperComponent mapperComponent = new MapperComponent(this.getClass().getName(), entityClass.getName(), modelClass.getName(), this);
            mapperList.add(mapperComponent);
        }
    }

    public E toEntity(M object) {
        boolean alreadySetted = false;
        try {
            E entityObject = entityClass.newInstance();

            for (String getter : modelMapper.keySet()) {
                try {
                    Method getterModelMethod = modelClass.getMethod(getter);
                    Method getterEntityMethod = entityClass.getMethod(getter);
                    String modelName = null;
                    Class modelReturnClass = getterModelMethod.getReturnType();
                    if (Collection.class.isAssignableFrom(modelReturnClass)) {
                        Type typeGeneric = getterModelMethod.getGenericReturnType();
                        if (typeGeneric instanceof ParameterizedType) {
                            ParameterizedType paramType = (ParameterizedType) typeGeneric;
                            Type[] argTypes = paramType.getActualTypeArguments();
                            if (argTypes.length > 0) {
                                modelName = cleanTypeName(argTypes[0].toString());
                            }
                        }
                    } else if (ModelData.class.isAssignableFrom(modelReturnClass)) {
                        modelName = modelReturnClass.getName();
                    }

                    if (modelName != null) {
                        try {
                            Class dumpedClass = Class.forName(modelName);
                            MapperComponent mapperComponent = getMapperComponentFromName(mapperNameFromModelName(dumpedClass.getName()));
                            if (mapperComponent != null) {
                                Method setterEntityMethod = entityClass.getMethod(modelMapper.get(getter), getterEntityMethod.getReturnType());

                                Method mapperToEntityMethod = null;
                                try {
                                    mapperToEntityMethod = mapperComponent.mapper.getClass().getMethod("toEntity", modelReturnClass);
                                } catch (NoSuchMethodException e) {
                                    if (isDebug) {
                                        Log.e(LOG_TAG, e.getMessage());
                                        e.printStackTrace();
                                    }
                                }

                                if (mapperToEntityMethod != null) {
                                    try {
                                        Object modelValue = getterModelMethod.invoke(object);
                                        Object value = mapperToEntityMethod.invoke(mapperComponent.mapper, modelValue);
                                        setterEntityMethod.invoke(entityObject, value);
                                        alreadySetted = true;
                                    } catch (IllegalArgumentException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    } catch (IllegalAccessException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    } catch (InvocationTargetException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch (ClassNotFoundException cle) {
                            if (isDebug) {
                                Log.e(LOG_TAG, cle.getMessage());
                                cle.printStackTrace();
                            }
                        } catch (NoSuchMethodException e) {
                            if (isDebug) {
                                Log.e(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!alreadySetted) {
                        Method setterEntityMethod = null;
                        try {
                            setterEntityMethod = entityClass.getMethod(modelMapper.get(getter), getterEntityMethod.getReturnType());
                        } catch (NoSuchMethodException nsme) {
                            if (entityClass.getSuperclass() != null) {
                                setterEntityMethod = entityClass.getSuperclass().getMethod(modelMapper.get(getter), getterEntityMethod.getReturnType());
                            }
                        }
                        if (setterEntityMethod != null) {
                            try {
                                if (isDebug) {
                                    Log.d(LOG_TAG, setterEntityMethod.getName()+" "+entityObject+ " "+getterModelMethod.getName()+ " "+ object);
                                }
                                Object modelValueReturnedFromGetter = getterModelMethod.invoke(object);
                                setterEntityMethod.invoke(entityObject, modelValueReturnedFromGetter);
                            } catch (IllegalArgumentException e) {
                                if (isDebug) {
                                    Log.e(LOG_TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            } catch (IllegalAccessException e) {
                                if (isDebug) {
                                    Log.e(LOG_TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            } catch (InvocationTargetException e) {
                                if (isDebug) {
                                    Log.e(LOG_TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                } catch (SecurityException e) {
                    if (isDebug) {
                        Log.e(LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                } catch (NoSuchMethodException e) {
                    if (isDebug) {
                        Log.e(LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            return entityObject;
        } catch (IllegalAccessException iae) {
            if (isDebug) {
                Log.e(LOG_TAG, iae.getMessage());
                iae.printStackTrace();
            }
            return null;
        } catch (InstantiationException ie) {
            if (isDebug) {
                Log.e(LOG_TAG, ie.getMessage());
                ie.printStackTrace();
            }
            return null;
        }
    }

    public M fromEntity(E object) {
        boolean alreadySetted = false;
        try {
            M modelObject = modelClass.newInstance();

            for (String getter : entityMapper.keySet()) {
                try {
                    Method getterEntityMethod = entityClass.getMethod(getter);
                    Method getterModelMethod = modelClass.getMethod(getter);
                    String entityName = null;
                    Class clazz = getterEntityMethod.getReturnType();
                    if (Collection.class.isAssignableFrom(clazz)) {
                        Type typeGeneric = getterEntityMethod.getGenericReturnType();
                        if (typeGeneric instanceof ParameterizedType) {
                            ParameterizedType paramType = (ParameterizedType) typeGeneric;
                            Type[] argTypes = paramType.getActualTypeArguments();
                            if (argTypes.length > 0) {
                                entityName = cleanTypeName(argTypes[0].toString());
                            }
                        }
                    } else if (ModelEntity.class.isAssignableFrom(clazz)) {
                        entityName = clazz.getName();
                    }

                    if (entityName != null) {
                        try {
                            Class dumpedClass = Class.forName(entityName);
                            MapperComponent mapperComponent = getMapperComponentFromName(mapperNameFromEntityName(dumpedClass.getName()));
                            if (mapperComponent != null) {
                                Method setterModelMethod = modelClass.getMethod(entityMapper.get(getter), Class.forName(mapperComponent.modelName));

                                Method mapperFromEntityMethod = null;

                                try {
                                    mapperFromEntityMethod = mapperComponent.mapper.getClass().getMethod("fromEntity", dumpedClass);
                                } catch (NoSuchMethodException e) {
                                    mapperFromEntityMethod = mapperComponent.mapper.getClass().getSuperclass().getMethod("fromEntity", dumpedClass);
                                }

                                if (mapperFromEntityMethod != null) {
                                    try {
                                        Object value = getterEntityMethod.invoke(object);
                                        setterModelMethod.invoke(modelObject, mapperFromEntityMethod.invoke(mapperComponent.mapper, value));
                                        alreadySetted = true;
                                    } catch (IllegalArgumentException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    } catch (IllegalAccessException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    } catch (InvocationTargetException e) {
                                        if (isDebug) {
                                            Log.e(LOG_TAG, e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch (ClassNotFoundException cle) {
                            if (isDebug) {
                                Log.e(LOG_TAG, cle.getMessage());
                                cle.printStackTrace();
                            }
                        }
                    }

                    if (!alreadySetted) {
                        Method setterModelMethod = null;
                        try {
                            setterModelMethod = modelClass.getMethod(entityMapper.get(getter), getterModelMethod.getReturnType());
                        } catch (NoSuchMethodException nsme) {
                            if (modelClass.getSuperclass() != null) {
                                setterModelMethod = modelClass.getSuperclass().getMethod(entityMapper.get(getter), getterModelMethod.getReturnType());
                            }
                        }
                        try {
                            setterModelMethod.invoke(modelObject, getterEntityMethod.invoke(object));
                        } catch (IllegalArgumentException e) {
                            if (isDebug) {
                                Log.e(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (IllegalAccessException e) {
                            if (isDebug) {
                                Log.e(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (InvocationTargetException e) {
                            if (isDebug) {
                                Log.e(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (SecurityException e) {
                    if (isDebug) {
                        Log.e(LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                } catch (NoSuchMethodException e) {
                    if (isDebug) {
                        Log.e(LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            return modelObject;
        } catch (IllegalAccessException iae) {
            if (isDebug) {
                Log.e(LOG_TAG, iae.getMessage());
                iae.printStackTrace();
            }
            return null;
        } catch (InstantiationException ie) {
            if (isDebug) {
                Log.e(LOG_TAG, ie.getMessage());
                ie.printStackTrace();
            }
            return null;
        }
    }

    private String mapperNameFromEntityName(String modelName) {
        return modelName.replace("Entity", "Mapper").replace(".entities.", ".mappers.");
    }

    private boolean isAEntityModel(Class clazz) {
        return ModelEntity.class.isAssignableFrom(clazz);
    }

    private String modelNameFromEntity(String entityName) {
        return entityName.replace("Entity", "Model");
    }

    private String mapperNameFromModelName(String entityName) {
        return entityName.replace("Data", "Mapper").replace(".datasource.model.", ".domain.mappers.");
    }

    private String cleanTypeName(String typeName) {
        return typeName.replace("class ", "");
    }

    private MapperComponent getMapperComponentFromName(String name) {
        MapperComponent dumbMapper = new MapperComponent(name, entityClass.getName(), modelClass.getName(), null);
        int position = mapperList.indexOf(dumbMapper);
        if (position >= 0) {
            return mapperList.get(position);
        } else {
            return null;
        }
    }

    private class MapperComponent {

        private String mapperName;
        private String entityName;
        private String modelName;
        private Object mapper;

        MapperComponent(String mapperName, String entity, String model, Object mapper) {
            this.mapperName = mapperName;
            this.entityName = entity;
            this.modelName = model;
            this.mapper = mapper;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof BasePropertiesMapper.MapperComponent) {
                MapperComponent component = (MapperComponent) object;
                return this.mapperName.equals(component.mapperName);
            }

            return false;
        }
    }
}