(function () {
    const dashboard = window.ITASSET_DASHBOARD || {};
    const charts = new Map();
    let pluginTextoCentralRegistrado = false;
    let pluginEtiquetasBarrasRegistrado = false;

    function numero(valor) {
        return new Intl.NumberFormat('es-UY').format(Number(valor || 0));
    }

    function leerColor(varName) {
        return getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
    }

    function obtenerPaleta() {
        return {
            primary: leerColor('--dashboard-color-primary'),
            success: leerColor('--dashboard-color-success'),
            warning: leerColor('--dashboard-color-warning'),
            info: leerColor('--dashboard-color-info'),
            danger: leerColor('--dashboard-color-danger'),
            muted: leerColor('--dashboard-color-muted')
        };
    }

    function colorDesdeClase(clase, paleta) {
        const valor = String(clase || '');
        if (valor.includes('success')) {
            return paleta.success;
        }
        if (valor.includes('warning')) {
            return paleta.warning;
        }
        if (valor.includes('info')) {
            return paleta.info;
        }
        if (valor.includes('danger')) {
            return paleta.danger;
        }
        if (valor.includes('muted')) {
            return paleta.muted;
        }
        return paleta.primary;
    }

    function registrarPluginTextoCentral() {
        if (pluginTextoCentralRegistrado || typeof Chart === 'undefined') {
            return;
        }

        Chart.register({
            id: 'itassetCenterText',
            beforeDraw(chart, _args, pluginOptions) {
                if (!pluginOptions || !pluginOptions.enabled) {
                    return;
                }

                const { ctx, chartArea } = chart;
                if (!chartArea) {
                    return;
                }

                const centroX = (chartArea.left + chartArea.right) / 2;
                const centroY = (chartArea.top + chartArea.bottom) / 2;

                ctx.save();
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillStyle = pluginOptions.color || '#0f172a';
                ctx.font = pluginOptions.valueFont || '800 24px system-ui, sans-serif';
                ctx.fillText(numero(pluginOptions.total), centroX, centroY - 7);
                ctx.fillStyle = pluginOptions.labelColor || '#64748b';
                ctx.font = pluginOptions.labelFont || '700 13px system-ui, sans-serif';
                ctx.fillText(pluginOptions.label || 'Activos', centroX, centroY + 17);
                ctx.restore();
            }
        });

        pluginTextoCentralRegistrado = true;
    }

    function registrarPluginEtiquetasBarras() {
        if (pluginEtiquetasBarrasRegistrado || typeof Chart === 'undefined') {
            return;
        }

        Chart.register({
            id: 'itassetBarValues',
            afterDatasetsDraw(chart, _args, pluginOptions) {
                if (!pluginOptions || !pluginOptions.enabled) {
                    return;
                }

                const { ctx, chartArea } = chart;
                if (!chartArea) {
                    return;
                }

                const horizontal = chart.config.options && chart.config.options.indexAxis === 'y';
                ctx.save();
                ctx.fillStyle = pluginOptions.color || '#334155';
                ctx.font = pluginOptions.font || '700 11px system-ui, sans-serif';
                ctx.textBaseline = 'middle';

                chart.getSortedVisibleDatasetMetas().forEach((meta) => {
                    const dataset = chart.data.datasets[meta.index] || {};
                    meta.data.forEach((element, index) => {
                        const valor = dataset.data ? dataset.data[index] : null;
                        if (valor === null || valor === undefined) {
                            return;
                        }

                        if (horizontal) {
                            const x = Math.min(element.x + 8, chartArea.right - 4);
                            const y = element.y;
                            ctx.textAlign = 'left';
                            ctx.fillText(numero(valor), x, y);
                        } else {
                            const x = element.x;
                            const y = Math.max(element.y - 10, chartArea.top + 10);
                            ctx.textAlign = 'center';
                            ctx.fillText(numero(valor), x, y);
                        }
                    });
                });

                ctx.restore();
            }
        });

        pluginEtiquetasBarrasRegistrado = true;
    }

    function prepararDatos(items) {
        const lista = Array.isArray(items) ? items.filter(Boolean) : [];
        return {
            labels: lista.map((item) => item.nombre || ''),
            values: lista.map((item) => Number(item.cantidad || 0)),
            colors: lista.map((item) => colorDesdeClase(item.colorClase, obtenerPaleta())),
            links: lista.map((item) => item.url || '#'),
            items: lista
        };
    }

    function destruirGrafico(key) {
        const existente = charts.get(key);
        if (existente) {
            existente.destroy();
            charts.delete(key);
        }
    }

    function crearGraficoActivosCategoria() {
        const canvas = document.getElementById('activosCategoriaChart');
        if (!canvas || typeof Chart === 'undefined') {
            return;
        }

        const datos = prepararDatos(dashboard.activosPorCategoria);
        if (!datos.labels.length || datos.values.every((valor) => valor <= 0)) {
            return;
        }

        destruirGrafico('activosCategoriaChart');

        const grafico = new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: datos.labels,
                datasets: [{
                    data: datos.values,
                    backgroundColor: datos.colors,
                    borderColor: datos.colors,
                    borderWidth: 1,
                    borderRadius: 8,
                    barThickness: 18,
                    maxBarThickness: 22
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                layout: {
                    padding: {
                        right: 28
                    }
                },
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: {
                            precision: 0,
                            color: '#64748b'
                        },
                        grid: {
                            color: 'rgba(148, 163, 184, .18)'
                        }
                    },
                    y: {
                        ticks: {
                            color: '#334155',
                            font: {
                                size: 12,
                                weight: '600'
                            }
                        },
                        grid: {
                            display: false
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label(context) {
                                return ` ${numero(context.parsed.x)}`;
                            }
                        }
                    },
                    itassetBarValues: {
                        enabled: true,
                        color: '#334155'
                    }
                }
            }
        });

        charts.set('activosCategoriaChart', grafico);
    }

    function crearGraficoActivosEstado() {
        const canvas = document.getElementById('activosEstadoChart');
        if (!canvas || typeof Chart === 'undefined') {
            return;
        }

        const datos = prepararDatos(dashboard.activosPorEstado);
        const total = datos.values.reduce((suma, valor) => suma + valor, 0);
        if (!datos.labels.length || total <= 0) {
            return;
        }

        destruirGrafico('activosEstadoChart');

        const paleta = obtenerPaleta();
        const grafico = new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: datos.labels,
                datasets: [{
                    data: datos.values,
                    backgroundColor: datos.colors,
                    borderColor: '#ffffff',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '68%',
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label(context) {
                                const porcentaje = total > 0 ? ((context.parsed / total) * 100).toFixed(1) : '0.0';
                                return ` ${context.label}: ${numero(context.parsed)} (${porcentaje}%)`;
                            }
                        }
                    },
                    itassetCenterText: {
                        enabled: true,
                        total,
                        label: 'Activos',
                        color: paleta.primary
                    }
                }
            }
        });

        charts.set('activosEstadoChart', grafico);
    }

    function crearGraficoSolicitudesEstado() {
        const canvas = document.getElementById('solicitudesEstadoChart');
        if (!canvas || typeof Chart === 'undefined') {
            return;
        }

        const datos = prepararDatos(dashboard.solicitudesPorEstado);
        if (!datos.labels.length || datos.values.every((valor) => valor <= 0)) {
            return;
        }

        destruirGrafico('solicitudesEstadoChart');

        const grafico = new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: datos.labels,
                datasets: [{
                    data: datos.values,
                    backgroundColor: datos.colors,
                    borderColor: datos.colors,
                    borderWidth: 1,
                    borderRadius: 8,
                    barThickness: 30,
                    maxBarThickness: 34
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                layout: {
                    padding: {
                        top: 24,
                        right: 10
                    }
                },
                scales: {
                    x: {
                        ticks: {
                            color: '#64748b',
                            maxRotation: 0,
                            minRotation: 0,
                            font: {
                                size: 11,
                                weight: '600'
                            }
                        },
                        grid: {
                            display: false
                        }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            precision: 0,
                            color: '#64748b'
                        },
                        grid: {
                            color: 'rgba(148, 163, 184, .18)'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label(context) {
                                return ` ${numero(context.parsed.y)}`;
                            }
                        }
                    },
                    itassetBarValues: {
                        enabled: true,
                        color: '#334155'
                    }
                }
            }
        });

        charts.set('solicitudesEstadoChart', grafico);
    }

    function inicializarDashboard() {
        if (typeof Chart === 'undefined') {
            return;
        }

        registrarPluginTextoCentral();
        registrarPluginEtiquetasBarras();
        crearGraficoActivosCategoria();
        crearGraficoActivosEstado();
        crearGraficoSolicitudesEstado();
    }

    document.addEventListener('DOMContentLoaded', inicializarDashboard);
})();
