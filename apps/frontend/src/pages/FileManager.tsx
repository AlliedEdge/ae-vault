import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import {
  Upload,
  Download,
  Trash2,
  Folder,
  File,
  FolderPlus,
  Search,
  Grid3x3,
  List,
  ArrowLeft,
  HardDrive,
  MoreVertical,
  FileText,
  Image as ImageIcon,
  Film,
  Music,
  LogOut,
  User,
} from 'lucide-react';
import { Button, Card, Input } from '../components/ui';
import { useAuthStore } from '../store/authStore';
import axiosInstance from '../lib/axios';

interface FileMetadata {
  fileId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  sha256Hash: string;
  storageKey: string;
  isDuplicate: boolean;
  downloadCount: number;
  uploadedAt: string;
  lastModified: string;
}

interface FolderData {
  folderId: string;
  folderName: string;
  parentFolderId: string | null;
  folderPath: string;
  createdAt: string;
}

const FileManager = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [folders, setFolders] = useState<FolderData[]>([]);
  const [currentFolder, setCurrentFolder] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [storageUsed, setStorageUsed] = useState(0);
  const [storageQuota, setStorageQuota] = useState(5368709120); // 5GB default
  const [showNewFolderDialog, setShowNewFolderDialog] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  // Load files and folders
  useEffect(() => {
    loadFiles();
    loadFolders();
    loadStorageInfo();
  }, [currentFolder]);

  const loadFiles = async () => {
    try {
      const params = currentFolder ? { folderId: currentFolder } : {};
      const response = await axiosInstance.get('/files', { params });
      
      console.log('[FileManager] loadFiles response:', response.data);
      
      // Backend returns ApiResponse wrapper, unwrap it
      const data = response.data.data || response.data;
      setFiles(data.content || data || []);
    } catch (error: any) {
      console.error('[FileManager] Failed to load files:', error);
      console.error('[FileManager] Error response:', error.response?.data);
    }
  };

  const loadFolders = async () => {
    try {
      const params = currentFolder ? { parentFolderId: currentFolder } : {};
      const response = await axiosInstance.get('/folders', { params });
      
      console.log('[FileManager] loadFolders response:', response.data);
      
      // Backend returns ApiResponse wrapper, unwrap it
      const data = response.data.data || response.data;
      setFolders(data || []);
    } catch (error: any) {
      console.error('[FileManager] Failed to load folders:', error);
      console.error('[FileManager] Error response:', error.response?.data);
    }
  };

  const loadStorageInfo = async () => {
    try {
      console.log('[FileManager] Loading storage info...');
      const response = await axiosInstance.get('/users/storage');
      console.log('[FileManager] Storage info response:', response.data);
      
      if (response.data.success) {
        const storageData = response.data.data;
        setStorageUsed(storageData.usedStorage || 0);
        setStorageQuota(storageData.totalStorage || 5368709120); // 5GB default
      }
    } catch (error) {
      console.error('[FileManager] Failed to load storage info:', error);
      // Fallback to user object
      setStorageUsed(user?.storageUsed || 0);
      setStorageQuota(user?.storageQuota || 5368709120);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    console.log('[FileManager] Starting file upload:', {
      name: file.name,
      size: file.size,
      type: file.type,
    });

    setIsUploading(true);
    setUploadProgress(0);

    const formData = new FormData();
    formData.append('file', file);
    if (currentFolder) {
      formData.append('folderId', currentFolder);
    }

    try {
      const response = await axiosInstance.post('/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          const progress = progressEvent.total
            ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
            : 0;
          setUploadProgress(progress);
          console.log('[FileManager] Upload progress:', progress + '%');
        },
      });

      console.log('[FileManager] Upload successful:', response.data);
      
      await loadFiles();
      loadStorageInfo();
      
      alert('File uploaded successfully!');
    } catch (error: any) {
      console.error('[FileManager] Upload failed:', error);
      console.error('[FileManager] Error response:', error.response?.data);
      
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Upload failed. Please check backend logs.';
      
      alert(`Upload failed: ${errorMessage}\n\nCheck console for details.`);
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
      event.target.value = '';
    }
  };

  const handleFileDownload = async (fileId: string, fileName: string) => {
    try {
      const response = await axiosInstance.get(`/files/${fileId}/download`, {
        responseType: 'blob',
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      // Reload files to update download count
      await loadFiles();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Download failed');
      console.error('Download error:', error);
    }
  };

  const handleFileDelete = async (fileId: string) => {
    if (!confirm('Are you sure you want to delete this file?')) return;

    try {
      await axiosInstance.delete(`/files/${fileId}`);
      await loadFiles();
      loadStorageInfo();
    } catch (error) {
      alert('Delete failed');
    }
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    try {
      await axiosInstance.post('/folders', {
        folderName: newFolderName,
        parentFolderId: currentFolder,
      });

      setNewFolderName('');
      setShowNewFolderDialog(false);
      await loadFolders();
    } catch (error) {
      alert('Failed to create folder');
    }
  };

  const handleFolderDelete = async (folderId: string) => {
    if (!confirm('Delete this folder and all its contents?')) return;

    try {
      await axiosInstance.delete(`/folders/${folderId}`);
      await loadFolders();
    } catch (error) {
      alert('Delete failed');
    }
  };

  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType.startsWith('image/')) return <ImageIcon className="w-5 h-5" />;
    if (mimeType.startsWith('video/')) return <Film className="w-5 h-5" />;
    if (mimeType.startsWith('audio/')) return <Music className="w-5 h-5" />;
    return <FileText className="w-5 h-5" />;
  };

  const storagePercentage = (storageUsed / storageQuota) * 100;

  const filteredFiles = files.filter((file) =>
    file.fileName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="min-h-screen w-full flex flex-col relative overflow-hidden bg-dark-950">
      {/* Animated background gradients */}
      <div className="absolute inset-0 -z-10">
        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            rotate: [0, 90, 0],
          }}
          transition={{
            duration: 20,
            repeat: Infinity,
            ease: 'linear',
          }}
          className="absolute top-0 -left-20 w-96 h-96 bg-primary-600/30 rounded-full blur-3xl"
        />
        <motion.div
          animate={{
            scale: [1, 1.3, 1],
            rotate: [0, -90, 0],
          }}
          transition={{
            duration: 25,
            repeat: Infinity,
            ease: 'linear',
          }}
          className="absolute bottom-0 -right-20 w-96 h-96 bg-primary-700/20 rounded-full blur-3xl"
        />
      </div>

      {/* Header with User Info and Logout */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full border-b border-dark-800/50 backdrop-blur-sm bg-dark-900/50"
      >
        <div className="max-w-7xl w-full mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary-500/20 flex items-center justify-center">
                <User className="w-5 h-5 text-primary-400" />
              </div>
              <div>
                <h2 className="text-white font-medium">
                  {user?.firstName && user?.lastName 
                    ? `${user.firstName} ${user.lastName}` 
                    : user?.username || 'User'}
                </h2>
                <p className="text-dark-400 text-sm">{user?.email}</p>
              </div>
            </div>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleLogout}
              className="flex items-center gap-2"
            >
              <LogOut className="w-4 h-4" />
              Logout
            </Button>
          </div>
        </div>
      </motion.div>

      {/* Main Content */}
      <div className="flex-1 p-6">
        <div className="max-w-7xl w-full mx-auto space-y-6">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between"
        >
          <div>
            <h1 className="text-4xl font-bold text-white">File Manager</h1>
            <p className="text-dark-300 mt-1">Manage your files and folders</p>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setViewMode(viewMode === 'grid' ? 'list' : 'grid')}
            >
              {viewMode === 'grid' ? <List className="w-4 h-4" /> : <Grid3x3 className="w-4 h-4" />}
            </Button>
          </div>
        </motion.div>

        {/* Storage Info */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.1 }}
        >
          <Card>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center">
                    <HardDrive className="w-6 h-6 text-blue-400" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-white">Storage Usage</h3>
                    <p className="text-dark-400 text-sm">
                      {formatBytes(storageUsed)} of {formatBytes(storageQuota)}
                    </p>
                  </div>
                </div>
                <span className="text-2xl font-bold text-white">{storagePercentage.toFixed(1)}%</span>
              </div>

              <div className="w-full h-2 bg-dark-800 rounded-full overflow-hidden">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${storagePercentage}%` }}
                  transition={{ duration: 1, ease: 'easeOut' }}
                  className={`h-full rounded-full ${
                    storagePercentage > 90
                      ? 'bg-red-500'
                      : storagePercentage > 70
                      ? 'bg-yellow-500'
                      : 'bg-blue-500'
                  }`}
                />
              </div>
            </div>
          </Card>
        </motion.div>

        {/* Action Bar */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <Card>
            <div className="flex flex-col sm:flex-row items-center gap-4">
              {/* Upload Button */}
              <label className="cursor-pointer">
                <input
                  type="file"
                  className="hidden"
                  onChange={handleFileUpload}
                  disabled={isUploading}
                  id="file-upload-input"
                />
                <Button 
                  variant="primary" 
                  size="md" 
                  disabled={isUploading} 
                  className="w-full sm:w-auto"
                  onClick={(e) => {
                    e.preventDefault();
                    document.getElementById('file-upload-input')?.click();
                  }}
                >
                  <Upload className="w-4 h-4" />
                  {isUploading ? `Uploading ${uploadProgress}%` : 'Upload File'}
                </Button>
              </label>

              {/* Create Folder Button */}
              <Button
                variant="secondary"
                size="md"
                onClick={() => setShowNewFolderDialog(true)}
                className="w-full sm:w-auto"
              >
                <FolderPlus className="w-4 h-4" />
                New Folder
              </Button>

              {/* Back Button */}
              {currentFolder && (
                <Button
                  variant="secondary"
                  size="md"
                  onClick={() => setCurrentFolder(null)}
                  className="w-full sm:w-auto"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Back
                </Button>
              )}

              {/* Search */}
              <div className="flex-1 w-full sm:w-auto">
                <Input
                  type="text"
                  placeholder="Search files..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  leftIcon={<Search className="w-5 h-5" />}
                />
              </div>
            </div>
          </Card>
        </motion.div>

        {/* Folders Grid */}
        {folders.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-4"
          >
            <h2 className="text-xl font-semibold text-white">Folders</h2>
            <div className={viewMode === 'grid' ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4' : 'space-y-2'}>
              {folders.map((folder) => (
                <Card
                  key={folder.folderId}
                  className="hover:border-primary-500/50 transition-colors cursor-pointer group"
                >
                  <div className="flex items-center gap-3">
                    <div
                      className="flex-1 flex items-center gap-3"
                      onClick={() => setCurrentFolder(folder.folderId)}
                    >
                      <div className="w-10 h-10 rounded-lg bg-yellow-500/10 flex items-center justify-center flex-shrink-0">
                        <Folder className="w-5 h-5 text-yellow-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h4 className="text-white font-medium truncate">{folder.folderName}</h4>
                        <p className="text-dark-400 text-xs truncate">{folder.folderPath}</p>
                        <p className="text-dark-500 text-xs mt-1">
                          Created {new Date(folder.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleFolderDelete(folder.folderId)}
                      className="opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          </motion.div>
        )}

        {/* Files Grid/List */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="space-y-4"
        >
          <h2 className="text-xl font-semibold text-white">
            Files {filteredFiles.length > 0 && `(${filteredFiles.length})`}
          </h2>

          {filteredFiles.length === 0 ? (
            <Card>
              <p className="text-dark-400 text-center py-8">No files to display</p>
            </Card>
          ) : (
            <div className={viewMode === 'grid' ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4' : 'space-y-2'}>
              {filteredFiles.map((file) => (
                <Card
                  key={file.fileId}
                  className="hover:border-primary-500/50 transition-colors group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary-500/10 flex items-center justify-center flex-shrink-0">
                      {getFileIcon(file.mimeType)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="text-white font-medium truncate">{file.fileName}</h4>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-dark-400 text-xs">{formatBytes(file.fileSize)}</span>
                        {file.isDuplicate && (
                          <span className="text-yellow-400 text-xs">(Duplicate)</span>
                        )}
                      </div>
                      <p className="text-dark-500 text-xs mt-1">
                        Uploaded {new Date(file.uploadedAt).toLocaleString()}
                      </p>
                      <p className="text-dark-500 text-xs">
                        Downloaded {file.downloadCount || 0} times
                      </p>
                    </div>
                    <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleFileDownload(file.fileId, file.fileName)}
                      >
                        <Download className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => handleFileDelete(file.fileId)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </motion.div>
        </div>
      </div>

      {/* New Folder Dialog */}
      <AnimatePresence>
        {showNewFolderDialog && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
            onClick={() => setShowNewFolderDialog(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md"
            >
              <Card>
                <div className="space-y-4">
                  <h3 className="text-xl font-bold text-white">Create New Folder</h3>
                  <Input
                    type="text"
                    label="Folder Name"
                    placeholder="Enter folder name"
                    value={newFolderName}
                    onChange={(e) => setNewFolderName(e.target.value)}
                    leftIcon={<Folder className="w-5 h-5" />}
                    autoFocus
                  />
                  <div className="flex gap-3">
                    <Button
                      variant="secondary"
                      fullWidth
                      onClick={() => {
                        setShowNewFolderDialog(false);
                        setNewFolderName('');
                      }}
                    >
                      Cancel
                    </Button>
                    <Button variant="primary" fullWidth onClick={handleCreateFolder}>
                      Create
                    </Button>
                  </div>
                </div>
              </Card>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default FileManager;
