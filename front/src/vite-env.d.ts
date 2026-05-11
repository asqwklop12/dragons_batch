/// <reference types="vite/client" />

type ChartTooltipContext = {
  dataset: { label?: string };
  parsed: { y: number };
};

type ChartConfig = {
  type: 'line';
  data: {
    labels: string[];
    datasets: Array<{
      label: string;
      data: number[];
      borderColor: string;
      backgroundColor: string;
      fill: boolean;
      tension: number;
      pointBackgroundColor: string;
      pointBorderColor: string;
      pointBorderWidth: number;
      pointRadius: number;
    }>;
  };
  options: {
    responsive: boolean;
    maintainAspectRatio: boolean;
    interaction: {
      intersect: boolean;
      mode: 'index';
    };
    plugins: {
      legend: {
        display: boolean;
        position: 'top';
      };
      tooltip: {
        callbacks: {
          label: (context: ChartTooltipContext) => string;
        };
      };
    };
    scales: {
      x: {
        title: {
          display: boolean;
          text: string;
        };
      };
      y: {
        beginAtZero: boolean;
        title: {
          display: boolean;
          text: string;
        };
        ticks: {
          callback: (value: string | number) => string;
        };
      };
    };
  };
};

type ChartInstance = {
  destroy: () => void;
};

type ChartConstructor = new (context: CanvasRenderingContext2D, config: ChartConfig) => ChartInstance;

interface Window {
  Chart?: ChartConstructor;
}
