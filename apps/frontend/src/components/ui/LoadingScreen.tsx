import { motion } from 'framer-motion';
import Logo from './Logo';
import { Loader2 } from 'lucide-react';

interface LoadingScreenProps {
  message?: string;
  progress?: number;
}

/**
 * Full-screen loading animation
 * Displays during app initialization
 */
export const LoadingScreen: React.FC<LoadingScreenProps> = ({
  message = 'Loading...',
  progress,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-dark-950">
      {/* Animated background gradients */}
      <div className="absolute inset-0 overflow-hidden">
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
          className="absolute top-0 -left-20 w-96 h-96 bg-primary-600/20 rounded-full blur-3xl"
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
          className="absolute bottom-0 -right-20 w-96 h-96 bg-primary-700/10 rounded-full blur-3xl"
        />
      </div>

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center gap-8 max-w-md px-6">
        {/* Logo with pulse animation */}
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{
            duration: 0.5,
            ease: 'easeOut',
          }}
        >
          <motion.div
            animate={{
              scale: [1, 1.05, 1],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          >
            <Logo size="lg" />
          </motion.div>
        </motion.div>

        {/* Spinner */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
        >
          <Loader2 className="w-12 h-12 text-primary-500 animate-spin" />
        </motion.div>

        {/* Message */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-center space-y-3"
        >
          <p className="text-lg text-white font-medium">{message}</p>
          
          {/* Progress bar (optional) */}
          {typeof progress === 'number' && (
            <div className="w-64 h-2 bg-dark-800 rounded-full overflow-hidden">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${progress}%` }}
                transition={{ duration: 0.3, ease: 'easeOut' }}
                className="h-full bg-gradient-to-r from-primary-600 to-primary-500 rounded-full"
              />
            </div>
          )}
        </motion.div>

        {/* Animated dots */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="flex gap-2"
        >
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              animate={{
                scale: [1, 1.2, 1],
                opacity: [0.3, 1, 0.3],
              }}
              transition={{
                duration: 1.5,
                repeat: Infinity,
                delay: i * 0.2,
                ease: 'easeInOut',
              }}
              className="w-2 h-2 bg-primary-500 rounded-full"
            />
          ))}
        </motion.div>
      </div>
    </div>
  );
};
