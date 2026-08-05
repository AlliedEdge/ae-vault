import { motion } from 'framer-motion';
import { Logo, Button } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { LogOut, User as UserIcon } from 'lucide-react';

const Home = () => {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center p-4 relative overflow-hidden">
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

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="text-center space-y-8 max-w-2xl w-full"
      >
        <Logo size="lg" />
        
        <div className="space-y-3">
          <h1 className="text-4xl font-bold text-white">
            Welcome to Ziboto
          </h1>
          <p className="text-dark-300 text-lg">
            Your authentication is set up and working!
          </p>
        </div>

        {/* User Info Card */}
        {user && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
            className="glass-effect rounded-2xl p-8 space-y-4"
          >
            <div className="flex items-center justify-center gap-3 mb-4">
              <div className="w-16 h-16 rounded-full bg-gradient-to-br from-primary-600 to-primary-700 flex items-center justify-center">
                <UserIcon className="w-8 h-8 text-white" />
              </div>
            </div>

            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-white">{user.name}</h2>
              <p className="text-primary-400">{user.email}</p>
              {user.role && (
                <p className="text-dark-400 text-sm">Role: {user.role}</p>
              )}
            </div>

            <div className="pt-4 flex gap-3 justify-center">
              <Button
                variant="danger"
                size="md"
                onClick={handleLogout}
                className="gap-2"
              >
                <LogOut className="w-4 h-4" />
                Logout
              </Button>
            </div>
          </motion.div>
        )}

        <div className="bg-dark-900/40 backdrop-blur-xl border border-dark-700/50 rounded-2xl p-8 max-w-2xl">
          <div className="space-y-4 text-left">
            <h3 className="text-lg font-semibold text-white">
              Authentication Features
            </h3>
            <ul className="space-y-2 text-dark-400 text-sm">
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Login & Registration
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> JWT Token Management
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Automatic Token Refresh
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Session Expiration Handling
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Protected Routes
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Zustand State Management
              </li>
              <li className="flex items-center gap-2">
                <span className="text-green-500">✓</span> Axios Interceptors
              </li>
            </ul>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default Home;
