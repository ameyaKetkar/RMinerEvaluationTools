package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Duplication;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.TypeCreation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.annotation.*;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * Using this annotation it is possible to access fields by getter and setter types. Before this annotation can be
 * used, it needs to be installed with two types. The getter type must be defined in a single-method interface
 * with a single method that returns an {@link java.lang.Object} type and takes no arguments. The getter interface
 * must similarly return {@code void} and take a single {@link java.lang.Object} argument. After installing these
 * interfaces with the {@link FieldProxy.Binder}, this
 * binder needs to be registered with a {@link net.bytebuddy.implementation.MethodDelegation} before it can be used.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface FieldProxy {

    /**
     * A placeholder name to indicate that a field name should be inferred by the name of the intercepted
     * method by the Java bean naming conventions.
     */
    String BEAN_PROPERTY = "";

    /**
     * Determines if the proxy should be serializable.
     *
     * @return {@code true} if the proxy should be serializable.
     */
    boolean serializableProxy() default false;

    /**
     * Determines the name of the field that is to be accessed. If this property is not set, a field name is inferred
     * by the intercepted method after the Java beans naming conventions.
     *
     * @return The name of the field to be accessed.
     */
    String value() default "";

    /**
     * Determines which type defines the field that is to be accessed. If this property is not set, the most field
     * that is defined highest in the type hierarchy is accessed.
     *
     * @return The type that defines the accessed field.
     */
    Class<?> definingType() default void.class;

    /**
     * A binder for the {@link FieldProxy} annotation.
     */
    class Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> {

        /**
         * A reference to the method that declares the field annotation's defining type property.
         */
        private static final MethodDescription DEFINING_TYPE;

        /**
         * A reference to the method that declares the field annotation's field name property.
         */
        private static final MethodDescription FIELD_NAME;

        /**
         * A reference to the method that declares the field annotation's serializable proxy property.
         */
        private static final MethodDescription SERIALIZABLE_PROXY;

        /*
         * Fetches a reference to all annotation properties.
         */
        static {
            MethodList methodList = new TypeDescription.ForLoadedType(FieldProxy.class).getDeclaredMethods();
            DEFINING_TYPE = methodList.filter(named("definingType")).getOnly();
            FIELD_NAME = methodList.filter(named("value")).getOnly();
            SERIALIZABLE_PROXY = methodList.filter(named("serializableProxy")).getOnly();
        }

        /**
         * The getter method to be implemented by a getter proxy.
         */
        private final MethodDescription getterMethod;

        /**
         * The setter method to be implemented by a setter proxy.
         */
        private final MethodDescription setterMethod;

        /**
         * Creates a new binder for the {@link FieldProxy}
         * annotation.
         *
         * @param getterMethod The getter method to be implemented by a getter proxy.
         * @param setterMethod The setter method to be implemented by a setter proxy.
         */
        protected Binder(MethodDescription getterMethod, MethodDescription setterMethod) {
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
        }

        /**
         * Creates a binder by installing two proxy types which are implemented by this binder if a field getter
         * or a field setter is requested by using the
         * {@link FieldProxy} annotation.
         *
         * @param getterType The type which should be used for getter proxies. The type must
         *                   represent an interface which defines a single method which returns an
         *                   {@link java.lang.Object} return type and does not take any arguments. The use of generics
         *                   is permitted.
         * @param setterType The type which should be uses for setter proxies. The type must
         *                   represent an interface which defines a single method which returns {@code void}
         *                   and takes a signle {@link java.lang.Object}-typed argument. The use of generics
         *                   is permitted.
         * @return A binder for the {@link FieldProxy}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(Class<?> getterType,
                                                                                             Class<?> setterType) {
            return install(new TypeDescription.ForLoadedType(nonNull(getterType)), new TypeDescription.ForLoadedType(nonNull(setterType)));
        }

        /**
         * Creates a binder by installing two proxy types which are implemented by this binder if a field getter
         * or a field setter is requested by using the
         * {@link FieldProxy} annotation.
         *
         * @param getterType The type which should be used for getter proxies. The type must
         *                   represent an interface which defines a single method which returns an
         *                   {@link java.lang.Object} return type and does not take any arguments. The use of generics
         *                   is permitted.
         * @param setterType The type which should be uses for setter proxies. The type must
         *                   represent an interface which defines a single method which returns {@code void}
         *                   and takes a signle {@link java.lang.Object}-typed argument. The use of generics
         *                   is permitted.
         * @return A binder for the {@link FieldProxy}
         * annotation.
         */
        public static TargetMethodAnnotationDrivenBinder.ParameterBinder<FieldProxy> install(TypeDescription getterType,
                                                                                             TypeDescription setterType) {
            MethodDescription getterMethod = onlyMethod(nonNull(getterType));
            if (!getterMethod.getReturnType().represents(Object.class)) {
                throw new IllegalArgumentException(getterMethod + " must take a single Object-typed parameter");
            } else if (getterMethod.getParameters().size() != 0) {
                throw new IllegalArgumentException(getterMethod + " must not declare parameters");
            }
            MethodDescription setterMethod = onlyMethod(nonNull(setterType));
            if (!setterMethod.getReturnType().represents(void.class)) {
                throw new IllegalArgumentException(setterMethod + " must return void");
            } else if (setterMethod.getParameters().size() != 1 || !setterMethod.getParameters().get(0).getType().represents(Object.class)) {
                throw new IllegalArgumentException(setterMethod + " must declare a single Object-typed parameters");
            }
            return new Binder(getterMethod, setterMethod);
        }

        /**
         * Extracts the only method from a given type description which is validated for the required properties for
         * using the type as a proxy base type.
         *
         * @param typeDescription The type description to evaluate.
         * @return The only method which was found to be compatible to the proxy requirements.
         */
        private static MethodDescription onlyMethod(TypeDescription typeDescription) {
            if (!typeDescription.isInterface()) {
                throw new IllegalArgumentException(typeDescription + " is not an interface");
            } else if (typeDescription.getInterfaces().size() > 0) {
                throw new IllegalArgumentException(typeDescription + " must not extend other interfaces");
            } else if (!typeDescription.isPublic()) {
                throw new IllegalArgumentException(typeDescription + " is mot public");
            }
            MethodList methodCandidates = typeDescription.getDeclaredMethods().filter(not(isStatic()));
            if (methodCandidates.size() != 1) {
                throw new IllegalArgumentException(typeDescription + " must declare exactly one non-static method");
            }
            return methodCandidates.getOnly();
        }

        @Override
        public Class<FieldProxy> getHandledType() {
            return FieldProxy.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<FieldProxy> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            AccessType accessType;
            if (target.getType().equals(getterMethod.getDeclaringType())) {
                accessType = AccessType.GETTER;
            } else if (target.getType().equals(setterMethod.getDeclaringType())) {
                accessType = AccessType.SETTER;
            } else {
                throw new IllegalStateException(target + " uses a @Field annotation on an non-installed type");
            }
            FieldLocator.Resolution resolution = FieldLocator.of(annotation.getValue(FIELD_NAME, String.class), source)
                    .lookup(annotation.getValue(DEFINING_TYPE, TypeDescription.class), implementationTarget.getTypeDescription())
                    .resolve(implementationTarget.getTypeDescription());
            return resolution.isValid()
                    ? new MethodDelegationBinder.ParameterBinding.Anonymous(new AccessorProxy(
                    resolution.getFieldDescription(),
                    assigner,
                    implementationTarget.getTypeDescription(),
                    accessType,
                    annotation.getValue(SERIALIZABLE_PROXY, Boolean.class)))
                    : MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && getterMethod.equals(((Binder) other).getterMethod)
                    && setterMethod.equals(((Binder) other).setterMethod);
        }

        @Override
        public int hashCode() {
            int result = getterMethod.hashCode();
            result = 31 * result + setterMethod.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FieldProxy.Binder{" +
                    "getterMethod=" + getterMethod +
                    ", setterMethod=" + setterMethod +
                    '}';
        }

        /**
         * Represents an implementation for implementing a proxy type constructor when a static field is accessed.
         */
        protected enum StaticFieldConstructor implements Implementation {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * A reference of the {@link Object} type default constructor.
             */
            protected final MethodDescription objectTypeDefaultConstructor;

            /**
             * Creates the constructor call singleton.
             */
            StaticFieldConstructor() {
                objectTypeDefaultConstructor = TypeDescription.OBJECT.getDeclaredMethods()
                        .filter(isConstructor())
                        .getOnly();
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public ByteCodeAppender appender(Target implementationTarget) {
                return new ByteCodeAppender.Simple(MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodInvocation.invoke(objectTypeDefaultConstructor),
                        MethodReturn.VOID);
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.StaticFieldConstructor." + name();
            }
        }

        /**
         * Determines the way a field is to be accessed.
         */
        protected enum AccessType {

            /**
             * Represents getter access for a field.
             */
            GETTER {
                @Override
                protected TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod) {
                    return getterMethod.getDeclaringType();
                }

                @Override
                protected Implementation access(FieldDescription fieldDescription,
                                                Assigner assigner,
                                                AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return new Getter(fieldDescription, assigner, methodAccessorFactory);
                }
            },

            /**
             * Represents setter access for a field.
             */
            SETTER {
                @Override
                protected TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod) {
                    return setterMethod.getDeclaringType();
                }

                @Override
                protected Implementation access(FieldDescription fieldDescription,
                                                Assigner assigner,
                                                AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    return new Setter(fieldDescription, assigner, methodAccessorFactory);
                }
            };


            /**
             * Locates the type to be implemented by a field accessor proxy.
             *
             * @param getterMethod The getter method to be implemented by a getter proxy.
             * @param setterMethod The setter method to be implemented by a setter proxy.
             * @return The correct proxy type for the represented sort of accessor.
             */
            protected abstract TypeDescription proxyType(MethodDescription getterMethod, MethodDescription setterMethod);

            /**
             * Returns an implementation that implements the sort of accessor implementation that is represented by
             * this instance.
             *
             * @param fieldDescription      The field to be accessed.
             * @param assigner              The assigner to use.
             * @param methodAccessorFactory The accessed type's method accessor factory.
             * @return A suitable implementation.
             */
            protected abstract Implementation access(FieldDescription fieldDescription,
                                                     Assigner assigner,
                                                     AuxiliaryType.MethodAccessorFactory methodAccessorFactory);

            @Override
            public String toString() {
                return "FieldProxy.Binder.AccessType." + name();
            }

            /**
             * Implementation for a getter method.
             */
            protected static class Getter implements Implementation {

                /**
                 * The field that is being accessed.
                 */
                private final FieldDescription accessedField;

                /**
                 * The assigner to use.
                 */
                private final Assigner assigner;

                /**
                 * The accessed type's method accessor factory.
                 */
                private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

                /**
                 * Creates a new getter implementation.
                 *
                 * @param accessedField         The field that is being accessed.
                 * @param assigner              The assigner to use.
                 * @param methodAccessorFactory The accessed type's method accessor factory.
                 */
                protected Getter(FieldDescription accessedField,
                                 Assigner assigner,
                                 AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    this.accessedField = accessedField;
                    this.assigner = assigner;
                    this.methodAccessorFactory = methodAccessorFactory;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Getter getter = (Getter) other;
                    return accessedField.equals(getter.accessedField)
                            && assigner.equals(getter.assigner)
                            && methodAccessorFactory.equals(getter.methodAccessorFactory);
                }

                @Override
                public int hashCode() {
                    int result = accessedField.hashCode();
                    result = 31 * result + assigner.hashCode();
                    result = 31 * result + methodAccessorFactory.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.AccessType.Getter{" +
                            "accessedField=" + accessedField +
                            ", assigner=" + assigner +
                            ", methodAccessorFactory=" + methodAccessorFactory +
                            '}';
                }

                /**
                 * A byte code appender for a getter method.
                 */
                protected class Appender implements ByteCodeAppender {

                    /**
                     * The generated accessor type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new appender for a setter method.
                     *
                     * @param implementationTarget The implementation target of the current instrumentation.
                     */
                    protected Appender(Target implementationTarget) {
                        typeDescription = implementationTarget.getTypeDescription();
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Context implementationContext,
                                      MethodDescription instrumentedMethod) {
                        MethodDescription getterMethod = methodAccessorFactory.registerGetterFor(accessedField);
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                accessedField.isStatic()
                                        ? StackManipulation.LegalTrivial.INSTANCE
                                        : new StackManipulation.Compound(
                                        MethodVariableAccess.REFERENCE.loadOffset(0),
                                        FieldAccess.forField(typeDescription.getDeclaredFields()
                                                .filter((named(AccessorProxy.FIELD_NAME))).getOnly()).getter()),
                                MethodInvocation.invoke(getterMethod),
                                assigner.assign(getterMethod.getReturnType(), instrumentedMethod.getReturnType(), Assigner.DYNAMICALLY_TYPED),
                                MethodReturn.returning(instrumentedMethod.getReturnType())
                        ).apply(methodVisitor, implementationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private Getter getOuter() {
                        return Getter.this;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && Getter.this.equals(((Appender) other).getOuter())
                                && typeDescription.equals(((Appender) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        return typeDescription.hashCode() + 31 * Getter.this.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.AccessType.Getter.Appender{" +
                                "getter=" + Getter.this +
                                "typeDescription=" + typeDescription +
                                '}';
                    }
                }
            }

            /**
             * Implementation for a setter method.
             */
            protected static class Setter implements Implementation {

                /**
                 * The field that is being accessed.
                 */
                private final FieldDescription accessedField;

                /**
                 * The assigner to use.
                 */
                private final Assigner assigner;

                /**
                 * The accessed type's method accessor factory.
                 */
                private final AuxiliaryType.MethodAccessorFactory methodAccessorFactory;

                /**
                 * Creates a new setter implementation.
                 *
                 * @param accessedField         The field that is being accessed.
                 * @param assigner              The assigner to use.
                 * @param methodAccessorFactory The accessed type's method accessor factory.
                 */
                protected Setter(FieldDescription accessedField,
                                 Assigner assigner,
                                 AuxiliaryType.MethodAccessorFactory methodAccessorFactory) {
                    this.accessedField = accessedField;
                    this.assigner = assigner;
                    this.methodAccessorFactory = methodAccessorFactory;
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new Appender(implementationTarget);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Setter getter = (Setter) other;
                    return accessedField.equals(getter.accessedField)
                            && assigner.equals(getter.assigner)
                            && methodAccessorFactory.equals(getter.methodAccessorFactory);
                }

                @Override
                public int hashCode() {
                    int result = accessedField.hashCode();
                    result = 31 * result + assigner.hashCode();
                    result = 31 * result + methodAccessorFactory.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.AccessType.Setter{" +
                            "accessedField=" + accessedField +
                            ", assigner=" + assigner +
                            ", methodAccessorFactory=" + methodAccessorFactory +
                            '}';
                }

                /**
                 * A byte code appender for a setter method.
                 */
                protected class Appender implements ByteCodeAppender {

                    /**
                     * The generated accessor type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new appender for a setter method.
                     *
                     * @param implementationTarget The implementation target of the current instrumentation.
                     */
                    protected Appender(Target implementationTarget) {
                        typeDescription = implementationTarget.getTypeDescription();
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor,
                                      Context implementationContext,
                                      MethodDescription instrumentedMethod) {
                        TypeDescription parameterType = instrumentedMethod.getParameters().get(0).getType();
                        MethodDescription setterMethod = methodAccessorFactory.registerSetterFor(accessedField);
                        StackManipulation.Size stackSize = new StackManipulation.Compound(
                                accessedField.isStatic()
                                        ? StackManipulation.LegalTrivial.INSTANCE
                                        : new StackManipulation.Compound(
                                        MethodVariableAccess.REFERENCE.loadOffset(0),
                                        FieldAccess.forField(typeDescription.getDeclaredFields()
                                                .filter((named(AccessorProxy.FIELD_NAME))).getOnly()).getter()),
                                MethodVariableAccess.forType(parameterType).loadOffset(1),
                                assigner.assign(parameterType, setterMethod.getParameters().get(0).getType(), Assigner.DYNAMICALLY_TYPED),
                                MethodInvocation.invoke(setterMethod),
                                MethodReturn.VOID
                        ).apply(methodVisitor, implementationContext);
                        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private Setter getOuter() {
                        return Setter.this;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && Setter.this.equals(((Appender) other).getOuter())
                                && typeDescription.equals(((Appender) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        return typeDescription.hashCode() + 31 * Setter.this.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.AccessType.Setter.Appender{" +
                                "setter=" + Setter.this +
                                "typeDescription=" + typeDescription +
                                '}';
                    }
                }
            }
        }

        /**
         * Represents an implementation for implementing a proxy type constructor when a non-static field is accessed.
         */
        protected static class InstanceFieldConstructor implements Implementation {

            /**
             * The instrumented type from which a field is to be accessed.
             */
            private final TypeDescription instrumentedType;

            /**
             * Creates a new implementation for implementing a field accessor proxy's constructor when accessing
             * a non-static field.
             *
             * @param instrumentedType The instrumented type from which a field is to be accessed.
             */
            protected InstanceFieldConstructor(TypeDescription instrumentedType) {
                this.instrumentedType = instrumentedType;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType.withField(AccessorProxy.FIELD_NAME,
                        this.instrumentedType,
                        Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE);
            }

            @Override
            public ByteCodeAppender appender(Target implementationTarget) {
                return new Appender(implementationTarget);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentedType.equals(((InstanceFieldConstructor) other).instrumentedType);
            }

            @Override
            public int hashCode() {
                return instrumentedType.hashCode();
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.InstanceFieldConstructor{" +
                        "instrumentedType=" + instrumentedType +
                        '}';
            }

            /**
             * An appender for implementing an
             * {@link FieldProxy.Binder.InstanceFieldConstructor}.
             */
            protected static class Appender implements ByteCodeAppender {

                /**
                 * The field to be set within the constructor.
                 */
                private final FieldDescription fieldDescription;

                /**
                 * Creates a new appender.
                 *
                 * @param implementationTarget The implementation target of the current implementation.
                 */
                protected Appender(Target implementationTarget) {
                    fieldDescription = implementationTarget.getTypeDescription()
                            .getDeclaredFields()
                            .filter((named(AccessorProxy.FIELD_NAME)))
                            .getOnly();
                }

                @Override
                public Size apply(MethodVisitor methodVisitor,
                                  Context implementationContext,
                                  MethodDescription instrumentedMethod) {
                    StackManipulation.Size stackSize = new StackManipulation.Compound(
                            MethodVariableAccess.REFERENCE.loadOffset(0),
                            MethodInvocation.invoke(StaticFieldConstructor.INSTANCE.objectTypeDefaultConstructor),
                            MethodVariableAccess.loadThisReferenceAndArguments(instrumentedMethod),
                            FieldAccess.forField(fieldDescription).putter(),
                            MethodReturn.VOID
                    ).apply(methodVisitor, implementationContext);
                    return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldDescription.equals(((Appender) other).fieldDescription);
                }

                @Override
                public int hashCode() {
                    return fieldDescription.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.InstanceFieldConstructor.Appender{" +
                            "fieldDescription=" + fieldDescription +
                            '}';
                }
            }
        }

        /**
         * A field locator is responsible for locating the type a field is defined in.
         */
        protected abstract static class FieldLocator {

            /**
             * Returns a field locator for a given field.
             *
             * @param fieldName         The field's name which might represent
             *                          {@link FieldProxy#BEAN_PROPERTY}
             *                          if the field's name should be derived from a method's name.
             * @param methodDescription The intercepted method.
             * @return An appropriate field locator.
             */
            protected static FieldLocator of(String fieldName, MethodDescription methodDescription) {
                return BEAN_PROPERTY.equals(fieldName)
                        ? Legal.consider(methodDescription)
                        : new Legal(fieldName);
            }

            /**
             * Locates a field of a given name on a specific type.
             *
             * @param typeDescription  The type which defines the field or a representation of {@code void} for
             *                         looking up a type implicitly within the type hierarchy.
             * @param instrumentedType The instrumented type from which a field is to be accessed.
             * @return A corresponding lookup engine.
             */
            protected abstract LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType);

            /**
             * A resolution represents the result of a field location.
             */
            protected abstract static class Resolution {

                /**
                 * Determines if a field lookup was successful.
                 *
                 * @return {@code true} if a field lookup was successful.
                 */
                protected abstract boolean isValid();

                /**
                 * Returns a description of the located field. This method must only be called for valid
                 * resolutions.
                 *
                 * @return The located field.
                 */
                protected abstract FieldDescription getFieldDescription();

                /**
                 * A resolution for a non-located field.
                 */
                protected static class Unresolved extends Resolution {

                    @Override
                    protected boolean isValid() {
                        return false;
                    }

                    @Override
                    protected FieldDescription getFieldDescription() {
                        throw new IllegalStateException("Cannot resolve an unresolved field lookup");
                    }

                    @Override
                    public int hashCode() {
                        return 17;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return other == this || (other != null && other.getClass() == getClass());
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldLocator.Resolution.Unresolved{}";
                    }
                }

                /**
                 * A resolution for a successfully located field.
                 */
                protected static class Resolved extends Resolution {

                    /**
                     * The located field.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * Creates a new successful resolution.
                     *
                     * @param fieldDescription The located field.
                     */
                    protected Resolved(FieldDescription fieldDescription) {
                        this.fieldDescription = fieldDescription;
                    }

                    @Override
                    protected boolean isValid() {
                        return true;
                    }

                    @Override
                    protected FieldDescription getFieldDescription() {
                        return fieldDescription;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldDescription.equals(((Resolved) other).fieldDescription);
                    }

                    @Override
                    public int hashCode() {
                        return fieldDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldLocator.Resolution.Resolved{" +
                                "fieldDescription=" + fieldDescription +
                                '}';
                    }
                }
            }

            /**
             * A lookup engine is responsible for finding a specific field in a type hierarchy.
             */
            protected abstract static class LookupEngine {

                /**
                 * Locates a field if possible and returns a corresponding resolution.
                 *
                 * @param instrumentedType The instrumented type from which a field is to be accessed.
                 * @return A resolution of the field name lookup.
                 */
                protected abstract Resolution resolve(TypeDescription instrumentedType);

                /**
                 * Represents a lookup engine that can only produce illegal look-ups.
                 */
                protected static class Illegal extends LookupEngine {

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        return new Resolution.Unresolved();
                    }

                    @Override
                    public int hashCode() {
                        return 17;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return other == this || (other != null && other.getClass() == getClass());
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldLocator.LookupEngine.Illegal{}";
                    }
                }

                /**
                 * Represents a lookup engine that tries to find the most specific field in a class hierarchy.
                 */
                protected static class ForHierarchy extends LookupEngine {

                    /**
                     * The name of the field to be found.
                     */
                    private final String fieldName;

                    /**
                     * Creates a new lookup engine that looks up a field name within the class hierarchy of the
                     * instrumented type.
                     *
                     * @param fieldName The name of the field to be found.
                     */
                    protected ForHierarchy(String fieldName) {
                        this.fieldName = fieldName;
                    }

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        TypeDescription currentType = instrumentedType;
                        do {
                            FieldList fieldList = currentType.getDeclaredFields().filter(named(fieldName).and(isVisibleTo(instrumentedType)));
                            if (fieldList.size() == 1) {
                                return new Resolution.Resolved(fieldList.getOnly());
                            }
                        } while ((currentType = currentType.getSuperType()) != null);
                        return new Resolution.Unresolved();
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldName.equals(((ForHierarchy) other).fieldName);
                    }

                    @Override
                    public int hashCode() {
                        return fieldName.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldLocator.LookupEngine.ForHierarchy{" +
                                "fieldName='" + fieldName + '\'' +
                                '}';
                    }
                }

                /**
                 * Represents a lookup engine that tries to find a field for a given type.
                 */
                protected static class ForExplicitType extends LookupEngine {

                    /**
                     * The name of the field.
                     */
                    private final String fieldName;

                    /**
                     * The type which is supposed to define a field with the given field name.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new lookup engine for a given type.
                     *
                     * @param fieldName       The name of the field to be found.
                     * @param typeDescription The type which is supposed to define a field with the given field name.
                     */
                    protected ForExplicitType(String fieldName, TypeDescription typeDescription) {
                        this.fieldName = fieldName;
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    protected Resolution resolve(TypeDescription instrumentedType) {
                        FieldList fieldList = typeDescription.getDeclaredFields().filter(named(fieldName).and(isVisibleTo(instrumentedType)));
                        return fieldList.size() == 1
                                ? new Resolution.Resolved(fieldList.getOnly())
                                : new Resolution.Unresolved();
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && fieldName.equals(((ForExplicitType) other).fieldName)
                                && typeDescription.equals(((ForExplicitType) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        int result = fieldName.hashCode();
                        result = 31 * result + typeDescription.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "FieldProxy.Binder.FieldLocator.LookupEngine.ForExplicitType{" +
                                "fieldName='" + fieldName + '\'' +
                                ", typeDescription=" + typeDescription +
                                '}';
                    }
                }
            }

            /**
             * Represents a field locator for a field whos name could be located.
             */
            protected static class Legal extends FieldLocator {

                /**
                 * The name of the field.
                 */
                private final String fieldName;

                /**
                 * Creates a new field locator for a legal field name.
                 *
                 * @param fieldName The name of the field.
                 */
                protected Legal(String fieldName) {
                    this.fieldName = fieldName;
                }

                /**
                 * Considers a given method to expose a field name by following the Java bean naming conventions
                 * for getter and setter methods.
                 *
                 * @param methodDescription The method to consider for such a field name identification.
                 * @return A correspoding field name locator.
                 */
                protected static FieldLocator consider(MethodDescription methodDescription) {
                    String fieldName;
                    if (isSetter().matches(methodDescription)) {
                        fieldName = methodDescription.getInternalName().substring(3);
                    } else if (isGetter().matches(methodDescription)) {
                        fieldName = methodDescription.getInternalName()
                                .substring(methodDescription.getInternalName().startsWith("is") ? 2 : 3);
                    } else {
                        return new Illegal();
                    }
                    return new Legal(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1));
                }

                @Override
                protected LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    return typeDescription.represents(void.class)
                            ? new LookupEngine.ForHierarchy(fieldName)
                            : new LookupEngine.ForExplicitType(fieldName, TargetType.resolve(typeDescription, instrumentedType, TargetType.MATCHER));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && fieldName.equals(((Legal) other).fieldName);
                }

                @Override
                public int hashCode() {
                    return fieldName.hashCode();
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldLocator.Legal{" +
                            "fieldName='" + fieldName + '\'' +
                            '}';
                }
            }

            /**
             * Represents an illegal field locator which can impossible locate a field.
             */
            protected static class Illegal extends FieldLocator {

                @Override
                protected LookupEngine lookup(TypeDescription typeDescription, TypeDescription instrumentedType) {
                    return new LookupEngine.Illegal();
                }

                @Override
                public int hashCode() {
                    return 31;
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || (other != null && other.getClass() == getClass());
                }

                @Override
                public String toString() {
                    return "FieldProxy.Binder.FieldLocator.Illegal{}";
                }
            }
        }

        /**
         * A proxy type for accessing a field either by a getter or a setter.
         */
        protected class AccessorProxy implements AuxiliaryType, StackManipulation {

            /**
             * The name of the field that stores the accessed instance if any.
             */
            protected static final String FIELD_NAME = "instance";

            /**
             * The field that is being accessed.
             */
            private final FieldDescription accessedField;

            /**
             * The type which is accessed.
             */
            private final TypeDescription instrumentedType;

            /**
             * The assigner to use.
             */
            private final Assigner assigner;

            /**
             * The access type to implement.
             */
            private final AccessType accessType;

            /**
             * {@code true} if the generated proxy should be serializable.
             */
            private final boolean serializableProxy;

            /**
             * @param accessedField     The field that is being accessed.
             * @param assigner          The assigner to use.
             * @param instrumentedType  The type which is accessed.
             * @param accessType        The assigner to use.
             * @param serializableProxy {@code true} if the generated proxy should be serializable.
             */
            protected AccessorProxy(FieldDescription accessedField,
                                    Assigner assigner,
                                    TypeDescription instrumentedType,
                                    AccessType accessType,
                                    boolean serializableProxy) {
                this.accessedField = accessedField;
                this.assigner = assigner;
                this.instrumentedType = instrumentedType;
                this.accessType = accessType;
                this.serializableProxy = serializableProxy;
            }

            @Override
            public DynamicType make(String auxiliaryTypeName,
                                    ClassFileVersion classFileVersion,
                                    MethodAccessorFactory methodAccessorFactory) {
                return new ByteBuddy(classFileVersion)
                        .subclass(accessType.proxyType(getterMethod, setterMethod), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(auxiliaryTypeName)
                        .modifiers(DEFAULT_TYPE_MODIFIER)
                        .implement(serializableProxy ? new Class<?>[]{Serializable.class} : new Class<?>[0])
                        .defineConstructor(accessedField.isStatic()
                                ? Collections.<TypeDescription>emptyList()
                                : Collections.singletonList(instrumentedType))
                        .intercept(accessedField.isStatic()
                                ? StaticFieldConstructor.INSTANCE
                                : new InstanceFieldConstructor(instrumentedType))
                        .method(isDeclaredBy(accessType.proxyType(getterMethod, setterMethod)))
                        .intercept(accessType.access(accessedField, assigner, methodAccessorFactory))
                        .make();
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
                TypeDescription auxiliaryType = implementationContext.register(this);
                return new Compound(
                        TypeCreation.forType(auxiliaryType),
                        Duplication.SINGLE,
                        accessedField.isStatic()
                                ? LegalTrivial.INSTANCE
                                : MethodVariableAccess.REFERENCE.loadOffset(0),
                        MethodInvocation.invoke(auxiliaryType.getDeclaredMethods().filter(isConstructor()).getOnly())
                ).apply(methodVisitor, implementationContext);
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Binder getOuter() {
                return Binder.this;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AccessorProxy that = (AccessorProxy) other;
                return serializableProxy == that.serializableProxy
                        && accessType == that.accessType
                        && accessedField.equals(that.accessedField)
                        && assigner.equals(that.assigner)
                        && Binder.this.equals(that.getOuter())
                        && instrumentedType.equals(that.instrumentedType);
            }

            @Override
            public int hashCode() {
                int result = accessedField.hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + assigner.hashCode();
                result = 31 * result + Binder.this.hashCode();
                result = 31 * result + accessType.hashCode();
                result = 31 * result + (serializableProxy ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "FieldProxy.Binder.AccessorProxy{" +
                        "accessedField=" + accessedField +
                        ", instrumentedType=" + instrumentedType +
                        ", assigner=" + assigner +
                        ", accessType=" + accessType +
                        ", serializableProxy=" + serializableProxy +
                        ", binder=" + Binder.this +
                        '}';
            }
        }
    }
}
