import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button } from '../components/ui';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthOperations } from '../hooks/useAuthOperations';

// Validation schema
const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
});

type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

const ForgotPassword = () => {
  const [isSuccess, setIsSuccess] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');
  const { forgotPassword } = useAuthOperations();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    try {
      await forgotPassword.handleExecute({ email: data.email });
      setSubmittedEmail(data.email);
      setIsSuccess(true);
    } catch (error) {
      // Error is already displayed by the hook
      console.error('Forgot password failed:', error);
    }
  };

  const handleResend = async () => {
    try {
      await forgotPassword.handleExecute({ email: submittedEmail });
    } catch (error) {
      console.error('Resend failed:', error);
    }
  };

  return (
    <AuthLayout
      title={isSuccess ? 'Check Your Email' : 'Forgot Password?'}
      subtitle={
        isSuccess
          ? 'We sent a password reset link to your email'
          : "No worries, we'll send you reset instructions"
      }
    >
      <AnimatePresence mode="wait">
        {!isSuccess ? (
          <motion.form
            key="form"
            initial={{ opacity: 1 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-5"
          >
            {/* Error Message */}
            {forgotPassword.error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400"
              >
                {forgotPassword.error}
              </motion.div>
            )}

            {/* Email Input */}
            <Input
              {...register('email')}
              type="email"
              label="Email Address"
              placeholder="you@example.com"
              error={errors.email?.message}
              leftIcon={<Mail className="w-5 h-5" />}
              autoComplete="email"
              autoFocus
              disabled={forgotPassword.isLoading}
            />

            {/* Submit Button */}
            <Button
              type="submit"
              variant="primary"
              size="lg"
              fullWidth
              isLoading={forgotPassword.isLoading}
              disabled={forgotPassword.isLoading}
            >
              {forgotPassword.isLoading ? 'Sending...' : 'Send Reset Link'}
            </Button>

            {/* Back to Login */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="text-center"
            >
              <Link
                to="/login"
                className="inline-flex items-center gap-2 text-sm text-dark-300 hover:text-primary-400 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
                tabIndex={forgotPassword.isLoading ? -1 : 0}
              >
                <ArrowLeft className="w-4 h-4" />
                Back to login
              </Link>
            </motion.div>
          </motion.form>
        ) : (
          <motion.div
            key="success"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3 }}
            className="space-y-6"
          >
            {/* Success Icon */}
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
              <p className="text-dark-200">
                We sent a password reset link to
              </p>
              <p className="text-primary-400 font-medium text-lg">
                {submittedEmail}
              </p>
              <p className="text-sm text-dark-400">
                Click the link in the email to reset your password. If you don't
                see the email, check your spam folder.
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
                Back to Login
              </Button>

              <Button
                type="button"
                variant="ghost"
                size="md"
                fullWidth
                isLoading={forgotPassword.isLoading}
                disabled={forgotPassword.isLoading}
                onClick={handleResend}
              >
                {forgotPassword.isLoading ? 'Resending...' : "Didn't receive the email? Resend"}
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
                <Link
                  to="/support"
                  className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
                >
                  Contact support
                </Link>
              </p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </AuthLayout>
  );
};

export default ForgotPassword;
