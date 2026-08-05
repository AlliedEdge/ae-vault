import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Lock, CheckCircle, AlertCircle } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button, PasswordStrengthIndicator } from '../components/ui';
import { motion } from 'framer-motion';
import { useAuthOperations } from '../hooks/useAuthOperations';

// Validation schema
const resetPasswordSchema = z
  .object({
    password: z
      .string()
      .min(1, 'Password is required')
      .min(8, 'Password must be at least 8 characters')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>;

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const { resetPassword } = useAuthOperations();
  
  const [hasError] = useState(!token);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    mode: 'onChange',
    defaultValues: {
      password: '',
      confirmPassword: '',
    },
  });

  const password = watch('password');

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return;

    try {
      await resetPassword.handleExecute({
        token,
        newPassword: data.password,
      });
    } catch (error) {
      // Error is already displayed by the hook
      console.error('Reset password failed:', error);
    }
  };

  // Invalid or expired token
  if (hasError) {
    return (
      <AuthLayout
        title="Invalid Reset Link"
        subtitle="This password reset link is invalid or has expired"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-6"
        >
          {/* Error Icon */}
          <div className="flex justify-center">
            <div className="relative">
              <div className="absolute inset-0 bg-red-500/20 rounded-full blur-xl" />
              <div className="relative bg-red-500/10 p-4 rounded-full">
                <AlertCircle className="w-16 h-16 text-red-500" />
              </div>
            </div>
          </div>

          {/* Error Message */}
          <div className="text-center space-y-3">
            <p className="text-dark-300">
              The password reset link you used is either invalid or has expired.
              Reset links are valid for 1 hour.
            </p>
          </div>

          {/* Action Buttons */}
          <div className="space-y-3">
            <Button
              type="button"
              variant="primary"
              size="lg"
              fullWidth
              onClick={() => (window.location.href = '/forgot-password')}
            >
              Request New Link
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
        </motion.div>
      </AuthLayout>
    );
  }

  // Success state
  if (resetPassword.isSuccess) {
    return (
      <AuthLayout
        title="Password Reset Successful"
        subtitle="Your password has been successfully reset"
      >
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
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
              You can now sign in with your new password.
            </p>
          </motion.div>

          {/* Action Button */}
          <Button
            type="button"
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => (window.location.href = '/login')}
          >
            Continue to Login
          </Button>
        </motion.div>
      </AuthLayout>
    );
  }

  // Reset password form
  return (
    <AuthLayout
      title="Reset Password"
      subtitle="Enter your new password below"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Error Message */}
        {resetPassword.error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400"
          >
            {resetPassword.error}
          </motion.div>
        )}

        {/* New Password Input */}
        <div>
          <Input
            {...register('password')}
            type="password"
            label="New Password"
            placeholder="Create a strong password"
            error={errors.password?.message}
            leftIcon={<Lock className="w-5 h-5" />}
            showPasswordToggle
            autoComplete="new-password"
            autoFocus
            disabled={resetPassword.isLoading}
          />
          <PasswordStrengthIndicator password={password} />
        </div>

        {/* Confirm Password Input */}
        <Input
          {...register('confirmPassword')}
          type="password"
          label="Confirm New Password"
          placeholder="Re-enter your password"
          error={errors.confirmPassword?.message}
          leftIcon={<Lock className="w-5 h-5" />}
          showPasswordToggle
          autoComplete="new-password"
          disabled={resetPassword.isLoading}
        />

        {/* Password Requirements */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="bg-dark-800/50 border border-dark-700 rounded-lg p-4 space-y-2"
        >
          <p className="text-xs font-medium text-dark-300 mb-2">
            Password must contain:
          </p>
          <ul className="space-y-1.5 text-xs text-dark-400">
            <li className="flex items-center gap-2">
              <div
                className={`w-1.5 h-1.5 rounded-full ${
                  password?.length >= 8 ? 'bg-green-500' : 'bg-dark-600'
                }`}
              />
              At least 8 characters
            </li>
            <li className="flex items-center gap-2">
              <div
                className={`w-1.5 h-1.5 rounded-full ${
                  /[A-Z]/.test(password || '') ? 'bg-green-500' : 'bg-dark-600'
                }`}
              />
              One uppercase letter
            </li>
            <li className="flex items-center gap-2">
              <div
                className={`w-1.5 h-1.5 rounded-full ${
                  /[a-z]/.test(password || '') ? 'bg-green-500' : 'bg-dark-600'
                }`}
              />
              One lowercase letter
            </li>
            <li className="flex items-center gap-2">
              <div
                className={`w-1.5 h-1.5 rounded-full ${
                  /[0-9]/.test(password || '') ? 'bg-green-500' : 'bg-dark-600'
                }`}
              />
              One number
            </li>
          </ul>
        </motion.div>

        {/* Submit Button */}
        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={resetPassword.isLoading}
          disabled={resetPassword.isLoading}
        >
          {resetPassword.isLoading ? 'Resetting password...' : 'Reset Password'}
        </Button>

        {/* Back to Login */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-center"
        >
          <Link
            to="/login"
            className="text-sm text-dark-300 hover:text-primary-400 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={resetPassword.isLoading ? -1 : 0}
          >
            Back to login
          </Link>
        </motion.div>
      </form>
    </AuthLayout>
  );
};

export default ResetPassword;
