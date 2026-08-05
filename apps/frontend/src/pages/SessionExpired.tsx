import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Clock, RefreshCw, LogOut } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Button } from '../components/ui';
import { motion } from 'framer-motion';

const SessionExpired = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [countdown, setCountdown] = useState(10);
  const [isPaused, setIsPaused] = useState(false);

  // Get the page user was trying to access (if available)
  const returnUrl = location.state?.from || '/';

  // Countdown for auto-redirect
  useEffect(() => {
    if (!isPaused && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (!isPaused && countdown === 0) {
      navigate('/login', { state: { from: returnUrl } });
    }
  }, [countdown, isPaused, navigate, returnUrl]);

  const handleLoginNow = () => {
    navigate('/login', { state: { from: returnUrl } });
  };

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <AuthLayout
      title="Session Expired"
      subtitle="Your session has timed out due to inactivity"
    >
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        {/* Warning Icon */}
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{
            type: 'spring',
            stiffness: 200,
            damping: 15,
            delay: 0.1,
          }}
          className="flex justify-center"
        >
          <div className="relative">
            {/* Pulsing rings */}
            <motion.div
              animate={{
                scale: [1, 1.2, 1],
                opacity: [0.5, 0.2, 0.5],
              }}
              transition={{
                duration: 2,
                repeat: Infinity,
                ease: 'easeInOut',
              }}
              className="absolute inset-0 bg-orange-500/30 rounded-full blur-xl"
            />
            <motion.div
              animate={{
                scale: [1, 1.3, 1],
                opacity: [0.3, 0.1, 0.3],
              }}
              transition={{
                duration: 2,
                repeat: Infinity,
                ease: 'easeInOut',
                delay: 0.5,
              }}
              className="absolute inset-0 bg-orange-500/20 rounded-full blur-2xl"
            />
            
            {/* Icon container */}
            <div className="relative bg-orange-500/10 p-4 rounded-full">
              <Clock className="w-16 h-16 text-orange-500" />
            </div>
          </div>
        </motion.div>

        {/* Session Info */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="text-center space-y-3"
        >
          <p className="text-dark-200">
            For your security, we've logged you out after a period of
            inactivity.
          </p>
          <p className="text-sm text-dark-400">
            Please sign in again to continue where you left off.
          </p>
        </motion.div>

        {/* Auto-redirect Notice */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="bg-orange-500/10 border border-orange-500/30 rounded-lg p-4"
          onMouseEnter={() => setIsPaused(true)}
          onMouseLeave={() => setIsPaused(false)}
        >
          <div className="flex items-center justify-center gap-3">
            <RefreshCw className="w-5 h-5 text-orange-400" />
            <p className="text-sm text-orange-300">
              Redirecting to login in{' '}
              <span className="font-bold text-orange-400 text-lg">
                {countdown}
              </span>{' '}
              {countdown === 1 ? 'second' : 'seconds'}...
            </p>
          </div>
          {isPaused && (
            <p className="text-xs text-orange-400/70 text-center mt-2">
              (Paused)
            </p>
          )}
        </motion.div>

        {/* Session Details (Optional) */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="bg-dark-800/50 border border-dark-700 rounded-lg p-4 space-y-2"
        >
          <div className="flex items-start gap-3">
            <LogOut className="w-5 h-5 text-dark-400 flex-shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="text-sm font-medium text-dark-300">
                Why did this happen?
              </p>
              <p className="text-xs text-dark-500 leading-relaxed">
                Sessions expire after 30 minutes of inactivity to protect your
                account. Your data is safe and you can resume by logging in again.
              </p>
            </div>
          </div>
        </motion.div>

        {/* Action Buttons */}
        <div className="space-y-3">
          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            onClick={handleLoginNow}
          >
            Sign In Again
          </Button>

          <Button
            type="button"
            variant="secondary"
            size="md"
            fullWidth
            onClick={handleGoHome}
          >
            Go to Homepage
          </Button>
        </div>

        {/* Security Tip */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
          className="text-center pt-4"
        >
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-primary-500/5 border border-primary-500/20 rounded-lg">
            <span className="text-primary-400">💡</span>
            <p className="text-xs text-dark-400">
              <span className="text-primary-400 font-medium">Security tip:</span>{' '}
              Always log out when using shared devices
            </p>
          </div>
        </motion.div>
      </motion.div>
    </AuthLayout>
  );
};

export default SessionExpired;
