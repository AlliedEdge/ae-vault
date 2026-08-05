import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, Lock, User } from 'lucide-react';
import { AuthLayout } from '../components/layout';
import { Input, Button, PasswordStrengthIndicator } from '../components/ui';
import { motion } from 'framer-motion';
import { useAuthStore } from '../store/authStore';

// Validation schema
const registerSchema = z
  .object({
    username: z
      .string()
      .min(1, 'Username is required')
      .min(3, 'Username must be at least 3 characters')
      .max(50, 'Username must be less than 50 characters')
      .regex(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens'),
    firstName: z
      .string()
      .max(100, 'First name must be less than 100 characters')
      .optional(),
    lastName: z
      .string()
      .max(100, 'Last name must be less than 100 characters')
      .optional(),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address'),
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

type RegisterFormData = z.infer<typeof registerSchema>;

const Register = () => {
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);
  const error = useAuthStore((state) => state.error);
  const clearError = useAuthStore((state) => state.clearError);
  
  const [isLoading, setIsLoading] = useState(false);

  const {
    register: registerField,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onChange',
    defaultValues: {
      username: '',
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    clearError();

    try {
      await register({
        username: data.username,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
      });
      // Navigate to initialization page to load required data
      navigate('/initializing', { replace: true });
    } catch (error) {
      console.error('Registration failed:', error);
      // Error is handled by the store
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create Account"
      subtitle="Sign up to get started with Ziboto"
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

        {/* Username Input */}
        <Input
          {...registerField('username')}
          type="text"
          label="Username"
          placeholder="johndoe"
          error={errors.username?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="username"
          disabled={isLoading}
        />

        {/* First Name Input */}
        <Input
          {...registerField('firstName')}
          type="text"
          label="First Name (Optional)"
          placeholder="John"
          error={errors.firstName?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="given-name"
          disabled={isLoading}
        />

        {/* Last Name Input */}
        <Input
          {...registerField('lastName')}
          type="text"
          label="Last Name (Optional)"
          placeholder="Doe"
          error={errors.lastName?.message}
          leftIcon={<User className="w-5 h-5" />}
          autoComplete="family-name"
          disabled={isLoading}
        />

        {/* Email Input */}
        <Input
          {...registerField('email')}
          type="email"
          label="Email Address"
          placeholder="you@example.com"
          error={errors.email?.message}
          leftIcon={<Mail className="w-5 h-5" />}
          autoComplete="email"
          disabled={isLoading}
        />

        {/* Password Input */}
        <div>
          <Input
            {...registerField('password')}
            type="password"
            label="Password"
            placeholder="Create a strong password"
            error={errors.password?.message}
            leftIcon={<Lock className="w-5 h-5" />}
            showPasswordToggle
            autoComplete="new-password"
            disabled={isLoading}
          />
          <PasswordStrengthIndicator password={password} />
        </div>

        {/* Confirm Password Input */}
        <Input
          {...registerField('confirmPassword')}
          type="password"
          label="Confirm Password"
          placeholder="Re-enter your password"
          error={errors.confirmPassword?.message}
          leftIcon={<Lock className="w-5 h-5" />}
          showPasswordToggle
          autoComplete="new-password"
          disabled={isLoading}
        />

        {/* Terms and Privacy */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="text-xs text-dark-400 text-center"
        >
          By creating an account, you agree to our{' '}
          <Link
            to="/terms"
            className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={isLoading ? -1 : 0}
          >
            Terms of Service
          </Link>{' '}
          and{' '}
          <Link
            to="/privacy"
            className="text-primary-400 hover:text-primary-300 transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
            tabIndex={isLoading ? -1 : 0}
          >
            Privacy Policy
          </Link>
        </motion.div>

        {/* Submit Button */}
        <Button
          type="submit"
          variant="primary"
          size="lg"
          fullWidth
          isLoading={isLoading}
          disabled={isLoading}
        >
          {isLoading ? 'Creating account...' : 'Create Account'}
        </Button>

        {/* Sign In Link */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="text-center mt-6"
        >
          <p className="text-sm text-dark-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-primary-400 hover:text-primary-300 font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 rounded"
              tabIndex={isLoading ? -1 : 0}
            >
              Sign in
            </Link>
          </p>
        </motion.div>
      </form>
    </AuthLayout>
  );
};

export default Register;
