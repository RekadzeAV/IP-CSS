import { render, screen, waitFor } from '@testing-library/react';
import ProtectedRoute from './ProtectedRoute';
import { fetchCurrentUser } from '@/store/slices/authSlice';

const pushMock = jest.fn();
const dispatchMock = jest.fn();
const useAppSelectorMock = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}));

jest.mock('@/store/hooks', () => ({
  useAppDispatch: () => dispatchMock,
  useAppSelector: (selector: (state: unknown) => unknown) => useAppSelectorMock(selector),
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders children for authenticated user', () => {
    useAppSelectorMock.mockImplementation((selector: (state: unknown) => unknown) =>
      selector({ auth: { isAuthenticated: true, loading: false } })
    );

    render(
      <ProtectedRoute>
        <div>Secret Content</div>
      </ProtectedRoute>
    );

    expect(screen.getByText('Secret Content')).toBeInTheDocument();
    expect(pushMock).not.toHaveBeenCalled();
  });

  it('redirects to login when user fetch is rejected', async () => {
    useAppSelectorMock.mockImplementation((selector: (state: unknown) => unknown) =>
      selector({ auth: { isAuthenticated: false, loading: false } })
    );
    dispatchMock.mockResolvedValue({ type: fetchCurrentUser.rejected.type });

    render(
      <ProtectedRoute>
        <div>Hidden Content</div>
      </ProtectedRoute>
    );

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith('/login');
    });
  });
});
