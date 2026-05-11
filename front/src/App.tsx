import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import {
  fetchBatchConfig,
  fetchBatchStatuses,
  fetchLatestPrices,
  fetchPricesByDate,
  runMonthlyBatch,
  searchPrices,
  type BatchConfig,
  type BatchMonthlyRunResponse,
  type BatchStatusItem,
  type PriceItem,
} from './api';
import { DEFAULT_ITEM_CATEGORY, ITEM_CATEGORIES } from './constants';

type MonthlyRunForm = {
  itemCategoryCode: string;
  year: string;
  month: string;
};

type TrendSeriesOption = {
  key: string;
  label: string;
  items: PriceItem[];
};

function createInitialMonthlyForm(): MonthlyRunForm {
  const now = new Date();
  return {
    itemCategoryCode: DEFAULT_ITEM_CATEGORY,
    year: String(now.getFullYear()),
    month: String(now.getMonth() + 1).padStart(2, '0'),
  };
}

function getToday() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateTime(value: string | null) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function formatPrice(price: number | null | undefined) {
  if (price == null) return '-';
  return `${price.toLocaleString()}원`;
}

function getBackendOrigin() {
  if (typeof window === 'undefined') return '';
  if (window.location.port === '5173') return 'http://localhost:8080';
  return window.location.origin;
}

function getCategoryMeta(itemCategoryCode: string) {
  return (
    ITEM_CATEGORIES.find((category) => category.value === itemCategoryCode) ?? {
      value: itemCategoryCode,
      label: itemCategoryCode,
      emoji: '📦',
    }
  );
}

function filterByCategory(items: PriceItem[], categoryCode: string) {
  return items.filter((item) => item.marketCode === categoryCode);
}

function getStatusTone(status: string) {
  switch (status) {
    case 'COMPLETED':
      return 'statusSuccess';
    case 'FAILED':
      return 'statusFailed';
    case 'STARTING':
    case 'STARTED':
      return 'statusRunning';
    default:
      return 'statusIdle';
  }
}

function getRankTone(rankName: string) {
  if (rankName.includes('상품')) return 'rankGood';
  if (rankName.includes('중품')) return 'rankMid';
  if (rankName.includes('하품')) return 'rankLow';
  return 'rankDefault';
}

function buildSmoothPath(points: Array<{ x: number; y: number }>) {
  if (points.length === 0) return '';
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`;
  if (points.length === 2) {
    return `M ${points[0].x} ${points[0].y} L ${points[1].x} ${points[1].y}`;
  }

  let path = `M ${points[0].x} ${points[0].y}`;

  for (let index = 0; index < points.length - 1; index += 1) {
    const p0 = points[index - 1] ?? points[index];
    const p1 = points[index];
    const p2 = points[index + 1];
    const p3 = points[index + 2] ?? p2;

    const control1X = p1.x + (p2.x - p0.x) / 6;
    const control1Y = p1.y + (p2.y - p0.y) / 6;
    const control2X = p2.x - (p3.x - p1.x) / 6;
    const control2Y = p2.y - (p3.y - p1.y) / 6;

    path += ` C ${control1X} ${control1Y}, ${control2X} ${control2Y}, ${p2.x} ${p2.y}`;
  }

  return path;
}

function buildAreaPath(points: Array<{ x: number; y: number }>, baselineY: number) {
  if (points.length === 0) return '';
  const smoothLine = buildSmoothPath(points);
  const last = points[points.length - 1];
  const first = points[0];
  return `${smoothLine} L ${last.x} ${baselineY} L ${first.x} ${baselineY} Z`;
}

function createScaledPoints<T extends { value: number }>(
  rows: T[],
  width: number,
  height: number,
  paddingX: number,
  paddingTop: number,
  paddingBottom: number,
) {
  const values = rows.map((row) => row.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const rawRange = Math.max(max - min, 1);
  const paddedMin = Math.max(0, min - rawRange * 0.18);
  const paddedMax = max + rawRange * 0.18;
  const scaledRange = Math.max(paddedMax - paddedMin, 1);
  const usableWidth = width - paddingX * 2;
  const usableHeight = height - paddingTop - paddingBottom;
  const stepX = rows.length === 1 ? 0 : usableWidth / (rows.length - 1);

  return rows.map((row, index) => {
    const x = paddingX + stepX * index;
    const normalized = (row.value - paddedMin) / scaledRange;
    const y = paddingTop + usableHeight - usableHeight * normalized;
    return { ...row, x, y };
  });
}

export default function App() {
  const [currentCategory, setCurrentCategory] = useState(DEFAULT_ITEM_CATEGORY);
  const [selectedDate, setSelectedDate] = useState(getToday);
  const [searchInput, setSearchInput] = useState('');
  const [viewLabel, setViewLabel] = useState(getCategoryMeta(DEFAULT_ITEM_CATEGORY).label);

  const [priceItems, setPriceItems] = useState<PriceItem[]>([]);
  const [isPriceLoading, setIsPriceLoading] = useState(true);
  const [priceError, setPriceError] = useState<string | null>(null);

  const [batchConfig, setBatchConfig] = useState<BatchConfig | null>(null);
  const [batchStatuses, setBatchStatuses] = useState<BatchStatusItem[]>([]);
  const [latestMonthlyRun, setLatestMonthlyRun] = useState<BatchMonthlyRunResponse | null>(null);
  const [monthlyForm, setMonthlyForm] = useState(createInitialMonthlyForm);
  const [batchError, setBatchError] = useState<string | null>(null);
  const [monthlyRunError, setMonthlyRunError] = useState<string | null>(null);
  const [isBatchLoading, setIsBatchLoading] = useState(true);
  const [isMonthlySubmitting, setIsMonthlySubmitting] = useState(false);
  const [isHistoryRefreshing, setIsHistoryRefreshing] = useState(false);
  const [selectedTrendItem, setSelectedTrendItem] = useState<string | null>(null);
  const [selectedTrendSeriesKey, setSelectedTrendSeriesKey] = useState('');
  const [trendItems, setTrendItems] = useState<PriceItem[]>([]);
  const [isTrendLoading, setIsTrendLoading] = useState(false);
  const [trendError, setTrendError] = useState<string | null>(null);
  const [isPriceTableExpanded, setIsPriceTableExpanded] = useState(false);

  const trendCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const trendChartRef = useRef<ChartInstance | null>(null);

  const batchPageHref = `${getBackendOrigin()}/batch.html`;

  function resetTrend() {
    setSelectedTrendItem(null);
    setSelectedTrendSeriesKey('');
    setTrendItems([]);
    setTrendError(null);
  }

  const monthlyHistory = useMemo(
    () => batchStatuses.filter((item) => item.jobName === 'kamisMonthlyPriceJob').slice(0, 5),
    [batchStatuses],
  );

  const chartData = useMemo(() => {
    const grouped = new Map<string, { total: number; count: number; unit: string }>();

    priceItems.forEach((item) => {
      if (!item.price) return;

      const current = grouped.get(item.itemName) ?? { total: 0, count: 0, unit: item.unit };
      current.total += item.price;
      current.count += 1;
      if (!current.unit && item.unit) current.unit = item.unit;
      grouped.set(item.itemName, current);
    });

    const rows = [...grouped.entries()]
      .map(([label, value], index) => ({
        label,
        value: Math.round(value.total / value.count),
        unit: value.unit,
        color: `hsl(${(index * 37 + 120) % 360}, 60%, 55%)`,
      }))
      .sort((left, right) => right.value - left.value)
      .slice(0, 10);

    const max = rows[0]?.value ?? 0;

    return rows.map((row) => ({
      ...row,
      ratio: max > 0 ? row.value / max : 0,
    }));
  }, [priceItems]);

  const lineChart = useMemo(() => {
    if (chartData.length === 0) {
      return null;
    }

    const width = 760;
    const height = 260;
    const paddingX = 40;
    const paddingTop = 20;
    const paddingBottom = 36;
    const points = createScaledPoints(chartData, width, height, paddingX, paddingTop, paddingBottom);
    const baselineY = height - paddingBottom;
    const linePath = buildSmoothPath(points);
    const areaPath = buildAreaPath(points, baselineY);

    return { width, height, paddingBottom, points, linePath, areaPath };
  }, [chartData]);

  const visiblePriceItems = useMemo(
    () => (isPriceTableExpanded ? priceItems : priceItems.slice(0, 5)),
    [isPriceTableExpanded, priceItems],
  );

  const priceCards = useMemo(() => {
    const seen = new Set<string>();
    return priceItems.filter((item) => {
      if (seen.has(item.itemName)) return false;
      seen.add(item.itemName);
      return true;
    }).slice(0, 12);
  }, [priceItems]);

  const trendSeriesOptions = useMemo<TrendSeriesOption[]>(() => {
    const grouped = new Map<string, TrendSeriesOption>();

    trendItems.forEach((item) => {
      const key = [item.marketCode, item.kindCode, item.rankCode, item.unit].join('|');
      const label = [item.marketName, item.kindName, item.rankName, item.unit].filter(Boolean).join(' · ');
      const current = grouped.get(key) ?? { key, label: label || item.itemName, items: [] };
      current.items.push(item);
      grouped.set(key, current);
    });

    return [...grouped.values()].sort((left, right) => left.label.localeCompare(right.label, 'ko-KR'));
  }, [trendItems]);

  const selectedTrendSeries = useMemo(() => {
    if (trendSeriesOptions.length === 0) return null;
    return trendSeriesOptions.find((series) => series.key === selectedTrendSeriesKey) ?? trendSeriesOptions[0];
  }, [selectedTrendSeriesKey, trendSeriesOptions]);

  const dailyTrend = useMemo(() => {
    if (selectedTrendSeries == null) return null;

    const grouped = new Map<string, { total: number; count: number }>();

    selectedTrendSeries.items.forEach((item) => {
      if (!item.price || !item.regDay) return;
      const current = grouped.get(item.regDay) ?? { total: 0, count: 0 };
      current.total += item.price;
      current.count += 1;
      grouped.set(item.regDay, current);
    });

    const rows = [...grouped.entries()]
      .map(([regDay, value]) => ({
        regDay,
        value: Math.round(value.total / value.count),
      }))
      .sort((left, right) => left.regDay.localeCompare(right.regDay));

    return rows.length === 0 ? null : rows;
  }, [selectedTrendSeries]);

  async function loadPrices(
    task: Promise<{ data: PriceItem[] }>,
    label: string,
    options?: { categoryCode?: string },
  ) {
    setIsPriceLoading(true);
    setPriceError(null);

    try {
      const response = await task;
      const nextItems = options?.categoryCode ? filterByCategory(response.data, options.categoryCode) : response.data;
      setPriceItems(nextItems);
      setViewLabel(label);
      return nextItems;
    } catch (error) {
      setPriceError(error instanceof Error ? error.message : '가격 데이터를 불러오지 못했습니다.');
      setPriceItems([]);
      setViewLabel(label);
      return [];
    } finally {
      setIsPriceLoading(false);
    }
  }

  async function loadLatest(categoryCode = currentCategory) {
    const category = getCategoryMeta(categoryCode);
    const isCategoryChanged = categoryCode !== currentCategory;
    setCurrentCategory(categoryCode);
    if (isCategoryChanged) {
      resetTrend();
    }
    const nextItems = await loadPrices(fetchLatestPrices(500), category.label, { categoryCode });
    if (nextItems[0]?.regDay) {
      setSelectedDate(nextItems[0].regDay);
    }
  }

  async function loadByDate() {
    if (!selectedDate) {
      await loadLatest();
      return;
    }

    const category = getCategoryMeta(currentCategory);
    await loadPrices(fetchPricesByDate(selectedDate), `${category.label} · ${selectedDate}`, {
      categoryCode: currentCategory,
    });
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const keyword = searchInput.trim();
    if (!keyword) return;
    await loadPrices(searchPrices(keyword), `"${keyword}" 검색 결과`);
  }

  async function refreshBatchOverview() {
    setIsHistoryRefreshing(true);
    setBatchError(null);

    try {
      const [config, statuses] = await Promise.all([fetchBatchConfig(), fetchBatchStatuses(10)]);
      setBatchConfig(config);
      setBatchStatuses(statuses.data);
    } catch (error) {
      setBatchError(error instanceof Error ? error.message : '배치 상태를 불러오지 못했습니다.');
    } finally {
      setIsBatchLoading(false);
      setIsHistoryRefreshing(false);
    }
  }

  useEffect(() => {
    void Promise.all([loadLatest(DEFAULT_ITEM_CATEGORY), refreshBatchOverview()]);
  }, []);

  async function loadItemTrend(itemName: string, itemMarketCode?: string) {
    setSelectedTrendItem(itemName);
    setSelectedTrendSeriesKey('');
    setIsTrendLoading(true);
    setTrendError(null);

    try {
      const response = await searchPrices(itemName);
      const exactItems = response.data.filter(
        (item) => item.itemName === itemName && (itemMarketCode == null || item.marketCode === itemMarketCode),
      );
      setTrendItems(exactItems);
    } catch (error) {
      setTrendError(error instanceof Error ? error.message : '추이 데이터를 불러오지 못했습니다.');
      setTrendItems([]);
    } finally {
      setIsTrendLoading(false);
    }
  }

  async function handleMonthlySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMonthlyRunError(null);
    setIsMonthlySubmitting(true);

    try {
      const result = await runMonthlyBatch(monthlyForm);
      setLatestMonthlyRun(result);
      await refreshBatchOverview();
    } catch (error) {
      setMonthlyRunError(error instanceof Error ? error.message : '월별 배치 실행에 실패했습니다.');
    } finally {
      setIsMonthlySubmitting(false);
    }
  }

  useEffect(() => {
    const canvas = trendCanvasRef.current;
    const Chart = window.Chart;

    if (trendChartRef.current != null) {
      trendChartRef.current.destroy();
      trendChartRef.current = null;
    }

    if (canvas == null || dailyTrend == null || selectedTrendSeries == null) {
      return;
    }

    if (Chart == null) {
      setTrendError('Chart.js를 불러오지 못했습니다.');
      return;
    }

    const context = canvas.getContext('2d');
    if (context == null) {
      setTrendError('차트를 렌더링할 수 없습니다.');
      return;
    }

    trendChartRef.current = new Chart(context, {
      type: 'line',
      data: {
        labels: dailyTrend.map((row) => row.regDay),
        datasets: [
          {
            label: selectedTrendSeries.label,
            data: dailyTrend.map((row) => row.value),
            borderColor: '#2e7d32',
            backgroundColor: 'rgba(46, 125, 50, 0.14)',
            fill: true,
            tension: 0.35,
            pointBackgroundColor: '#42a5f5',
            pointBorderColor: '#ffffff',
            pointBorderWidth: 2,
            pointRadius: 5,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          intersect: false,
          mode: 'index',
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
          },
          tooltip: {
            callbacks: {
              label: (context) => `${context.dataset.label}: ${formatPrice(context.parsed.y)}`,
            },
          },
        },
        scales: {
          x: {
            title: {
              display: true,
              text: '날짜',
            },
          },
          y: {
            beginAtZero: false,
            title: {
              display: true,
              text: '가격',
            },
            ticks: {
              callback: (value) => formatPrice(Number(value)),
            },
          },
        },
      },
    });

    return () => {
      if (trendChartRef.current != null) {
        trendChartRef.current.destroy();
        trendChartRef.current = null;
      }
    };
  }, [dailyTrend, selectedTrendSeries]);

  function updateMonthlyField<Key extends keyof MonthlyRunForm>(key: Key, value: MonthlyRunForm[Key]) {
    setMonthlyForm((current) => ({
      ...current,
      [key]: value,
    }));
  }

  function syncMonthlyCategory() {
    setMonthlyForm((current) => ({
      ...current,
      itemCategoryCode: currentCategory,
    }));
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <div className={styles.logo}>🌾</div>
          <div>
            <h1>KAMIS 농산물 가격 정보</h1>
            <p>농산물유통정보 실시간 가격 조회</p>
          </div>
        </div>
        <nav className={styles.headerNav}>
          <a href={batchPageHref}>⚙️ 배치 관리</a>
        </nav>
      </header>

      <section className={styles.hero}>
        <h2>오늘의 농산물 가격은?</h2>
        <p>품목명을 검색하거나 카테고리를 선택해 가격 정보를 확인하세요</p>
        <form className={styles.searchBar} onSubmit={handleSearch}>
          <input
            aria-label="품목명 검색"
            placeholder="예: 배추, 사과, 쌀..."
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
          <button type="submit">🔍 검색</button>
        </form>
      </section>

      <main className={styles.main}>
        <div className={styles.sectionTitle}>📂 카테고리별 가격 조회</div>
        <div className={styles.categoryTabs}>
          {ITEM_CATEGORIES.map((category) => (
            <button
              className={`${styles.categoryTab} ${currentCategory === category.value ? styles.categoryTabActive : ''}`}
              key={category.value}
              onClick={() => void loadLatest(category.value)}
              type="button"
            >
              <span>{category.emoji}</span>
              {category.label}
            </button>
          ))}
        </div>

        <div className={styles.filterRow}>
          <label htmlFor="main-date">조회 날짜</label>
          <input
            id="main-date"
            type="date"
            value={selectedDate}
            onChange={(event) => setSelectedDate(event.target.value)}
          />
          <button className={styles.btnGreen} onClick={() => void loadByDate()} type="button">
            조회
          </button>
          <button className={styles.btnOutline} onClick={() => void loadLatest()} type="button">
            최신 데이터
          </button>
        </div>

        <div className={styles.sectionTitle}>🗓️ 월별 가격 수집</div>
        <section className={styles.gridTwo}>
          <article className={styles.card}>
            <div className={styles.cardHead}>
              <h3>월별 배치 실행</h3>
              <span className={styles.meta}>POST /api/batch/run-monthly</span>
            </div>
            <div className={styles.cardBody}>
              <p className={styles.hint}>
                기존 메인 화면에서 바로 월별 데이터를 수집합니다. 연/월은 <strong>year / month</strong> 형식으로
                전송됩니다.
              </p>
              <form className={styles.monthlyForm} onSubmit={handleMonthlySubmit}>
                <div className={styles.field}>
                  <label htmlFor="monthly-category">카테고리</label>
                  <select
                    id="monthly-category"
                    value={monthlyForm.itemCategoryCode}
                    onChange={(event) => updateMonthlyField('itemCategoryCode', event.target.value)}
                  >
                    {ITEM_CATEGORIES.map((category) => (
                      <option key={category.value} value={category.value}>
                        {category.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.field}>
                  <label htmlFor="monthly-year">연도</label>
                  <input
                    id="monthly-year"
                    inputMode="numeric"
                    maxLength={4}
                    placeholder="2024"
                    value={monthlyForm.year}
                    onChange={(event) => updateMonthlyField('year', event.target.value.replace(/\D/g, '').slice(0, 4))}
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="monthly-month">월</label>
                  <input
                    id="monthly-month"
                    inputMode="numeric"
                    maxLength={2}
                    placeholder="01"
                    value={monthlyForm.month}
                    onChange={(event) =>
                      updateMonthlyField('month', event.target.value.replace(/\D/g, '').slice(0, 2))
                    }
                  />
                </div>
                <button className={styles.btnGreen} disabled={isMonthlySubmitting} type="submit">
                  {isMonthlySubmitting ? '실행 중...' : '월별 실행'}
                </button>
                <button className={styles.btnOutline} onClick={syncMonthlyCategory} type="button">
                  현재 탭 반영
                </button>
              </form>

              <div className={styles.resultBox}>
                {monthlyRunError ? (
                  <div className={styles.alertError}>월별 배치 실행 실패: {monthlyRunError}</div>
                ) : latestMonthlyRun ? (
                  <>
                    <strong>{latestMonthlyRun.status}</strong>
                    실행 ID {latestMonthlyRun.jobExecutionId} · 카테고리 {latestMonthlyRun.itemCategoryCode} · 대상{' '}
                    {latestMonthlyRun.year}-{latestMonthlyRun.month}
                    <div className={styles.metaLine}>
                      시작 {formatDateTime(latestMonthlyRun.startTime)} · 종료 {formatDateTime(latestMonthlyRun.endTime)}
                    </div>
                  </>
                ) : (
                  <>
                    <strong>대기 중</strong>
                    월별 배치를 실행하면 여기에서 성공/실패, 실행 ID, 대상 월을 확인할 수 있습니다.
                  </>
                )}
              </div>
            </div>
          </article>

          <article className={styles.card}>
            <div className={styles.cardHead}>
              <h3>월별 배치 상태</h3>
              <span className={styles.meta}>GET /api/batch/config · GET /api/batch/status</span>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.batchSummary}>
                <div className={styles.summaryPill}>
                  <span className={styles.summaryLabel}>API 설정</span>
                  <span className={styles.summaryValue}>
                    {batchConfig == null
                      ? '확인 중...'
                      : batchConfig.mockMode
                        ? 'Mock 모드'
                        : batchConfig.apiConfigured
                          ? '준비 완료'
                          : '설정 필요'}
                  </span>
                </div>
                <div className={styles.summaryPill}>
                  <span className={styles.summaryLabel}>최근 월별 실행 수</span>
                  <span className={styles.summaryValue}>{isBatchLoading ? '확인 중...' : `${monthlyHistory.length}건`}</span>
                </div>
              </div>

              <div className={styles.batchActionRow}>
                <button className={styles.btnOutline} onClick={() => void refreshBatchOverview()} type="button">
                  {isHistoryRefreshing ? '새로고침 중...' : '이력 새로고침'}
                </button>
              </div>

              {batchError ? <div className={styles.alertError}>월별 상태 조회 실패: {batchError}</div> : null}

              <div className={styles.batchList}>
                {isBatchLoading ? (
                  <div className={styles.loadingState}>이력 불러오는 중...</div>
                ) : monthlyHistory.length === 0 ? (
                  <div className={styles.emptyState}>최근 월별 실행 이력이 없습니다.</div>
                ) : (
                  monthlyHistory.map((item) => {
                    const params = item.params ?? {};
                    const year = params.year ?? params.yyyy ?? '-';
                    const month = params.month ?? params.mm ?? '-';

                    return (
                      <article className={styles.batchItem} key={item.jobExecutionId}>
                        <div className={styles.batchItemHead}>
                          <div className={styles.batchItemTitle}>실행 #{item.jobExecutionId}</div>
                          <span className={`${styles.statusChip} ${styles[getStatusTone(item.status)]}`}>{item.status}</span>
                        </div>
                        <div className={styles.batchItemMeta}>
                          카테고리{' '}
                          {params.itemCategoryCode
                            ? getCategoryMeta(params.itemCategoryCode).label
                            : '-'}{' '}
                          · 대상 {year}-{month}
                        </div>
                        <div className={styles.batchItemMeta}>
                          시작 {formatDateTime(item.startTime)} · 종료 {formatDateTime(item.endTime)}
                        </div>
                      </article>
                    );
                  })
                )}
              </div>
            </div>
          </article>
        </section>

        <section className={styles.gridTwo}>
          <article className={styles.card}>
            <div className={styles.cardHead}>
              <h3>{viewLabel} 가격 비교 차트</h3>
              <span className={styles.meta}>{priceItems.length}건</span>
            </div>
            <div className={styles.cardBody}>
              {isPriceLoading ? (
                <div className={styles.loadingState}>데이터 불러오는 중...</div>
              ) : priceError ? (
                <div className={styles.alertError}>오류: {priceError}</div>
              ) : chartData.length === 0 ? (
                <div className={styles.emptyState}>
                  먼저 데이터를 수집해주세요.
                  <small>배치를 실행하면 데이터가 저장됩니다.</small>
                </div>
              ) : (
                <div className={styles.chartWrap}>
                  {lineChart ? (
                    <>
                      <svg
                        aria-label="가격 비교 꺾은선 차트"
                        className={styles.lineChartSvg}
                        viewBox={`0 0 ${lineChart.width} ${lineChart.height}`}
                      >
                        <defs>
                          <linearGradient id="price-line-gradient" x1="0%" x2="100%" y1="0%" y2="0%">
                            {lineChart.points.map((point) => (
                              <stop
                                key={point.label}
                                offset={`${((point.x - 40) / (lineChart.width - 80)) * 100}%`}
                                stopColor={point.color}
                              />
                            ))}
                          </linearGradient>
                        </defs>
                        <line
                          className={styles.lineAxis}
                          x1="40"
                          x2={String(lineChart.width - 40)}
                          y1={String(lineChart.height - lineChart.paddingBottom)}
                          y2={String(lineChart.height - lineChart.paddingBottom)}
                        />
                        <path className={styles.lineArea} d={lineChart.areaPath} />
                        <path className={styles.linePath} d={lineChart.linePath} />
                        {lineChart.points.map((point) => (
                          <g
                            className={styles.clickablePoint}
                            key={point.label}
                            onClick={() => void loadItemTrend(point.label)}
                          >
                            <circle
                              className={styles.linePoint}
                              cx={point.x}
                              cy={point.y}
                              r="6"
                              style={{ fill: point.color }}
                            />
                            <text className={styles.lineValue} fill={point.color} x={point.x} y={point.y - 12}>
                              {formatPrice(point.value)}
                            </text>
                            <text
                              className={styles.lineLabel}
                              fill={point.color}
                              x={point.x}
                              y={lineChart.height - 12}
                            >
                              {point.label}
                            </text>
                          </g>
                        ))}
                      </svg>
                      <div className={styles.lineLegend}>
                        {lineChart.points.map((point) => (
                          <button
                            className={styles.lineLegendItem}
                            key={point.label}
                            onClick={() => void loadItemTrend(point.label)}
                            type="button"
                          >
                            <span className={styles.lineLegendDot} style={{ backgroundColor: point.color }} />
                            <div>
                              <strong>{point.label}</strong>
                              <p>{formatPrice(point.value)}</p>
                            </div>
                          </button>
                        ))}
                      </div>
                    </>
                  ) : null}
                </div>
              )}
            </div>
          </article>

          <article className={styles.card}>
            <div className={styles.cardHead}>
              <h3>품목 가격 카드</h3>
              <span className={styles.meta}>{priceItems.length}건</span>
            </div>
            <div className={styles.cardBody}>
              {isPriceLoading ? (
                <div className={styles.loadingState}>데이터 불러오는 중...</div>
              ) : priceError ? (
                <div className={styles.alertError}>오류: {priceError}</div>
              ) : priceCards.length === 0 ? (
                <div className={styles.emptyState}>데이터 없음</div>
              ) : (
                <div className={styles.priceGrid}>
                  {priceCards.map((item) => (
                    <button
                      className={styles.priceCard}
                      key={`${item.itemName}-${item.id}`}
                      onClick={() => void loadItemTrend(item.itemName, item.marketCode)}
                      type="button"
                    >
                      <div className={styles.itemName}>{item.itemName}</div>
                      <div className={styles.itemKind}>{item.kindName || '-'}</div>
                      <div className={styles.itemPrice}>{formatPrice(item.price)}</div>
                      <div className={styles.itemUnit}>{item.unit || '-'}</div>
                      <div className={styles.itemMarket}>📍 {item.marketName || '-'}</div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </article>
        </section>

        <article className={styles.card}>
          <div className={styles.cardHead}>
            <h3>{selectedTrendItem ? `${selectedTrendItem} 날짜별 추이` : '품목 날짜별 추이'}</h3>
            <span className={styles.meta}>
              {selectedTrendItem
                ? 'Chart.js · /api/prices/search 결과'
                : '품목을 선택해 주세요'}
            </span>
          </div>
          <div className={styles.cardBody}>
            {selectedTrendItem == null ? (
              <div className={styles.emptyState}>차트 포인트나 가격 카드를 클릭하면 날짜별 변화를 확인할 수 있습니다.</div>
            ) : isTrendLoading ? (
              <div className={styles.loadingState}>날짜별 추이 불러오는 중...</div>
            ) : trendError ? (
              <div className={styles.alertError}>오류: {trendError}</div>
            ) : dailyTrend == null || selectedTrendSeries == null ? (
              <div className={styles.emptyState}>선택한 품목의 날짜별 추이 데이터가 없습니다.</div>
            ) : (
              <div className={styles.chartWrap}>
                <div className={styles.trendToolbar}>
                  <label htmlFor="trend-series">가격 계열</label>
                  <select
                    id="trend-series"
                    value={selectedTrendSeries.key}
                    onChange={(event) => setSelectedTrendSeriesKey(event.target.value)}
                  >
                    {trendSeriesOptions.map((series) => (
                      <option key={series.key} value={series.key}>
                        {series.label} · {series.items.length}건
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.chartCanvasWrap}>
                  <canvas
                    ref={trendCanvasRef}
                    aria-label={`${selectedTrendItem} 날짜별 가격 변동 차트`}
                    className={styles.chartCanvas}
                  />
                </div>
              </div>
            )}
          </div>
        </article>

        <article className={styles.card}>
          <div className={styles.cardHead}>
            <h3>{viewLabel} 가격 목록</h3>
            <span className={styles.meta}>총 {priceItems.length}건</span>
          </div>
          <div className={styles.tableCardBody}>
            {isPriceLoading ? (
              <div className={styles.loadingState}>불러오는 중...</div>
            ) : priceError ? (
              <div className={styles.alertError}>오류: {priceError}</div>
            ) : priceItems.length === 0 ? (
              <div className={styles.emptyState}>데이터가 없습니다.</div>
            ) : (
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>품목</th>
                      <th>품종</th>
                      <th>등급</th>
                      <th>시장</th>
                      <th>가격</th>
                      <th>단위</th>
                      <th>날짜</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visiblePriceItems.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.itemName || '-'}</strong>
                        </td>
                        <td>{item.kindName || '-'}</td>
                        <td>
                          <span className={`${styles.rankBadge} ${styles[getRankTone(item.rankName)]}`}>
                            {item.rankName || '-'}
                          </span>
                        </td>
                        <td>{item.marketName || '-'}</td>
                        <td className={styles.priceValue}>{formatPrice(item.price)}</td>
                        <td>{item.unit || '-'}</td>
                        <td>{item.regDay || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {priceItems.length > 5 ? (
                  <div className={styles.tableFooter}>
                    <button
                      className={styles.btnOutline}
                      onClick={() => setIsPriceTableExpanded((current) => !current)}
                      type="button"
                    >
                      {isPriceTableExpanded ? '접기' : `더보기 (${priceItems.length - visiblePriceItems.length}건)`}
                    </button>
                  </div>
                ) : null}
              </div>
            )}
          </div>
        </article>
      </main>
    </div>
  );
}

const styles = {
  page: 'page',
  header: 'header',
  headerLeft: 'headerLeft',
  logo: 'logo',
  headerNav: 'headerNav',
  hero: 'hero',
  searchBar: 'searchBar',
  main: 'main',
  sectionTitle: 'sectionTitle',
  categoryTabs: 'categoryTabs',
  categoryTab: 'categoryTab',
  categoryTabActive: 'categoryTabActive',
  filterRow: 'filterRow',
  btnGreen: 'btnGreen',
  btnOutline: 'btnOutline',
  gridTwo: 'gridTwo',
  card: 'card',
  cardHead: 'cardHead',
  cardBody: 'cardBody',
  meta: 'meta',
  monthlyForm: 'monthlyForm',
  field: 'field',
  hint: 'hint',
  resultBox: 'resultBox',
  metaLine: 'metaLine',
  batchSummary: 'batchSummary',
  summaryPill: 'summaryPill',
  summaryLabel: 'summaryLabel',
  summaryValue: 'summaryValue',
  batchActionRow: 'batchActionRow',
  batchList: 'batchList',
  batchItem: 'batchItem',
  batchItemHead: 'batchItemHead',
  batchItemTitle: 'batchItemTitle',
  batchItemMeta: 'batchItemMeta',
  statusChip: 'statusChip',
  statusSuccess: 'statusSuccess',
  statusFailed: 'statusFailed',
  statusRunning: 'statusRunning',
  statusIdle: 'statusIdle',
  chartWrap: 'chartWrap',
  lineChartSvg: 'lineChartSvg',
  lineAxis: 'lineAxis',
  lineArea: 'lineArea',
  linePath: 'linePath',
  linePoint: 'linePoint',
  lineValue: 'lineValue',
  lineLabel: 'lineLabel',
  lineLegend: 'lineLegend',
  lineLegendItem: 'lineLegendItem',
  lineLegendDot: 'lineLegendDot',
  clickablePoint: 'clickablePoint',
  chartCanvasWrap: 'chartCanvasWrap',
  chartCanvas: 'chartCanvas',
  trendToolbar: 'trendToolbar',
  monthlyTrendPath: 'monthlyTrendPath',
  monthlyTrendPoint: 'monthlyTrendPoint',
  monthlyTrendLabel: 'monthlyTrendLabel',
  monthlyTrendLegend: 'monthlyTrendLegend',
  monthlyTrendLegendItem: 'monthlyTrendLegendItem',
  priceGrid: 'priceGrid',
  priceCard: 'priceCard',
  itemName: 'itemName',
  itemKind: 'itemKind',
  itemPrice: 'itemPrice',
  itemUnit: 'itemUnit',
  itemMarket: 'itemMarket',
  tableCardBody: 'tableCardBody',
  tableWrap: 'tableWrap',
  table: 'table',
  tableFooter: 'tableFooter',
  priceValue: 'priceValue',
  rankBadge: 'rankBadge',
  rankGood: 'rankGood',
  rankMid: 'rankMid',
  rankLow: 'rankLow',
  rankDefault: 'rankDefault',
  alertError: 'alertError',
  loadingState: 'loadingState',
  emptyState: 'emptyState',
} as const;
