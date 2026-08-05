# Ziboto Frontend

Production-grade React application with JWT authentication, designed for stateless backend architecture with load balancing support.

## Architecture

```
React → Nginx Load Balancer → Spring Boot Instances → Redis + PostgreSQL
```

**Key Features:**
- ✅ Stateless JWT authentication (works with any backend instance)
- ✅ Automatic token refresh on expiry
- ✅ Load balancer compatible (no session affinity required)
- ✅ Redis-backed rate limiting support
- ✅ PostgreSQL-backed refresh token validation
- ✅ Automatic retry with exponential backoff
- ✅ Comprehensive error handling

## Features

### 🔐 Authentication Pages

- **Login** - Email/password login
- **Register** - Account creation with password strength indicator
- **Forgot Password** - Password recovery with email verification
- **Reset Password** - Secure password reset with token validation
- **Email Verification** - Email confirmation with success state
- **Session Expired** - User-friendly session timeout handling

### ✨ UI/UX Features

- Premium dark theme with purple accents
- Glassmorphism design with backdrop blur effects
- Smooth animations using Framer Motion
- Responsive design (mobile, tablet, desktop)
- Keyboard accessibility (Tab navigation, focus states)
- Loading states and disabled buttons during submission
- Real-time inline validation with error messages
- Password show/hide toggle
- Password strength indicator with visual feedback
- Auto-redirect with countdown timers
- Animated success states with confetti effects

### 🛠️ Technical Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **Tailwind CSS** - Utility-first CSS
- **Framer Motion** - Animation library
- **React Hook Form** - Form management
- **Zod** - Schema validation
- **React Router** - Client-side routing
- **Zustand** - State management
- **Axios** - HTTP client with interceptors
- **Lucide React** - Icon library

## 📚 Architecture Documentation

**Comprehensive documentation has been created for production deployment:**

### Quick Start
1. **[ARCHITECTURE_SUMMARY.md](./ARCHITECTURE_SUMMARY.md)** ⭐ **START HERE**
   - Overview of architecture and design decisions
   - What's already correct vs. what needs implementation
   - Next steps and recommendations

### Deep Dive
2. **[PRODUCTION_BACKEND_ARCHITECTURE.md](./PRODUCTION_BACKEND_ARCHITECTURE.md)**
   - Complete authentication flows (login, refresh, logout)
   - Token management strategy and lifecycle
   - Redis and PostgreSQL integration patterns
   - Load balancer compatibility analysis
   - Security considerations and best practices
   - Monitoring, logging, and troubleshooting

3. **[FRONTEND_BACKEND_ALIGNMENT.md](./FRONTEND_BACKEND_ALIGNMENT.md)**
   - Line-by-line analysis of frontend implementation
   - Frontend-backend contract and API specifications
   - Optional optimizations (preemptive refresh, device fingerprinting)
   - Backend requirements for each frontend feature

### Reference & Deployment
4. **[ARCHITECTURE_QUICK_REFERENCE.md](./ARCHITECTURE_QUICK_REFERENCE.md)**
   - Visual architecture diagrams
   - Authentication flow diagrams
   - Error handling matrix and status codes
   - Common issues and solutions
   - Useful commands for debugging

5. **[DEPLOYMENT_CHECKLIST.md](./DEPLOYMENT_CHECKLIST.md)**
   - Step-by-step production deployment guide
   - Infrastructure setup (Nginx, Redis, PostgreSQL)
   - Security configuration (SSL/TLS, CORS, headers)
   - Testing procedures and verification steps
   - Monitoring and alerting setup

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running (Spring Boot on port 8080)
- PostgreSQL database
- Redis server

### Installation

```bash
# Install dependencies
npm install

# Create .env file
cp .env.example .env
# Edit .env and set VITE_API_URL=http://localhost:8080/api/v1

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Environment Configuration

Create `.env` file:
```env
VITE_API_URL=http://localhost:8080/api/v1
```

For production:
```env
VITE_API_URL=https://api.ziboto.com/api/v1
```

## Routes

| Route | Page | Description |
|-------|------|-------------|
| `/` | Redirect | Redirects to `/login` |
| `/login` | Login | User login page |
| `/register` | Register | New account creation |
| `/forgot-password` | Forgot Password | Password recovery |
| `/reset-password` | Reset Password | Password reset (requires token) |
| `/verify-email` | Email Verification | Email confirmation (requires token) |
| `/session-expired` | Session Expired | Session timeout notification |

## Project Structure

```
src/
├── components/
│   ├── auth/
│   │   ├── ProtectedRoute.tsx  # Route guard for authenticated users
│   │   ├── GuestRoute.tsx      # Route guard for unauthenticated users
│   │   └── PublicRoute.tsx     # Public routes accessible to all
│   ├── layout/
│   │   ├── AuthLayout.tsx      # Main auth page layout
│   │   └── index.ts
│   └── ui/
│       ├── Button.tsx          # Reusable button component
│       ├── Input.tsx           # Input with validation
│       ├── Card.tsx            # Glassmorphism card
│       ├── Checkbox.tsx        # Custom checkbox
│       ├── PasswordStrengthIndicator.tsx
│       └── index.ts
├── context/
│   └── AuthContext.tsx         # Authentication context provider
├── hooks/
│   ├── useTokenRefresh.ts      # Automatic token refresh
│   └── useAuthOperations.ts    # Auth operation hooks
├── lib/
│   └── axios.ts                # Axios instance with interceptors
├── pages/
│   ├── Login.tsx
│   ├── Register.tsx
│   ├── ForgotPassword.tsx
│   ├── ResetPassword.tsx
│   ├── EmailVerificationSuccess.tsx
│   ├── SessionExpired.tsx
│   ├── InitializingApp.tsx
│   ├── Home.tsx
│   └── index.ts
├── services/
│   ├── authService.ts          # Authentication API calls
│   └── tokenService.ts         # Token storage and management
├── store/
│   └── authStore.ts            # Zustand authentication state
├── types/
│   └── api.types.ts            # TypeScript DTOs for API
├── utils/
│   ├── apiErrorHandler.ts      # Error handling utilities
│   └── retryHandler.ts         # Retry logic with exponential backoff
├── App.tsx                     # Router configuration
├── main.tsx                    # App entry point
└── index.css                   # Global styles + Tailwind
```

## Authentication Architecture

### Token Management
- **Access Token**: JWT, 15-minute expiry, stored in localStorage
- **Refresh Token**: UUID, 7-day expiry, stored in localStorage + PostgreSQL
- **Automatic Refresh**: Axios interceptor handles token refresh on 401 errors
- **Token Revocation**: Redis blacklist on logout

### Authentication Flows

#### Login Flow
1. User submits email/password → POST `/api/v1/auth/login`
2. Backend validates credentials (PostgreSQL) and rate limit (Redis)
3. Backend generates JWT tokens
4. Frontend stores tokens in localStorage
5. Redirect to protected route

#### Token Refresh Flow
1. Access token expires → 401 error
2. Axios interceptor catches 401
3. POST `/api/v1/auth/refresh` with refreshToken
4. Backend validates refresh token (PostgreSQL)
5. Backend generates new access token
6. Frontend updates stored tokens
7. Retry original request with new token

#### Logout Flow
1. POST `/api/v1/auth/logout` with access token
2. Backend adds token to Redis blacklist
3. Backend deletes refresh token from PostgreSQL
4. Frontend clears localStorage
5. Redirect to login page

### State Management

Uses **Zustand** for authentication state:
- User profile
- Authentication status
- Loading states (login, register, logout, refresh)
- Error messages
- Success messages

### Error Handling

- **Retry Logic**: Exponential backoff for network/server errors
- **Rate Limiting**: Handles 429 errors gracefully
- **User-Friendly Messages**: Converts technical errors to readable messages
- **Validation Errors**: Displays field-specific validation errors

See **[PRODUCTION_BACKEND_ARCHITECTURE.md](./PRODUCTION_BACKEND_ARCHITECTURE.md)** for detailed architecture documentation.

## Component Documentation

### UI Components

#### Button
- Variants: `primary`, `secondary`, `ghost`, `danger`
- Sizes: `sm`, `md`, `lg`
- Props: `isLoading`, `fullWidth`, `disabled`

#### Input
- Features: Password toggle, left icon, validation errors
- Props: `label`, `error`, `helperText`, `showPasswordToggle`

#### Card
- Variants: `default`, `glass`, `glass-light`
- Padding options: `none`, `sm`, `md`, `lg`

#### PasswordStrengthIndicator
- Real-time password strength calculation
- Visual bars with color-coded strength levels
- Strength labels: Weak, Fair, Good, Strong

### Layout Components

#### AuthLayout
- Glassmorphism card with backdrop blur
- Animated background gradients
- Grid pattern overlay
- Responsive centering
- Logo display

## Customization

### Theme Colors

Edit `tailwind.config.js` to customize colors:

```js
colors: {
  primary: { /* Purple shades */ },
  dark: { /* Dark theme shades */ },
}
```

### Animations

Animations are defined in:
- `tailwind.config.js` - Keyframes and animation utilities
- `index.css` - Custom CSS animations
- Individual components - Framer Motion animations

## Validation Schemas

All forms use Zod for validation:

- Email: Valid email format required
- Password: Min 8 chars, uppercase, lowercase, number
- Password confirmation: Must match password
- Name: 2-50 characters

## Backend Requirements

The frontend expects these endpoints from the Spring Boot backend:

### Authentication Endpoints
- `POST /api/v1/auth/login` - Login with email/password
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/logout` - Logout (add token to Redis blacklist)
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/forgot-password` - Request password reset
- `POST /api/v1/auth/reset-password` - Reset password with token
- `POST /api/v1/auth/verify-email` - Verify email with token
- `POST /api/v1/auth/resend-verification` - Resend verification email

### User Endpoints
- `GET /api/v1/users/me` - Get current user profile

### Backend Implementation Checklist
- [ ] JWT token generation (Access: 15 min, Refresh: 7 days)
- [ ] Token refresh endpoint with PostgreSQL validation
- [ ] Redis token blacklist on logout
- [ ] Redis-backed rate limiting
- [ ] Refresh token storage in PostgreSQL
- [ ] Audit logging in PostgreSQL
- [ ] CORS configuration for frontend origin
- [ ] Spring Security with JWT validation

See **[API_INTEGRATION.md](./API_INTEGRATION.md)** for detailed request/response formats.

## Production Deployment

### Frontend Build
```bash
npm run build  # Creates dist/ folder
```

### Deploy Options
- Nginx (serve static files)
- S3 + CloudFront
- Vercel / Netlify
- Any static hosting provider

### Nginx Configuration (Example)
```nginx
server {
    listen 443 ssl http2;
    server_name ziboto.com;
    
    root /var/www/ziboto/dist;
    index index.html;
    
    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Content-Security-Policy "default-src 'self'" always;
}
```

See **[DEPLOYMENT_CHECKLIST.md](./DEPLOYMENT_CHECKLIST.md)** for complete deployment guide.

## Common Issues

### Token Refresh Loop
**Symptoms**: Infinite 401 errors, continuous /auth/refresh calls  
**Solution**: Axios interceptor doesn't retry /auth/refresh (already implemented)

### CORS Errors
**Symptoms**: Browser blocks requests  
**Solution**: Configure CORS on backend/Nginx to allow frontend origin

### User Logged Out Unexpectedly
**Symptoms**: isAuthenticated becomes false randomly  
**Causes**: Token expired, blacklisted, or deleted  
**Solution**: Check backend logs, verify refresh token in database

See **[ARCHITECTURE_QUICK_REFERENCE.md](./ARCHITECTURE_QUICK_REFERENCE.md)** for more troubleshooting.

## Performance

### Bundle Size
- Target: < 500KB gzipped
- Code splitting for large pages
- Lazy load routes with React.lazy()

### API Response Times
- Login: < 500ms
- Token refresh: < 200ms
- API calls: < 100ms (p50), < 500ms (p99)

## Security

### Frontend Security
- ✅ Tokens in localStorage (with CSP)
- ✅ HTTPS enforced in production
- ✅ No sensitive data in URLs
- ✅ Authorization header for all authenticated requests
- ✅ Automatic token cleanup on logout

### Backend Security (Required)
- ⚠️ Strong JWT secret (256-bit minimum)
- ⚠️ Short access token expiry (15 minutes)
- ⚠️ Refresh token rotation (one-time use)
- ⚠️ Redis token blacklist
- ⚠️ Rate limiting (Redis)
- ⚠️ Audit logging (PostgreSQL)

## Future Enhancements

- [ ] Multi-factor authentication (MFA)
- [ ] Biometric authentication
- [ ] Magic link login
- [ ] Password-less authentication
- [ ] Account recovery questions
- [ ] Device management
- [ ] Login history
- [ ] OAuth provider integration (backend)
- [ ] Rate limiting feedback
- [ ] CAPTCHA integration

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Accessibility

- WCAG 2.1 Level AA compliant
- Keyboard navigation support
- Screen reader friendly
- Focus indicators
- ARIA labels and roles
- Semantic HTML

## License

See root LICENSE file.
