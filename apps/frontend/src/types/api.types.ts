/**
 * API Type Definitions
 * DTOs and interfaces for Spring Boot backend integration
 */

// ============================================================================
// Request DTOs
// ============================================================================

export interface LoginRequestDto {
  usernameOrEmail: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequestDto {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface RefreshTokenRequestDto {
  refreshToken: string;
}

export interface ForgotPasswordRequestDto {
  email: string;
}

export interface ResetPasswordRequestDto {
  token: string;
  newPassword: string;
}

export interface VerifyEmailRequestDto {
  token: string;
}

export interface ResendVerificationRequestDto {
  email: string;
}

// ============================================================================
// Response DTOs
// ============================================================================

export interface UserDto {
  id: string;
  email: string;
  name: string;
  role?: string;
  emailVerified?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponseDto {
  user: UserDto;
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface RefreshTokenResponseDto {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface MessageResponseDto {
  message: string;
  success?: boolean;
  timestamp?: string;
}

export interface UserProfileResponseDto extends UserDto {
  phoneNumber?: string;
  avatarUrl?: string;
  bio?: string;
  preferences?: Record<string, any>;
}

// ============================================================================
// Error Response DTOs
// ============================================================================

export interface ApiErrorResponse {
  message: string;
  error?: string;
  statusCode: number;
  timestamp?: string;
  path?: string;
  details?: Record<string, any>;
  validationErrors?: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}

// ============================================================================
// API State Types
// ============================================================================

export interface ApiRequestState {
  isLoading: boolean;
  error: string | null;
  data: any | null;
}

export interface ApiError {
  message: string;
  statusCode?: number;
  details?: Record<string, any>;
  validationErrors?: ValidationError[];
}

// ============================================================================
// Retry Configuration
// ============================================================================

export interface RetryConfig {
  maxRetries: number;
  retryDelay: number;
  retryableStatusCodes: number[];
  shouldRetry?: (error: any) => boolean;
}

export const DEFAULT_RETRY_CONFIG: RetryConfig = {
  maxRetries: 3,
  retryDelay: 1000,
  retryableStatusCodes: [408, 429, 500, 502, 503, 504],
};

// ============================================================================
// API Response Wrapper
// ============================================================================

export interface ApiResponse<T = any> {
  data: T;
  status: number;
  message?: string;
  timestamp?: string;
}

// ============================================================================
// Pagination Types
// ============================================================================

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
