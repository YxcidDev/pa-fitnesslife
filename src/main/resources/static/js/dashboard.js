Chart.defaults.color = '#d3d3d3';
Chart.defaults.borderColor = 'rgba(255, 255, 255, 0.1)';
Chart.defaults.font.family = "'Work Sans', sans-serif";

const COLORS = {
    primary: '#eb6608',
    primaryLight: 'rgba(235, 102, 8, 0.3)',
    secondary: '#c75006',
    white: '#ffffff',
    gray: '#d3d3d3',
    cardBg: '#323333',
    success: '#28a745',
    danger: '#dc3545'
};

let revenueChart = null;
let planSalesChart = null;
let accessByDayChart = null;
let genderChart = null;

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
});

async function loadDashboardData() {
    try {
        await Promise.all([
            loadMainMetrics(),
            loadMonthlyRevenue(),
            loadRecentTransactions(),
            loadPlanSales(),
            loadAccessByDay(),
            loadGenderDistribution(),
            loadPopularClasses(),
            loadExpiringPlans()
        ]);
    } catch (error) {
        console.error('Error cargando datos del dashboard:', error);
        showError('Error al cargar los datos del dashboard');
    }
}

async function loadMainMetrics() {
    try {
        const response = await fetch('/api/dashboard/metrics');
        const data = await response.json();

        updateMetricCard('activeUsers', data.activeUsers);
        updateMetricCard('accessesToday', data.accessesToday);
        updateMetricCard('revenueThisMonth', data.revenueThisMonth);
        updateMetricCard('activePlans', data.activePlans);
    } catch (error) {
        console.error('Error cargando métricas:', error);
    }
}

function updateMetricCard(metricId, data) {
    const valueEl = document.getElementById(`${metricId}-value`);
    const trendEl = document.getElementById(`${metricId}-trend`);
    const iconEl = document.getElementById(`${metricId}-trend-icon`);

    if (valueEl) {
        if (metricId === 'revenueThisMonth') {
            valueEl.textContent = formatCurrency(data.value);
        } else {
            valueEl.textContent = formatNumber(data.value);
        }
    }

    if (trendEl && iconEl) {
        const growth = data.growth;
        const isPositive = growth >= 0;

        trendEl.textContent = `${isPositive ? '+' : ''}${growth.toFixed(1)}%`;
        trendEl.className = `metric-trend ${isPositive ? 'positive' : 'negative'}`;
        iconEl.className = `bi bi-arrow-${isPositive ? 'up' : 'down'}-short`;
    }
}

async function loadMonthlyRevenue() {
    try {
        const response = await fetch('/api/dashboard/monthly-revenue');
        const data = await response.json();

        const ctx = document.getElementById('revenueChart');
        if (!ctx) return;

        if (revenueChart) {
            revenueChart.destroy();
        }

        revenueChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.map(d => d.month),
                datasets: [{
                    label: 'Ingresos',
                    data: data.map(d => d.revenue),
                    borderColor: COLORS.primary,
                    backgroundColor: COLORS.primaryLight,
                    tension: 0.4,
                    fill: true,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: COLORS.primary,
                    pointBorderColor: COLORS.white,
                    pointBorderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: COLORS.cardBg,
                        titleColor: COLORS.white,
                        bodyColor: COLORS.gray,
                        borderColor: COLORS.primary,
                        borderWidth: 1,
                        padding: 12,
                        displayColors: false,
                        callbacks: {
                            label: (context) => `Ingresos: ${formatCurrency(context.parsed.y)}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => formatCurrency(value, true)
                        },
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error cargando ingresos mensuales:', error);
    }
}

async function loadRecentTransactions() {
    try {
        const response = await fetch('/api/dashboard/recent-transactions');
        const transactions = await response.json();

        const container = document.getElementById('transactions-list');
        if (!container) return;

        if (transactions.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-receipt"></i>
                    <p>No hay transacciones recientes</p>
                </div>
            `;
            return;
        }

        container.innerHTML = transactions.map(t => `
            <div class="transaction-item fade-in">
                <div class="transaction-header">
                    <span class="transaction-user">${t.userName}</span>
                    <span class="transaction-amount">${formatCurrency(t.amount)}</span>
                </div>
                <div class="transaction-details">
                    <span class="transaction-plan">${t.planName}</span>
                    <span class="transaction-date">${formatDate(t.date)}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error cargando transacciones:', error);
    }
}

async function loadPlanSales() {
    try {
        const response = await fetch('/api/dashboard/plan-sales');
        const data = await response.json();

        const ctx = document.getElementById('planSalesChart');
        if (!ctx) return;

        if (planSalesChart) {
            planSalesChart.destroy();
        }

        const colors = [
            '#eb6608',
            '#c75006',
            '#ff8533',
            '#ff9d5c',
            '#ffb685'
        ];

        planSalesChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.map(d => d.planName),
                datasets: [{
                    data: data.map(d => d.count),
                    backgroundColor: colors.slice(0, data.length),
                    borderColor: COLORS.cardBg,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            usePointStyle: true,
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: COLORS.cardBg,
                        titleColor: COLORS.white,
                        bodyColor: COLORS.gray,
                        borderColor: COLORS.primary,
                        borderWidth: 1,
                        padding: 12,
                        callbacks: {
                            label: (context) => {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const percentage = data[context.dataIndex].percentage.toFixed(1);
                                return `${label}: ${value} (${percentage}%)`;
                            }
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error cargando ventas de planes:', error);
    }
}

async function loadAccessByDay() {
    try {
        const response = await fetch('/api/dashboard/access-by-day');
        const data = await response.json();

        const ctx = document.getElementById('accessByDayChart');
        if (!ctx) return;

        if (accessByDayChart) {
            accessByDayChart.destroy();
        }

        accessByDayChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.map(d => d.day),
                datasets: [{
                    label: 'Accesos',
                    data: data.map(d => d.count),
                    backgroundColor: COLORS.primary,
                    borderRadius: 4,
                    borderSkipped: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: COLORS.cardBg,
                        titleColor: COLORS.white,
                        bodyColor: COLORS.gray,
                        borderColor: COLORS.primary,
                        borderWidth: 1,
                        padding: 12,
                        displayColors: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        },
                        grid: {
                            color: 'rgba(255, 255, 255, 0.05)'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error cargando accesos por día:', error);
    }
}

async function loadGenderDistribution() {
    try {
        const response = await fetch('/api/dashboard/gender-distribution');
        const data = await response.json();

        const ctx = document.getElementById('genderChart');
        if (!ctx) return;

        if (genderChart) {
            genderChart.destroy();
        }

        const genderLabels = {
            'MALE': 'Hombres',
            'FEMALE': 'Mujeres',
            'OTHER': 'Otros'
        };

        const genderColors = {
            'MALE': '#eb6608',
            'FEMALE': '#ff8533',
            'OTHER': '#ffb685'
        };

        genderChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.map(d => genderLabels[d.gender] || d.gender),
                datasets: [{
                    data: data.map(d => d.count),
                    backgroundColor: data.map(d => genderColors[d.gender] || COLORS.gray),
                    borderColor: COLORS.cardBg,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            usePointStyle: true,
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: COLORS.cardBg,
                        titleColor: COLORS.white,
                        bodyColor: COLORS.gray,
                        borderColor: COLORS.primary,
                        borderWidth: 1,
                        padding: 12
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error cargando distribución de género:', error);
    }
}

async function loadPopularClasses() {
    try {
        const response = await fetch('/api/dashboard/popular-classes');
        const classes = await response.json();

        const container = document.getElementById('popular-classes-list');
        if (!container) return;

        if (classes.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-calendar-check"></i>
                    <p>No hay clases disponibles</p>
                </div>
            `;
            return;
        }

        container.innerHTML = classes.map((c, index) => {
            const percentage = (c.enrolled / c.capacity * 100).toFixed(0);
            return `
                <div class="transaction-item fade-in" style="animation-delay: ${index * 0.1}s">
                    <div class="transaction-header">
                        <span class="transaction-user">${c.name}</span>
                        <span class="transaction-amount">${c.enrolled}/${c.capacity}</span>
                    </div>
                    <div class="transaction-details">
                        <span class="transaction-plan">
                             ${c.instructor}
                        </span>
                        <span class="transaction-date">${percentage}% lleno</span>
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        console.error('Error cargando clases populares:', error);
    }
}

async function loadExpiringPlans() {
    try {
        const response = await fetch('/api/dashboard/expiring-plans');
        const plans = await response.json();

        const container = document.getElementById('expiring-plans-list');
        if (!container) return;

        if (plans.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-calendar-x"></i>
                    <p>No hay planes por vencer en los próximos 7 días</p>
                </div>
            `;
            return;
        }

        container.innerHTML = plans.map((p, index) => `
            <div class="expiring-item fade-in" style="animation-delay: ${index * 0.1}s">
                <div class="expiring-header">
                    <span class="expiring-user">${p.userName}</span>
                    <span class="expiring-days">
                        ${p.daysRemaining} día${p.daysRemaining !== 1 ? 's' : ''}
                    </span>
                </div>
                <div class="expiring-details">
                    Plan ${p.planName} - Vence ${formatDate(p.expirationDate)}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error cargando planes por vencer:', error);
    }
}

function formatCurrency(value, short = false) {
    if (short && value >= 1000000) {
        return `$${(value / 1000000).toFixed(1)}M`;
    }
    if (short && value >= 1000) {
        return `$${(value / 1000).toFixed(1)}K`;
    }
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    }).format(value);
}

function formatNumber(value) {
    return new Intl.NumberFormat('es-CO').format(value);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const options = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    return date.toLocaleDateString('es-CO', options);
}

function showError(message) {
    console.error(message);
}

function downloadReport() {
    window.print();
}

let peakHoursHeatmap = null;

loadPeakHoursHeatmap()

async function loadPeakHoursHeatmap() {
    try {
        const response = await fetch('/api/dashboard/peak-hours-heatmap');
        const data = await response.json();

        const ctx = document.getElementById('peakHoursHeatmap');
        if (!ctx) return;

        if (peakHoursHeatmap) {
            peakHoursHeatmap.destroy();
        }

        const values = data.map(d => d.v);
        const maxValue = Math.max(...values);
        const minValue = Math.min(...values);

        peakHoursHeatmap = new Chart(ctx, {
            type: 'matrix',
            data: {
                datasets: [{
                    label: 'Accesos',
                    data: data,
                    backgroundColor(context) {
                        const value = context.dataset.data[context.dataIndex].v;
                        const alpha = (value - minValue) / (maxValue - minValue || 1);

                        if (value === 0) {
                            return 'rgba(50, 51, 51, 0.3)';
                        }

                        const r = 235;
                        const g = Math.floor(102 + (alpha * 50));
                        const b = Math.floor(8 + (alpha * 20));

                        return `rgba(${r}, ${g}, ${b}, ${0.3 + (alpha * 0.7)})`;
                    },
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    borderWidth: 1,
                    borderRadius: 2,
                    width: ({ chart }) => (chart.chartArea || {}).width / 7 - 2,
                    height: ({ chart }) => (chart.chartArea || {}).height / 17 - 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: COLORS.cardBg,
                        titleColor: COLORS.white,
                        bodyColor: COLORS.gray,
                        borderColor: COLORS.primary,
                        borderWidth: 1,
                        padding: 12,
                        displayColors: false,
                        callbacks: {
                            title: (context) => {
                                const item = context[0].raw;
                                return `${item.x} - ${item.y}`;
                            },
                            label: (context) => {
                                const value = context.raw.v;
                                return `Accesos: ${value}`;
                            },
                            afterLabel: (context) => {
                                const value = context.raw.v;
                                if (value === 0) return 'Sin actividad';
                                if (value < 5) return 'Baja actividad';
                                if (value < 15) return 'Actividad media';
                                if (value < 30) return 'Alta actividad';
                                return 'Muy alta actividad';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        type: 'category',
                        labels: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'],
                        offset: true,
                        grid: {
                            display: false
                        },
                        ticks: {
                            color: COLORS.gray,
                            font: {
                                size: 12,
                                weight: 600
                            }
                        }
                    },
                    y: {
                        type: 'category',
                        labels: [
                            '06:00', '07:00', '08:00', '09:00', '10:00', '11:00',
                            '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
                            '18:00', '19:00', '20:00', '21:00', '22:00'
                        ],
                        offset: true,
                        grid: {
                            display: false
                        },
                        ticks: {
                            color: COLORS.gray,
                            font: {
                                size: 11
                            }
                        }
                    }
                }
            }
        });

        addHeatmapLegend(maxValue);

    } catch (error) {
        console.error('Error cargando heatmap de horas pico:', error);
    }
}

function addHeatmapLegend(maxValue) {
    const container = document.getElementById('peakHoursHeatmap');
    if (!container) return;

    let legendDiv = container.parentElement.querySelector('.heatmap-legend');

    if (!legendDiv) {
        legendDiv = document.createElement('div');
        legendDiv.className = 'heatmap-legend';
        container.parentElement.appendChild(legendDiv);
    }

    const ranges = [
        { label: 'Sin actividad', color: 'rgba(50, 51, 51, 0.3)' },
        { label: `1-${Math.floor(maxValue * 0.25)}`, color: 'rgba(235, 102, 8, 0.4)' },
        { label: `${Math.floor(maxValue * 0.25)}-${Math.floor(maxValue * 0.5)}`, color: 'rgba(235, 127, 8, 0.6)' },
        { label: `${Math.floor(maxValue * 0.5)}-${Math.floor(maxValue * 0.75)}`, color: 'rgba(235, 152, 28, 0.8)' },
        { label: `${Math.floor(maxValue * 0.75)}+`, color: 'rgba(235, 102, 8, 1)' }
    ];

    legendDiv.innerHTML = `
        <div style="display: flex; gap: 1rem; justify-content: center; align-items: center; margin-top: 1rem; flex-wrap: wrap;">
            ${ranges.map(r => `
                <div style="display: flex; align-items: center; gap: 0.5rem; font-size: 11px; color: var(--text-gray);">
                    <div style="width: 20px; height: 20px; background: ${r.color}; border-radius: 2px; border: 1px solid rgba(255,255,255,0.1);"></div>
                    <span>${r.label}</span>
                </div>
            `).join('')}
        </div>
    `;
}