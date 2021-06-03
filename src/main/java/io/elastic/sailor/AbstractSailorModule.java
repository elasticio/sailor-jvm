package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

abstract class AbstractSailorModule extends AbstractModule {

    void bindRequiredStringEnvVar(final String name) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(Utils.getEnvVar(name));
    }

    void bindOptionalStringEnvVar(final String name) {

        final String value = Utils.getOptionalEnvVar(name);

        if (value == null) {
            return;
        }

        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindOptionalIntegerEnvVar(final String name, int defaultValue) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalIntegerValue(name, defaultValue));
    }

    void bindOptionalIntegerEnvVarIfExists(final String name) {

        final String value = Utils.getOptionalEnvVar(name);

        if (value == null) {
            return;
        }

        bind(Integer.class)
            .annotatedWith(Names.named(name))
            .toInstance(Integer.parseInt(value));
    }


    void bindOptionalLongEnvVar(final String name, long defaultValue) {
        bind(Long.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalLongValue(name, defaultValue));
    }

    void bindOptionalYesNoEnvVar(final String name) {
        bind(Boolean.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalYesNoValue(name));
    }

    void bindOptionalBooleanValue(final String key, final boolean defaultValue) {
        final boolean value = getOptionalBooleanValue(key, defaultValue);

        bind(Boolean.class)
                .annotatedWith(Names.named(key))
                .toInstance(value);
    }

    <T extends Enum> void  bindEnum(Class<T> clazz, final String key, T defaultValue) {

        T value = defaultValue;

        final String strValue = Utils.getOptionalEnvVar(key);

        if (strValue != null) {
            value = (T) Enum.valueOf(clazz, strValue.toUpperCase());
        }

        bind(clazz)
                .annotatedWith(Names.named(key))
                .toInstance(value);
    }

    public static int getOptionalIntegerValue(final String key, int defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Integer.parseInt(value);
        }

        return defaultValue;
    }

    public static long getOptionalLongValue(final String key, long defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Long.parseLong(value);
        }

        return defaultValue;
    }

    public static boolean getOptionalYesNoValue(final String key) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return true;
        }

        return false;
    }

    public static boolean getOptionalBooleanValue(final String key, final boolean defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }


}
