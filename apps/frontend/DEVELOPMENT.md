# Development Guide

## Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at `http://localhost:5173`

## Available Scripts

- `npm run dev` - Start development server with hot reload
- `npm run build` - Build for production
- `npm run preview` - Preview production build locally
- `npm run lint` - Run oxlint for code quality

## Development Workflow

### Testing Pages

You can test all authentication pages by navigating to:

- http://localhost:5173/ - Home page
- http://localhost:5173/login - Login page
- http://localhost:5173/register - Register page
- http://localhost:5173/forgot-password - Forgot password
- http://localhost:5173/reset-password?token=test123 - Reset password (with token)
- http://localhost:5173/verify-email?token=test123 - Email verification (with token)
- http://localhost:5173/session-expired - Session expired

### Adding New Pages

1. Create a new component in `src/pages/YourPage.tsx`
2. Export it from `src/pages/index.ts`
3. Add the route in `src/App.tsx`

Example:
```tsx
// src/pages/Dashboard.tsx
import { AuthLayout } from '../components/layout';

const Dashboard = () => {
  return (
    <AuthLayout title="Dashboard">
      {/* Your content */}
    </AuthLayout>
  );
};

export default Dashboard;
```

### Creating Custom Components

Place reusable components in `src/components/ui/`:

```tsx
// src/components/ui/YourComponent.tsx
import { motion } from 'framer-motion';

interface YourComponentProps {
  // Props definition
}

const YourComponent: React.FC<YourComponentProps> = (props) => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      {/* Component content */}
    </motion.div>
  );
};

export default YourComponent;
```

Don't forget to export from `src/components/ui/index.ts`.

## Styling Guide

### Using Tailwind Classes

```tsx
<div className="bg-dark-900 text-white p-4 rounded-lg">
  Content
</div>
```

### Custom Utilities

Available in `index.css`:

- `glass-effect` - Glassmorphism background
- `glass-effect-light` - Lighter glassmorphism
- `text-gradient` - Purple gradient text
- `shadow-glow-sm/md/lg` - Purple glow shadows

### Color Palette

**Primary (Purple):**
- `primary-50` to `primary-950`
- Main: `primary-500` (#a855f7)

**Dark Theme:**
- `dark-50` to `dark-950`
- Background: `dark-950` (#020617)
- Cards: `dark-900` (#0f172a)

## Animation Patterns

### Framer Motion Basics

```tsx
<motion.div
  initial={{ opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.5 }}
>
  Animated content
</motion.div>
```

### Page Transitions

```tsx
<AnimatePresence mode="wait">
  {condition ? (
    <motion.div
      key="view1"
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 20 }}
    >
      View 1
    </motion.div>
  ) : (
    <motion.div
      key="view2"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
    >
      View 2
    </motion.div>
  )}
</AnimatePresence>
```

## Form Validation

### Using React Hook Form + Zod

```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(8, 'Min 8 characters'),
});

type FormData = z.infer<typeof schema>;

const MyForm = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = (data: FormData) => {
    console.log(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        {...register('email')}
        label="Email"
        error={errors.email?.message}
      />
      {/* More fields */}
    </form>
  );
};
```

## Backend Integration

### API Call Pattern

```tsx
const onSubmit = async (data: FormData) => {
  setIsLoading(true);
  
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    
    if (!response.ok) {
      throw new Error('Login failed');
    }
    
    const result = await response.json();
    // Handle success
    navigate('/dashboard');
  } catch (error) {
    // Handle error
    console.error(error);
  } finally {
    setIsLoading(false);
  }
};
```

### Environment Variables

Create `.env` file:

```env
VITE_API_URL=http://localhost:3000/api
VITE_GOOGLE_CLIENT_ID=your_google_client_id
VITE_GITHUB_CLIENT_ID=your_github_client_id
```

Access in code:

```tsx
const apiUrl = import.meta.env.VITE_API_URL;
```

## Accessibility Checklist

- [ ] All interactive elements are keyboard accessible
- [ ] Focus states are visible
- [ ] Form inputs have labels
- [ ] Error messages are announced to screen readers
- [ ] Images have alt text (if any)
- [ ] Color contrast meets WCAG AA standards
- [ ] Page has meaningful title
- [ ] Headings are properly structured

## Performance Tips

1. **Code splitting**: Routes are automatically split by React Router
2. **Lazy loading**: Use `React.lazy()` for heavy components
3. **Image optimization**: Use WebP format and lazy loading
4. **Animation performance**: Use `transform` and `opacity` for animations
5. **Bundle analysis**: Run `npm run build` and check dist size

## Common Issues

### Tailwind styles not applying

1. Check `tailwind.config.js` content paths
2. Ensure `index.css` imports Tailwind directives
3. Restart dev server

### Animations not working

1. Verify Framer Motion is installed
2. Check for conflicting CSS transitions
3. Ensure parent has proper positioning context

### Form validation not triggering

1. Check Zod schema is correct
2. Verify zodResolver is used
3. Ensure field names match schema

## Resources

- [React Documentation](https://react.dev)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [Framer Motion Docs](https://www.framer.com/motion/)
- [React Hook Form](https://react-hook-form.com)
- [Zod Documentation](https://zod.dev)
- [Lucide Icons](https://lucide.dev)

## Getting Help

If you encounter issues:

1. Check browser console for errors
2. Review component documentation
3. Check TypeScript errors
4. Verify all dependencies are installed
5. Clear node_modules and reinstall if needed

```bash
rm -rf node_modules package-lock.json
npm install
```
