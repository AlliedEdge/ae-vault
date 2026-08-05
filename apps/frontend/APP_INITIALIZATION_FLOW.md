# Application Initialization Flow

## Overview

This document describes the application initialization flow that occurs after successful user authentication (login or registration). The flow ensures all required data is loaded before displaying the dashboard.

## Architecture

### Key Components

1. **InitializingApp** (`/pages/InitializingApp.tsx`)
   - Main orchestration component for the initialization sequence
   - Manages initialization state and progress
   - Displays loading screen during initialization
   - Renders Dashboard once complete

2. **LoadingScreen** (`/components/ui/LoadingScreen.tsx`)
   - Full-screen loading animation
   - Shows initialization progress and messages
   - Animated background with logo and spinner

3. **Dashboard** (`/pages/Dashboard.tsx`)
   - Main application interface shown after initialization
   - Displays user info, storage quota, recent files, and workspace
   - Receives pre-loaded data as props

4. **appInitService** (`/services/appInitService.ts`)
   - Service layer for fetching initialization data
   - Handles parallel data loading
   - Implements retry logic with exponential backoff

## Initialization Sequence

### Step-by-Step Flow

After successful login/registration, the user is redirected to `/initializing`:

```
1. Verify Authentication (20% progress)
   ├─ Check if user is authenticated
   ├─ Call checkAuth() if needed
   └─ Redirect to login if not authenticated

2. Fetch Current User (40% progress)
   ├─ Verify user data is available
   └─ Use data from auth store

3. Fetch Storage Quota (60% progress)
   ├─ API: GET /storage/quota
   └─ Returns: { used, total, percentage }

4. Fetch Recent Files (80% progress)
   ├─ API: GET /files/recent?limit=10
   └─ Returns: Array of file objects

5. Fetch Workspace Information (90% progress)
   ├─ API: GET /workspace
   └─ Returns: Workspace object

6. Complete Initialization (100% progress)
   ├─ All data loaded successfully
   └─ Render Dashboard component
```

### Data Fetching Strategy

- **Parallel Loading**: Steps 3-5 are executed in parallel using `Promise.all()` for optimal performance
- **Retry Logic**: Automatic retry with exponential backoff (1s, 2s, 4s) up to 3 attempts
- **Error Handling**: On failure, shows error message and redirects to login after 3 seconds

## Routes

### Updated Route Structure

```tsx
/                      → Redirects to /initializing (if authenticated)
/initializing          → InitializingApp → Dashboard (protected)
/login                 → Login → /initializing
/register              → Register → /initializing
/home                  → Legacy Home page (kept for compatibility)
```

## Data Interfaces

### InitializationData

```typescript
interface InitializationData {
  user: User;
  storageQuota: StorageQuota;
  recentFiles: RecentFile[];
  workspace: Workspace;
}
```

### StorageQuota

```typescript
interface StorageQuota {
  used: number;         // Bytes used
  total: number;        // Total bytes available
  percentage: number;   // Usage percentage (0-100)
}
```

### RecentFile

```typescript
interface RecentFile {
  id: string;
  name: string;
  type: string;
  size: number;         // File size in bytes
  modifiedAt: string;   // ISO date string
  path: string;         // File path
}
```

### Workspace

```typescript
interface Workspace {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  memberCount?: number;
  owner?: {
    id: string;
    name: string;
    email: string;
  };
}
```

## Backend API Requirements

The frontend expects the following API endpoints to be implemented:

### 1. Get Storage Quota
```
GET /api/v1/storage/quota
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "used": 1073741824,      // 1 GB in bytes
  "total": 10737418240,    // 10 GB in bytes
  "percentage": 10
}
```

### 2. Get Recent Files
```
GET /api/v1/files/recent?limit=10
Authorization: Bearer {accessToken}

Response: 200 OK
[
  {
    "id": "file-123",
    "name": "document.pdf",
    "type": "application/pdf",
    "size": 1048576,
    "modifiedAt": "2026-08-04T10:30:00Z",
    "path": "/documents/document.pdf"
  }
]
```

### 3. Get Workspace Info
```
GET /api/v1/workspace
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": "workspace-123",
  "name": "My Workspace",
  "description": "Personal workspace",
  "createdAt": "2026-01-01T00:00:00Z",
  "updatedAt": "2026-08-04T10:00:00Z",
  "memberCount": 5,
  "owner": {
    "id": "user-123",
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

## Error Handling

### Initialization Failure

If any step fails:
1. Error message is displayed on loading screen
2. After 3 seconds, user is redirected to login
3. Login page shows error message: "Failed to load application data. Please try again."

### Retry Logic

The service automatically retries failed requests:
- **Attempt 1**: Immediate
- **Attempt 2**: After 1 second
- **Attempt 3**: After 2 seconds
- **Attempt 4**: After 4 seconds (final)

## User Experience

### Loading Screen Features

- **Animated Logo**: Pulsing animation to indicate activity
- **Spinner**: Rotating loading indicator
- **Progress Bar**: Visual representation of initialization progress (0-100%)
- **Status Messages**: Clear text describing current step
- **Animated Dots**: Additional loading animation
- **Background Gradients**: Smooth animated gradients for visual appeal

### Progress Indicators

Each step has a dedicated progress percentage:
- Verifying auth: 20%
- Fetching user: 40%
- Fetching storage: 60%
- Fetching files: 80%
- Fetching workspace: 90%
- Complete: 100%

## Dashboard Features

Once initialization is complete, the dashboard displays:

1. **Header**
   - Logo
   - Logout button

2. **Welcome Section**
   - Personalized greeting with user name
   - Brief description

3. **Stats Grid**
   - User info card (name, email, role)
   - Storage quota card (usage with progress bar)
   - Workspace card (name, description, member count)

4. **Recent Files Section**
   - Grid of recent files with metadata
   - File size and last modified date
   - Clickable cards for file access

5. **System Status**
   - Authentication status
   - Data synchronization status
   - System health indicators

## Testing the Flow

### Manual Testing Steps

1. **Login Flow**
   ```
   1. Navigate to /login
   2. Enter valid credentials
   3. Click "Sign In"
   4. Observe loading screen with progress
   5. Verify dashboard displays with loaded data
   ```

2. **Registration Flow**
   ```
   1. Navigate to /register
   2. Fill in registration form
   3. Click "Create Account"
   4. Observe loading screen with progress
   5. Verify dashboard displays with loaded data
   ```

3. **Error Handling**
   ```
   1. Mock API failure (network error or 500 response)
   2. Observe error message on loading screen
   3. Verify redirect to login after 3 seconds
   4. Check error message on login page
   ```

### Mock Data for Development

If backend endpoints are not ready, you can temporarily mock the responses in `appInitService.ts`:

```typescript
async getStorageQuota(): Promise<StorageQuota> {
  // Mock data for development
  return {
    used: 1073741824,      // 1 GB
    total: 10737418240,    // 10 GB
    percentage: 10,
  };
}

async getRecentFiles(limit: number = 10): Promise<RecentFile[]> {
  // Mock data for development
  return [
    {
      id: '1',
      name: 'Project Proposal.pdf',
      type: 'application/pdf',
      size: 2048576,
      modifiedAt: new Date().toISOString(),
      path: '/documents/Project Proposal.pdf',
    },
    // ... more mock files
  ];
}

async getWorkspaceInfo(): Promise<Workspace> {
  // Mock data for development
  return {
    id: '1',
    name: 'My Workspace',
    description: 'Personal workspace for projects',
    createdAt: new Date('2026-01-01').toISOString(),
    updatedAt: new Date().toISOString(),
    memberCount: 5,
    owner: {
      id: '1',
      name: 'John Doe',
      email: 'john@example.com',
    },
  };
}
```

## Performance Considerations

- **Parallel Loading**: All data is fetched in parallel, reducing total loading time
- **Minimum Display Time**: Each step has a minimum display duration (200-500ms) for UX smoothness
- **Retry Strategy**: Exponential backoff prevents overwhelming the server
- **Progress Feedback**: Clear visual progress keeps users informed

## Future Enhancements

Potential improvements to consider:

1. **Caching**: Cache initialization data to speed up subsequent loads
2. **Incremental Loading**: Show partial dashboard as data arrives
3. **Offline Support**: Handle offline scenarios gracefully
4. **Background Refresh**: Periodically refresh data without full reload
5. **Skeleton Screens**: Show skeleton UI during loading instead of spinner
6. **Analytics**: Track initialization times and failure rates

## Migration Notes

### From Old Flow

Previously, users were redirected directly to `/` (Home page) after login. The new flow:

1. Redirects to `/initializing` instead
2. Loads all required data
3. Shows Dashboard component (not Home)
4. Home page is still available at `/home` for backward compatibility

### Updating Existing Code

If you have links or redirects to `/` in your code:
- They will now go through the initialization flow
- This is the desired behavior for authenticated users
- No changes needed unless you want to skip initialization

## Troubleshooting

### Issue: Infinite loading screen
**Solution**: Check browser console for API errors. Verify backend endpoints are accessible.

### Issue: Quick flash then redirect to login
**Solution**: Check authentication state. User may not be properly authenticated.

### Issue: Dashboard shows incomplete data
**Solution**: Verify all API responses match expected interfaces. Check for missing fields.

### Issue: Progress bar doesn't move
**Solution**: Check that initialization steps are completing. Review network tab for failed requests.

## Summary

The new initialization flow provides:
- ✅ Proper data loading before dashboard display
- ✅ Clear visual feedback during initialization
- ✅ Robust error handling with retries
- ✅ Optimal performance with parallel loading
- ✅ Better user experience with progress indicators
- ✅ Separation of concerns (loading vs. displaying data)
