import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Button } from '../components/ui';
import { motion } from 'framer-motion';

const EmailVerificationSuccess = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  
  const [isVerifying, setIsVerifying] = useState(true);
  const [isSuccess, setIsSuccess] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [countdown, setCountdown] = useState(5);

  useEffect(() => {
    // Simulate API verification
    const verifyEmail = async () => {
      if (!token) {
        setHasError(true);
        setIsVerifying(false);
        return;
      }

      // Simulate API call
      console.log('Verifying email with token:', token);
      
      // TODO: Implement actual email verification logic
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Simulate success (change to setHasError(true) to test error state)
      setIsSuccess(true);
      setIsVerifying(false);
    };

    verifyEmail();
  }, [token]);

  // Countdown for auto-redirect
  useEffect(() => {
    if (isSuccess && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (isSuccess && countdown === 0) {
      window.location.href = '/login';
    }
  }, [isSuccess, countdown]);

  // Verifying state
  if (isVerifying) {
    return (
      <AuthLayout
        title="Verifying Your Email"
        subtitle="Please wait while we verify your email address"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Loading Icon */}
          <div className="flex justify-center">
            <div className="relative">
              <div className="absolute inset-0 bg-primary-500/20 rounded-full blur-xl" />
              <div className="relative bg-primary-500/10 p-4 rounded-full">
                <Loader2 className="w-16 h-16 text-primary-400 animate-spin" />
              </div>
            </div>
          </div>

          {/* Loading Message */}
          <div className="text-center space-y-3">
            <p className="text-dark-300 animate-pulse">
              Verifying your email address...
            </p>
          </div>
        </motion.div>
      </AuthLayout>
    );
  }

  // Error state
  if (hasError) {
    return (
      <AuthLayout
        title="Verification Failed"
        subtitle="We couldn't verify your email address"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Error Icon */}
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
              <div className="absolute inset-0 bg-red-500/20 rounded-full blur-xl" />
              <div className="relative bg-red-500/10 p-4 rounded-full">
                <AlertCircle className="w-16 h-16 text-red-500" />
              </div>
            </div>
          </motion.div>

          {/* Error Message */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="text-center space-y-3"
          >
            <p className="text-dark-300">
              The verification link you used is either invalid or has expired.
              Verification links are valid for 24 hours.
            </p>
          </motion.div>

          {/* Action Buttons */}
          <div className="space-y-3">
            <Button
              type="button"
              variant="primary"
              size="lg"
              fullWidth
              onClick={() => console.log('Resend verification email')}
            >
              Resend Verification Email
            </Button>

            <Button
              type="button"
              variant="ghost"
              size="md"
              fullWidth
              onClick={() => (window.location.href = '/login')}
            >
              Back to Login
            </Button>
          </div>

          {/* Help Text */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="text-center pt-4"
          >
            <p className="text-xs text-dark-500">
              Need help?{' '}
              <a
                href="/support"
                className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
              >
                Contact support
              </a>
            </p>
          </motion.div>
        </motion.div>
      </AuthLayout>
    );
  }

  // Success state
  return (
    <AuthLayout
      title="Email Verified!"
      subtitle="Your email has been successfully verified"
    >
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        {/* Success Icon with Confetti Effect */}
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{
            type: 'spring',
            stiffness: 200,
            damping: 15,
            delay: 0.1,
          }}
          className="relative flex justify-center"
        >
          {/* Confetti particles */}
          {[...Array(8)].map((_, i) => (
            <motion.div
              key={i}
              initial={{ scale: 0, x: 0, y: 0, opacity: 1 }}
              animate={{
                scale: [0, 1, 1],
                x: [0, Math.cos((i * Math.PI) / 4) * 60],
                y: [0, Math.sin((i * Math.PI) / 4) * 60],
                opacity: [1, 1, 0],
              }}
              transition={{
                duration: 1,
                delay: 0.3,
                ease: 'easeOut',
              }}
              className="absolute w-2 h-2 rounded-full"
              style={{
                backgroundColor: [
                  '#a855f7',
                  '#c084fc',
                  '#e879f9',
                  '#f472b6',
                  '#fb923c',
                  '#fbbf24',
                  '#34d399',
                  '#60a5fa',
                ][i],
              }}
            />
          ))}

          {/* Success icon */}
          <div className="relative z-10">
            <div className="absolute inset-0 bg-green-500/20 rounded-full blur-xl" />
            <div className="relative bg-green-500/10 p-4 rounded-full">
              <CheckCircle className="w-16 h-16 text-green-500" />
            </div>
          </div>
        </motion.div>

        {/* Success Message */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="text-center space-y-3"
        >
          <p className="text-dark-200 text-lg">
            Welcome to Ziboto!
          </p>
          <p className="text-dark-400 text-sm">
            Your account is now active. You can sign in and start using all
            features.
          </p>
        </motion.div>

        {/* Auto-redirect Notice */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="bg-primary-500/10 border border-primary-500/30 rounded-lg p-4 text-center"
        >
          <p className="text-sm text-primary-300">
            Redirecting to login in{' '}
            <span className="font-bold text-primary-400 text-lg">
              {countdown}
            </span>{' '}
            {countdown === 1 ? 'second' : 'seconds'}...
          </p>
        </motion.div>

        {/* Action Buttons */}
        <div className="space-y-3">
          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => (window.location.href = '/login')}
          >
            Continue to Login
          </Button>

          <Button
            type="button"
            variant="ghost"
            size="md"
            fullWidth
            onClick={() => (window.location.href = '/')}
          >
            Go to Homepage
          </Button>
        </div>

        {/* Additional Info */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5 }}
          className="text-center pt-4 space-y-2"
        >
          <p className="text-xs text-dark-500">
            🎉 You're all set! Enjoy using Ziboto.
          </p>
        </motion.div>
      </motion.div>
    </AuthLayout>
  );
};

export default EmailVerificationSuccess;
