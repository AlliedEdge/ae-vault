# Changelog - Frontend

All notable changes to the Ziboto Frontend application are documented here.

## [Unreleased]

### Added
- FileManager page with complete file management UI:
  - File upload with progress tracking
  - File download functionality
  - File deletion with confirmation
  - Folder creation and management
  - Folder navigation with breadcrumbs
  - File search functionality
  - Grid and list view modes
  - Storage usage visualization
  - User profile display with logout
  - Responsive design for mobile and desktop
  - Animated UI with Framer Motion
  - File type icons (image, video, audio, document)
  - Duplicate file indication
  - Download count display
- Added FileManager to pages index exports

### Changed
- Updated package-lock.json with latest dependencies
- Enhanced App.tsx with improved routing logic
- Refined GuestRoute component for better authentication flow
- Improved AuthContext with enhanced state management
- Updated useTokenRefresh hook for better token handling
- Enhanced axios instance configuration
- Improved Dashboard page layout and functionality
- Updated InitializingApp page for better loading states
- Refined authentication services (authService, tokenService)
- Enhanced authStore with file management support

### Removed
- Obsolete implementation documentation files:
  - API_INTEGRATION.md
  - API_QUICK_REFERENCE.md
  - APP_INITIALIZATION_FLOW.md
  - ARCHITECTURE_QUICK_REFERENCE.md
  - ARCHITECTURE_SUMMARY.md
  - AUTH_IMPLEMENTATION.md
  - AUTH_SETUP_COMPLETE.md
  - DEPLOYMENT_CHECKLIST.md
  - DEVELOPMENT.md
  - DOCUMENTATION_INDEX.md
  - FRONTEND_BACKEND_ALIGNMENT.md
  - IMPLEMENTATION_COMPLETE.md
  - INITIALIZATION_QUICK_START.md
  - INITIALIZATION_SUMMARY.md
  - INTEGRATION_SUMMARY.md
  - JWT_ARCHITECTURE.md
  - JWT_AUTHENTICATION.md
  - JWT_IMPLEMENTATION_SUMMARY.md
  - JWT_QUICK_REFERENCE.md
  - JWT_README.md
  - PRODUCTION_BACKEND_ARCHITECTURE.md
  - QUICK_REFERENCE.md
  - ROUTING.md
  - ROUTING_ARCHITECTURE.md
  - ROUTING_IMPLEMENTATION_SUMMARY.md
  - ROUTING_QUICK_REFERENCE.md
  - ROUTING_README.md
  - SETUP_COMPLETE.md
  - SPRING_BOOT_INTEGRATION.md

---

## [0.2.0] - 2026-08-05

### Added
- Complete React + TypeScript + Vite application setup
- Authentication pages:
  - Login page with form validation
  - Register page with password strength indicator
  - Dashboard page with user profile
  - Home page (landing)
  - ForgotPassword page
  - ResetPassword page
  - EmailVerificationSuccess page
  - SessionExpired page
  - InitializingApp page with loading animation
- Authentication components:
  - ProtectedRoute for authenticated users only
  - GuestRoute for unauthenticated users only
  - PublicRoute for all users
- Layout components:
  - AuthLayout with animated backgrounds
- UI components:
  - Button with multiple variants (primary, secondary, danger, ghost)
  - Card with glassmorphism effect
  - Checkbox with custom styling
  - Input with validation and icons
  - LoadingScreen with animated spinner
  - Logo component
  - PasswordStrengthIndicator
- AuthContext for global authentication state
- Zustand store (authStore) for state management
- Custom hooks:
  - useApi for API calls with loading/error states
  - useAuthOperations for login/register/logout
  - useTokenRefresh for automatic token refresh
- Services:
  - authService for authentication API calls
  - tokenService for token management
  - appInitService for application initialization
- Axios instance with:
  - Request interceptors for auth headers
  - Response interceptors for error handling
  - Automatic token refresh on 401
  - Retry logic for failed requests
- Utilities:
  - apiErrorHandler for consistent error handling
  - retryHandler for request retries
  - jwtTestUtils for JWT testing
- TypeScript types and interfaces (api.types.ts)
- TailwindCSS configuration with custom theme:
  - Dark color palette
  - Custom animations
  - Responsive breakpoints
- Framer Motion integration for animations
- Lucide React for icons
- React Router DOM for navigation
- Package configuration:
  - package.json with all dependencies
  - vite.config.ts
  - tsconfig.json
  - postcss.config.js
  - .oxlintrc.json
- Assets:
  - Hero image
  - React and Vite logos
  - Favicon
  - Icons sprite
- README with setup instructions
- .env.example for environment variables

### Styling
- Dark theme with primary brand colors
- Glassmorphism effects
- Animated gradients
- Responsive design
- Custom scrollbars
- Smooth transitions

---

## [0.0.1] - 2026-08-01

### Added
- Initial frontend project structure placeholder
