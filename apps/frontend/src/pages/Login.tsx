import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, Lock } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button, Checkbox } from '../components/ui';
import { motion } from 'framer-motion';
import { useAuthStore } from '../store/authStore';

// Validation schema
const loginSchema = z.object({
  usernameOrEmail: z
    .string()
    .min(1, 'Email or username is required'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters'),
  rememberMe: z.boolean().optional(),
});

type LoginFormData = z.infer<typeof loginSchema>;

const Login = () => {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const error = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: '',
      password: '',
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    clearError();
    
    try {
      await login(data);
      // Navigate to initialization page to load required data
      navigate('/initializing', { replace: true });
    } catch (error) {
      console.error('Login failed:', error);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Welcome Back"
      subtitle="Sign in to your account to continue"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {/* Error Message */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 text-sm text-red-400"
          >
            {error}
          </motion.div>
        )}

        {/* Email/Username Input */}
        <Input
          {...register('usernameOrEmail')}
          type="text"
          label="Email or Username"
          placeholder="you@example.com or username"
          error={errors.usernameOrEmail?.message}
          leftIcon={<Mail className="w-5 h-5" />}
          autoComplete="username"
          disabled={isLoading}
        />

        {/* Password Input */}
        <Input
          {...register('password')}
          type="password"
          label="Password"
          placeholder="Enter your password"
          error={errors.password?.message}
          leftIcon={<Lock className="w-5 h-5" />}
          showPasswordToggle
          autoComplete="current-password"
          disabled={isLoading}
        />

        {/* Remember Me & Forgot Password */}
        <div className="flex items-center justify-between">
          <Checkbox
            {...register('rememberMe')}
            label="Remember me"
            disabled={isLoading}
          />

          <Link
            to="/forgot-password"
            className="text-sm text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={isLoading ? -1 : 0}
          >
            Forgot password?
          </Link>
        </div>

        {/* Submit Button */}
        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Signing in...' : 'Sign In'}
        </Button>

        {/* Sign Up Link */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="text-center mt-6"
        >
          <p className="text-sm text-dark-400">
            Don't have an account?{' '}
            <Link
              to="/register"
              className="text-primary-400 hover:text-primary-300 font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
              tabIndex={isLoading ? -1 : 0}
            >
              Sign up
            </Link>
          </p>
        </motion.div>
      </form>
    </AuthLayout>
  );
};

export default Login;
