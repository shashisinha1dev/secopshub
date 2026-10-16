import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import {
  Activity, AlertOctagon, AlertTriangle, ArrowRight, ArrowUpRight,
  Bell, BookOpenCheck, Check, CheckCircle2, ChevronDown, ChevronLeft, ChevronRight,
  CircleHelp, Clock3, CloudLightning, FileBarChart2, FileCheck2, FileSearch, Filter,
  Fingerprint, LayoutDashboard, LoaderCircle, LockKeyhole, LogOut, Menu,
  Plus, RefreshCw, Search, Shield, ShieldAlert, ShieldCheck, Siren, SlidersHorizontal,
  Sparkles, Target, TrendingUp, UserRound, X,
} from 'lucide-react';
import {
  ApiError, createIncident, getAlerts, getAuditEvents, getIncidents, getQuarterlyReport,
  getReferences, getRelationships, login, submitAlert, updateSeverity, updateStatus,
} from './api';
import type { Alert, AuditEvent, Incident, PageResult, QuarterlyReport, Relationship, Session } from './types';

type View = 'overview' | 'incidents' | 'alerts' | 'reports';

const severityValues = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const statusValues = ['NEW', 'TRIAGED', 'INVESTIGATING', 'CONTAINMENT', 'RECOVERY', 'RESOLVED', 'CLOSED'];
const fiscalYears = ['FY2026-27', 'FY2025-26', 'FY2024-25'];
const quarters = ['Q1', 'Q2', 'Q3', 'Q4'];

function pretty(value?: string | null) {
  if (!value) return '—';
  return value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}
function shortDate(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(date);
}
function localDateTime() {
  const d = new Date();
  const offset = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - offset).toISOString().slice(0, 19);
}
function allowedNext(status: string) {
  const transitions: Record<string, string[]> = {
    NEW: ['TRIAGED'], TRIAGED: ['INVESTIGATING'], INVESTIGATING: ['CONTAINMENT', 'RESOLVED'],
    CONTAINMENT: ['RECOVERY'], RECOVERY: ['RESOLVED'], RESOLVED: ['CLOSED', 'INVESTIGATING'], CLOSED: [],
  };
  return transitions[status] ?? [];
}
function roleTitle(roles: string[]) {
  if (roles.includes('INCIDENT_MANAGER')) return 'Incident Manager';
  if (roles.includes('SOC_ANALYST')) return 'SOC Analyst';
  if (roles.includes('AUDITOR')) return 'Security Auditor';
  if (roles.includes('SYSTEM_INTEGRATION')) return 'System Integration';
  return roles.map(pretty).join(', ') || 'User';
}

function App() {
  const [session, setSession] = useState<Session | null>(() => {
    try {
      const raw = localStorage.getItem('secopshub.session');
      if (!raw) return null;
      const saved = JSON.parse(raw) as Session;
      if (saved.expiresAt <= Date.now()) { localStorage.removeItem('secopshub.session'); return null; }
      return saved;
    } catch { return null; }
  });
  const [view, setView] = useState<View>('overview');
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [incidentTotal, setIncidentTotal] = useState(0);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [report, setReport] = useState<QuarterlyReport | null>(null);
  const [reportYear, setReportYear] = useState('FY2026-27');
  const [reportQuarter, setReportQuarter] = useState('Q3');
  const [reference, setReference] = useState<{ severities: string[]; statuses: string[]; classificationStatuses: string[]; violationCategories: { code: string; label: string; description: string }[] } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [query, setQuery] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [showAlertCreate, setShowAlertCreate] = useState(false);
  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);
  const [mobileNav, setMobileNav] = useState(false);

  const isManager = !!session?.roles.includes('INCIDENT_MANAGER');
  const canUpdateStatus = !!session?.roles.some((r) => ['SOC_ANALYST', 'INCIDENT_MANAGER'].includes(r));
  const canCreate = !!session?.roles.some((r) => ['SOC_ANALYST', 'INCIDENT_MANAGER'].includes(r));
  const canSubmitAlert = !!session?.roles.some((r) => ['SYSTEM_INTEGRATION', 'SOC_ANALYST', 'INCIDENT_MANAGER'].includes(r));

  const runLoad = useCallback(async (showSpinner = true) => {
    if (!session) return;
    if (showSpinner) setLoading(true);
    setError('');
    try {
      const results = await Promise.allSettled([
        getIncidents(session, { size: 100 }),
        getAlerts(session),
        getQuarterlyReport(session, reportYear, reportQuarter),
        getReferences(session),
      ]);
      const failed = results.filter((r) => r.status === 'rejected') as PromiseRejectedResult[];
      const incidentsResult = results[0];
      if (incidentsResult.status === 'fulfilled') {
        const page = incidentsResult.value as PageResult<Incident>;
        setIncidents(page.content ?? []);
        setIncidentTotal(page.totalElements ?? page.content?.length ?? 0);
      } else {
        throw incidentsResult.reason;
      }
      if (results[1].status === 'fulfilled') setAlerts(results[1].value as Alert[]);
      if (results[2].status === 'fulfilled') setReport(results[2].value as QuarterlyReport);
      if (results[3].status === 'fulfilled') setReference(results[3].value as typeof reference);
      if (failed.length > 0) {
        const reasons = failed.map((r) => r.reason).filter(Boolean);
        const first = reasons[0];
        if (first instanceof ApiError && first.status === 401) {
          localStorage.removeItem('secopshub.session');
          setSession(null);
          setError('Your session has expired. Please sign in again.');
        } else if (view === 'alerts' && results[1].status === 'rejected') {
          setError('Alerts could not be loaded. Check that Alert Ingestion Service is running and your role can view alerts.');
        }
      }
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        localStorage.removeItem('secopshub.session'); setSession(null);
        setError('Your session has expired. Please sign in again.');
      } else setError(e instanceof Error ? e.message : 'Unable to load SecOpsHub data.');
    } finally { if (showSpinner) setLoading(false); }
  }, [session, reportYear, reportQuarter, view]);

  useEffect(() => { if (session) void runLoad(); }, [session, runLoad]);

  useEffect(() => {
    if (!notice) return;
    const t = window.setTimeout(() => setNotice(''), 4200);
    return () => window.clearTimeout(t);
  }, [notice]);

  const visibleIncidents = useMemo(() => incidents.filter((incident) => {
    const search = query.trim().toLowerCase();
    const matchesSearch = !search || [incident.incidentId, incident.title, incident.description, incident.incidentType, incident.natureOfViolation, incident.assignedTo, incident.businessUnit]
      .some((v) => String(v ?? '').toLowerCase().includes(search));
    return matchesSearch && (!severityFilter || incident.severity === severityFilter) && (!statusFilter || incident.status === statusFilter);
  }), [incidents, query, severityFilter, statusFilter]);

  const overviewCounts = useMemo(() => ({
    critical: incidents.filter((i) => i.severity === 'CRITICAL').length,
    high: incidents.filter((i) => i.severity === 'HIGH').length,
    open: incidents.filter((i) => !['RESOLVED', 'CLOSED'].includes(i.status)).length,
    overdue: incidents.filter((i) => !['RESOLVED', 'CLOSED'].includes(i.status) && i.severity === 'CRITICAL').length,
  }), [incidents]);

  async function handleLogin(username: string, password: string) {
    setLoading(true); setError('');
    try {
      const next = await login(username, password);
      localStorage.setItem('secopshub.session', JSON.stringify(next));
      setSession(next); setView('overview'); setNotice(`Signed in as ${roleTitle(next.roles)}.`);
    } catch (e) { setError(e instanceof Error ? e.message : 'Sign in failed.'); }
    finally { setLoading(false); }
  }

  function logout() {
    localStorage.removeItem('secopshub.session');
    setSession(null); setIncidents([]); setAlerts([]); setReport(null); setSelectedIncident(null);
    setError(''); setNotice('You have been signed out.');
  }

  async function createNewIncident(payload: Record<string, unknown>) {
    if (!session) return;
    setLoading(true); setError('');
    try {
      const created = await createIncident(session, payload);
      setShowCreate(false); setNotice(`Incident ${created.incidentId} created successfully.`);
      await runLoad(false); setSelectedIncident(created);
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not create incident.'); }
    finally { setLoading(false); }
  }

  async function createNewAlert(payload: Record<string, unknown>) {
    if (!session) return;
    setLoading(true); setError('');
    try {
      const alert = await submitAlert(session, payload);
      setShowAlertCreate(false); setNotice(`Alert ${alert.externalAlertId} submitted with status ${pretty(alert.status)}.`);
      setView('alerts'); await runLoad(false);
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not submit alert.'); }
    finally { setLoading(false); }
  }

  async function refreshReport(year: string, quarter: string) {
    setReportYear(year); setReportQuarter(quarter);
    if (!session) return;
    setLoading(true); setError('');
    try { setReport(await getQuarterlyReport(session, year, quarter)); }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not load report.'); }
    finally { setLoading(false); }
  }

  if (!session) return <LoginScreen onLogin={handleLogin} loading={loading} error={error} />;

  const navItems: { id: View; label: string; icon: typeof LayoutDashboard; count?: number }[] = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'incidents', label: 'Incident register', icon: ShieldAlert, count: incidentTotal },
    { id: 'alerts', label: 'Alert inbox', icon: Bell, count: alerts.length },
    { id: 'reports', label: 'Quarterly reports', icon: FileBarChart2 },
  ];

  return (
    <div className="app-shell">
      {mobileNav && <button className="mobile-scrim" onClick={() => setMobileNav(false)} aria-label="Close navigation" />}
      <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`}>
        <div className="brand-lockup">
          <div className="brand-mark"><ShieldCheck size={23} strokeWidth={2.4} /></div>
          <div><div className="brand-name">SecOps<span>Hub</span></div><div className="brand-kicker">SECURITY OPERATIONS</div></div>
          <button className="icon-button sidebar-close" onClick={() => setMobileNav(false)} aria-label="Close menu"><X size={18}/></button>
        </div>
        <div className="workspace-pill"><div className="workspace-dot"/><div><strong>Acme Enterprise</strong><small>Security workspace</small></div><ChevronDown size={15}/></div>
        <div className="nav-label">WORKSPACE</div>
        <nav className="main-nav" aria-label="Main navigation">
          {navItems.map((item) => {
            const Icon = item.icon;
            return <button key={item.id} onClick={() => { setView(item.id); setMobileNav(false); setError(''); }} className={`nav-item ${view === item.id ? 'nav-active' : ''}`}>
              <Icon size={18} strokeWidth={1.9}/><span>{item.label}</span>{item.count !== undefined && <small>{item.count}</small>}
            </button>;
          })}
        </nav>
        <div className="sidebar-divider"/>
        <div className="nav-label">OPERATIONS</div>
        <div className="operation-card"><div className="operation-icon"><Activity size={17}/></div><div><strong>Event pipeline</strong><small><span className="pulse-dot"/> Connected locally</small></div></div>
        <div className="sidebar-bottom">
          <div className="help-card"><div className="help-icon"><CircleHelp size={18}/></div><div><strong>Need a hand?</strong><small>View your API docs and guides</small></div><ArrowRight size={15}/></div>
          <div className="user-profile"><div className="avatar">{session.username.slice(0, 1).toUpperCase()}</div><div className="profile-copy"><strong>{session.username}</strong><small>{roleTitle(session.roles)}</small></div><button className="icon-button" aria-label="Sign out" title="Sign out" onClick={logout}><LogOut size={17}/></button></div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="topbar-left"><button className="icon-button mobile-menu" onClick={() => setMobileNav(true)} aria-label="Open navigation"><Menu size={21}/></button><div className="breadcrumb">Workspace <ChevronRight size={14}/> <strong>{navItems.find((n) => n.id === view)?.label}</strong></div></div>
          <div className="topbar-actions"><div className="env-indicator"><span className="pulse-dot"/> Local environment</div><button className="icon-button topbar-bell" aria-label="Notifications"><Bell size={18}/><i/></button><div className="topbar-avatar">{session.username.slice(0, 1).toUpperCase()}</div></div>
        </header>

        <div className="content-wrap">
          {error && <div className="alert-banner error-banner"><AlertTriangle size={18}/><span>{error}</span><button onClick={() => setError('')} aria-label="Dismiss error"><X size={16}/></button></div>}
          {notice && <div className="alert-banner success-banner"><CheckCircle2 size={18}/><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss notice"><X size={16}/></button></div>}
          <div className="page-heading-row">
            <div><div className="eyebrow"><span className="eyebrow-line"/> SECURITY OPERATIONS CENTER</div><h1>{view === 'overview' ? 'Good to see you again.' : view === 'incidents' ? 'Incident register' : view === 'alerts' ? 'Alert inbox' : 'Quarterly intelligence'}</h1><p className="page-subtitle">{view === 'overview' ? 'Here’s what is happening across your security environment.' : view === 'incidents' ? 'Investigate, prioritize, and track security incidents across your organization.' : view === 'alerts' ? 'Monitor incoming signals and follow their incident-processing status.' : 'Understand incident trends, severity distribution, and recurring violations.'}</p></div>
            <div className="heading-actions"><button className="btn btn-secondary" onClick={() => void runLoad()} disabled={loading}><RefreshCw size={16} className={loading ? 'spin' : ''}/> Refresh</button>{canCreate && view !== 'alerts' && view !== 'reports' && <button className="btn btn-primary" onClick={() => setShowCreate(true)}><Plus size={17}/> New incident</button>}{canSubmitAlert && view === 'alerts' && <button className="btn btn-primary" onClick={() => setShowAlertCreate(true)}><Plus size={17}/> Submit alert</button>}</div>
          </div>

          {loading && <div className="loading-line"><span/></div>}
          {view === 'overview' && <OverviewView incidents={incidents} alerts={alerts} counts={overviewCounts} total={incidentTotal} onView={setView} onSelect={setSelectedIncident} />}
          {view === 'incidents' && <IncidentsView incidents={visibleIncidents} total={incidentTotal} query={query} setQuery={setQuery} severityFilter={severityFilter} setSeverityFilter={setSeverityFilter} statusFilter={statusFilter} setStatusFilter={setStatusFilter} onSelect={setSelectedIncident} canCreate={canCreate} onCreate={() => setShowCreate(true)} />}
          {view === 'alerts' && <AlertsView alerts={alerts} canSubmit={canSubmitAlert} onCreate={() => setShowAlertCreate(true)} />}
          {view === 'reports' && <ReportsView report={report} year={reportYear} quarter={reportQuarter} onChange={refreshReport} />}
          <footer className="app-footer"><span><span className="pulse-dot"/> All systems operational</span><span>SecOpsHub Community Edition <b>·</b> Secure by design</span></footer>
        </div>
      </main>

      {showCreate && <CreateIncidentModal onClose={() => setShowCreate(false)} onSubmit={createNewIncident} username={session.username} categories={reference?.violationCategories ?? []} busy={loading}/>}
      {showAlertCreate && <CreateAlertModal onClose={() => setShowAlertCreate(false)} onSubmit={createNewAlert} username={session.username} busy={loading}/>}
      {selectedIncident && <IncidentDetailModal incident={selectedIncident} session={session} canUpdateStatus={canUpdateStatus} canUpdateSeverity={isManager} onClose={() => setSelectedIncident(null)} onUpdated={async (updated) => { setSelectedIncident(updated); setNotice(`Incident ${updated.incidentId} updated.`); await runLoad(false); }} />}
    </div>
  );
}

function LoginScreen({ onLogin, loading, error }: { onLogin: (username: string, password: string) => Promise<void>; loading: boolean; error: string }) {
  const [username, setUsername] = useState('manager@secopshub.local');
  const [password, setPassword] = useState('Manager@123');
  const [showPassword, setShowPassword] = useState(false);
  const [showDemo, setShowDemo] = useState(true);
  function submit(e: FormEvent) { e.preventDefault(); void onLogin(username.trim(), password); }
  const demoUsers = [
    { role: 'Incident Manager', username: 'manager@secopshub.local', password: 'Manager@123', permission: 'Manage severity and incident lifecycle' },
    { role: 'SOC Analyst', username: 'analyst@secopshub.local', password: 'Analyst@123', permission: 'Investigate incidents and submit alerts' },
    { role: 'Security Auditor', username: 'auditor@secopshub.local', password: 'Auditor@123', permission: 'Read incidents and quarterly reports' },
  ];
  return <div className="login-page">
    <section className="login-visual">
      <div className="login-brand"><div className="brand-mark"><ShieldCheck size={23}/></div><div className="brand-name">SecOps<span>Hub</span></div></div>
      <div className="visual-content"><div className="visual-chip"><span className="pulse-dot"/> THREAT RESPONSE PLATFORM</div><h1>Clarity in the<br/><span>chaos of security.</span></h1><p>One command center to triage alerts, investigate incidents, and build a stronger security posture.</p>
        <div className="visual-stats"><div><ShieldAlert size={18}/><strong>Incident-first</strong><small>Response workflow</small></div><div><Activity size={18}/><strong>Event-driven</strong><small>Alert processing</small></div><div><Fingerprint size={18}/><strong>Role-aware</strong><small>Access control</small></div></div>
      </div>
      <div className="visual-foot"><span>SECURITY OPERATIONS PLATFORM</span><span>BUILT FOR THE RESPONSE TEAM</span></div>
      <div className="visual-grid"/>
    </section>
    <section className="login-panel"><div className="login-panel-inner">
      <div className="mobile-login-brand"><div className="brand-mark"><ShieldCheck size={23}/></div><div className="brand-name">SecOps<span>Hub</span></div></div>
      <div className="login-overline">WELCOME BACK</div><h2>Sign in to your workspace</h2><p className="login-desc">Use your SecOpsHub account to securely continue.</p>
      <form className="login-form" onSubmit={submit}>
        <label>Email address</label><div className="input-icon-wrap"><UserRound size={17}/><input type="email" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="you@company.com" required /></div>
        <div className="label-row"><label>Password</label><span className="muted-label">DEMO ENVIRONMENT</span></div><div className="input-icon-wrap"><LockKeyhole size={17}/><input type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required/><button type="button" className="show-password" onClick={() => setShowPassword((v) => !v)}>{showPassword ? 'Hide' : 'Show'}</button></div>
        {error && <div className="login-error"><AlertTriangle size={16}/>{error}</div>}
        <button type="submit" className="btn btn-primary login-submit" disabled={loading}>{loading ? <><LoaderCircle size={17} className="spin"/> Signing in…</> : <>Sign in securely <ArrowRight size={17}/></>}</button>
      </form>
      <button className="demo-toggle" onClick={() => setShowDemo((v) => !v)}><Sparkles size={16}/> Demo credentials <ChevronDown size={15} className={showDemo ? 'chevron-up' : ''}/></button>
      {showDemo && <div className="demo-users">{demoUsers.map((u) => <button key={u.username} className={`demo-user ${username === u.username ? 'demo-user-selected' : ''}`} onClick={() => { setUsername(u.username); setPassword(u.password); }}><div className="demo-avatar">{u.role.split(' ').map((s) => s[0]).join('').slice(0, 2)}</div><div className="demo-user-copy"><strong>{u.role}</strong><small>{u.permission}</small></div><span className="demo-select">{username === u.username ? <Check size={15}/> : <ArrowRight size={14}/>}</span></button>)}</div>}
      <div className="login-security-note"><ShieldCheck size={16}/><span>Protected session <b>·</b> JWT authentication <b>·</b> Role-based permissions</span></div>
      <div className="login-legal">Local portfolio demo. Do not use demo credentials or local signing keys in production.</div>
    </div></section>
  </div>;
}

function OverviewView({ incidents, alerts, counts, total, onView, onSelect }: { incidents: Incident[]; alerts: Alert[]; counts: { critical: number; high: number; open: number; overdue: number }; total: number; onView: (view: View) => void; onSelect: (incident: Incident) => void }) {
  const recent = [...incidents].sort((a, b) => new Date(b.reportedAt ?? b.createdAt ?? 0).getTime() - new Date(a.reportedAt ?? a.createdAt ?? 0).getTime()).slice(0, 5);
  const severityCount = severityValues.map((severity) => ({ severity, count: incidents.filter((i) => i.severity === severity).length }));
  const maxCount = Math.max(1, ...severityCount.map((i) => i.count));
  const inProgressAlerts = alerts.filter((a) => !['INCIDENT_CREATED', 'PROCESSED'].includes(a.status)).length;
  return <>
    <div className="overview-banner"><div className="banner-copy"><div className="banner-kicker"><Sparkles size={14}/> SECURITY POSTURE SNAPSHOT</div><h2>Stay ahead of the next incident.</h2><p>Review high-priority cases, follow alert processing, and keep your response workflow moving.</p><button className="banner-link" onClick={() => onView('incidents')}>Open incident register <ArrowRight size={16}/></button></div><div className="banner-graphic"><div className="radar-ring ring-one"/><div className="radar-ring ring-two"/><div className="radar-ring ring-three"/><div className="radar-center"><ShieldCheck size={35}/></div><span className="radar-point point-one"/><span className="radar-point point-two"/><span className="radar-point point-three"/></div></div>
    <div className="section-heading"><div><h2>Operational overview</h2><p>Key signals from the incident register</p></div><span className="section-meta"><span className="pulse-dot"/> Updated just now</span></div>
    <div className="metric-grid">
      <MetricCard label="Total incidents" value={total} note="Across current register" icon={ShieldAlert} tone="blue" trend={<><ArrowUpRight size={14}/> Register</>}/>
      <MetricCard label="Critical severity" value={counts.critical} note="Require priority attention" icon={AlertOctagon} tone="red" trend={<><Siren size={14}/> P1</>}/>
      <MetricCard label="Open incidents" value={counts.open} note="Not yet resolved or closed" icon={Activity} tone="amber" trend={<><Clock3 size={14}/> Active</>}/>
      <MetricCard label="Alerts processing" value={inProgressAlerts} note="Awaiting incident creation" icon={Bell} tone="violet" trend={<><CloudLightning size={14}/> Queue</>}/>
    </div>
    <div className="overview-panels">
      <section className="panel severity-panel"><div className="panel-heading"><div><h3>Incidents by severity</h3><p>Distribution across loaded incidents</p></div><button className="panel-menu" onClick={() => onView('incidents')} aria-label="View incidents"><ArrowRight size={16}/></button></div><div className="severity-chart">{severityCount.map((item) => <div className="severity-row" key={item.severity}><div className={`severity-dot sev-${item.severity.toLowerCase()}`}/><span className="severity-name">{pretty(item.severity)}</span><div className="severity-track"><span className={`severity-fill sev-bg-${item.severity.toLowerCase()}`} style={{ width: `${(item.count / maxCount) * 100}%` }}/></div><strong>{item.count}</strong></div>)}</div><div className="panel-footnote"><span>Severity reflects assessed business impact.</span><button onClick={() => onView('incidents')}>View all <ArrowRight size={13}/></button></div></section>
      <section className="panel pipeline-panel"><div className="panel-heading"><div><h3>Alert pipeline</h3><p>Asynchronous event processing</p></div><div className="live-pill"><span className="pulse-dot"/> LIVE</div></div><div className="pipeline-stats"><div><small>Received alerts</small><strong>{alerts.length}</strong></div><div><small>Waiting / processing</small><strong>{inProgressAlerts}</strong></div></div><div className="pipeline-line"><div className="pipeline-step done"><span><Check size={13}/></span><div><strong>Alert ingestion</strong><small>Validate &amp; normalize</small></div></div><div className="pipeline-connector"/><div className="pipeline-step active"><span><Activity size={13}/></span><div><strong>Message broker</strong><small>RabbitMQ event queue</small></div></div><div className="pipeline-connector"/><div className="pipeline-step"><span><FileCheck2 size={13}/></span><div><strong>Incident created</strong><small>Persist &amp; acknowledge</small></div></div></div><button className="pipeline-footer" onClick={() => onView('alerts')}>View alert inbox <ArrowRight size={14}/></button></section>
    </div>
    <section className="panel recent-panel"><div className="panel-heading"><div><h3>Recent incidents</h3><p>Latest activity from the incident register</p></div><button className="btn btn-small btn-secondary" onClick={() => onView('incidents')}>View all incidents <ArrowRight size={14}/></button></div><IncidentTable incidents={recent} onSelect={onSelect} compact emptyTitle="Nothing to investigate yet" emptyText="Create an incident or submit a security alert to get started."/></section>
  </>;
}

function MetricCard({ label, value, note, icon: Icon, tone, trend }: { label: string; value: number; note: string; icon: typeof ShieldAlert; tone: string; trend: ReactNode }) {
  return <div className="metric-card"><div className="metric-card-top"><span>{label}</span><div className={`metric-icon metric-${tone}`}><Icon size={18}/></div></div><div className="metric-value">{value.toLocaleString('en-IN')}</div><div className="metric-card-bottom"><span>{note}</span><small className={`metric-trend ${tone}`}>{trend}</small></div></div>;
}

function IncidentsView({ incidents, total, query, setQuery, severityFilter, setSeverityFilter, statusFilter, setStatusFilter, onSelect, canCreate, onCreate }: { incidents: Incident[]; total: number; query: string; setQuery: (s: string) => void; severityFilter: string; setSeverityFilter: (s: string) => void; statusFilter: string; setStatusFilter: (s: string) => void; onSelect: (i: Incident) => void; canCreate: boolean; onCreate: () => void }) {
  return <>
    <div className="register-intro"><div className="register-symbol"><FileSearch size={24}/></div><div><strong>All tracked incidents</strong><p>Search by incident ID, title, violation, team, or assigned analyst.</p></div><div className="register-total"><strong>{total}</strong><small>Total records</small></div></div>
    <section className="panel register-panel"><div className="register-toolbar"><div className="search-box"><Search size={17}/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search incidents, IDs, assets…" aria-label="Search incidents"/></div><div className="filter-set"><div className="filter-select"><Filter size={15}/><select value={severityFilter} onChange={(e) => setSeverityFilter(e.target.value)} aria-label="Filter severity"><option value="">All severities</option>{severityValues.map((v) => <option key={v} value={v}>{pretty(v)}</option>)}</select><ChevronDown size={13}/></div><div className="filter-select"><SlidersHorizontal size={15}/><select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Filter status"><option value="">All statuses</option>{statusValues.map((v) => <option key={v} value={v}>{pretty(v)}</option>)}</select><ChevronDown size={13}/></div></div></div>
      <IncidentTable incidents={incidents} onSelect={onSelect} />
      <div className="table-pagination"><span>Showing <strong>{incidents.length}</strong> of <strong>{total}</strong> loaded records</span><div className="pagination-buttons"><button className="pagination-btn" disabled aria-label="Previous page"><ChevronLeft size={16}/></button><button className="pagination-btn page-current">1</button><button className="pagination-btn" disabled aria-label="Next page"><ChevronRight size={16}/></button></div></div>
    </section>
    {incidents.length === 0 && canCreate && <div className="empty-action"><span>No incidents match these filters.</span><button className="text-action" onClick={onCreate}>Create an incident <ArrowRight size={14}/></button></div>}
  </>;
}

function IncidentTable({ incidents, onSelect, compact = false, emptyTitle = 'No incidents found', emptyText = 'Try changing your filters or create a new incident.' }: { incidents: Incident[]; onSelect: (i: Incident) => void; compact?: boolean; emptyTitle?: string; emptyText?: string }) {
  return <div className={`table-scroll ${compact ? 'table-compact' : ''}`}><table className="data-table"><thead><tr><th>Incident</th><th>Severity</th><th>Status</th><th>Violation nature</th><th>Assigned to</th><th>Reported</th><th/></tr></thead><tbody>
    {incidents.map((incident) => <tr key={incident.incidentId} onClick={() => onSelect(incident)} tabIndex={0} onKeyDown={(e) => { if (e.key === 'Enter') onSelect(incident); }}>
      <td><div className="incident-title-cell"><div className="incident-type-icon"><Shield size={16}/></div><div><strong>{incident.title}</strong><small>{incident.incidentId}</small></div></div></td>
      <td><SeverityBadge value={incident.severity}/></td><td><StatusBadge value={incident.status}/></td><td><span className="violation-label">{pretty(incident.natureOfViolation)}</span></td><td><span className="assigned-cell"><span className="tiny-avatar">{(incident.assignedTo || 'U').slice(0, 1).toUpperCase()}</span>{incident.assignedTo || 'Unassigned'}</span></td><td className="date-cell">{shortDate(incident.reportedAt)}</td><td><button className="row-open" aria-label={`Open ${incident.incidentId}`}><ArrowRight size={15}/></button></td>
    </tr>)}
    {incidents.length === 0 && <tr><td colSpan={7}><div className="empty-state"><div className="empty-state-icon"><FileSearch size={22}/></div><strong>{emptyTitle}</strong><p>{emptyText}</p></div></td></tr>}
  </tbody></table></div>;
}

function SeverityBadge({ value }: { value: string }) {
  return <span className={`severity-badge severity-${value.toLowerCase()}`}><span/>{pretty(value)}</span>;
}
function StatusBadge({ value }: { value: string }) {
  const state = ['RESOLVED', 'CLOSED'].includes(value) ? 'closed' : ['INVESTIGATING', 'CONTAINMENT', 'RECOVERY'].includes(value) ? 'progress' : value === 'TRIAGED' ? 'triaged' : 'new';
  return <span className={`status-badge status-${state}`}><span/>{pretty(value)}</span>;
}

function AlertsView({ alerts, canSubmit, onCreate }: { alerts: Alert[]; canSubmit: boolean; onCreate: () => void }) {
  return <>
    <div className="alert-callout"><div className="alert-callout-icon"><CloudLightning size={21}/></div><div><strong>Event-driven alert ingestion</strong><p>Alerts are accepted by the ingestion service, published to RabbitMQ, and converted into incidents asynchronously.</p></div><div className="alert-callout-status"><span className="pulse-dot"/> Connected</div></div>
    <section className="panel alert-list-panel"><div className="panel-heading"><div><h3>Incoming signals</h3><p>{alerts.length} alert{alerts.length === 1 ? '' : 's'} registered</p></div>{canSubmit && <button className="btn btn-primary btn-small" onClick={onCreate}><Plus size={15}/> Submit alert</button>}</div>
      {alerts.length === 0 ? <div className="empty-state"><div className="empty-state-icon"><Bell size={22}/></div><strong>No alerts have been received</strong><p>Submit a simulated alert to exercise the RabbitMQ event pipeline.</p>{canSubmit && <button className="btn btn-primary" onClick={onCreate}><Plus size={16}/> Submit simulated alert</button>}</div> : <div className="alert-cards">{[...alerts].sort((a,b) => new Date(b.createdAt ?? b.reportedAt ?? 0).getTime() - new Date(a.createdAt ?? a.reportedAt ?? 0).getTime()).map((alert) => <div className="alert-card" key={alert.alertId}><div className={`alert-card-symbol ${alert.severity.toLowerCase()}`}><Siren size={19}/></div><div className="alert-card-main"><div className="alert-card-title"><strong>{alert.title}</strong><SeverityBadge value={alert.severity}/></div><div className="alert-subline"><span>{alert.externalAlertId}</span><span>·</span><span>{alert.source}</span><span>·</span><span>{pretty(alert.incidentType)}</span></div><div className="alert-progress-row"><span className={`alert-status-chip ${alert.status === 'INCIDENT_CREATED' ? 'alert-status-done' : alert.status === 'FAILED' ? 'alert-status-failed' : 'alert-status-waiting'}`}><span/>{pretty(alert.status)}</span>{alert.createdIncidentId && <span className="alert-incident-link"><FileCheck2 size={13}/> {alert.createdIncidentId}</span>}{alert.lastError && <span className="alert-error-text" title={alert.lastError}>{alert.lastError}</span>}</div></div><div className="alert-card-date">{shortDate(alert.reportedAt ?? alert.createdAt)}</div></div>)}</div>}
    </section>
  </>;
}

function ReportsView({ report, year, quarter, onChange }: { report: QuarterlyReport | null; year: string; quarter: string; onChange: (year: string, quarter: string) => void }) {
  const maxSeverity = Math.max(1, ...Object.values(report?.countsBySeverity ?? {}));
  const violations = Object.entries(report?.countsByViolation ?? {}).sort((a,b) => b[1] - a[1]);
  const maxViolation = Math.max(1, ...violations.map(([,v]) => v));
  return <>
    <section className="report-hero"><div><div className="banner-kicker"><FileBarChart2 size={15}/> PERIODIC RISK REVIEW</div><h2>Make incident trends actionable.</h2><p>Compare severity, workflow status, and violation patterns for your selected reporting period.</p></div><div className="report-hero-icon"><TrendingUp size={52}/><div className="hero-orbit orbit-a"/><div className="hero-orbit orbit-b"/></div></section>
    <section className="panel report-controls"><div className="report-control-copy"><strong>Reporting period</strong><small>Use your organization's configured fiscal calendar.</small></div><div className="report-selects"><label>Fiscal year<select value={year} onChange={(e) => onChange(e.target.value, quarter)}>{fiscalYears.map((y) => <option key={y}>{y}</option>)}</select></label><label>Quarter<select value={quarter} onChange={(e) => onChange(year, e.target.value)}>{quarters.map((q) => <option key={q}>{q}</option>)}</select></label></div></section>
    <div className="report-summary-grid"><div className="report-summary"><span>Total incidents</span><strong>{report?.totalIncidents ?? '—'}</strong><small>Reported in {year} · {quarter}</small></div><div className="report-summary"><span>High + critical</span><strong>{(report?.countsBySeverity?.HIGH ?? 0) + (report?.countsBySeverity?.CRITICAL ?? 0)}</strong><small>Priority-severity cases</small></div><div className="report-summary"><span>Closed cases</span><strong>{(report?.countsByStatus?.CLOSED ?? 0) + (report?.countsByStatus?.RESOLVED ?? 0)}</strong><small>Resolved or closed</small></div><div className="report-summary"><span>Violation categories</span><strong>{violations.length}</strong><small>Categories represented</small></div></div>
    <div className="report-panels"><section className="panel report-chart-panel"><div className="panel-heading"><div><h3>Severity distribution</h3><p>Volume by assessed severity</p></div><span className="chart-period">{quarter}</span></div><div className="horizontal-chart">{severityValues.map((key) => { const value = report?.countsBySeverity?.[key] ?? 0; return <div className="horizontal-bar-row" key={key}><span>{pretty(key)}</span><div className="horizontal-track"><div className={`horizontal-fill sev-bg-${key.toLowerCase()}`} style={{ width: `${value / maxSeverity * 100}%` }}/></div><strong>{value}</strong></div>; })}</div><div className="chart-note"><span className="note-dot"/> Based on reported incidents in the selected period</div></section>
      <section className="panel report-chart-panel"><div className="panel-heading"><div><h3>Nature of violations</h3><p>Most frequently reported categories</p></div><BookOpenCheck size={18} className="muted-icon"/></div><div className="horizontal-chart violation-chart">{violations.length ? violations.slice(0, 6).map(([key,value]) => <div className="horizontal-bar-row" key={key}><span>{pretty(key)}</span><div className="horizontal-track"><div className="horizontal-fill violation-fill" style={{ width: `${value / maxViolation * 100}%` }}/></div><strong>{value}</strong></div>) : <div className="chart-empty">No violation data is available for this period.</div>}</div><div className="chart-note"><span className="note-dot violet-dot"/> Use recurring patterns to prioritize corrective actions</div></section></div>
    <section className="panel report-status-panel"><div className="panel-heading"><div><h3>Workflow status</h3><p>Cases by current lifecycle stage</p></div><Target size={18} className="muted-icon"/></div><div className="status-breakdown">{Object.entries(report?.countsByStatus ?? {}).map(([key, value]) => <div className="status-breakdown-item" key={key}><span className={`status-mini-dot status-mini-${['RESOLVED','CLOSED'].includes(key) ? 'green' : ['INVESTIGATING','CONTAINMENT','RECOVERY'].includes(key) ? 'blue' : 'amber'}`}/><span>{pretty(key)}</span><strong>{value}</strong></div>)}</div></section>
    <p className="report-disclaimer"><CircleHelp size={14}/> Reporting counts reflect the backend's fiscal-year and quarter fields. Confirm reporting-date rules with your security policy before using these figures for compliance submissions.</p>
  </>;
}

function ModalShell({ children, title, onClose, width = 'wide' }: { children: ReactNode; title: string; onClose: () => void; width?: 'narrow' | 'wide' }) {
  useEffect(() => { const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); }; window.addEventListener('keydown', onKey); document.body.classList.add('modal-open'); return () => { window.removeEventListener('keydown', onKey); document.body.classList.remove('modal-open'); }; }, [onClose]);
  return <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}><section className={`modal-card modal-${width}`} role="dialog" aria-modal="true" aria-label={title}><div className="modal-heading"><div><span className="modal-kicker">SECOPSHUB WORKSPACE</span><h2>{title}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close modal"><X size={20}/></button></div>{children}</section></div>;
}

function CreateIncidentModal({ onClose, onSubmit, username, categories, busy }: { onClose: () => void; onSubmit: (payload: Record<string, unknown>) => Promise<void>; username: string; categories: { code: string; label: string; description: string }[]; busy: boolean }) {
  const [form, setForm] = useState({ title: '', description: '', incidentType: 'UNAUTHORIZED_ACCESS', natureOfViolation: 'ACCESS_CONTROL', severity: 'HIGH', classificationStatus: 'SUSPECTED', reportedBy: username, assignedTo: '', businessUnit: 'Information Technology', policyReference: '', businessImpact: '', rootCause: '', correctiveAction: '' });
  const [formError, setFormError] = useState('');
  const set = (name: keyof typeof form, value: string) => setForm((f) => ({ ...f, [name]: value }));
  function submit(e: FormEvent) { e.preventDefault(); if (!form.title.trim() || !form.description.trim() || !form.reportedBy.trim()) { setFormError('Title, description, and reported by are required.'); return; } setFormError(''); void onSubmit({ ...form, occurredAt: localDateTime(), detectedAt: localDateTime(), reportedAt: localDateTime() }); }
  return <ModalShell title="Create security incident" onClose={onClose}><form onSubmit={submit} className="modal-form"><div className="form-section-title"><span>01</span><div><strong>Incident classification</strong><small>Define what happened and how urgently to respond.</small></div></div><div className="form-grid"><label className="field-full">Incident title *<input value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="e.g. Suspicious privileged account access" maxLength={180} required/></label><label className="field-full">Description *<textarea value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="Describe the security event and known context…" rows={3} maxLength={3000} required/></label><label>Incident type<input value={form.incidentType} onChange={(e) => set('incidentType', e.target.value.toUpperCase().replaceAll(' ', '_'))} required/></label><label>Nature of violation<select value={form.natureOfViolation} onChange={(e) => set('natureOfViolation', e.target.value)}>{(categories.length ? categories : [{ code: 'ACCESS_CONTROL', label: 'Access Control', description: '' }, { code: 'DATA_PROTECTION', label: 'Data Protection', description: '' }, { code: 'PHYSICAL_SECURITY', label: 'Physical Security', description: '' }, { code: 'MALWARE_ENDPOINT', label: 'Malware and Endpoint', description: '' }, { code: 'ACCEPTABLE_USE', label: 'Acceptable Use', description: '' }, { code: 'CHANGE_MANAGEMENT', label: 'Change Management', description: '' }, { code: 'THIRD_PARTY_SECURITY', label: 'Third-Party Security', description: '' }, { code: 'PRIVACY_COMPLIANCE', label: 'Privacy and Compliance', description: '' }]).map((c) => <option value={c.code} key={c.code}>{c.label}</option>)}</select></label><label>Severity<select value={form.severity} onChange={(e) => set('severity', e.target.value)}>{severityValues.map((v) => <option key={v}>{v}</option>)}</select></label><label>Classification<select value={form.classificationStatus} onChange={(e) => set('classificationStatus', e.target.value)}><option>SUSPECTED</option><option>CONFIRMED</option><option>NOT_SUBSTANTIATED</option></select></label></div>
    <div className="form-section-title"><span>02</span><div><strong>Ownership &amp; impact</strong><small>Help responders understand who and what is affected.</small></div></div><div className="form-grid"><label>Reported by *<input value={form.reportedBy} onChange={(e) => set('reportedBy', e.target.value)} required/></label><label>Assigned analyst<input value={form.assignedTo} onChange={(e) => set('assignedTo', e.target.value)} placeholder="analyst@company.com"/></label><label>Business unit<input value={form.businessUnit} onChange={(e) => set('businessUnit', e.target.value)}/></label><label>Policy / control reference<input value={form.policyReference} onChange={(e) => set('policyReference', e.target.value)} placeholder="e.g. IAM-STD-004"/></label><label className="field-full">Business impact<textarea value={form.businessImpact} onChange={(e) => set('businessImpact', e.target.value)} rows={2} placeholder="Potential confidentiality, integrity, availability, or operational impact"/></label></div>
    <div className="form-section-title"><span>03</span><div><strong>Initial response</strong><small>Record the current theory and recommended action, if known.</small></div></div><div className="form-grid"><label className="field-full">Suspected root cause<input value={form.rootCause} onChange={(e) => set('rootCause', e.target.value)} placeholder="Optional — don't assume a cause before investigation"/></label><label className="field-full">Recommended corrective action<textarea value={form.correctiveAction} onChange={(e) => set('correctiveAction', e.target.value)} rows={2} placeholder="Next investigation or containment action"/></label></div>
    {formError && <div className="form-error"><AlertTriangle size={15}/>{formError}</div>}<div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? <LoaderCircle size={16} className="spin"/> : <Plus size={16}/>} Create incident</button></div></form></ModalShell>;
}

function CreateAlertModal({ onClose, onSubmit, username, busy }: { onClose: () => void; onSubmit: (payload: Record<string, unknown>) => Promise<void>; username: string; busy: boolean }) {
  const [form, setForm] = useState({ title: 'Suspicious login sequence detected', description: 'Multiple failed logins followed by a successful sign-in from an unusual source IP. Review sign-in logs and validate MFA.', source: 'SecOpsHub Demo Connector', incidentType: 'UNAUTHORIZED_ACCESS', natureOfViolation: 'ACCESS_CONTROL', severity: 'HIGH', businessUnit: 'Information Technology', policyReference: 'IAM-STD-004' });
  const [customId, setCustomId] = useState(`SOC-${Date.now()}`);
  const set = (name: keyof typeof form, value: string) => setForm((f) => ({ ...f, [name]: value }));
  function submit(e: FormEvent) { e.preventDefault(); const now = localDateTime(); void onSubmit({ ...form, externalAlertId: customId.trim() || `SOC-${Date.now()}`, classificationStatus: 'SUSPECTED', reportedBy: username, assignedTo: '', occurredAt: now, detectedAt: now, reportedAt: now, businessImpact: 'Potential unauthorized account access.', rootCause: '', correctiveAction: 'Review sign-in events and validate MFA.' }); }
  return <ModalShell title="Submit security alert" onClose={onClose} width="narrow"><form onSubmit={submit} className="modal-form"><div className="alert-modal-intro"><div className="alert-modal-icon"><CloudLightning size={22}/></div><p>Simulate an alert from a connected security tool. The alert is queued and processed asynchronously via RabbitMQ.</p></div><div className="form-grid"><label className="field-full">External alert ID<input value={customId} onChange={(e) => setCustomId(e.target.value)} required maxLength={120}/></label><label className="field-full">Alert title<input value={form.title} onChange={(e) => set('title', e.target.value)} required maxLength={180}/></label><label className="field-full">Description<textarea value={form.description} onChange={(e) => set('description', e.target.value)} rows={3} required maxLength={3000}/></label><label>Source connector<input value={form.source} onChange={(e) => set('source', e.target.value)} required/></label><label>Severity<select value={form.severity} onChange={(e) => set('severity', e.target.value)}>{severityValues.map((v) => <option key={v}>{v}</option>)}</select></label><label>Incident type<input value={form.incidentType} onChange={(e) => set('incidentType', e.target.value.toUpperCase().replaceAll(' ', '_'))}/></label><label>Violation category<select value={form.natureOfViolation} onChange={(e) => set('natureOfViolation', e.target.value)}><option value="ACCESS_CONTROL">Access Control</option><option value="DATA_PROTECTION">Data Protection</option><option value="PHYSICAL_SECURITY">Physical Security</option><option value="MALWARE_ENDPOINT">Malware and Endpoint</option><option value="ACCEPTABLE_USE">Acceptable Use</option><option value="CHANGE_MANAGEMENT">Change Management</option><option value="PRIVACY_COMPLIANCE">Privacy and Compliance</option></select></label><label className="field-full">Business unit<input value={form.businessUnit} onChange={(e) => set('businessUnit', e.target.value)}/></label></div><div className="security-note"><ShieldCheck size={16}/><span>Submit returns as soon as the message is accepted. Incident creation can take a few moments.</span></div><div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? <LoaderCircle size={16} className="spin"/> : <CloudLightning size={16}/>} Queue alert</button></div></form></ModalShell>;
}

function IncidentDetailModal({ incident, session, canUpdateStatus, canUpdateSeverity, onClose, onUpdated }: { incident: Incident; session: Session; canUpdateStatus: boolean; canUpdateSeverity: boolean; onClose: () => void; onUpdated: (incident: Incident) => Promise<void> }) {
  const [detail, setDetail] = useState(incident);
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [relationships, setRelationships] = useState<Relationship[]>([]);
  const [newSeverity, setNewSeverity] = useState(incident.severity);
  const [severityRationale, setSeverityRationale] = useState('Severity reviewed by incident manager.');
  const [newStatus, setNewStatus] = useState(allowedNext(incident.status)[0] ?? '');
  const [statusRationale, setStatusRationale] = useState('Investigation progress updated.');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const refresh = useCallback(async () => {
    try { const [audit, related] = await Promise.all([getAuditEvents(session, incident.incidentId), getRelationships(session, incident.incidentId)]); setEvents(audit); setRelationships(related); } catch { /* Detail remains usable if history endpoints fail. */ }
  }, [session, incident.incidentId]);
  useEffect(() => { void refresh(); }, [refresh]);
  async function saveSeverity(e: FormEvent) { e.preventDefault(); if (newSeverity === detail.severity) return; setSaving(true); setError(''); try { const updated = await updateSeverity(session, detail.incidentId, newSeverity, severityRationale); setDetail(updated); await onUpdated(updated); await refresh(); } catch (e) { setError(e instanceof Error ? e.message : 'Could not update severity.'); } finally { setSaving(false); } }
  async function saveStatus(e: FormEvent) { e.preventDefault(); if (!newStatus) return; setSaving(true); setError(''); try { const updated = await updateStatus(session, detail.incidentId, newStatus, statusRationale); setDetail(updated); setNewStatus(allowedNext(updated.status)[0] ?? ''); await onUpdated(updated); await refresh(); } catch (e) { setError(e instanceof Error ? e.message : 'Could not update status.'); } finally { setSaving(false); } }
  return <ModalShell title="Incident details" onClose={onClose}><div className="incident-detail"><div className="detail-title-row"><div className="detail-title-icon"><ShieldAlert size={25}/></div><div className="detail-title-copy"><span className="detail-id">{detail.incidentId}{detail.sourceAlertId ? ` · Source alert ${detail.sourceAlertId}` : ''}</span><h3>{detail.title}</h3><p>{detail.description}</p><div className="detail-badges"><SeverityBadge value={detail.severity}/><StatusBadge value={detail.status}/><span className="classification-pill">{pretty(detail.classificationStatus)}</span></div></div></div>
    <div className="detail-meta-grid"><MetaItem label="Incident type" value={pretty(detail.incidentType)}/><MetaItem label="Nature of violation" value={pretty(detail.natureOfViolation)}/><MetaItem label="Reporting period" value={`${detail.fiscalYear ?? '—'} · ${detail.reportingQuarter ?? '—'}`}/><MetaItem label="Reported date" value={shortDate(detail.reportedAt)}/><MetaItem label="Reported by" value={detail.reportedBy}/><MetaItem label="Assigned analyst" value={detail.assignedTo || 'Unassigned'}/><MetaItem label="Business unit" value={detail.businessUnit || '—'}/><MetaItem label="Policy reference" value={detail.policyReference || '—'}/></div>
    {detail.businessImpact && <div className="detail-description-block"><strong>Business impact</strong><p>{detail.businessImpact}</p></div>}
    {(canUpdateStatus || canUpdateSeverity) && <div className="detail-actions-grid">{canUpdateStatus && <form className="detail-action-card" onSubmit={saveStatus}><div className="detail-action-title"><Activity size={16}/><strong>Update workflow</strong></div><label>Next valid status<select value={newStatus} onChange={(e) => setNewStatus(e.target.value)} disabled={!allowedNext(detail.status).length}><option value="">{allowedNext(detail.status).length ? 'Choose status' : 'No further transitions'}</option>{allowedNext(detail.status).map((s) => <option key={s} value={s}>{pretty(s)}</option>)}</select></label><label>Rationale<input value={statusRationale} onChange={(e) => setStatusRationale(e.target.value)} required/></label><button className="btn btn-primary btn-small" type="submit" disabled={saving || !newStatus || !statusRationale.trim()}>{saving ? <LoaderCircle size={14} className="spin"/> : <Check size={14}/>} Save status</button></form>}
      {canUpdateSeverity && <form className="detail-action-card" onSubmit={saveSeverity}><div className="detail-action-title"><AlertTriangle size={16}/><strong>Adjust severity</strong></div><label>Severity<select value={newSeverity} onChange={(e) => setNewSeverity(e.target.value)}>{severityValues.map((s) => <option key={s}>{s}</option>)}</select></label><label>Rationale<input value={severityRationale} onChange={(e) => setSeverityRationale(e.target.value)} required/></label><button className="btn btn-secondary btn-small" type="submit" disabled={saving || newSeverity === detail.severity || !severityRationale.trim()}>{saving ? <LoaderCircle size={14} className="spin"/> : <Check size={14}/>} Save severity</button></form>}</div>}
    {error && <div className="form-error"><AlertTriangle size={15}/>{error}</div>}
    <div className="detail-bottom-grid"><section className="detail-subpanel"><div className="detail-subpanel-title"><Clock3 size={16}/><strong>Activity timeline</strong><span>{events.length}</span></div>{events.length ? <div className="timeline">{events.map((event) => <div className="timeline-item" key={event.id}><span className="timeline-node"/><div><strong>{pretty(event.action)}</strong><p>{event.rationale || `${event.oldValue ?? ''}${event.oldValue && event.newValue ? ' → ' : ''}${event.newValue ?? ''}`}</p><small>{event.performedBy || 'System'} · {shortDate(event.occurredAt)}</small></div></div>)}</div> : <div className="subpanel-empty">No audit events available.</div>}</section><section className="detail-subpanel"><div className="detail-subpanel-title"><FileCheck2 size={16}/><strong>Related incidents</strong><span>{relationships.length}</span></div>{relationships.length ? relationships.map((r, i) => <div className="related-row" key={r.id ?? i}><div className="related-icon"><ArrowRight size={14}/></div><div><strong>{r.relatedIncidentId || r.sourceIncidentId || 'Related incident'}</strong><small>{pretty(r.relationshipType)}</small></div></div>) : <div className="subpanel-empty">No incident relationships yet.</div>}</section></div>
    <div className="detail-footer"><span><ShieldCheck size={15}/> Changes are recorded in the audit history.</span><button className="btn btn-secondary btn-small" onClick={onClose}>Close details</button></div></div></ModalShell>;
}
function MetaItem({ label, value }: { label: string; value: string }) { return <div className="meta-item"><small>{label}</small><strong>{value}</strong></div>; }

export default App;
