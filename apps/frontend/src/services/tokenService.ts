/**
 * Token Service
 * Handles storage and retrieval of authentication tokens
 * 
 * Security Model:
 * - Access Token: Stored in localStorage with encryption (persistent across page reloads)
 * - Refresh Token: Stored in localStorage with encryption (persistent across page reloads)
 * - Token Expiry: Calculated from JWT payload
 * 
 * Note: Both tokens are encrypted before storage to provide basic security.
 * For maximum security in production, consider using httpOnly cookies for refresh tokens.
 */

// Storage keys
const ACCESS_TOKEN_KEY = 'ziboto_access_token';
const REFRESH_TOKEN_KEY = 'ziboto_refresh_token';
const TOKEN_EXPIRY_KEY = 'ziboto_token_expiry';

/**
 * Simple encryption for localStorage (basic obfuscation)
 * For production, consider using Web Crypto API for stronger encryption
 */
const encryptToken = (token: string): string => {
  try {
    if (!token) {
      console.error('Error encrypting token: Token is empty or undefined');
      return token;
    }
    return btoa(token.split('').reverse().join(''));
  } catch (error) {
    console.error('Error encrypting token:', error);
    return token;
  }
};

const decryptToken = (encrypted: string): string => {
  try {
    if (!encrypted) {
      console.error('Error decrypting token: Encrypted token is empty or undefined');
      return encrypted;
    }
    return atob(encrypted).split('').reverse().join('');
  } catch (error) {
    console.error('Error decrypting token:', error);
    return encrypted;
  }
};

export const tokenService = {
  /**
   * Get access token from localStorage
   * Access tokens are now stored in localStorage (encrypted) to persist across page refreshes
   */
  getAccessToken(): string | null {
    try {
      const encrypted = localStorage.getItem(ACCESS_TOKEN_KEY);
      if (!encrypted) {
        return null;
      }
      return decryptToken(encrypted);
    } catch (error) {
      console.error('Error getting access token:', error);
      return null;
    }
  },

  /**
   * Get refresh token from localStorage
   * Refresh tokens are encrypted before storage
   */
  getRefreshToken(): string | null {
    try {
      const encrypted = localStorage.getItem(REFRESH_TOKEN_KEY);
      if (!encrypted) {
        console.log('[TokenService] No refresh token found in localStorage');
        return null;
      }
      
      const decrypted = decryptToken(encrypted);
      console.log('[TokenService] Refresh token retrieved from localStorage:', {
        encryptedPreview: encrypted.substring(0, 20) + '...',
        decryptedPreview: decrypted?.substring(0, 20) + '...',
      });
      return decrypted;
    } catch (error) {
      console.error('Error getting refresh token:', error);
      return null;
    }
  },

  /**
   * Store access and refresh tokens
   * - Access token: Encrypted in localStorage (persists across page reloads)
   * - Refresh token: Encrypted in localStorage (persists across page reloads)
   */
  setTokens(accessToken: string, refreshToken: string): void {
    try {
      console.log('[TokenService] setTokens called with:', {
        hasAccessToken: !!accessToken,
        hasRefreshToken: !!refreshToken,
        accessTokenLength: accessToken?.length || 0,
        refreshTokenLength: refreshToken?.length || 0,
        refreshTokenPreview: refreshToken?.substring(0, 20) + '...',
      });

      if (!accessToken || !refreshToken) {
        console.error('[TokenService] Cannot set tokens: accessToken or refreshToken is undefined');
        return;
      }

      // Store access token in localStorage (encrypted)
      const encryptedAccessToken = encryptToken(accessToken);
      localStorage.setItem(ACCESS_TOKEN_KEY, encryptedAccessToken);
      console.log('[TokenService] Access token saved to localStorage (encrypted):', {
        original: accessToken.substring(0, 20) + '...',
        encrypted: encryptedAccessToken.substring(0, 20) + '...'
      });
      
      // Store refresh token in localStorage (encrypted)
      const encryptedRefreshToken = encryptToken(refreshToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, encryptedRefreshToken);
      console.log('[TokenService] Refresh token saved to localStorage (encrypted):', {
        original: refreshToken.substring(0, 20) + '...',
        encrypted: encryptedRefreshToken.substring(0, 20) + '...'
      });
      
      // Decode JWT to get expiry
      const tokenData = this.decodeToken(accessToken);
      if (tokenData?.exp) {
        localStorage.setItem(TOKEN_EXPIRY_KEY, tokenData.exp.toString());
        console.log('[TokenService] Token expiry set:', new Date(tokenData.exp * 1000).toISOString());
      } else {
        console.warn('[TokenService] Could not extract expiry from token');
      }
      
      console.log('[TokenService] Tokens stored successfully');
    } catch (error) {
      console.error('Error setting tokens:', error);
    }
  },

  /**
   * Set only access token in localStorage
   * Used when refreshing the access token
   */
  setAccessToken(accessToken: string): void {
    try {
      const encryptedAccessToken = encryptToken(accessToken);
      localStorage.setItem(ACCESS_TOKEN_KEY, encryptedAccessToken);
      
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
   * Clears both access token and refresh token from localStorage
   */
  clearTokens(): void {
    try {
      // Clear localStorage
      localStorage.removeItem(ACCESS_TOKEN_KEY);
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
