import { AppRouter } from './app/AppRouter';
import { AuthProvider } from './features/auth/AuthProvider';

/**
 * Root component of the BD QuickMart admin web application.
 *
 * AuthProvider wraps the router so the session context is available to every
 * route. It does not need router hooks itself: on an unrecoverable auth
 * failure it simply clears the user, and RequireAuth redirects to /login.
 */
function App() {
  return (
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  );
}

export default App;
