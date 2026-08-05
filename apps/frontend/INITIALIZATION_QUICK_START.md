# Initialization Flow - Quick Start Guide

## 🚀 What's New

After login/registration, users now see a loading screen while the app fetches required data before displaying the dashboard.

## ✅ What Works Now

- Full-screen loading animation with progress bar
- Automatic data fetching (storage, files, workspace)
- Smooth transition to dashboard when ready
- Error handling with retry logic

## 🔧 Testing Without Backend

If backend APIs aren't ready yet, use the mock service:

### Quick Mock Setup

**Option 1: Environment Variable**

Add to `.env.development`:
```
VITE_USE_MOCK_DATA=true
```

Then update `appInitService.ts`:
```typescript
import { mockAppInitService } from './appInitService.mock';

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true';

async getStorageQuota(): Promise<StorageQuota> {
  if (USE_MOCK) return mockAppInitService.getStorageQuota();
  // ... real API call
}
```

**Option 2: Direct Replacement**

In `appInitService.ts`, temporarily replace API calls:
```typescript
import { mockStorageQuota, mockRecentFiles, mockWorkspace } from './appInitService.mock';

async getStorageQuota(): Promise<StorageQuota> {
  return mockStorageQuota;
}

async getRecentFiles(): Promise<RecentFile[]> {
  return mockRecentFiles;
}

async getWorkspaceInfo(): Promise<Workspace> {
  return mockWorkspace;
}
```

## 🧪 Testing the Flow

### 1. With Mock Data

```bash
# Add to .env.development
echo "VITE_USE_MOCK_DATA=true" >> .env.development

# Run dev server
npm run dev

# Login or register
# → You'll see loading screen
# → Then dashboard with mock data
```

### 2. With Real Backend

```bash
# Make sure backend is running on localhost:8080

# In .env.development
VITE_USE_MOCK_DATA=false

# Run dev server
npm run dev

# Login or register
# → Loading screen appears
# → Real data from backend loads
# → Dashboard displays
```

## 📋 Backend API Checklist

Your backend needs these three endpoints:

- [ ] `GET /api/v1/storage/quota`
- [ ] `GET /api/v1/files/recent`
- [ ] `GET /api/v1/workspace`

All require JWT authentication header.

See `APP_INITIALIZATION_FLOW.md` for detailed API specifications.

## 🎨 Customizing Loading Screen

Edit `src/components/ui/LoadingScreen.tsx`:

```typescript
// Change animation duration
transition={{ duration: 15 }} // Default: 20

// Change colors
className="bg-primary-600/30" // Change opacity/color

// Add custom message
message="Loading your workspace..."
```

## 🎯 Customizing Dashboard

Edit `src/pages/Dashboard.tsx`:

```typescript
// Add new sections
<motion.div className="...">
  <h2>New Section</h2>
  {/* Your content */}
</motion.div>

// Modify existing cards
// Change colors, icons, layout, etc.
```

## 🐛 Common Issues

### "Failed to load application data"
- Backend is not running
- API endpoints not implemented
- CORS issues
- **Solution**: Use mock data or check backend logs

### Loading screen stuck at certain percentage
- One of the API calls is hanging
- Network timeout
- **Solution**: Check browser console and network tab

### TypeScript errors
- Make sure to run `npm install` after pulling changes
- Clear `.tsbuildinfo` files
- **Solution**: `rm -rf dist && npm run build`

## 📚 Documentation

- **Quick Start**: This file
- **Detailed Guide**: `APP_INITIALIZATION_FLOW.md`
- **Summary**: `INITIALIZATION_SUMMARY.md`

## 🚦 Next Steps

1. [ ] Test with mock data
2. [ ] Implement backend APIs
3. [ ] Test with real backend
4. [ ] Deploy to staging
5. [ ] Monitor performance

## 💡 Tips

- Loading screen has minimum display times (200-500ms per step) for smooth UX
- Data is fetched in parallel for best performance
- Automatic retry with exponential backoff (3 attempts)
- Dashboard receives pre-loaded data as props (no additional loading needed)

## 🔗 Key Files

```
src/
├── services/
│   ├── appInitService.ts       # Data fetching logic
│   └── appInitService.mock.ts  # Mock data for testing
├── pages/
│   ├── InitializingApp.tsx     # Orchestrates loading
│   └── Dashboard.tsx           # Main dashboard UI
└── components/ui/
    └── LoadingScreen.tsx       # Loading animation
```

## ✨ Features at a Glance

- ✅ User info display
- ✅ Storage quota with progress bar
- ✅ Recent files list
- ✅ Workspace information
- ✅ Animated transitions
- ✅ Error handling
- ✅ Retry logic
- ✅ Progress indicators
- ✅ Responsive design

## 🎉 You're All Set!

The initialization flow is ready to use. Start with mock data for immediate testing, then connect to your backend when ready.

Need help? Check the detailed docs or review the code comments.
