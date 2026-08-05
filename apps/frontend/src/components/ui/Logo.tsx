import { motion } from 'framer-motion';
import { Shield } from 'lucide-react';

interface LogoProps {
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
  className?: string;
}

const Logo: React.FC<LogoProps> = ({ 
  size = 'md', 
  showText = true, 
  className = '' 
}) => {
  const sizes = {
    sm: { icon: 'w-6 h-6', text: 'text-xl' },
    md: { icon: 'w-10 h-10', text: 'text-3xl' },
    lg: { icon: 'w-16 h-16', text: 'text-5xl' },
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className={`flex items-center gap-3 ${className}`}
    >
      <motion.div
        whileHover={{ rotate: [0, -10, 10, -10, 0] }}
        transition={{ duration: 0.5 }}
        className="relative"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-primary-500 to-primary-700 rounded-xl blur-lg opacity-50" />
        <div className="relative bg-gradient-to-br from-primary-600 to-primary-700 p-2 rounded-xl shadow-glow-sm">
          <Shield className={`${sizes[size].icon} text-white`} />
        </div>
      </motion.div>
      
      {showText && (
        <motion.h1
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.1, duration: 0.3 }}
          className={`${sizes[size].text} font-bold text-gradient`}
        >
          Ziboto
        </motion.h1>
      )}
    </motion.div>
  );
};

export default Logo;
