import axiosInstance from '../lib/axios';

/**
 * Storage quota information
 */
export interface StorageQuota {
  used: number;
  total: number;
  percentage: number;
}

/**
 * Recent file information
 */
export interface RecentFile {
  id: string;
  name: string;
  type: string;
  size: number;
  modifiedAt: string;
  path: string;
}

/**
 * Workspace information
 */
export interface Workspace {
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

/**
 * Complete initialization data
 */
export interface InitializationData {
  user: any;
  storageQuota: StorageQuota;
  recentFiles: RecentFile[];
  workspace: Workspace;
}

/**
 * App initialization service
 * Handles fetching all required data after successful authentication
 */
class AppInitService {
  /**
   * Fetch storage quota for the current user
   */
  async getStorageQuota(): Promise<StorageQuota> {
    try {
      const response = await axiosInstance.get<StorageQuota>('/storage/quota');
      return response.data;
    } catch (error) {
      console.error('Failed to fetch storage quota:', error);
      throw error;
    }
  }

  /**
   * Fetch recent files for the current user
   */
  async getRecentFiles(limit: number = 10): Promise<RecentFile[]> {
    try {
      const response = await axiosInstance.get<RecentFile[]>('/files/recent', {
        params: { limit },
      });
      return response.data;
    } catch (error) {
      console.error('Failed to fetch recent files:', error);
      throw error;
    }
  }

  /**
   * Fetch workspace information for the current user
   */
  async getWorkspaceInfo(): Promise<Workspace> {
    try {
      const response = await axiosInstance.get<Workspace>('/workspace');
      return response.data;
    } catch (error) {
      console.error('Failed to fetch workspace info:', error);
      throw error;
    }
  }

  /**
   * Initialize the application
   * Fetches all required data in parallel for optimal performance
   * 
   * @param user - Current authenticated user
   * @returns Complete initialization data
   */
  async initializeApp(user: any): Promise<InitializationData> {
    try {
      // Fetch all data in parallel for better performance
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
    } catch (error) {
      console.error('App initialization failed:', error);
      throw error;
    }
  }

  /**
   * Initialize app with retry logic
   * Attempts to load data with exponential backoff
   * 
   * @param user - Current authenticated user
   * @param maxRetries - Maximum number of retry attempts
   * @returns Complete initialization data
   */
  async initializeAppWithRetry(
    user: any,
    maxRetries: number = 3
  ): Promise<InitializationData> {
    let lastError: Error | null = null;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await this.initializeApp(user);
      } catch (error) {
        lastError = error as Error;
        console.error(`Initialization attempt ${attempt} failed:`, error);

        if (attempt < maxRetries) {
          // Exponential backoff: 1s, 2s, 4s
          const delay = Math.pow(2, attempt - 1) * 1000;
          console.log(`Retrying in ${delay}ms...`);
          await new Promise((resolve) => setTimeout(resolve, delay));
        }
      }
    }

    throw lastError || new Error('App initialization failed after retries');
  }
}

export const appInitService = new AppInitService();
