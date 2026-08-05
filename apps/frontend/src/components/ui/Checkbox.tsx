import { forwardRef } from 'react';
import type { InputHTMLAttributes } from 'react';
import { motion } from 'framer-motion';
import { Check } from 'lucide-react';

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  error?: string;
}

const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, error, className = '', id, ...props }, ref) => {
    const checkboxId = id || `checkbox-${label?.replace(/\s+/g, '-').toLowerCase()}`;

    return (
      <div className={`flex items-start ${className}`}>
        <div className="relative flex items-center h-5">
          <input
            ref={ref}
            type="checkbox"
            id={checkboxId}
            className="peer sr-only"
            aria-invalid={error ? 'true' : 'false'}
            aria-describedby={error ? `${checkboxId}-error` : undefined}
            {...props}
          />
          <motion.div
            whileTap={{ scale: 0.95 }}
            className="w-5 h-5 rounded border-2 border-dark-600 bg-dark-800/50 
                       peer-checked:bg-gradient-to-br peer-checked:from-primary-600 peer-checked:to-primary-700 
                       peer-checked:border-primary-500
                       peer-focus-visible:ring-2 peer-focus-visible:ring-primary-500 peer-focus-visible:ring-offset-2 peer-focus-visible:ring-offset-dark-950
                       peer-disabled:opacity-50 peer-disabled:cursor-not-allowed
                       transition-all duration-200 cursor-pointer
                       flex items-center justify-center"
          >
            <motion.div
              initial={{ scale: 0, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0, opacity: 0 }}
              transition={{ duration: 0.15 }}
              className="hidden peer-checked:block"
            >
              <Check className="w-3.5 h-3.5 text-white" strokeWidth={3} />
            </motion.div>
          </motion.div>
        </div>

        {label && (
          <label
            htmlFor={checkboxId}
            className="ml-3 text-sm text-dark-200 cursor-pointer select-none peer-disabled:opacity-50 peer-disabled:cursor-not-allowed"
          >
            {label}
          </label>
        )}

        {error && (
          <p
            className="mt-1 text-sm text-red-400"
            id={`${checkboxId}-error`}
            role="alert"
          >
            {error}
          </p>
        )}
      </div>
    );
  }
);

Checkbox.displayName = 'Checkbox';

export default Checkbox;
