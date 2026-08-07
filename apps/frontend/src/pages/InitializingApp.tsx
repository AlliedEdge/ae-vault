import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { appInitService, type InitializationData } from '../services/appInitService';
import { LoadingScreen } from '../components/ui';
import Dashboard from './Dashboard';

/**
 * Initialization states
 */
type InitStep =
  | 'verifying-auth'
  | 'fetching-user'
  | 'fetching-storage'
  | 'fetching-files'
  | 'fetching-workspace'
  | 'complete'
  | 'error';

/**
 * Step progress mapping
 */
const STEP_PROGRESS: Record<Exclude<InitStep, 'error'>, number> = {
  'verifying-auth': 20,
  'fetching-user': 40,
  'fetching-storage': 60,
  'fetching-files': 80,
  'fetching-workspace': 90,
  'complete': 100,
};

/**
 * Step messages
 */
const STEP_MESSAGES: Record<Exclude<InitStep, 'error'>, string> = {
  'verifying-auth': 'Verifying authentication...',
  'fetching-user': 'Loading your profile...',
  'fetching-storage': 'Checking storage quota...',
  'fetching-files': 'Loading recent files...',
  'fetching-workspace': 'Loading workspace information...',
  'complete': 'Initialization complete!',
};

/**
 * InitializingApp component
 * Handles the initialization flow after successful login
 */
const InitializingApp = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, checkAuth } = useAuthStore();
  
  const [currentStep, setCurrentStep] = useState<InitStep>('verifying-auth');
  const [initData, setInitData] = useState<InitializationData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    initializeApplication();
  }, []);

  const initializeApplication = async () => {
    try {
      // Step 1: Verify authentication
      setCurrentStep('verifying-auth');
      await new Promise((resolve) => setTimeout(resolve, 500)); // Minimum display time
      
      if (!isAuthenticated) {
        await checkAuth();
      }

      if (!isAuthenticated) {
        // Not authenticated, redirect to login
        navigate('/login', { replace: true });
        return;
      }

      // Step 2: Fetch current user (already available from auth store)
      setCurrentStep('fetching-user');
      await new Promise((resolve) => setTimeout(resolve, 300));

      if (!user) {
        throw new Error('User data not available');
      }

      // Skip fetching additional data for now (endpoints not implemented yet)
      // TODO: Re-enable when backend endpoints are ready
      // setCurrentStep('fetching-storage');
      // setCurrentStep('fetching-files');
      // setCurrentStep('fetching-workspace');
      // const data = await appInitService.initializeAppWithRetry(user, 3);
      // setInitData(data);
      
      // Step 6: Complete
      setCurrentStep('complete');
      await new Promise((resolve) => setTimeout(resolve, 300)); // Show completion

      // Redirect to File Manager after successful initialization
      navigate('/files', { replace: true });

    } catch (err: any) {
      console.error('Initialization error:', err);
      setError(err.message || 'Failed to initialize application');
      setCurrentStep('error');

      // Redirect to login after error
      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: {
            message: 'Failed to load application data. Please try again.',
          },
        });
      }, 3000);
    }
  };

  // Show error state
  if (currentStep === 'error') {
    return (
      <LoadingScreen
        message={error || 'Initialization failed'}
      />
    );
  }

  // Show loading during initialization (including after complete, until navigation happens)
  return (
    <LoadingScreen
      message={STEP_MESSAGES[currentStep === 'complete' ? 'complete' : currentStep]}
      progress={STEP_PROGRESS[currentStep === 'complete' ? 'complete' : currentStep]}
    />
  );
};

export default InitializingApp;
