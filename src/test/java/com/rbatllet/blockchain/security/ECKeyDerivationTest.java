package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.concurrent.TimeUnit;
import java.math.BigInteger;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.security.ECKeyDerivation.ECKeyDerivationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the improved ECKeyDerivation class.
 * 
 * Tests cover:
 * - Basic key derivation functionality
 * - Input validation and error handling  
 * - Performance characteristics
 * - Cryptographic correctness and security
 * - Integration with the crypto ecosystem
 */
public class ECKeyDerivationTest {

    private ECKeyDerivation keyDerivation;
    private KeyPair testKeyPair;

    @BeforeEach
    void setUp() {
        keyDerivation = new ECKeyDerivation();
        testKeyPair = CryptoUtil.generateECKeyPair();
    }

    @AfterEach
    void tearDown() {
        keyDerivation = null;
        testKeyPair = null;
    }

    @Nested
    @DisplayName("Basic Key Derivation Tests")
    class BasicKeyDerivationTests {

        @Test
        @DisplayName("Should derive valid public key from EC private key")
        void testBasicKeyDerivation() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            assertNotNull(derivedKey, "Derived public key should not be null");
            assertEquals(CryptoUtil.EC_ALGORITHM, derivedKey.getAlgorithm(), 
                "Derived key should use EC algorithm");
            assertTrue(derivedKey instanceof ECPublicKey, 
                "Derived key should be instance of ECPublicKey");

            String testData = "Test message for signature verification";
            String signature = CryptoUtil.signData(testData, testKeyPair.getPrivate());
            assertTrue(CryptoUtil.verifySignature(testData, signature, derivedKey),
                "Derived public key should verify signatures from corresponding private key");
        }

        @Test
        @DisplayName("Should produce same result as original key pair")
        void testKeyPairConsistency() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());
            
            ECPublicKey originalECKey = (ECPublicKey) testKeyPair.getPublic();
            ECPublicKey derivedECKey = (ECPublicKey) derivedKey;
            
            assertEquals(originalECKey.getW().getAffineX(), derivedECKey.getW().getAffineX(),
                "X coordinates should match");
            assertEquals(originalECKey.getW().getAffineY(), derivedECKey.getW().getAffineY(),
                "Y coordinates should match");
            
            assertTrue(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), derivedKey),
                "verifyKeyPair should confirm the keys match");
        }

        @RepeatedTest(3)
        @DisplayName("Should consistently derive same public key")
        void testDerivationConsistency() {
            PublicKey derivedKey1 = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());
            PublicKey derivedKey2 = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            ECPublicKey ecKey1 = (ECPublicKey) derivedKey1;
            ECPublicKey ecKey2 = (ECPublicKey) derivedKey2;
            
            assertEquals(ecKey1.getW().getAffineX(), ecKey2.getW().getAffineX(),
                "Multiple derivations should produce same X coordinate");
            assertEquals(ecKey1.getW().getAffineY(), ecKey2.getW().getAffineY(),
                "Multiple derivations should produce same Y coordinate");
        }

        @Test
        @DisplayName("Should support derivation with custom curve parameters")
        void testCustomCurveParameters() throws Exception {
            ECParameterSpec curveParams = keyDerivation.getCurveParameters(CryptoUtil.EC_CURVE);
            
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(
                testKeyPair.getPrivate(), curveParams);

            assertNotNull(derivedKey, "Should derive key with custom parameters");
            assertTrue(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), derivedKey),
                "Derived key should form valid pair with private key");
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException for null private key")
        void testNullPrivateKey() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keyDerivation.derivePublicKeyFromPrivate(null),
                "Should throw IllegalArgumentException for null private key"
            );
            assertEquals("Private key cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-EC private key")
        void testNonECPrivateKey() throws Exception {
            KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance("RSA");
            rsaKeyGen.initialize(2048);
            KeyPair rsaKeyPair = rsaKeyGen.generateKeyPair();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keyDerivation.derivePublicKeyFromPrivate(rsaKeyPair.getPrivate()),
                "Should throw IllegalArgumentException for non-EC key"
            );
            assertTrue(exception.getMessage().contains("Expected EC key"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null curve parameters")
        void testNullCurveParameters() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate(), null),
                "Should throw exception for null curve parameters"
            );
            assertEquals("Curve parameters cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid curve name")
        void testInvalidCurveName() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> keyDerivation.getCurveParameters("invalid-curve-name"),
                "Should throw exception for invalid curve name"
            );
            assertTrue(exception.getMessage().contains("Unsupported or invalid curve"));
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Should derive keys efficiently")
        void testPerformance() {
            int iterations = 500;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < iterations; i++) {
                PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());
                assertNotNull(derivedKey, "Each derived key should be valid");
            }

            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 10000, 
                "Should complete " + iterations + " derivations in under 10 seconds, took: " + duration + "ms");
            
            double avgTime = duration / (double) iterations;
            System.out.println("Derived " + iterations + " keys in " + duration + "ms " +
                "(avg: " + String.format("%.3f", avgTime) + "ms per derivation)");
        }

        @Test
        @DisplayName("Should efficiently cache curve parameters")
        void testCurveParameterCaching() {
            String curveName = CryptoUtil.EC_CURVE;
            
            long startTime1 = System.nanoTime();
            ECParameterSpec params1 = keyDerivation.getCurveParameters(curveName);
            long duration1 = System.nanoTime() - startTime1;
            
            long startTime2 = System.nanoTime();
            ECParameterSpec params2 = keyDerivation.getCurveParameters(curveName);
            long duration2 = System.nanoTime() - startTime2;
            
            assertNotNull(params1, "First parameter lookup should succeed");
            assertNotNull(params2, "Second parameter lookup should succeed");
            assertSame(params1, params2, "Should return same cached instance");
            assertTrue(duration2 < duration1, "Cached lookup should be faster");
        }
    }

    @Nested
    @DisplayName("Security and Correctness Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should produce mathematically correct public keys")
        void testMathematicalCorrectness() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            ECPublicKey ecDerivedKey = (ECPublicKey) derivedKey;
            ECPrivateKey ecPrivateKey = (ECPrivateKey) testKeyPair.getPrivate();

            ECPoint publicPoint = ecDerivedKey.getW();
            assertNotNull(publicPoint, "Public key point should not be null");
            assertNotEquals(ECPoint.POINT_INFINITY, publicPoint, 
                "Public key point should not be at infinity");
            
            assertTrue(keyDerivation.isPointOnCurve(publicPoint, ecDerivedKey.getParams()),
                "Derived public key point should be on the curve");

            BigInteger privateScalar = ecPrivateKey.getS();
            assertNotNull(privateScalar, "Private key scalar should not be null");
            assertTrue(privateScalar.compareTo(BigInteger.ZERO) > 0,
                "Private key should be positive");
            assertTrue(privateScalar.compareTo(ecPrivateKey.getParams().getOrder()) < 0,
                "Private key should be less than curve order");
        }

        @Test
        @DisplayName("Should maintain cryptographic relationships")
        void testCryptographicRelationship() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            String[] testMessages = {
                "Short message",
                "A much longer message that tests the cryptographic relationship",
                "Message with special characters: !@#$%^&*()_+-=[]{}",
                "Unicode message: 你好世界 🌍",
                ""
            };

            for (String message : testMessages) {
                String signature = CryptoUtil.signData(message, testKeyPair.getPrivate());
                
                assertTrue(CryptoUtil.verifySignature(message, signature, derivedKey),
                    "Derived key should verify signature for message");
                assertTrue(CryptoUtil.verifySignature(message, signature, testKeyPair.getPublic()),
                    "Original key should verify signature for same message");
            }
        }

        @Test
        @DisplayName("Should validate point-on-curve correctly")
        void testPointOnCurveValidation() {
            ECPublicKey ecPublicKey = (ECPublicKey) testKeyPair.getPublic();
            ECPoint validPoint = ecPublicKey.getW();
            ECParameterSpec curveParams = ecPublicKey.getParams();
            
            assertTrue(keyDerivation.isPointOnCurve(validPoint, curveParams),
                "Valid EC public key point should be on curve");
            assertFalse(keyDerivation.isPointOnCurve(ECPoint.POINT_INFINITY, curveParams),
                "Point at infinity should not be considered on curve");
            assertFalse(keyDerivation.isPointOnCurve(null, curveParams),
                "Null point should not be considered on curve");
            
            ECPoint invalidPoint = new ECPoint(BigInteger.ONE, BigInteger.ONE);
            assertFalse(keyDerivation.isPointOnCurve(invalidPoint, curveParams),
                "Arbitrary point should not be on curve");
        }
    }

    @Nested
    @DisplayName("Key Pair Verification Tests")
    class KeyPairVerificationTests {

        @Test
        @DisplayName("Should correctly verify matching key pairs")
        void testValidKeyPairVerification() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            assertTrue(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), testKeyPair.getPublic()),
                "Original key pair should verify as valid");
            assertTrue(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), derivedKey),
                "Private key and derived public key should verify as valid pair");
        }

        @Test
        @DisplayName("Should reject non-matching key pairs")
        void testInvalidKeyPairVerification() {
            KeyPair anotherKeyPair = CryptoUtil.generateECKeyPair();

            assertFalse(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), anotherKeyPair.getPublic()),
                "Mismatched key pair should not verify as valid");
            assertFalse(keyDerivation.verifyKeyPair(anotherKeyPair.getPrivate(), testKeyPair.getPublic()),
                "Reverse mismatched key pair should not verify as valid");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate seamlessly with CryptoUtil")
        void testCryptoUtilIntegration() {
            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(testKeyPair.getPrivate());

            String originalData = "Integration test data for ECKeyDerivation";
            
            String signature = CryptoUtil.signData(originalData, testKeyPair.getPrivate());
            assertNotNull(signature, "Signature should not be null");
            
            assertTrue(CryptoUtil.verifySignature(originalData, signature, derivedKey),
                "Should verify signature with derived key");
            
            String publicKeyString = CryptoUtil.publicKeyToString(derivedKey);
            PublicKey reconstructedKey = CryptoUtil.stringToPublicKey(publicKeyString);
            
            assertTrue(CryptoUtil.verifySignature(originalData, signature, reconstructedKey),
                "Should verify signature with reconstructed key");
            assertTrue(keyDerivation.verifyKeyPair(testKeyPair.getPrivate(), reconstructedKey),
                "Reconstructed key should form valid pair with original private key");
        }

        @Test
        @DisplayName("Should work with hierarchical key management")
        void testHierarchicalKeyIntegration() {
            CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
            PrivateKey rootPrivateKey = CryptoUtil.stringToPrivateKey(rootKey.getPrivateKeyEncoded());

            PublicKey derivedRootPublicKey = keyDerivation.derivePublicKeyFromPrivate(rootPrivateKey);

            assertNotNull(derivedRootPublicKey, "Should derive public key from hierarchical private key");
            assertTrue(keyDerivation.verifyKeyPair(rootPrivateKey, derivedRootPublicKey),
                "Derived key should form valid pair with hierarchical private key");
            
            String testData = "Hierarchical key test";
            String signature = CryptoUtil.signData(testData, rootPrivateKey);
            assertTrue(CryptoUtil.verifySignature(testData, signature, derivedRootPublicKey),
                "Derived key should work with hierarchical key management");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle exception scenarios correctly")
        void testCustomExceptionHandling() {
            try {
                keyDerivation.derivePublicKeyFromPrivate(null);
                fail("Should have thrown an exception");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("Private key cannot be null"));
            } catch (ECKeyDerivationException e) {
                fail("Should throw IllegalArgumentException, not ECKeyDerivationException for null input");
            }
        }

        @Test
        @DisplayName("Should maintain consistency under error conditions")
        void testErrorConditionConsistency() {
            ECKeyDerivation derivation1 = new ECKeyDerivation();
            ECKeyDerivation derivation2 = new ECKeyDerivation();

            assertThrows(IllegalArgumentException.class,
                () -> derivation1.derivePublicKeyFromPrivate(null));
            assertThrows(IllegalArgumentException.class,
                () -> derivation2.derivePublicKeyFromPrivate(null));
            
            PublicKey key1 = derivation1.derivePublicKeyFromPrivate(testKeyPair.getPrivate());
            PublicKey key2 = derivation2.derivePublicKeyFromPrivate(testKeyPair.getPrivate());
            
            assertNotNull(key1, "First instance should still work after error");
            assertNotNull(key2, "Second instance should still work after error");
        }
    }
}
