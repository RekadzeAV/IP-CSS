import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AnalyticsRulesPage from './page';
import { analyticsRuleService } from '@/services/analyticsRuleService';

jest.mock('@/components/Layout/Layout', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

jest.mock('@/components/ProtectedRoute/ProtectedRoute', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock('@/store/hooks', () => ({
  useAppDispatch: () => jest.fn(),
  useAppSelector: (selector: (state: unknown) => unknown) =>
    selector({
      auth: {
        user: { id: 'u1', role: 'ADMIN' },
      },
      analytics: {
        recentEvents: [],
      },
    }),
}));

jest.mock('notistack', () => ({
  useSnackbar: () => ({
    enqueueSnackbar: jest.fn(),
  }),
}));

jest.mock('@/services/analyticsRuleService', () => ({
  analyticsRuleService: {
    getRules: jest.fn(),
    sendTestNotification: jest.fn(),
    enableRule: jest.fn(),
    disableRule: jest.fn(),
    deleteRule: jest.fn(),
    createRule: jest.fn(),
    updateRule: jest.fn(),
  },
}));

describe('AnalyticsRulesPage', () => {
  const mockedService = analyticsRuleService as jest.Mocked<typeof analyticsRuleService>;
  const prefsKey = 'analyticsRules.uiPrefs.v1';

  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.clear();
  });

  it('loads rules and renders counters and table rows', async () => {
    mockedService.getRules.mockResolvedValue([
      {
        id: 'r1',
        name: 'Rule One',
        enabled: true,
        analyticsType: 'MOTION_DETECTION',
        actions: { sendNotification: true, notifyInApp: true },
      },
      {
        id: 'r2',
        name: 'Rule Two',
        enabled: false,
        analyticsType: 'OBJECT_DETECTION',
        actions: { sendNotification: true, notifyInApp: false, notifyEmail: false, notifyTelegram: false },
      },
    ]);

    render(<AnalyticsRulesPage />);

    expect(screen.getByText('Правила аналитики')).toBeInTheDocument();
    await waitFor(() => {
      expect(mockedService.getRules).toHaveBeenCalledTimes(1);
    });

    expect(screen.getByText('Rule One')).toBeInTheDocument();
    expect(screen.getByText('Rule Two')).toBeInTheDocument();
    expect(screen.getByText('Total: 2')).toBeInTheDocument();
    expect(screen.getByText('Enabled: 1')).toBeInTheDocument();
    expect(screen.getByText('Problematic: 1')).toBeInTheDocument();
  });

  it('filters problematic rules and supports problematic-first sorting', async () => {
    mockedService.getRules.mockResolvedValue([
      {
        id: 'r-good',
        name: 'Good Rule',
        enabled: true,
        analyticsType: 'MOTION_DETECTION',
        actions: { sendNotification: true, notifyInApp: true },
      },
      {
        id: 'r-bad-1',
        name: 'Bad Rule One',
        enabled: true,
        analyticsType: 'OBJECT_DETECTION',
        actions: { sendNotification: true, notifyInApp: false, notifyEmail: false, notifyTelegram: false },
      },
      {
        id: 'r-bad-2',
        name: 'Bad Rule Two',
        enabled: false,
        analyticsType: 'FACE_DETECTION',
        actions: { sendNotification: false, sendWebhook: false },
      },
    ]);

    render(<AnalyticsRulesPage />);
    await waitFor(() => expect(mockedService.getRules).toHaveBeenCalledTimes(1));

    // Switch sorting to problematic first (by visible label text)
    const sortSelect = screen.getAllByRole('combobox')[1];
    await userEvent.click(sortSelect);
    await userEvent.click(screen.getByText('Сортировка: Problematic first'));

    // Enable "only problematic"
    const problematicSwitch = screen.getByLabelText(
      'Только проблемные правила (no actions / no notify channel)'
    );
    await userEvent.click(problematicSwitch);

    await waitFor(() => {
      expect(screen.queryByText('Good Rule')).not.toBeInTheDocument();
      expect(screen.getByText('Bad Rule One')).toBeInTheDocument();
      expect(screen.getByText('Bad Rule Two')).toBeInTheDocument();
    });
  });

  it('resets filters and clears persisted ui preferences', async () => {
    mockedService.getRules.mockResolvedValue([
      {
        id: 'r1',
        name: 'Rule One',
        enabled: true,
        analyticsType: 'MOTION_DETECTION',
        actions: { sendNotification: true, notifyInApp: true },
      },
      {
        id: 'r2',
        name: 'Rule Two',
        enabled: false,
        analyticsType: 'OBJECT_DETECTION',
        actions: { sendNotification: true, notifyInApp: false, notifyEmail: false, notifyTelegram: false },
      },
    ]);

    render(<AnalyticsRulesPage />);
    await waitFor(() => expect(mockedService.getRules).toHaveBeenCalledTimes(1));

    const searchInput = screen.getByLabelText('Поиск по названию, ID, типу, cameraId');
    await userEvent.type(searchInput, 'Rule Two');
    const problematicSwitch = screen.getByLabelText(
      'Только проблемные правила (no actions / no notify channel)'
    );
    await userEvent.click(problematicSwitch);

    await waitFor(() => {
      expect((searchInput as HTMLInputElement).value).toBe('Rule Two');
      expect((problematicSwitch as HTMLInputElement).checked).toBe(true);
      expect(window.localStorage.getItem(prefsKey)).not.toBeNull();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Сбросить фильтры' }));

    await waitFor(() => {
      expect((searchInput as HTMLInputElement).value).toBe('');
      expect((problematicSwitch as HTMLInputElement).checked).toBe(false);
      expect(window.localStorage.getItem(prefsKey)).toBe(
        JSON.stringify({
          searchQuery: '',
          statusFilter: 'all',
          showOnlyProblematic: false,
          sortBy: 'name',
        })
      );
    });
  });
});

