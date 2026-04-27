const featureCards = [
  {
    title: '일별 가격 조회',
    description: '기존 날짜 기준 가격 조회 화면을 React 구조로 옮길 준비를 합니다.',
    status: '준비됨',
  },
  {
    title: '월별 배치 실행',
    description: 'run-monthly API를 연결할 메인 화면 섹션을 이 위치에 추가합니다.',
    status: '다음 작업',
  },
  {
    title: '실행 이력 / 상태',
    description: '배치 상태와 최근 실행 결과를 메인 대시보드에서 함께 보여줄 예정입니다.',
    status: '예정',
  },
];

export default function App() {
  return (
    <div className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">DRAGONS BATCH</p>
          <h1>KAMIS 가격 조회 프론트엔드</h1>
          <p className="hero-copy">
            root/front 기준으로 React + TypeScript 구조를 먼저 만들었습니다.
            다음 단계에서 메인 화면에 월별 조회/배치 UI를 붙이면 됩니다.
          </p>
        </div>
      </header>

      <main className="content">
        <section className="panel panel-accent">
          <h2>현재 생성된 범위</h2>
          <ul className="check-list">
            <li>Vite 기반 React + TypeScript 엔트리</li>
            <li>front/index.html 진입점</li>
            <li>메인 대시보드용 기본 레이아웃</li>
          </ul>
        </section>

        <section className="feature-grid">
          {featureCards.map((card) => (
            <article className="panel feature-card" key={card.title}>
              <div className="feature-head">
                <h3>{card.title}</h3>
                <span className="badge">{card.status}</span>
              </div>
              <p>{card.description}</p>
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}
