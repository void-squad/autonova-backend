package com.voidsquad.chatbot.util;

import java.util.Objects;

/**
 * Describes the target type for a JSON path value.
 */
public final class TypeDescriptor {
    public enum Primitive {
        STRING, LONG, DOUBLE, BOOLEAN, INSTANT, OBJECT, MAP
    }

    private final Primitive primitive;
    private final boolean list;

    private TypeDescriptor(Primitive primitive, boolean list) {
        this.primitive = Objects.requireNonNull(primitive);
        this.list = list;
    }

    public static TypeDescriptor of(Primitive p) { return new TypeDescriptor(p, false); }
    public static TypeDescriptor listOf(Primitive p) { return new TypeDescriptor(p, true); }

    public Primitive getPrimitive() { return primitive; }
    public boolean isList() { return list; }

    @Override
    public String toString() {
        return list ? ("List<" + primitive + ">") : primitive.toString();
    }
}
