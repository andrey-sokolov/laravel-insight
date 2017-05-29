package net.rentalhost.idea.api;

import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocPropertyTag;

import java.util.List;
import java.util.Objects;

public enum PhpDocCommentUtil {
    ;

    public static PhpDocProperty findProperty(
        final PhpDocComment docComment,
        final String propertyName
    ) {
        final List<PhpDocPropertyTag> classPropertyTags = docComment.getPropertyTags();

        for (final PhpDocPropertyTag classPropertyTag : classPropertyTags) {
            final PhpDocProperty property = classPropertyTag.getProperty();

            if ((property != null) && Objects.equals(property.getName(), propertyName)) {
                return property;
            }
        }

        return null;
    }

    public static boolean hasProperty(
        final PhpDocComment docComment,
        final String propertyName
    ) {
        return findProperty(docComment, propertyName) != null;
    }
}
