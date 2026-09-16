import apiClient from '@/utils/api';
import type { ApiResponse } from '@/types';

export interface TestRuleNotificationRequest {
  userId?: string;
  cameraId?: string;
  notifyInApp?: boolean;
  notifyEmail?: boolean;
  notifyTelegram?: boolean;
}

export interface TestRuleNotificationResponse {
  ruleId: string;
  notificationId: string;
  channels: string[];
}

export interface AnalyticsRuleListItem {
  id: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  cameraId?: string | null;
  analyticsType?: string;
  priority?: number;
  conditions?: {
    minConfidence?: number;
    objectTypes?: string[];
    zones?: string[];
    timeWindow?: string | null;
    daysOfWeek?: number[];
    minObjectCount?: number;
  };
  actions?: {
    sendNotification?: boolean;
    notifyInApp?: boolean;
    notifyEmail?: boolean;
    notifyTelegram?: boolean;
    sendWebhook?: boolean;
  };
}

export interface AnalyticsRuleUpsertPayload {
  name: string;
  description?: string;
  cameraId?: string;
  analyticsType: string;
  enabled: boolean;
  priority: number;
  conditions: {
    minConfidence: number;
    objectTypes: string[];
    zones: string[];
    timeWindow?: string | null;
    daysOfWeek: number[];
    minObjectCount: number;
    maxObjectCount?: number | null;
    additionalConditions: Record<string, string>;
  };
  actions: {
    createEvent: boolean;
    eventType: string;
    eventSeverity: string;
    sendNotification: boolean;
    notifyInApp: boolean;
    notifyEmail: boolean;
    notifyTelegram: boolean;
    notificationType?: string | null;
    startRecording: boolean;
    recordingDuration: number;
    sendWebhook: boolean;
    webhookUrl?: string | null;
    additionalActions: Record<string, string>;
  };
}

export const analyticsRuleService = {
  async getRules(): Promise<AnalyticsRuleListItem[]> {
    const response = await apiClient.get<ApiResponse<AnalyticsRuleListItem[]>>('/analytics/rules');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch analytics rules');
  },

  async sendTestNotification(
    ruleId: string,
    payload: TestRuleNotificationRequest
  ): Promise<TestRuleNotificationResponse> {
    const response = await apiClient.post<ApiResponse<TestRuleNotificationResponse>>(
      `/analytics/rules/${ruleId}/test-notification`,
      payload
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to send test notification');
  },

  async enableRule(ruleId: string): Promise<void> {
    const response = await apiClient.post<ApiResponse<null>>(`/analytics/rules/${ruleId}/enable`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to enable analytics rule');
    }
  },

  async disableRule(ruleId: string): Promise<void> {
    const response = await apiClient.post<ApiResponse<null>>(`/analytics/rules/${ruleId}/disable`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to disable analytics rule');
    }
  },

  async deleteRule(ruleId: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(`/analytics/rules/${ruleId}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete analytics rule');
    }
  },

  async createRule(payload: AnalyticsRuleUpsertPayload): Promise<AnalyticsRuleListItem> {
    const response = await apiClient.post<ApiResponse<AnalyticsRuleListItem>>('/analytics/rules', payload);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to create analytics rule');
  },

  async updateRule(ruleId: string, payload: AnalyticsRuleUpsertPayload): Promise<AnalyticsRuleListItem> {
    const response = await apiClient.put<ApiResponse<AnalyticsRuleListItem>>(
      `/analytics/rules/${ruleId}`,
      payload
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to update analytics rule');
  },
};

