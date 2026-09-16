import { analyticsRuleService } from './analyticsRuleService';
import apiClient from '@/utils/api';

jest.mock('@/utils/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('analyticsRuleService', () => {
  const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('loads analytics rules list', async () => {
    mockedApiClient.get.mockResolvedValue({
      data: {
        success: true,
        data: [{ id: 'r1', name: 'Rule 1', enabled: true }],
        message: 'ok',
      },
    } as never);

    const result = await analyticsRuleService.getRules();
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe('r1');
    expect(mockedApiClient.get).toHaveBeenCalledWith('/analytics/rules');
  });

  it('sends test notification for rule', async () => {
    mockedApiClient.post.mockResolvedValue({
      data: {
        success: true,
        data: { ruleId: 'r1', notificationId: 'n1', channels: ['in-app'] },
        message: 'ok',
      },
    } as never);

    const result = await analyticsRuleService.sendTestNotification('r1', { notifyInApp: true });
    expect(result.notificationId).toBe('n1');
    expect(mockedApiClient.post).toHaveBeenCalledWith('/analytics/rules/r1/test-notification', {
      notifyInApp: true,
    });
  });

  it('creates and updates analytics rule', async () => {
    const payload = {
      name: 'Rule New',
      analyticsType: 'MOTION_DETECTION',
      enabled: true,
      priority: 0,
      description: undefined,
      cameraId: undefined,
      conditions: {
        minConfidence: 0.5,
        objectTypes: [],
        zones: [],
        timeWindow: null,
        daysOfWeek: [],
        minObjectCount: 1,
        maxObjectCount: null,
        additionalConditions: {},
      },
      actions: {
        createEvent: false,
        eventType: 'MOTION_DETECTION',
        eventSeverity: 'INFO',
        sendNotification: true,
        notifyInApp: true,
        notifyEmail: false,
        notifyTelegram: false,
        notificationType: 'INFO',
        startRecording: false,
        recordingDuration: 60,
        sendWebhook: false,
        webhookUrl: null,
        additionalActions: {},
      },
    };

    mockedApiClient.post.mockResolvedValue({
      data: {
        success: true,
        data: { id: 'r-new', name: 'Rule New', enabled: true },
        message: 'created',
      },
    } as never);
    mockedApiClient.put.mockResolvedValue({
      data: {
        success: true,
        data: { id: 'r-new', name: 'Rule New Updated', enabled: true },
        message: 'updated',
      },
    } as never);

    const created = await analyticsRuleService.createRule(payload);
    const updated = await analyticsRuleService.updateRule('r-new', payload);

    expect(created.id).toBe('r-new');
    expect(updated.name).toContain('Updated');
    expect(mockedApiClient.post).toHaveBeenCalledWith('/analytics/rules', payload);
    expect(mockedApiClient.put).toHaveBeenCalledWith('/analytics/rules/r-new', payload);
  });

  it('enables disables and deletes rule', async () => {
    mockedApiClient.post.mockResolvedValue({
      data: { success: true, data: null, message: 'ok' },
    } as never);
    mockedApiClient.delete.mockResolvedValue({
      data: { success: true, data: null, message: 'ok' },
    } as never);

    await analyticsRuleService.enableRule('r1');
    await analyticsRuleService.disableRule('r1');
    await analyticsRuleService.deleteRule('r1');

    expect(mockedApiClient.post).toHaveBeenNthCalledWith(1, '/analytics/rules/r1/enable');
    expect(mockedApiClient.post).toHaveBeenNthCalledWith(2, '/analytics/rules/r1/disable');
    expect(mockedApiClient.delete).toHaveBeenCalledWith('/analytics/rules/r1');
  });
});

