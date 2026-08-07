import { motion } from 'framer-motion';
import Logo from '../components/ui/Logo';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { 
  LogOut, 
  User as UserIcon, 
  HardDrive, 
  File, 
  Users,
  Clock,
  Folder,
  FolderOpen,
} from 'lucide-react';
import type { InitializationData } from '../services/appInitService';

interface DashboardProps {
  initData: InitializationData;
}

/**
 * Dashboard page
 * Displays after successful initialization with all loaded data
 */
const Dashboard: React.FC<DashboardProps> = ({ initData }) => {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { user, storageQuota, recentFiles, workspace } = initData;

  const handleLogout = async () => {
    await logout();
  };

  // Format bytes to readable size
  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  // Format date
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInHours = diffInMs / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return `${Math.floor(diffInHours)} hours ago`;
    } else if (diffInHours < 48) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString();
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col p-6 relative overflow-hidden bg-dark-950">
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

      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between mb-8"
      >
        <Logo size="md" />
        
        <Button
          variant="danger"
          size="sm"
          onClick={handleLogout}
          className="gap-2"
        >
          <LogOut className="w-4 h-4" />
          Logout
        </Button>
      </motion.header>

      {/* Main Content */}
      <div className="max-w-7xl w-full mx-auto space-y-8">
        {/* Welcome Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="space-y-2"
        >
          <h1 className="text-4xl font-bold text-white">
            Welcome back, {user.name}!
          </h1>
          <p className="text-dark-300 text-lg">
            Here's what's happening in your workspace
          </p>
        </motion.div>

        {/* Quick Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <Card className="bg-gradient-to-r from-primary-600/20 to-primary-700/20 border-primary-500/30">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 rounded-full bg-primary-500/20 flex items-center justify-center">
                  <FolderOpen className="w-7 h-7 text-primary-400" />
                </div>
                <div>
                  <h3 className="text-xl font-semibold text-white">Manage Your Files</h3>
                  <p className="text-dark-300 text-sm mt-1">
                    Upload, organize, and share your documents
                  </p>
                </div>
              </div>
              <Button
                variant="primary"
                size="lg"
                onClick={() => navigate('/files')}
                className="gap-2"
              >
                <Folder className="w-5 h-5" />
                Open File Manager
              </Button>
            </div>
          </Card>
        </motion.div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* User Info Card */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
          >
            <Card className="h-full">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-full bg-gradient-to-br from-primary-600 to-primary-700 flex items-center justify-center flex-shrink-0">
                  <UserIcon className="w-8 h-8 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-white truncate">
                    {user.name}
                  </h3>
                  <p className="text-primary-400 text-sm truncate">{user.email}</p>
                  {user.role && (
                    <p className="text-dark-400 text-xs mt-1">Role: {user.role}</p>
                  )}
                </div>
              </div>
            </Card>
          </motion.div>

          {/* Storage Quota Card */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.3 }}
          >
            <Card className="h-full">
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-lg bg-blue-500/10 flex items-center justify-center">
                    <HardDrive className="w-6 h-6 text-blue-400" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-white">Storage</h3>
                    <p className="text-dark-400 text-sm">
                      {formatBytes(storageQuota.used)} of {formatBytes(storageQuota.total)}
                    </p>
                  </div>
                </div>
                
                {/* Progress bar */}
                <div className="w-full h-2 bg-dark-800 rounded-full overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${storageQuota.percentage}%` }}
                    transition={{ duration: 1, delay: 0.5, ease: 'easeOut' }}
                    className={`h-full rounded-full ${
                      storageQuota.percentage > 90
                        ? 'bg-red-500'
                        : storageQuota.percentage > 70
                        ? 'bg-yellow-500'
                        : 'bg-blue-500'
                    }`}
                  />
                </div>
                
                <p className="text-xs text-dark-400">
                  {storageQuota.percentage}% used
                </p>
              </div>
            </Card>
          </motion.div>

          {/* Workspace Card */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.4 }}
          >
            <Card className="h-full">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-lg bg-green-500/10 flex items-center justify-center flex-shrink-0">
                  <Folder className="w-6 h-6 text-green-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-white truncate">
                    {workspace.name}
                  </h3>
                  {workspace.description && (
                    <p className="text-dark-400 text-sm truncate">
                      {workspace.description}
                    </p>
                  )}
                  {workspace.memberCount !== undefined && (
                    <div className="flex items-center gap-1 mt-1 text-dark-400 text-xs">
                      <Users className="w-3 h-3" />
                      <span>{workspace.memberCount} members</span>
                    </div>
                  )}
                </div>
              </div>
            </Card>
          </motion.div>
        </div>

        {/* Recent Files Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="space-y-4"
        >
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Clock className="w-6 h-6" />
            Recent Files
          </h2>

          {recentFiles.length === 0 ? (
            <Card>
              <p className="text-dark-400 text-center py-8">
                No recent files to display
              </p>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {recentFiles.map((file, index) => (
                <motion.div
                  key={file.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.6 + index * 0.1 }}
                >
                  <Card className="hover:border-primary-500/50 transition-colors cursor-pointer">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-primary-500/10 flex items-center justify-center flex-shrink-0">
                        <File className="w-5 h-5 text-primary-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h4 className="text-white font-medium truncate">
                          {file.name}
                        </h4>
                        <p className="text-dark-400 text-xs truncate">
                          {file.path}
                        </p>
                        <div className="flex items-center gap-3 mt-1 text-xs text-dark-500">
                          <span>{formatBytes(file.size)}</span>
                          <span>•</span>
                          <span>{formatDate(file.modifiedAt)}</span>
                        </div>
                      </div>
                    </div>
                  </Card>
                </motion.div>
              ))}
            </div>
          )}
        </motion.div>

        {/* Features Info */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
        >
          <Card>
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-white">
                System Status
              </h3>
              <ul className="space-y-2 text-dark-400 text-sm">
                <li className="flex items-center gap-2">
                  <span className="text-green-500">✓</span> Authentication Active
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-green-500">✓</span> Data Synchronized
                </li>
                <li className="flex items-center gap-2">
                  <span className="text-green-500">✓</span> All Systems Operational
                </li>
              </ul>
            </div>
          </Card>
        </motion.div>
      </div>
    </div>
  );
};

export default Dashboard;
