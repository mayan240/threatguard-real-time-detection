const API = 'http://localhost:8080/api';

const DEMO_EVENTS = [
  {
    id: 1,
    observedAt: new Date().toISOString(),
    sourceIp: '185.220.101.42',
    eventType: 'FAILED_LOGIN',
    username: 'root',
    assetName: 'auth-gateway',
    anomalyScore: 86,
    severity: 'CRITICAL',
    alertStatus: 'OPEN',
    rationale: 'Failed authentication, privileged account, suspicious source'
  },
  {
    id: 2,
    observedAt: new Date(Date.now() - 180000).toISOString(),
    sourceIp: '45.142.214.19',
    eventType: 'FILE_ACCESS',
    username: 'admin',
    assetName: 'customer-db',
    anomalyScore: 65,
    severity: 'HIGH',
    alertStatus: 'OPEN',
    rationale: 'Privileged account and suspicious source'
  },
  {
    id: 3,
    observedAt: new Date(Date.now() - 420000).toISOString(),
    sourceIp: '10.0.0.24',
    eventType: 'API_REQUEST',
    username: 'analyst',
    assetName: 'payments-api',
    anomalyScore: 18,
    severity: 'LOW',
    alertStatus: 'TRIAGED',
    rationale: 'Within the normal baseline'
  }
];

const DEMO_METRICS = {
  events: 3,
  critical: 1,
  high: 1,
  openAlerts: 2,
  averageRisk: 38.2
};

const DEMO_MODEL = {
  algorithm: 'Weka RandomForest (8 features)',
  baselineSamples: 12,
  validationSamples: 12,
  precision: 1,
  recall: 1,
  f1Score: 1
};

function createWebsiteEvent(website, isLive) {
  let assetName = website.url;
  try {
    assetName = new URL(website.url).hostname;
  } catch (error) {
    assetName = website.url;
  }
  return {
    id: 'local-website-health',
    observedAt: new Date().toISOString(),
    sourceIp: website.url,
    username: isLive ? 'website-monitor' : 'demo-monitor',
    eventType: website.status === 'HEALTHY' ? 'WEBSITE_HEALTHY' : 'WEBSITE_ANOMALY',
    assetName,
    anomalyScore: website.status === 'HEALTHY' ? 5 : 90,
    severity: website.status === 'HEALTHY' ? 'LOW' : 'CRITICAL',
    alertStatus: website.status === 'HEALTHY' ? 'RESOLVED' : 'OPEN',
    rationale: `${website.message}; response time ${website.responseTimeMs} ms`
  };
}

const state = {
  events: DEMO_EVENTS,
  metrics: DEMO_METRICS,
  model: DEMO_MODEL,
  dataMode: 'DEMO',
  selectedEvent: DEMO_EVENTS[0],
  websiteCheck: null,
  websiteError: '',
  websiteUrl: '',
  search: '',
  severityFilter: 'ALL',
  scenarioMessage: ''
};

const els = {
  root: document.getElementById('root')
};

function formatRiskValue(value) {
  return Number.isFinite(value) ? Number(value).toFixed(1) : '0.0';
}

function getStatusClass(status) {
  const normalized = String(status || '').toLowerCase();
  if (normalized === 'triaged') return 'status triaged';
  if (normalized === 'resolved') return 'status resolved';
  return 'status';
}

function getSeverityClass(severity) {
  const normalized = String(severity || '').toLowerCase();
  return normalized || 'low';
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[character]));
}

function visibleEvents() {
  const query = state.search.toLowerCase();
  return (state.events || []).filter((event) => {
    const searchable = `${event.assetName || ''} ${event.eventType || ''} ${event.username || ''} ${event.sourceIp || ''}`.toLowerCase();
    return (!query || searchable.includes(query))
      && (state.severityFilter === 'ALL' || event.severity === state.severityFilter);
  });
}

function renderMetricCards() {
  const cards = [
    { label: 'Events processed', value: state.metrics.events },
    { label: 'Critical alerts', value: state.metrics.critical, accent: 'red' },
    { label: 'Open alerts', value: state.metrics.openAlerts, accent: 'orange' },
    { label: 'Average risk', value: `${formatRiskValue(state.metrics.averageRisk)}`, accent: 'blue' }
  ];

  return cards.map(({ label, value, accent = '' }) => `
    <article class="card ${accent}">
      <p>${label}</p>
      <strong>${value}</strong>
      <span>dashboard value</span>
    </article>
  `).join('');
}

function renderEventsTable() {
  const events = visibleEvents();
  if (!events.length) {
    return '<tr><td colspan="4" class="empty">No events available</td></tr>';
  }

  return events.map((event) => {
    const isSelected = state.selectedEvent && String(state.selectedEvent.id) === String(event.id);
    return `
      <tr class="${isSelected ? 'selected' : ''}" data-id="${event.id}">
        <td>${new Date(event.observedAt).toLocaleTimeString()}</td>
        <td>${event.assetName || 'unknown'}</td>
        <td>${escapeHtml(event.eventType || 'UNKNOWN')}</td>
        <td>
          <span class="badge ${getSeverityClass(event.severity)}">${event.severity || 'LOW'}</span>
          ${event.anomalyScore ?? 0}
        </td>
      </tr>
    `;
  }).join('');
}

function renderSelectedEvent() {
  const event = state.selectedEvent;

  if (!event) {
    return '<p class="empty">Select an event.</p>';
  }

  return `
    <div class="detailTitle">
      <h2>${escapeHtml(event.eventType ? event.eventType.replaceAll('_', ' ') : 'Unknown event')}</h2>
      <span class="badge ${getSeverityClass(event.severity)}">${event.severity || 'LOW'}</span>
    </div>
    <dl>
      <div>
        <dt>Source IP</dt>
        <dd class="mono">${escapeHtml(event.sourceIp || 'n/a')}</dd>
      </div>
      <div>
        <dt>User</dt>
        <dd>${escapeHtml(event.username || 'n/a')}</dd>
      </div>
      <div>
        <dt>Reason</dt>
        <dd>${escapeHtml(event.rationale || 'No reasoning available')}</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd class="${getStatusClass(event.alertStatus)}">${event.alertStatus || 'OPEN'}</dd>
      </div>
    </dl>
    <div class="risk">
      <span>Risk score</span>
      <b>${event.anomalyScore ?? 0}</b>
      <i><em style="width:${Math.min(100, event.anomalyScore || 0)}%"></em></i>
    </div>
    <div class="triage">
      <button data-action="TRIAGED">Mark triaged</button>
      <button class="resolve" data-action="RESOLVED">Resolve</button>
    </div>
  `;
}

function renderModelPanel() {
  const model = state.model || DEMO_MODEL;
  return `
    <div>
      <p class="eyebrow">WEKA MODEL RESULTS</p>
      <h2>${model.algorithm || 'Weka model'}</h2>
      <p class="modelNote">Trained on ${model.baselineSamples || 0} normal records. Evaluated on ${model.validationSamples || 0} labelled records.</p>
    </div>
    <div class="productItems">
      <span>Precision ${(Number(model.precision || 0) * 100).toFixed(1)}%</span>
      <span>Recall ${(Number(model.recall || 0) * 100).toFixed(1)}%</span>
      <span>F1-score ${(Number(model.f1Score || 0) * 100).toFixed(1)}%</span>
    </div>
  `;
}

function renderWebsiteCheck() {
  const result = state.websiteCheck;
  const resultMarkup = result
    ? `<div class="healthResult ${result.status === 'HEALTHY' ? 'healthy' : 'unhealthy'}">
        <strong>${result.status}</strong>
        <span>HTTP ${result.httpStatus || '—'} · ${result.responseTimeMs} ms · ${result.message}</span>
      </div>`
    : '<p class="healthHint">Enter a public website URL to check its availability and response time.</p>';
  const errorMarkup = state.websiteError ? `<p class="healthError">${state.websiteError}</p>` : '';

  return `
    <section class="healthPanel">
      <div>
        <p class="eyebrow">WEBSITE HEALTH CHECK</p>
        <h2>Check a website</h2>
        <p class="healthHint">Live mode checks the website URL you enter. Demo mode shows current simulator events from the Java API.</p>
      </div>
      <form class="healthForm" id="healthForm">
        <input id="websiteUrl" type="url" value="${state.websiteUrl}" placeholder="https://example.com">
        <button type="submit">Check website</button>
      </form>
      ${resultMarkup}
      ${errorMarkup}
    </section>
  `;
}

function render() {
  els.root.innerHTML = `
    <main>
      <header>
        <div>
          <p class="eyebrow">THREATGUARD / STUDENT MINI PROJECT</p>
          <h1>Real-Time Threat &amp; Anomaly Detection</h1>
          <p class="subhead">Java Spring Boot and explainable security analytics.</p>
        </div>
        <div class="headerActions">
          <div class="modeSwitch" role="group" aria-label="Data source">
            <button class="${state.dataMode === 'DEMO' ? 'active' : ''}" data-mode="DEMO">Demo data</button>
            <button class="${state.dataMode === 'LIVE' ? 'active' : ''}" data-mode="LIVE">Live data</button>
          </div>
          <div class="plan">SOURCE <b>${state.dataMode === 'LIVE' ? 'REAL WEBSITE' : 'SIMULATED EVENTS'}</b></div>
          <div class="live"><i class="${state.dataMode === 'LIVE' && state.websiteCheck ? 'on' : ''}"></i>${state.dataMode === 'LIVE' ? (state.websiteCheck ? 'CHECKED' : 'WAITING FOR URL') : 'DEMO'}</div>
        </div>
      </header>

      <section class="metrics">
        ${renderMetricCards()}
      </section>

      <section class="workspaceBar">
        <div><b>Training dataset included</b><span>7,000 normal events + 3,000 anomaly events</span></div>
        <div class="integration">CSV FILE <code>data/curated_security_events.csv</code></div>
      </section>

      <section class="threatLab panel">
        <div>
          <p class="eyebrow">THREAT LAB</p>
          <h2>Test detection scenarios</h2>
          <p class="labHint">Create safe, synthetic events to demonstrate how the detector identifies common attacks.</p>
        </div>
        <div class="scenarioButtons">
          <button data-attack="bruteforce">Brute force</button>
          <button data-attack="scan">Endpoint scan</button>
          <button data-attack="dos">Application DoS</button>
          <button data-attack="sqli">SQL injection</button>
        </div>
        ${state.scenarioMessage ? `<p class="scenarioMessage">${escapeHtml(state.scenarioMessage)}</p>` : ''}
      </section>

      <section class="grid">
        <article class="panel stream">
          <div class="panelhead">
            <div>
              <p class="eyebrow">LIVE SECURITY EVENTS</p>
              <h2>Detected activity</h2>
            </div>
            <div class="eventFilters">
              <input id="eventSearch" type="search" value="${escapeHtml(state.search)}" placeholder="Search user, IP, asset">
              <select id="severityFilter">
                ${['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((value) =>
                  `<option value="${value}" ${state.severityFilter === value ? 'selected' : ''}>${value === 'ALL' ? 'All risks' : value}</option>`
                ).join('')}
              </select>
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>TIME</th>
                <th>ASSET</th>
                <th>EVENT</th>
                <th>RISK</th>
              </tr>
            </thead>
            <tbody>
              ${renderEventsTable()}
            </tbody>
          </table>
        </article>

        <aside class="panel detail">
          <p class="eyebrow">ALERT EXPLANATION</p>
          ${renderSelectedEvent()}
        </aside>
      </section>

      <section class="product">
        ${renderModelPanel()}
      </section>

      ${renderWebsiteCheck()}

      <section class="datasetPanel">
        <p class="eyebrow">TRAINING DATA USED BY WEKA</p>
        <h2>Dataset features</h2>
        <div class="featureGrid">
          <div class="feature"><code>req_count_60s</code><span>requests in rolling window</span></div>
          <div class="feature"><code>failed_auth_count</code><span>HTTP 401/403 responses</span></div>
          <div class="feature"><code>error_404_count</code><span>endpoint scan signal</span></div>
          <div class="feature"><code>unique_endpoints</code><span>distinct paths</span></div>
          <div class="feature"><code>error_rate_ratio</code><span>4xx + 5xx ratio</span></div>
          <div class="feature"><code>avg_response_time_ms</code><span>processing latency</span></div>
          <div class="feature"><code>avg_payload_bytes</code><span>payload volume</span></div>
          <div class="feature"><code>signature_hit_count</code><span>SQLi/XSS/traversal hits</span></div>
          <div class="feature"><code>threat_class</code><span>Normal or attack class</span></div>
        </div>
        <a class="datasetLink" href="./training-data.html" target="_blank" rel="noopener">View training CSV</a>
      </section>
    </main>
  `;

  attachEventHandlers();
}

function attachEventHandlers() {
  document.querySelectorAll('[data-attack]').forEach((button) => {
    button.addEventListener('click', async () => {
      button.disabled = true;
      state.scenarioMessage = 'Generating synthetic traffic...';
      render();
      try {
        const response = await fetch(`${API}/demo/simulate-attack?type=${encodeURIComponent(button.dataset.attack)}`, { method: 'POST' });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Simulation failed');
        state.scenarioMessage = `${result.type} generated ${result.requestsGenerated} requests (risk ${result.riskScore}).`;
        if (state.dataMode === 'LIVE') await loadDemoData();
        else render();
      } catch (error) {
        state.scenarioMessage = error.message;
        render();
      } finally {
        button.disabled = false;
      }
    });
  });

  const eventSearch = document.getElementById('eventSearch');
  if (eventSearch) {
    eventSearch.addEventListener('input', () => {
      state.search = eventSearch.value;
      render();
      const refreshedSearch = document.getElementById('eventSearch');
      refreshedSearch.focus();
      refreshedSearch.setSelectionRange(state.search.length, state.search.length);
    });
  }
  const severityFilter = document.getElementById('severityFilter');
  if (severityFilter) {
    severityFilter.addEventListener('change', () => {
      state.severityFilter = severityFilter.value;
      render();
    });
  }

  document.querySelectorAll('[data-mode]').forEach((button) => {
    button.addEventListener('click', () => {
      const mode = button.dataset.mode;
      state.dataMode = mode;
      state.websiteCheck = null;
      state.websiteError = '';
      if (mode === 'DEMO') loadDemoData();
      else {
        state.events = [];
        state.metrics = { events: 0, critical: 0, high: 0, openAlerts: 0, averageRisk: 0 };
        state.selectedEvent = null;
        render();
      }
    });
  });

  const healthForm = document.getElementById('healthForm');
  if (healthForm) {
    healthForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const input = document.getElementById('websiteUrl');
      state.websiteUrl = input.value.trim();
      if (!state.websiteUrl) {
        state.websiteError = 'Enter a real website URL to use Live Data.';
        render();
        return;
      }
      state.websiteError = '';
      state.websiteCheck = null;
      render();
      try {
        const response = await fetch(`${API}/website/check`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ url: input.value.trim() })
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'Website check failed.');
        state.websiteCheck = result;
        const websiteEvent = createWebsiteEvent(result, true);
        state.events = [websiteEvent];
        state.selectedEvent = websiteEvent;
        state.metrics = {
          events: 1,
          critical: result.status === 'HEALTHY' ? 0 : 1,
          high: 0,
          openAlerts: result.status === 'HEALTHY' ? 0 : 1,
          averageRisk: websiteEvent.anomalyScore
        };
      } catch (error) {
        state.websiteError = error.message;
      }
      render();
    });
  }

  document.querySelectorAll('tr[data-id]').forEach((row) => {
    row.addEventListener('click', () => {
      const id = row.dataset.id;
      const selected = state.events.find((event) => String(event.id) === id) || null;
      state.selectedEvent = selected;
      render();
    });
  });

  document.querySelectorAll('[data-action]').forEach((button) => {
    button.addEventListener('click', async () => {
      const status = button.dataset.action;
      if (!state.selectedEvent) return;

      if (state.dataMode === 'DEMO') {
        const updatedEvents = state.events.map((event) =>
          event.id === state.selectedEvent.id ? { ...event, alertStatus: status } : event
        );
        state.events = updatedEvents;
        state.selectedEvent = { ...state.selectedEvent, alertStatus: status };
        render();
        return;
      }

      try {
        const response = await fetch(`${API}/alerts/${state.selectedEvent.id}`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status })
        });

        if (!response.ok) {
          throw new Error('Patch failed');
        }

        render();
      } catch (error) {
        console.error('Unable to update alert status', error);
      }
    });
  });
}

async function loadDemoData() {
  try {
    const [eventsResponse, metricsResponse, modelResponse] = await Promise.all([
      fetch(`${API}/events`),
      fetch(`${API}/metrics`),
      fetch(`${API}/ml/metrics`)
    ]);

    if (!eventsResponse.ok || !metricsResponse.ok || !modelResponse.ok) {
      throw new Error('API unavailable');
    }

    state.events = await eventsResponse.json();
    state.metrics = await metricsResponse.json();
    state.model = await modelResponse.json();
    state.selectedEvent = state.events[0] || null;
  } catch (error) {
    state.events = DEMO_EVENTS;
    state.metrics = DEMO_METRICS;
    state.model = DEMO_MODEL;
    state.selectedEvent = state.events[0];
    state.websiteError = 'Java API unavailable. Showing fallback demo events.';
  }

  render();
}

render();
loadDemoData();
setInterval(() => {
  if (state.dataMode === 'DEMO') loadDemoData();
}, 5000);
