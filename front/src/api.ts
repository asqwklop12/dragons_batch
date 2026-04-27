export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  message: string | null;
};

export type BatchConfig = {
  apiConfigured: boolean;
  mockMode: boolean;
  baseUrl: string;
  certKeySet: boolean;
  certIdSet: boolean;
};

export type BatchMonthlyRunResponse = {
  jobExecutionId: number;
  status: string;
  startTime: string | null;
  endTime: string | null;
  itemCategoryCode: string;
  year: string;
  month: string;
  mockMode: boolean;
};

export type BatchStatusItem = {
  jobInstanceId: number;
  jobExecutionId: number;
  jobName: string;
  status: string;
  startTime: string | null;
  endTime: string | null;
  exitCode: string | null;
  params: Record<string, string>;
};

export type BatchStatusResponse = {
  count: number;
  mockMode: boolean;
  apiConfigured: boolean;
  data: BatchStatusItem[];
};

export type PriceItem = {
  id: number;
  itemCode: string;
  itemName: string;
  kindCode: string;
  kindName: string;
  marketCode: string;
  marketName: string;
  rankCode: string;
  rankName: string;
  price: number;
  unit: string;
  regDay: string;
  createdAt: string;
};

export type PriceListResponse = {
  count: number;
  data: PriceItem[];
};

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
    ...init,
  });

  let payload: ApiResponse<T> | null = null;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new Error('서버 응답을 해석하지 못했습니다.');
  }

  if (!response.ok || !payload.success || payload.data == null) {
    throw new Error(payload.message ?? '요청 처리에 실패했습니다.');
  }

  return payload.data;
}

export function fetchBatchConfig() {
  return request<BatchConfig>('/api/batch/config');
}

export function fetchBatchStatuses(limit = 10) {
  const searchParams = new URLSearchParams({ limit: String(limit) });
  return request<BatchStatusResponse>(`/api/batch/status?${searchParams.toString()}`);
}

export function runMonthlyBatch(input: {
  itemCategoryCode: string;
  year: string;
  month: string;
}) {
  const body = new URLSearchParams(input);
  return request<BatchMonthlyRunResponse>('/api/batch/run-monthly', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    body,
  });
}

export function fetchLatestPrices(limit = 50) {
  const searchParams = new URLSearchParams({ limit: String(limit) });
  return request<PriceListResponse>(`/api/prices/latest?${searchParams.toString()}`);
}

export function fetchPricesByDate(regDay: string) {
  const searchParams = new URLSearchParams({ regDay });
  return request<PriceListResponse>(`/api/prices?${searchParams.toString()}`);
}

export function searchPrices(itemName: string) {
  const searchParams = new URLSearchParams({ itemName });
  return request<PriceListResponse>(`/api/prices/search?${searchParams.toString()}`);
}
