import type { StorageQuota, RecentFile, Workspace, InitializationData } from './appInitService';

/**
 * Mock App Initialization Service
 * Use this for testing when the backend is not available
 * 
 * To use:
 * 1. In appInitService.ts, comment out the real API calls
 * 2. Import and use these mock functions instead
 */

/**
 * Mock storage quota data
 */
export const mockStorageQuota: StorageQuota = {
  used: 3221225472,      // 3 GB
  total: 10737418240,    // 10 GB
  percentage: 30,
};

/**
 * Mock recent files data
 */
export const mockRecentFiles: RecentFile[] = [
  {
    id: '1',
    name: 'Project Proposal.pdf',
    type: 'application/pdf',
    size: 2048576,  // 2 MB
    modifiedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
    path: '/documents/Project Proposal.pdf',
  },
  {
    id: '2',
    name: 'Financial Report Q2.xlsx',
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    size: 1536000,  // 1.5 MB
    modifiedAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), // 5 hours ago
    path: '/reports/Financial Report Q2.xlsx',
  },
  {
    id: '3',
    name: 'Team Photo.jpg',
    type: 'image/jpeg',
    size: 3145728,  // 3 MB
    modifiedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // 1 day ago
    path: '/images/Team Photo.jpg',
  },
  {
    id: '4',
    name: 'Meeting Notes.docx',
    type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    size: 524288,   // 512 KB
    modifiedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(), // 3 days ago
    path: '/notes/Meeting Notes.docx',
  },
  {
    id: '5',
    name: 'Presentation.pptx',
    type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    size: 5242880,  // 5 MB
    modifiedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), // 1 week ago
    path: '/presentations/Presentation.pptx',
  },
  {
    id: '6',
    name: 'Code Review.md',
    type: 'text/markdown',
    size: 8192,     // 8 KB
    modifiedAt: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000).toISOString(), // 10 days ago
    path: '/docs/Code Review.md',
  },
];

/**
 * Mock workspace data
 */
export const mockWorkspace: Workspace = {
  id: 'workspace-1',
  name: 'My Workspace',
  description: 'Personal workspace for all my projects and files',
  createdAt: new Date('2026-01-01').toISOString(),
  updatedAt: new Date().toISOString(),
  memberCount: 5,
  owner: {
    id: 'user-1',
    name: 'John Doe',
    email: 'john@example.com',
  },
};

/**
 * Mock initialization - simulates API delay
 */
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Mock service class matching the real appInitService interface
 */
class MockAppInitService {
  async getStorageQuota(): Promise<StorageQuota> {
    await delay(300); // Simulate network delay
    return mockStorageQuota;
  }

  async getRecentFiles(limit: number = 10): Promise<RecentFile[]> {
    await delay(400); // Simulate network delay
    return mockRecentFiles.slice(0, limit);
  }

  async getWorkspaceInfo(): Promise<Workspace> {
    await delay(350); // Simulate network delay
    return mockWorkspace;
  }

  async initializeApp(user: any): Promise<InitializationData> {
    const [storageQuota, recentFiles, workspace] = await Promise.all([
      this.getStorageQuota(),
      this.getRecentFiles(),
      this.getWorkspaceInfo(),
    ]);

    return {
      user,
      storageQuota,
      recentFiles,
      workspace,
    };
  }

  async initializeAppWithRetry(
    user: any,
    _maxRetries: number = 3
  ): Promise<InitializationData> {
    // For mock, we don't need retry logic - just call once
    return this.initializeApp(user);
  }
}

export const mockAppInitService = new MockAppInitService();

/**
 * HOW TO USE MOCK SERVICE:
 * 
 * Option 1: Temporary replacement in appInitService.ts
 * 
 * Import at the top:
 * import { mockAppInitService } from './appInitService.mock';
 * 
 * Then in the class methods, replace API calls with mock calls:
 * 
 * async getStorageQuota(): Promise<StorageQuota> {
 *   return mockAppInitService.getStorageQuota();
 * }
 * 
 * Option 2: Environment-based switching
 * 
 * Create a condition based on environment:
 * 
 * const USE_MOCK_DATA = import.meta.env.VITE_USE_MOCK_DATA === 'true';
 * 
 * if (USE_MOCK_DATA) {
 *   return mockAppInitService.getStorageQuota();
 * } else {
 *   // Real API call
 * }
 * 
 * Then in .env.development:
 * VITE_USE_MOCK_DATA=true
 */
