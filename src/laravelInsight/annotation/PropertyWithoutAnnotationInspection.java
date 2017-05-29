package net.rentalhost.idea.laravelInsight.annotation;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import net.rentalhost.idea.api.PhpClassUtil;
import net.rentalhost.idea.api.PhpDocCommentUtil;
import net.rentalhost.idea.api.PhpExpressionUtil;
import net.rentalhost.idea.laravelInsight.resources.LaravelClasses;

public class PropertyWithoutAnnotationInspection extends PhpInspection {
    private static final String messagePropertyUndefined = "@property $%s was not annotated";

    @NotNull
    @Override
    public String getShortName() {
        return "PropertyWithoutAnnotationInspection";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(
        @NotNull final ProblemsHolder problemsHolder,
        final boolean b
    ) {
        return new ElementVisitor(problemsHolder);
    }

    private static class ElementVisitor extends PhpElementVisitor {
        private final ProblemsHolder problemsHolder;

        ElementVisitor(final ProblemsHolder problemsHolder) {
            this.problemsHolder = problemsHolder;
        }

        @Override
        public void visitPhpField(final Field field) {
            final String fieldName = field.getName();

            if (!Objects.equals(fieldName, "casts") &&
                !Objects.equals(fieldName, "dates")) {
                return;
            }

            if (!PhpType.intersects(field.getType(), PhpType.ARRAY)) {
                return;
            }

            final PhpClass fieldClass = field.getContainingClass();

            assert fieldClass != null;

            if (!PhpClassUtil.hasSuperOfType(fieldClass, LaravelClasses.ELOQUENT_MODEL.toString())) {
                return;
            }

            final PsiElement fieldValue = field.getDefaultValue();

            if (!(fieldValue instanceof ArrayCreationExpression)) {
                return;
            }

            final Iterable<ArrayHashElement> fieldHashes = ((ArrayCreationExpression) fieldValue).getHashElements();

            for (final ArrayHashElement fieldHash : fieldHashes) {
                final PhpPsiElement fieldHashValue = fieldHash.getValue();
                assert fieldHashValue != null;

                final PhpExpression fieldHashResolvedValue = PhpExpressionUtil.from((PhpExpression) fieldHashValue);

                if (!(fieldHashResolvedValue instanceof StringLiteralExpression)) {
                    continue;
                }

                final PhpPsiElement hashKey = fieldHash.getKey();
                assert hashKey != null;

                final PhpExpression hashKeyResolvedValue = PhpExpressionUtil.from((PhpExpression) hashKey);

                if (!(hashKeyResolvedValue instanceof StringLiteralExpression)) {
                    continue;
                }

                final String hashKeyContents = ((StringLiteralExpression) hashKeyResolvedValue).getContents();

                PhpClass fieldClassCurrent = fieldClass;
                boolean  isNotAnnotated    = true;

                while (fieldClassCurrent != null) {
                    final PhpDocComment classDocComment = fieldClassCurrent.getDocComment();

                    if (classDocComment != null) {
                        if (PhpDocCommentUtil.hasProperty(classDocComment, hashKeyContents)) {
                            isNotAnnotated = false;
                            break;
                        }
                    }

                    fieldClassCurrent = PhpClassUtil.getSuper(fieldClassCurrent);
                }

                if (isNotAnnotated) {
                    registerPropertyUndefined(hashKey, hashKeyContents);
                }
            }
        }

        private void registerPropertyUndefined(
            final PsiElement hashKey,
            final String hashKeyContents
        ) {
            problemsHolder.registerProblem(hashKey,
                                           String.format(messagePropertyUndefined, hashKeyContents),
                                           ProblemHighlightType.WEAK_WARNING);
        }
    }
}
