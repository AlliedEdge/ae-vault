/**
 * Token Service
 * Handles storage and retrieval of authentication tokens
 * 
 * Security Model:
 * - Access Token: Stored in memory only (more secure, lost on page refresh)
 * - Refresh Token: Stored in localStorage with encryption (persistent across page reloads)
 * - Token Expiry: Calculated from JWT payload
 * 
 * Note: For maximum security, refresh tokens should be stored in httpOnly cookies.
 * This implementation uses localStorage as a fallback when httpOnly cookies aren't available.
 */

// Storage keys
const REFRESH_TOKEN_KEY = 'ziboto_refresh_token';
const TOKEN_EXPIRY_KEY = 'ziboto_token_expiry';

// In-memory storage for access token (cleared on page refresh)
let accessTokenMemory: string | null = null;

/**
 * Simple encryption for localStorage (basic obfuscation)
 * For production, consider using Web Crypto API for stronger encryption
 */
const encryptToken = (token: string): string => {
  try {
    return btoa(token.split('').reverse().join(''));
  } catch (error) {
    console.error('Error encrypting token:', error);
    return token;
  }
};

const decryptToken = (encrypted: string): string => {
  try {
    return atob(encrypted).split('').reverse().join('');
  } catch (error) {
    console.error('Error decrypting token:', error);
    return encrypted;
  }
};

export const tokenService = {
  /**
   * Get access token from memory
   * Access tokens are stored in memory only and cleared on page refresh
   */
  getAccessToken(): string | null {
    return accessTokenMemory;
  },

  /**
   * Get refresh token from localStorage
   * Refresh tokens are encrypted before storage
   */
  getRefreshToken(): string | null {
    try {
      const encrypted = localStorage.getItem(REFRESH_TOKEN_KEY);
      return encrypted ? decryptToken(encrypted) : null;
    } catch (error) {
      console.error('Error getting refresh token:', error);
      return null;
    }
  },

  /**
   * Store access and refresh tokens
   * - Access token: In-memory only (cleared on page refresh)
   * - Refresh token: Encrypted in localStorage (persists across page reloads)
   */
  setTokens(accessToken: string, refreshToken: string): void {
    try {
      // Store access token in memory
      accessTokenMemory = accessToken;
      
      // Store refresh token in localStorage (encrypted)
      localStorage.setItem(REFRESH_TOKEN_KEY, encryptToken(refreshToken));
      
      // Decode JWT to get expiry
      const tokenData = this.decodeToken(accessToken);
      if (tokenData?.exp) {
        localStorage.setItem(TOKEN_EXPIRY_KEY, tokenData.exp.toString());
      }
      
      console.log('[TokenService] Tokens stored successfully');
    } catch (error) {
      console.error('Error setting tokens:', error);
    }
  },

  /**
   * Set only access token in memory
   * Used when refreshing the access token
   */
  setAccessToken(accessToken: string): void {
    try {
      accessTokenMemory = accessToken;
      
      // Update expiry
      const tokenData = this.decodeToken(accessToken);
      if (tokenData?.exp) {
        localStorage.setItem(TOKEN_EXPIRY_KEY, tokenData.exp.toString());
      }
      
      console.log('[TokenService] Access token refreshed');
    } catch (error) {
      console.error('Error setting access token:', error);
    }
  },

  /**
   * Clear all tokens from storage
   * Clears both in-memory access token and localStorage refresh token
   */
  clearTokens(): void {
    try {
      // Clear in-memory access token
      accessTokenMemory = null;
      
      // Clear localStorage
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(TOKEN_EXPIRY_KEY);
      
      console.log('[TokenService] All tokens cleared');
    } catch (error) {
      console.error('Error clearing tokens:', error);
    }
  },

  /**
   * Check if access token is expired
   * Uses a 1-minute buffer to refresh before actual expiry
   */
  isTokenExpired(): boolean {
    try {
      const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
      if (!expiry) return true;

      const expiryTime = parseInt(expiry, 10) * 1000; // Convert to milliseconds
      const currentTime = Date.now();

      // Consider token expired 1 minute before actual expiry (buffer for refresh)
      return currentTime >= expiryTime - 60 * 1000;
    } catch (error) {
      console.error('Error checking token expiry:', error);
      return true;
    }
  },

  /**
   * Check if token is about to expire (within 5 minutes)
   * Used to trigger proactive token refresh
   */
  isTokenExpiringSoon(): boolean {
    try {
      const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
      if (!expiry) return true;

      const expiryTime = parseInt(expiry, 10) * 1000;
      const currentTime = Date.now();

      // Check if token expires within 5 minutes
      return currentTime >= expiryTime - 5 * 60 * 1000;
    } catch (error) {
      console.error('Error checking token expiry:', error);
      return true;
    }
  },

  /**
   * Check if user has valid tokens
   * Validates both access token (in memory) and refresh token (in storage)
   */
  hasValidTokens(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    return !!(accessToken && refreshToken && !this.isTokenExpired());
  },

  /**
   * Check if refresh token is available
   * Used to determine if we can attempt token refresh
   */
  hasRefreshToken(): boolean {
    return !!this.getRefreshToken();
  },

  /**
   * Decode JWT token (simple base64 decode)
   * Note: This doesn't validate the token, just decodes it
   */
  decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );

      return JSON.parse(jsonPayload);
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  },

  /**
   * Get user data from token
   */
  getUserFromToken(): any {
    const token = this.getAccessToken();
    if (!token) return null;

    const decoded = this.decodeToken(token);
    return decoded ? {
      id: decoded.sub || decoded.userId || decoded.id,
      email: decoded.email,
      name: decoded.name,
      role: decoded.role,
      ...decoded,
    } : null;
  },

  /**
   * Get time until token expires (in milliseconds)
   */
  getTimeUntilExpiry(): number {
    try {
      const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
      if (!expiry) return 0;

      const expiryTime = parseInt(expiry, 10) * 1000;
      const currentTime = Date.now();

      return Math.max(0, expiryTime - currentTime);
    } catch (error) {
      console.error('Error getting time until expiry:', error);
      return 0;
    }
  },
};
