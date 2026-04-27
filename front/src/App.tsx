import { useEffect, useMemo, useState, type FormEvent } from 'react';
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

function createInitialMonthlyForm(): MonthlyRunForm {
  const now = new Date();
  return {
    itemCategoryCode: DEFAULT_ITEM_CATEGORY,
    year: String(now.getFullYear()),
    month: String(now.getMonth() + 1).padStart(2, '0'),
  };
}

function getToday() {
  return new Date().toISOString().slice(0, 10);
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

  const batchPageHref = `${getBackendOrigin()}/batch.html`;

  const monthlyHistory = useMemo(
    () => batchStatuses.filter((item) => item.jobName === 'kamisMonthlyPriceJob').slice(0, 5),
    [batchStatuses],
  );

  const chartData = useMemo(() => {
    const grouped = new Map<string, { total: number; count: number }>();

    priceItems.forEach((item) => {
      if (!item.price) return;

      const current = grouped.get(item.itemName) ?? { total: 0, count: 0 };
      current.total += item.price;
      current.count += 1;
      grouped.set(item.itemName, current);
    });

    const rows = [...grouped.entries()]
      .map(([label, value], index) => ({
        label,
        value: Math.round(value.total / value.count),
        color: `hsl(${(index * 37 + 120) % 360}, 60%, 55%)`,
      }))
      .sort((left, right) => right.value - left.value)
      .slice(0, 10);

    const max = rows[0]?.value ?? 0;

    return rows.map((row) => ({
      ...row,
      ratio: max > 0 ? Math.max((row.value / max) * 100, 10) : 0,
    }));
  }, [priceItems]);

  const priceCards = useMemo(() => {
    const seen = new Set<string>();
    return priceItems.filter((item) => {
      if (seen.has(item.itemName)) return false;
      seen.add(item.itemName);
      return true;
    }).slice(0, 12);
  }, [priceItems]);

  async function loadPrices(task: Promise<{ data: PriceItem[] }>, label: string) {
    setIsPriceLoading(true);
    setPriceError(null);

    try {
      const response = await task;
      setPriceItems(response.data);
      setViewLabel(label);
    } catch (error) {
      setPriceError(error instanceof Error ? error.message : '가격 데이터를 불러오지 못했습니다.');
      setPriceItems([]);
      setViewLabel(label);
    } finally {
      setIsPriceLoading(false);
    }
  }

  async function loadLatest(categoryCode = currentCategory) {
    const category = getCategoryMeta(categoryCode);
    setCurrentCategory(categoryCode);
    await loadPrices(fetchLatestPrices(50), category.label);
  }

  async function loadByDate() {
    if (!selectedDate) {
      await loadLatest();
      return;
    }

    const category = getCategoryMeta(currentCategory);
    await loadPrices(fetchPricesByDate(selectedDate), `${category.label} · ${selectedDate}`);
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
                        {category.value} · {category.label}
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
                      : batchConfig.apiConfigured
                        ? batchConfig.mockMode
                          ? 'Mock 모드'
                          : '준비 완료'
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
                {monthlyHistory.length === 0 ? (
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
                          카테고리 {params.itemCategoryCode ?? '-'} · 대상 {year}-{month}
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
                  {chartData.map((item) => (
                    <div className={styles.chartItem} key={item.label}>
                      <div className={styles.chartValue}>{formatPrice(item.value)}</div>
                      <div className={styles.chartTrack}>
                        <div className={styles.chartBar} style={{ height: `${item.ratio}%`, backgroundColor: item.color }} />
                      </div>
                      <div className={styles.chartLabel}>{item.label}</div>
                    </div>
                  ))}
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
                    <article className={styles.priceCard} key={`${item.itemName}-${item.id}`}>
                      <div className={styles.itemName}>{item.itemName}</div>
                      <div className={styles.itemKind}>{item.kindName || '-'}</div>
                      <div className={styles.itemPrice}>{formatPrice(item.price)}</div>
                      <div className={styles.itemUnit}>{item.unit || '-'}</div>
                      <div className={styles.itemMarket}>📍 {item.marketName || '-'}</div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </article>
        </section>

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
                    {priceItems.map((item) => (
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
  chartItem: 'chartItem',
  chartValue: 'chartValue',
  chartTrack: 'chartTrack',
  chartBar: 'chartBar',
  chartLabel: 'chartLabel',
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
