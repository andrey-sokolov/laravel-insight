package net.rentalhost.idea.api;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpUseImpl;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PhpClassUtil {
    ;

    @NotNull
    public static Iterable<PhpUse> getTraitsDeclared(@NotNull final PsiElement classObject) {
        assert classObject instanceof PhpClass;

        final List<PhpUseList> usesLists = PsiTreeUtil.getChildrenOfTypeAsList(classObject, PhpUseList.class);
        final Stack<PhpUse>    result    = new Stack<>();

        for (final PhpUseList useList : usesLists) {
            for (final PhpUse useDeclaration : useList.getDeclarations()) {
                if (useDeclaration.isTraitImport()) {
                    result.push(useDeclaration);
                }
            }
        }

        return result;
    }

    @Nullable
    public static ClassReference findSuperOfType(
        @NotNull final PhpClass classObject,
        @NotNull final String superNameExpected
    ) {
        return RecursionResolver.resolve(classObject, resolver -> {
            final PhpClass       classCurrent        = (PhpClass) resolver.getObject();
            final ClassReference classSuperReference = getSuperReference(classCurrent);

            if (classSuperReference == null) {
                return null;
            }

            if (Objects.equals(classSuperReference.getFQN(), superNameExpected)) {
                return classSuperReference;
            }

            final PhpClass classCurrentResolved = (PhpClass) classSuperReference.resolve();

            if (classCurrentResolved == null) {
                return null;
            }

            return (ClassReference) resolver.resolve(classCurrentResolved);
        });
    }

    @Nullable
    public static ClassReference findTraitOfType(
        @NotNull final PhpClass classObject,
        @NotNull final String traitNameExpected
    ) {
        return RecursionResolver.resolve(classObject, resolver -> {
            final PhpClass         classCurrent = (PhpClass) resolver.getObject();
            final Iterable<PhpUse> classTraits  = getTraitsDeclared(classCurrent);

            for (final PhpUse classTrait : classTraits) {
                final PhpReference traitTargetReference = classTrait.getTargetReference();
                assert traitTargetReference != null;

                if (Objects.equals(traitTargetReference.getFQN(), traitNameExpected)) {
                    return (ClassReference) traitTargetReference;
                }

                final PhpClass traitResolved = (PhpClass) traitTargetReference.resolve();

                if (traitResolved == null) {
                    continue;
                }

                final ClassReference traitOfTrait = (ClassReference) resolver.resolve(traitResolved);

                if (traitOfTrait == null) {
                    continue;
                }

                return traitOfTrait;
            }

            final PhpClass classCurrentResolved = getSuper(classCurrent);

            if (classCurrentResolved == null) {
                return null;
            }

            return (ClassReference) resolver.resolve(classCurrentResolved);
        });
    }

    @Nullable
    public static ClassReference getSuperReference(@NotNull final PhpClass phpClass) {
        final List<ClassReference> classExtendsList = phpClass.getExtendsList().getReferenceElements();

        if (classExtendsList.isEmpty()) {
            return null;
        }

        return classExtendsList.get(0);
    }

    @Nullable
    public static PhpClass getSuper(@NotNull final PhpClass phpClass) {
        final ClassReference superReference = getSuperReference(phpClass);

        if (superReference == null) {
            return null;
        }

        return (PhpClass) superReference.resolve();
    }

    @Nullable
    public static PhpClass getTraitContainingClass(@NotNull final PhpUse trait) {
        if (trait.isTraitImport()) {
            final PhpUseList useList = PhpUseImpl.getUseList(trait);
            assert useList != null;

            return (PhpClass) useList.getParent();
        }

        return null;
    }

    @Nullable
    public static Field findPropertyDeclaration(
        @NotNull final PhpClass classObject,
        @NotNull final String propertyNameExpected
    ) {
        return RecursionResolver.resolve(classObject, resolver -> {
            final PhpClass classCurrent   = (PhpClass) resolver.getObject();
            final Field    classComponent = findClassComponentByName(propertyNameExpected, classCurrent.getFields());

            if (classComponent != null) {
                return classComponent;
            }

            final PhpClass classSuperResolved = getSuper(classCurrent);

            if (classSuperResolved == null) {
                return null;
            }

            return (Field) resolver.resolve(classSuperResolved);
        });
    }

    @Nullable
    private static <ComponentType extends PhpNamedElement> ComponentType findClassComponentByName(
        @NotNull final String propertyNameExpected,
        @NotNull final Iterable<ComponentType> classComponents
    ) {
        for (final ComponentType classField : classComponents) {
            if (classField.getName().equals(propertyNameExpected)) {
                return classField;
            }
        }

        return null;
    }

    @Nullable
    public static Method findMethodDeclaration(
        @NotNull final PhpClass classObject,
        @NotNull final String methodNameExpected
    ) {
        return RecursionResolver.resolve(classObject, resolver -> {
            final PhpClass classCurrent   = (PhpClass) resolver.getObject();
            final Method   classComponent = findClassComponentByName(methodNameExpected, classCurrent.getMethods());

            if (classComponent != null) {
                return classComponent;
            }

            final PhpClass classSuperResolved = getSuper(classCurrent);

            if (classSuperResolved == null) {
                return null;
            }

            return (Method) resolver.resolve(classSuperResolved);
        });
    }
}
