/**
 * JWT Test Utilities
 * Helper functions for testing JWT authentication in development
 */

import { tokenService } from '../services/tokenService';

/**
 * Generate a mock JWT token for testing
 * WARNING: Only for testing! Never use in production.
 */
export const generateMockToken = (payload: Record<string, any>, expiresInMinutes: number = 15): string => {
  const header = {
    alg: 'HS256',
    typ: 'JWT',
  };

  const now = Math.floor(Date.now() / 1000);
  const tokenPayload = {
    sub: payload.userId || 'test-user-id',
    email: payload.email || 'test@example.com',
    name: payload.name || 'Test User',
    role: payload.role || 'USER',
    iat: now,
    exp: now + expiresInMinutes * 60,
    ...payload,
  };

  // Base64 encode (this is NOT a real JWT signature!)
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify(tokenPayload));
  const mockSignature = btoa('mock-signature');

  return `${encodedHeader}.${encodedPayload}.${mockSignature}`;
};

/**
 * Test token storage and retrieval
 */
export const testTokenService = () => {
  console.group('🧪 Token Service Test');

  // Generate test tokens
  const accessToken = generateMockToken({ userId: 'test-123', email: 'test@example.com' }, 15);
  const refreshToken = generateMockToken({ userId: 'test-123', email: 'test@example.com' }, 10080); // 7 days

  console.log('1. Storing tokens...');
  tokenService.setTokens(accessToken, refreshToken);

  console.log('2. Retrieving access token...');
  const retrievedAccessToken = tokenService.getAccessToken();
  console.log('   ✓ Access token retrieved:', retrievedAccessToken ? 'Yes' : 'No');

  console.log('3. Retrieving refresh token...');
  const retrievedRefreshToken = tokenService.getRefreshToken();
  console.log('   ✓ Refresh token retrieved:', retrievedRefreshToken ? 'Yes' : 'No');

  console.log('4. Checking token expiry...');
  const isExpired = tokenService.isTokenExpired();
  console.log('   ✓ Token expired:', isExpired ? 'Yes' : 'No (expected)');

  console.log('5. Checking valid tokens...');
  const hasValid = tokenService.hasValidTokens();
  console.log('   ✓ Has valid tokens:', hasValid ? 'Yes' : 'No');

  console.log('6. Decoding token...');
  const decoded = tokenService.decodeToken(accessToken);
  console.log('   ✓ Decoded payload:', decoded);

  console.log('7. Getting time until expiry...');
  const timeUntilExpiry = tokenService.getTimeUntilExpiry();
  console.log('   ✓ Time until expiry:', Math.round(timeUntilExpiry / 1000), 'seconds');

  console.log('8. Clearing tokens...');
  tokenService.clearTokens();
  const clearedAccessToken = tokenService.getAccessToken();
  const clearedRefreshToken = tokenService.getRefreshToken();
  console.log('   ✓ Tokens cleared:', !clearedAccessToken && !clearedRefreshToken ? 'Yes' : 'No');

  console.groupEnd();
};

/**
 * Test expired token handling
 */
export const testExpiredToken = () => {
  console.group('🧪 Expired Token Test');

  // Generate expired token (expired 5 minutes ago)
  const expiredToken = generateMockToken({ userId: 'test-123' }, -5);
  const validRefreshToken = generateMockToken({ userId: 'test-123' }, 10080);

  console.log('1. Storing expired access token...');
  tokenService.setTokens(expiredToken, validRefreshToken);

  console.log('2. Checking if token is expired...');
  const isExpired = tokenService.isTokenExpired();
  console.log('   ✓ Token expired:', isExpired ? 'Yes (expected)' : 'No');

  console.log('3. Checking valid tokens...');
  const hasValid = tokenService.hasValidTokens();
  console.log('   ✓ Has valid tokens:', hasValid ? 'No (expected)' : 'Yes');

  console.log('4. Cleaning up...');
  tokenService.clearTokens();

  console.groupEnd();
};

/**
 * Test token encryption
 */
export const testTokenEncryption = () => {
  console.group('🧪 Token Encryption Test');

  const refreshToken = generateMockToken({ userId: 'test-123' }, 10080);

  console.log('1. Original refresh token length:', refreshToken.length);

  console.log('2. Storing refresh token...');
  tokenService.setTokens(generateMockToken({}, 15), refreshToken);

  console.log('3. Checking localStorage...');
  const encryptedToken = localStorage.getItem('ziboto_refresh_token');
  console.log('   ✓ Token in localStorage (encrypted):', encryptedToken?.substring(0, 50) + '...');

  console.log('4. Retrieving refresh token...');
  const decryptedToken = tokenService.getRefreshToken();
  console.log('   ✓ Tokens match after encryption/decryption:', decryptedToken === refreshToken ? 'Yes' : 'No');

  console.log('5. Cleaning up...');
  tokenService.clearTokens();

  console.groupEnd();
};

/**
 * Simulate token refresh scenario
 */
export const simulateTokenRefresh = () => {
  console.group('🧪 Token Refresh Simulation');

  // Old tokens
  const oldAccessToken = generateMockToken({ userId: 'test-123', email: 'test@example.com' }, -1); // Expired
  const oldRefreshToken = generateMockToken({ userId: 'test-123' }, 10080);

  console.log('1. Storing old (expired) tokens...');
  tokenService.setTokens(oldAccessToken, oldRefreshToken);
  console.log('   ✓ Old access token expired:', tokenService.isTokenExpired() ? 'Yes' : 'No');

  // Simulate refresh response with new tokens
  const newAccessToken = generateMockToken({ userId: 'test-123', email: 'test@example.com' }, 15);
  const newRefreshToken = generateMockToken({ userId: 'test-123' }, 10080);

  console.log('2. Simulating token refresh (storing new tokens)...');
  tokenService.setTokens(newAccessToken, newRefreshToken);

  console.log('3. Verifying new access token...');
  const hasValidTokens = tokenService.hasValidTokens();
  console.log('   ✓ New tokens valid:', hasValidTokens ? 'Yes' : 'No');

  console.log('4. Cleaning up...');
  tokenService.clearTokens();

  console.groupEnd();
};

/**
 * Run all tests
 */
export const runAllJwtTests = () => {
  console.clear();
  console.log('🚀 Running JWT Authentication Tests...\n');

  testTokenService();
  console.log('\n');

  testExpiredToken();
  console.log('\n');

  testTokenEncryption();
  console.log('\n');

  simulateTokenRefresh();
  console.log('\n');

  console.log('✅ All tests completed!');
  console.log('Note: These are client-side tests only. Backend validation is still required.');
};

/**
 * Get current authentication status
 */
export const getAuthStatus = () => {
  const status = {
    hasAccessToken: !!tokenService.getAccessToken(),
    hasRefreshToken: !!tokenService.getRefreshToken(),
    isExpired: tokenService.isTokenExpired(),
    isExpiringSoon: tokenService.isTokenExpiringSoon(),
    hasValidTokens: tokenService.hasValidTokens(),
    timeUntilExpiry: tokenService.getTimeUntilExpiry(),
    decodedAccessToken: tokenService.getAccessToken() 
      ? tokenService.decodeToken(tokenService.getAccessToken()!)
      : null,
  };

  console.group('🔐 Current Authentication Status');
  console.table({
    'Has Access Token': status.hasAccessToken ? '✅' : '❌',
    'Has Refresh Token': status.hasRefreshToken ? '✅' : '❌',
    'Token Expired': status.isExpired ? '⚠️ Yes' : '✅ No',
    'Expiring Soon': status.isExpiringSoon ? '⚠️ Yes' : '✅ No',
    'Has Valid Tokens': status.hasValidTokens ? '✅ Yes' : '❌ No',
    'Time Until Expiry': status.timeUntilExpiry > 0 
      ? `${Math.round(status.timeUntilExpiry / 1000)}s` 
      : 'Expired',
  });
  
  if (status.decodedAccessToken) {
    console.log('Decoded Access Token:', status.decodedAccessToken);
  }
  
  console.groupEnd();

  return status;
};

// Make functions available in browser console for easy testing
if (typeof window !== 'undefined') {
  (window as any).jwtTest = {
    runAll: runAllJwtTests,
    testTokenService,
    testExpiredToken,
    testEncryption: testTokenEncryption,
    simulateRefresh: simulateTokenRefresh,
    getStatus: getAuthStatus,
    generateMockToken,
  };
  
  console.log('💡 JWT Test utilities loaded! Use window.jwtTest in console:');
  console.log('   - window.jwtTest.runAll() - Run all tests');
  console.log('   - window.jwtTest.getStatus() - Check current auth status');
  console.log('   - window.jwtTest.testTokenService() - Test token storage');
  console.log('   - window.jwtTest.testExpiredToken() - Test expired token handling');
  console.log('   - window.jwtTest.testEncryption() - Test token encryption');
  console.log('   - window.jwtTest.simulateRefresh() - Simulate token refresh');
}
