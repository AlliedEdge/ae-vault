import { motion } from 'framer-motion';
import { useMemo } from 'react';

interface PasswordStrengthIndicatorProps {
  password: string;
  showLabel?: boolean;
}

export type PasswordStrength = {
  score: number; // 0-4
  label: string;
  color: string;
  percentage: number;
};

export const calculatePasswordStrength = (password: string): PasswordStrength => {
  if (!password) {
    return { score: 0, label: '', color: '', percentage: 0 };
  }

  let score = 0;

  // Length check
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;

  // Character variety checks
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^a-zA-Z0-9]/.test(password)) score++;

  const strengths = [
    { score: 0, label: '', color: 'bg-dark-600', percentage: 0 },
    { score: 1, label: 'Weak', color: 'bg-red-500', percentage: 25 },
    { score: 2, label: 'Fair', color: 'bg-orange-500', percentage: 50 },
    { score: 3, label: 'Good', color: 'bg-yellow-500', percentage: 75 },
    { score: 4, label: 'Strong', color: 'bg-green-500', percentage: 100 },
  ];

  return strengths[Math.min(score, 4)];
};

const PasswordStrengthIndicator: React.FC<PasswordStrengthIndicatorProps> = ({
  password,
  showLabel = true,
}) => {
  const strength = useMemo(() => calculatePasswordStrength(password), [password]);

  if (!password) return null;

  return (
    <div className="mt-3 space-y-2">
      <div className="flex gap-1.5 h-1.5">
        {[1, 2, 3, 4].map((level) => (
          <div
            key={level}
            className="flex-1 rounded-full bg-dark-700 overflow-hidden"
          >
            <motion.div
              initial={{ width: 0 }}
              animate={{
                width: strength.score >= level ? '100%' : '0%',
              }}
              transition={{ duration: 0.3, delay: level * 0.05 }}
              className={`h-full ${strength.score >= level ? strength.color : ''}`}
            />
          </div>
        ))}
      </div>

      {showLabel && strength.label && (
        <motion.p
          initial={{ opacity: 0, y: -5 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-xs text-dark-300"
        >
          Password strength:{' '}
          <span
            className={`font-medium ${
              strength.score === 1
                ? 'text-red-400'
                : strength.score === 2
                ? 'text-orange-400'
                : strength.score === 3
                ? 'text-yellow-400'
                : 'text-green-400'
            }`}
          >
            {strength.label}
          </span>
        </motion.p>
      )}
    </div>
  );
};

export default PasswordStrengthIndicator;
