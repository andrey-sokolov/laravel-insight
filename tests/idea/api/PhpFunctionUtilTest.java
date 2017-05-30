package net.rentalhost.idea.api;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.junit.Assert;

import java.util.Set;

import net.rentalhost.suite.FixtureSuite;

public class PhpFunctionUtilTest extends FixtureSuite {
    private static PhpNamedElement getFunction(
        final PsiElement fileSample,
        final String functionName
    ) {
        return getElementByName(fileSample, functionName);
    }

    private static boolean hasOnlyTypes(
        final PsiElement fileSample,
        final String functionName,
        final PhpType... expectedTypesList
    ) {
        final Set<String> returnTypes = valueOf(PhpFunctionUtil.getReturnType(getFunction(fileSample, functionName))).getTypes();

        for (final PhpType expectedTypes : expectedTypesList) {
            for (final String expectedType : expectedTypes.getTypes()) {
                if (!returnTypes.contains(expectedType)) {
                    return false;
                }

                returnTypes.remove(expectedType);
            }
        }

        return returnTypes.isEmpty();
    }

    public void testGetReturnType() {
        final PsiFile fileSample = getResourceFile("api/PhpFunctionUtil.returnTypes.php");

        // Bogus test...
        Assert.assertFalse(hasOnlyTypes(fileSample, "respectPhpdocReturnType_StringOnly", PhpType.STRING, PhpType.NULL));

        // Tests...
        final PhpType typeUnresolvableQualifier = PhpType.builder().add("\\UnresolvableQualifier").build();

        Assert.assertTrue(hasOnlyTypes(fileSample, "respectPhpdocReturnType_StringOnly", PhpType.STRING));
        Assert.assertTrue(hasOnlyTypes(fileSample, "respectPhpdocReturnType_StringOrNull", PhpType.STRING, PhpType.NULL));
        Assert.assertTrue(hasOnlyTypes(fileSample, "respectPhpdocReturnType_NullOrString", PhpType.STRING, PhpType.NULL));
        Assert.assertTrue(hasOnlyTypes(fileSample, "respectPhpdocReturnType_AllScalarTypes", PhpType.SCALAR));
        Assert.assertTrue(hasOnlyTypes(fileSample, "respectPhpdocReturnType_UnresolvableQualifier", typeUnresolvableQualifier));
    }
}