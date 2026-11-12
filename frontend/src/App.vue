<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as api from './api'

type Status = 'SAVED' | 'APPLIED' | 'WRITTEN_TEST' | 'INTERVIEWING' | 'OFFER' | 'REJECTED'
type Application = { id: number; company: string; position: string; city: string; status: Status; appliedAt: string; source: string; note: string }
type Interview = api.ApiInterview
const demoApplications: Application[] = [
  { id: 1, company: '示例科技', position: '前端开发实习生', city: '广州', status: 'INTERVIEWING', appliedAt: '2026-10-14', source: '招聘平台', note: '示例：整理项目介绍与常见问题' },
  { id: 2, company: '示例制造', position: '信息化实施助理', city: '佛山', status: 'APPLIED', appliedAt: '2026-10-13', source: '校园招聘', note: '示例：复习数据库基础' },
  { id: 3, company: '示例网络', position: '初级前端开发', city: '广州', status: 'WRITTEN_TEST', appliedAt: '2026-10-12', source: '招聘平台', note: '示例：完成在线笔试' },
  { id: 4, company: '示例软件', position: '软件测试助理', city: '佛山', status: 'SAVED', appliedAt: '2026-10-16', source: '公司官网', note: '示例：完善简历后投递' },
  { id: 5, company: '示例信息', position: '产品运营助理', city: '广州', status: 'OFFER', appliedAt: '2026-10-08', source: '校园招聘', note: '示例：跟进 Offer 回复期限' },
]
const hasSavedCredentials = ref(Boolean(api.savedCredentials()))
const demoMode = ref(!hasSavedCredentials.value)
const columns: { key: Status; label: string; color: string }[] = [
  { key: 'SAVED', label: '待投递', color: 'slate' }, { key: 'APPLIED', label: '已投递', color: 'blue' }, { key: 'WRITTEN_TEST', label: '笔试', color: 'violet' }, { key: 'INTERVIEWING', label: '面试中', color: 'amber' }, { key: 'OFFER', label: 'Offer', color: 'green' }, { key: 'REJECTED', label: '已拒绝', color: 'red' },
]
const applications = ref<Application[]>(demoMode.value ? demoApplications.map((item) => ({ ...item })) : [])
const keyword = ref(''); const activeFilter = ref<Status | 'ALL'>('ALL'); const showForm = ref(false)
const activeView = ref<'board' | 'applications' | 'interviews' | 'analytics'>('board')
const form = ref({ company: '', position: '', city: '广州', status: 'SAVED' as Status, source: 'BOSS直聘', note: '' })
const editingApplicationId = ref<number | null>(null)
const authMode = ref<'login' | 'register'>('login'); const showAuth = ref(false); const authError = ref(''); const authBusy = ref(false)
const currentUser = ref<{ username: string; email: string } | null>(null); const apiConnected = ref(false)
const dataBusy = ref(false)
const connectionError = ref('')
const actionError = ref('')
const authForm = ref({ username: '', email: '', password: '' })
const formError = ref('')
const remoteSummary = ref<{ total: number; byStatus: Record<string, number>; offerConversionRate: number } | null>(null)
const selectedApplication = ref<Application | null>(null)
const interviews = ref<Interview[]>([])
const interviewBusy = ref(false)
const interviewError = ref('')
const showInterviewForm = ref(false)
const editingInterviewId = ref<number | null>(null)
const interviewForm = ref({ roundNumber: 1, interviewedAt: '', result: 'PENDING' as Interview['result'], questions: '', reflection: '' })
function requireRemoteConnection() {
  if (!demoMode.value && !apiConnected.value) {
    connectionError.value = '真实账号当前未连接后端，已禁止本地写入。请检查服务或重新登录后重试。'
    return false
  }
  return true
}
function clearUserData() {
  applications.value = []
  remoteSummary.value = null
  selectedApplication.value = null
  interviews.value = []
  showInterviewForm.value = false
  showForm.value = false
}
function expireSession() {
  api.clearCredentials()
  hasSavedCredentials.value = false
  currentUser.value = null
  apiConnected.value = false
  demoMode.value = false
  clearUserData()
  connectionError.value = '登录凭据已失效，请重新登录；当前未加载演示数据。'
}
function reportActionFailure(message: string, error: unknown) {
  actionError.value = message
  if (error instanceof api.ApiError && (error.status === 401 || error.status === 403)) {
    expireSession()
    return
  }
  if (error instanceof TypeError) {
    apiConnected.value = false
    connectionError.value = '后端连接中断，操作未能确认保存。恢复服务后请重新登录并检查数据。'
  }
}
const visibleApplications = computed(() => applications.value.filter((item) => (`${item.company}${item.position}${item.city}`.toLowerCase().includes(keyword.value.trim().toLowerCase())) && (activeFilter.value === 'ALL' || item.status === activeFilter.value)))
const totalApplied = computed(() => remoteSummary.value ? remoteSummary.value.total - (remoteSummary.value.byStatus.SAVED ?? 0) : applications.value.filter((item) => item.status !== 'SAVED').length)
const interviewing = computed(() => remoteSummary.value?.byStatus.INTERVIEWING ?? applications.value.filter((item) => item.status === 'INTERVIEWING').length)
const offers = computed(() => remoteSummary.value?.byStatus.OFFER ?? applications.value.filter((item) => item.status === 'OFFER').length)
const conversion = computed(() => remoteSummary.value?.offerConversionRate ?? (totalApplied.value ? Math.round(offers.value / totalApplied.value * 100) : 0))
const savedCount = computed(() => applications.value.filter((item) => item.status === 'SAVED').length)
const reviewCount = computed(() => applications.value.filter((item) => item.status === 'INTERVIEWING').length)
const nearDeadlineCount = computed(() => applications.value.filter((item) => item.status === 'OFFER').length)
const interviewApplications = computed(() => applications.value.filter((item) => ['INTERVIEWING', 'OFFER'].includes(item.status)))
const filteredRecords = computed(() => applications.value.filter((item) => {
  const text = `${item.company} ${item.position} ${item.city} ${item.source}`.toLowerCase()
  return text.includes(keyword.value.trim().toLowerCase()) && (activeFilter.value === 'ALL' || item.status === activeFilter.value)
}))
const statusStats = computed(() => columns.map((column) => ({ ...column, count: applications.value.filter((item) => item.status === column.key).length })))
const cityStats = computed(() => Object.entries(applications.value.reduce<Record<string, number>>((result, item) => { const city = item.city || '未填写'; result[city] = (result[city] || 0) + 1; return result }, {})).sort((a, b) => b[1] - a[1]))
const sourceStats = computed(() => Object.entries(applications.value.reduce<Record<string, number>>((result, item) => { const source = item.source || '未填写'; result[source] = (result[source] || 0) + 1; return result }, {})).sort((a, b) => b[1] - a[1]))
const maxStatusCount = computed(() => Math.max(1, ...statusStats.value.map((item) => item.count)))
const pageTitle = computed(() => ({ board: '求职看板', applications: '投递记录', interviews: '面试复盘', analytics: '数据分析' }[activeView.value]))
const pageSubtitle = computed(() => ({ board: '把每一次投递，变成可追踪的进度。', applications: '集中查看和维护你的全部岗位投递。', interviews: '记录每一轮面试，持续沉淀和改进。', analytics: '用数据了解当前求职进度和重点。' }[activeView.value]))
function goTo(view: 'board' | 'applications' | 'interviews' | 'analytics') { activeView.value = view }
function applicationsIn(status: Status) { return visibleApplications.value.filter((item) => item.status === status) }
async function submitApplication() {
  if (!requireRemoteConnection()) return
  if (!form.value.company.trim() || !form.value.position.trim()) return
  const local = { ...form.value, appliedAt: new Date().toISOString().slice(0, 10) }
  if (editingApplicationId.value !== null) {
    const id = editingApplicationId.value
    if (apiConnected.value) {
      const current = applications.value.find((item) => item.id === id)
      if (!current) return
      try { await api.updateApplication({ ...current, ...local }); await loadRemoteData() }
      catch (error) { reportActionFailure('修改失败，服务器未确认保存，请检查连接或输入后重试。', error); formError.value = '修改失败，请检查后端连接后重试。'; return }
    } else {
      const index = applications.value.findIndex((item) => item.id === id)
      if (index >= 0) applications.value[index] = { id, ...local }
    }
    showForm.value = false; editingApplicationId.value = null; resetForm(); return
  }
  if (apiConnected.value) { try { const created = await api.createApplication(local); applications.value.unshift({ id: created.id, ...local }); await loadRemoteData(); showForm.value = false; resetForm(); return } catch (error) { reportActionFailure('保存失败，服务器未确认保存，请检查连接或输入后重试。', error); formError.value = '保存失败，请检查后端连接后重试。'; return } }
  applications.value.unshift({ id: Date.now(), ...local }); showForm.value = false; resetForm()
}
function resetForm() { form.value = { company: '', position: '', city: '广州', status: 'SAVED', source: 'BOSS直聘', note: '' } }
function beginEdit(application: Application) { editingApplicationId.value = application.id; form.value = { company: application.company, position: application.position, city: application.city, status: application.status, source: application.source, note: application.note }; formError.value = ''; showForm.value = true }
async function removeApplication(application: Application) {
  if (!requireRemoteConnection()) return
  if (!window.confirm(`确定删除 ${application.company} 的「${application.position}」记录及其面试复盘吗？`)) return
  if (apiConnected.value) { try { await api.deleteApplication(application.id); await loadRemoteData() } catch (error) { reportActionFailure('删除失败，服务器未确认结果，请检查连接后重试。', error); formError.value = '删除失败，请检查后端连接后重试。'; showForm.value = true } }
  else applications.value = applications.value.filter((item) => item.id !== application.id)
}
async function rejectApplication(application: Application) {
  if (!requireRemoteConnection()) return
  if (apiConnected.value) { try { await api.updateApplication({ ...application, status: 'REJECTED' }); await loadRemoteData() } catch (error) { reportActionFailure('更新阶段失败，服务器未确认保存，请检查连接或输入后重试。', error); formError.value = '更新阶段失败，请检查后端连接后重试。' } }
  else application.status = 'REJECTED'
}
async function advance(application: Application) { if (!requireRemoteConnection()) return; const index = columns.findIndex((column) => column.key === application.status); if (index < 0 || index >= 4) return; const next = columns[index + 1].key; if (apiConnected.value) { try { await api.updateApplication({ ...application, status: next }); await loadRemoteData() } catch (error) { reportActionFailure('更新阶段失败，服务器未确认保存，请检查连接或输入后重试。', error); formError.value = '更新阶段失败，请检查后端连接后重试。' } } else application.status = next }
async function submitAuth() {
  authError.value = ''; authBusy.value = true
  try {
    if (authMode.value === 'register') await api.register(authForm.value)
    api.saveCredentials({ username: authForm.value.username, password: authForm.value.password })
    currentUser.value = await api.me(); apiConnected.value = true; hasSavedCredentials.value = true; demoMode.value = false; clearUserData(); showAuth.value = false
    await loadRemoteData()
  } catch { api.clearCredentials(); hasSavedCredentials.value = false; currentUser.value = null; apiConnected.value = false; demoMode.value = true; authError.value = authMode.value === 'register' ? '注册失败，请检查用户名、邮箱或密码。' : '登录失败，请检查账号、密码或后端服务。' } finally { authBusy.value = false }
}
async function loadRemoteData() {
  dataBusy.value = true; connectionError.value = ''
  try { const [items, summary] = await Promise.all([api.listApplications(), api.dashboard()]); applications.value = items.map((item) => ({ id: item.id, company: item.company, position: item.position, city: item.city ?? '', status: item.status as Status, appliedAt: item.appliedAt ?? '', source: item.source ?? '', note: item.note ?? '' })); remoteSummary.value = summary; apiConnected.value = true; actionError.value = '' }
  catch (error) {
    if (error instanceof api.ApiError && (error.status === 401 || error.status === 403)) expireSession()
    else { apiConnected.value = false; connectionError.value = '暂时无法同步真实数据，当前已暂停写操作；恢复服务后请重试同步。' }
  }
  finally { dataBusy.value = false }
}
function logout() { api.clearCredentials(); hasSavedCredentials.value = false; currentUser.value = null; apiConnected.value = false; demoMode.value = true; clearUserData(); applications.value = demoApplications.map((item) => ({ ...item })) }
async function openInterviews(application: Application) {
  selectedApplication.value = application; interviews.value = []; interviewError.value = ''; showInterviewForm.value = false
  if (!apiConnected.value) { interviewError.value = '登录并连接后端后可保存真实复盘。'; return }
  interviewBusy.value = true
  try { interviews.value = await api.listInterviews(application.id) } catch (error) { reportActionFailure('面试复盘加载失败，后端连接或登录状态异常。', error); interviewError.value = '复盘加载失败，请检查后端连接。' } finally { interviewBusy.value = false }
}
function beginInterview(interview?: Interview) {
  editingInterviewId.value = interview?.id ?? null
  interviewForm.value = interview ? { roundNumber: interview.roundNumber, interviewedAt: interview.interviewedAt ?? '', result: interview.result, questions: interview.questions ?? '', reflection: interview.reflection ?? '' } : { roundNumber: interviews.value.length + 1, interviewedAt: '', result: 'PENDING', questions: '', reflection: '' }
  showInterviewForm.value = true
}
async function saveInterview() {
  if (!selectedApplication.value || !apiConnected.value) return
  interviewError.value = ''
  const payload = { ...interviewForm.value, interviewedAt: interviewForm.value.interviewedAt || undefined }
  interviewBusy.value = true
  try {
    if (editingInterviewId.value) await api.updateInterview(selectedApplication.value.id, { ...payload, id: editingInterviewId.value, applicationId: selectedApplication.value.id })
    else await api.createInterview(selectedApplication.value.id, payload)
    interviews.value = await api.listInterviews(selectedApplication.value.id); showInterviewForm.value = false
  } catch (error) { reportActionFailure('面试复盘保存失败，服务器未确认保存。', error); interviewError.value = '保存失败，请稍后重试。' } finally { interviewBusy.value = false }
}
async function removeInterview(interview: Interview) {
  if (!selectedApplication.value || !apiConnected.value || !window.confirm(`确定删除第 ${interview.roundNumber} 轮面试复盘吗？`)) return
  interviewBusy.value = true
  try { await api.deleteInterview(selectedApplication.value.id, interview.id); interviews.value = interviews.value.filter((item) => item.id !== interview.id) }
  catch (error) { reportActionFailure('面试复盘删除失败，服务器未确认结果。', error); interviewError.value = '删除失败，请稍后重试。' } finally { interviewBusy.value = false }
}
async function restoreSession() {
  connectionError.value = ''
  demoMode.value = false
  try {
    currentUser.value = await api.me()
    await loadRemoteData()
  } catch (error) {
    apiConnected.value = false
    if (error instanceof api.ApiError && (error.status === 401 || error.status === 403)) {
      expireSession()
      return
    }
    connectionError.value = currentUser.value
      ? '后端暂时不可用，已保留当前页面的数据快照和登录状态；恢复服务后可重试同步。'
      : '后端暂时不可用，真实数据尚未加载；登录状态已保留，恢复服务后可重试。'
  }
}
onMounted(async () => { if (api.savedCredentials()) await restoreSession() })
</script>

<template>
  <main class="shell">
    <aside class="sidebar"><div class="brand"><span class="brand-mark">O</span><span>OfferFlow</span></div><nav><button class="nav-item" :class="{ active: activeView === 'board' }" @click="goTo('board')">▦ <span>求职看板</span></button><button class="nav-item" :class="{ active: activeView === 'applications' }" @click="goTo('applications')">◷ <span>投递记录</span></button><button class="nav-item" :class="{ active: activeView === 'interviews' }" @click="goTo('interviews')">◌ <span>面试复盘</span></button><button class="nav-item" :class="{ active: activeView === 'analytics' }" @click="goTo('analytics')">▤ <span>数据分析</span></button></nav><div class="sidebar-bottom"><div class="avatar">{{ currentUser?.username?.slice(0, 1) || '林' }}</div><div><b>{{ currentUser?.username || '求职中的你' }}</b><small>{{ apiConnected ? '已连接真实数据' : hasSavedCredentials ? '连接异常' : demoMode ? '演示模式' : '登录失效' }}</small></div></div></aside>
    <section class="content">
      <header class="topbar"><div><p class="eyebrow">2026 年秋招 <span class="connection" :class="{ online: apiConnected }">● {{ apiConnected ? '已连接' : hasSavedCredentials ? '连接异常' : demoMode ? '演示模式' : '登录失效' }}</span></p><h1>{{ pageTitle }}</h1><p class="subtitle">{{ pageSubtitle }}</p></div><div class="top-actions"><button v-if="currentUser && !apiConnected" class="secondary" @click="restoreSession">重试同步</button><button v-if="currentUser" class="user-button" @click="logout">退出 {{ currentUser.username }}</button><button v-else-if="hasSavedCredentials" class="secondary" @click="restoreSession">重试连接</button><button v-else class="secondary" @click="showAuth = true">登录 / 注册</button><button class="primary" @click="editingApplicationId = null; resetForm(); formError = ''; showForm = true">＋ 新增投递</button></div></header>
      <p v-if="demoMode" class="demo-notice">当前展示的是示例记录；登录并连接后端后会显示你的真实数据。</p>
      <p v-if="connectionError" class="connection-error" role="status">{{ connectionError }}</p>
      <p v-if="actionError" class="connection-error" role="alert">{{ actionError }}</p>
      <div v-if="dataBusy" class="sync-indicator" role="status">正在同步你的求职数据…</div>
      <section class="metrics"><article><p>累计投递</p><strong>{{ totalApplied }}</strong><span>{{ apiConnected ? '你的求职记录' : demoMode ? '示例数据' : '暂不可用' }}</span></article><article><p>面试进行中</p><strong>{{ interviewing }}</strong><span>需要准备</span></article><article><p>已获 Offer</p><strong>{{ offers }}</strong><span>继续跟进</span></article><article><p>Offer 转化率</p><strong>{{ conversion }}%</strong><span>{{ apiConnected ? '按当前记录计算' : demoMode ? '示例数据，仅供预览' : '暂不可用' }}</span></article></section>
      <template v-if="activeView === 'board'">
      <section class="action-strip"><div class="action-heading"><span class="action-dot">!</span><div><b>今日行动</b><small>把最重要的下一步先完成</small></div></div><button class="action-item" @click="activeFilter = 'SAVED'"><strong>{{ savedCount }}</strong><span>条待投递</span><em>查看 →</em></button><button class="action-item" @click="activeFilter = 'INTERVIEWING'"><strong>{{ reviewCount }}</strong><span>场面试待复盘</span><em>整理 →</em></button><button class="action-item" @click="activeFilter = 'OFFER'"><strong>{{ nearDeadlineCount }}</strong><span>个 Offer 待跟进</span><em>跟进 →</em></button></section>
      <section class="toolbar"><label class="search">⌕<input v-model="keyword" placeholder="搜索公司、岗位或城市" /></label><div class="filters"><button :class="{ selected: activeFilter === 'ALL' }" @click="activeFilter = 'ALL'">全部</button><button v-for="column in columns" :key="column.key" :class="{ selected: activeFilter === column.key }" @click="activeFilter = column.key">{{ column.label }}</button></div></section>
      <section class="board"><div v-for="column in columns" :key="column.key" class="column"><header><div class="column-title"><i :class="column.color"></i>{{ column.label }}</div><span>{{ applicationsIn(column.key).length }}</span></header><div class="cards"><article v-for="application in applicationsIn(column.key)" :key="application.id" class="job-card"><div class="card-heading"><div class="company-logo">{{ application.company.slice(0, 1) }}</div><div class="card-actions"><button title="编辑投递" @click="beginEdit(application)">✎</button><button v-if="column.key !== 'OFFER' && column.key !== 'REJECTED'" title="推进到下一阶段" @click="advance(application)">→</button><button v-if="column.key !== 'OFFER' && column.key !== 'REJECTED'" title="标记为已拒绝" @click="rejectApplication(application)">×</button><button title="删除投递" @click="removeApplication(application)">⌫</button></div></div><h2>{{ application.position }}</h2><b>{{ application.company }}</b><p class="meta">{{ application.city }} · {{ application.source }}</p><p class="note">{{ application.note || '暂无备注' }}</p><footer><button class="review-link" @click="openInterviews(application)">复盘</button><span>{{ application.appliedAt.slice(5).replace('-', '/') }}</span><span class="tag">{{ column.label }}</span></footer></article><button v-if="!applicationsIn(column.key).length" class="empty" @click="form.status = column.key; showForm = true">＋ 添加记录</button></div></div></section>
      </template>
      <template v-else-if="activeView === 'applications'">
        <section class="page-panel"><div class="panel-toolbar"><label class="search">⌕<input v-model="keyword" placeholder="搜索公司、岗位、城市或来源" /></label><div class="filters"><button :class="{ selected: activeFilter === 'ALL' }" @click="activeFilter = 'ALL'">全部</button><button v-for="column in columns" :key="column.key" :class="{ selected: activeFilter === column.key }" @click="activeFilter = column.key">{{ column.label }}</button></div></div><div class="record-table"><div class="record-row record-head"><span>公司 / 岗位</span><span>城市</span><span>来源</span><span>阶段</span><span>投递日期</span><span>操作</span></div><div v-for="application in filteredRecords" :key="application.id" class="record-row"><div><b>{{ application.company }}</b><small>{{ application.position }}</small></div><span>{{ application.city || '—' }}</span><span>{{ application.source || '—' }}</span><span class="tag">{{ columns.find((column) => column.key === application.status)?.label }}</span><span>{{ application.appliedAt || '—' }}</span><div class="row-actions"><button class="text-button" @click="beginEdit(application)">编辑</button><button class="danger-button" @click="removeApplication(application)">删除</button></div></div><div v-if="!filteredRecords.length" class="review-empty">没有符合条件的投递记录。</div></div></section>
      </template>
      <template v-else-if="activeView === 'interviews'">
        <section class="page-panel"><div class="section-heading"><div><h2>需要复盘的岗位</h2><p>选择一个岗位查看或新增面试记录。</p></div><span class="panel-count">{{ interviewApplications.length }} 个岗位</span></div><div class="interview-grid"><article v-for="application in interviewApplications" :key="application.id" class="interview-card"><div class="company-logo">{{ application.company.slice(0, 1) }}</div><div><h3>{{ application.position }}</h3><p>{{ application.company }} · {{ application.city }}</p><span class="tag">{{ columns.find((column) => column.key === application.status)?.label }}</span></div><button class="primary" @click="openInterviews(application)">查看复盘</button></article><div v-if="!interviewApplications.length" class="review-empty">暂无面试中的岗位，推进投递阶段后即可在这里记录复盘。</div></div></section>
      </template>
      <template v-else>
        <section class="analytics-grid"><article class="analytics-card wide"><div class="section-heading"><div><h2>招聘阶段分布</h2><p>共 {{ applications.length }} 条记录</p></div></div><div class="status-bars"><div v-for="item in statusStats" :key="item.key" class="status-bar"><span>{{ item.label }}</span><div><i :class="item.color" :style="{ width: `${Math.max(4, item.count / maxStatusCount * 100)}%` }"></i></div><b>{{ item.count }}</b></div></div></article><article class="analytics-card"><h2>城市分布</h2><div class="stat-list"><div v-for="item in cityStats" :key="item[0]"><span>{{ item[0] }}</span><b>{{ item[1] }}</b></div></div></article><article class="analytics-card"><h2>投递来源</h2><div class="stat-list"><div v-for="item in sourceStats" :key="item[0]"><span>{{ item[0] }}</span><b>{{ item[1] }}</b></div></div></article></section>
      </template>
    </section>
    <div v-if="showForm" class="modal-backdrop" @click.self="showForm = false"><form class="modal" @submit.prevent="submitApplication"><header><div><p class="eyebrow">{{ editingApplicationId ? '更新记录' : '创建记录' }}</p><h2>{{ editingApplicationId ? '编辑投递' : '新增一条投递' }}</h2></div><button type="button" class="close" @click="showForm = false">×</button></header><p v-if="formError" class="form-error">{{ formError }}</p><label>公司名称<input v-model="form.company" required placeholder="例如：美的集团" /></label><label>岗位名称<input v-model="form.position" required placeholder="例如：前端开发实习生" /></label><div class="form-row"><label>城市<input v-model="form.city" /></label><label>当前阶段<select v-model="form.status"><option v-for="column in columns" :key="column.key" :value="column.key">{{ column.label }}</option></select></label></div><label>投递来源<input v-model="form.source" /></label><label>备注<textarea v-model="form.note" placeholder="记录下一步行动或面试重点"></textarea></label><footer><button type="button" class="secondary" @click="showForm = false">取消</button><button class="primary" type="submit">保存记录</button></footer></form></div>
    <div v-if="showAuth" class="modal-backdrop" @click.self="showAuth = false"><form class="modal auth-modal" @submit.prevent="submitAuth"><header><div><p class="eyebrow">{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</p><h2>{{ authMode === 'login' ? '登录 OfferFlow' : '注册 OfferFlow' }}</h2></div><button type="button" class="close" @click="showAuth = false">×</button></header><label>用户名<input v-model="authForm.username" required minlength="3" autocomplete="username" placeholder="你的用户名" /></label><label v-if="authMode === 'register'">邮箱<input v-model="authForm.email" required type="email" placeholder="you@example.com" /></label><label>密码<input v-model="authForm.password" required minlength="8" type="password" autocomplete="current-password" placeholder="至少 8 位" /></label><p v-if="authError" class="form-error">{{ authError }}</p><footer><button type="button" class="text-button" @click="authMode = authMode === 'login' ? 'register' : 'login'; authError = ''">{{ authMode === 'login' ? '创建新账号' : '已有账号，直接登录' }}</button><button class="primary" type="submit" :disabled="authBusy">{{ authBusy ? '处理中…' : authMode === 'login' ? '登录' : '注册并登录' }}</button></footer></form></div>
    <div v-if="selectedApplication" class="modal-backdrop" @click.self="selectedApplication = null"><section class="modal review-modal"><header><div><p class="eyebrow">面试复盘 · {{ selectedApplication.company }}</p><h2>{{ selectedApplication.position }}</h2></div><button class="close" @click="selectedApplication = null">×</button></header><p v-if="interviewError" class="form-error">{{ interviewError }}</p><div v-if="interviewBusy && !showInterviewForm" class="review-loading">正在加载复盘…</div><div v-else-if="!showInterviewForm" class="review-list"><div v-if="!interviews.length" class="review-empty">还没有复盘记录，记下每轮面试的问题和改进点。</div><article v-for="interview in interviews" :key="interview.id" class="review-item"><div><b>第 {{ interview.roundNumber }} 轮</b><span>{{ interview.result === 'PASSED' ? '通过' : interview.result === 'FAILED' ? '未通过' : '待定' }}</span></div><p><strong>面试问题</strong>{{ interview.questions || '暂无记录' }}</p><p><strong>复盘与改进</strong>{{ interview.reflection || '暂无记录' }}</p><footer><button class="text-button" @click="beginInterview(interview)">编辑</button><button class="danger-button" @click="removeInterview(interview)">删除</button></footer></article></div><form v-else class="review-form" @submit.prevent="saveInterview"><div class="form-row"><label>轮次<input v-model.number="interviewForm.roundNumber" type="number" min="1" max="20" required /></label><label>结果<select v-model="interviewForm.result"><option value="PENDING">待定</option><option value="PASSED">通过</option><option value="FAILED">未通过</option></select></label></div><label>面试时间<input v-model="interviewForm.interviewedAt" type="datetime-local" /></label><label>面试问题<textarea v-model="interviewForm.questions" placeholder="记录被问到的问题"></textarea></label><label>复盘与改进<textarea v-model="interviewForm.reflection" placeholder="哪些地方做得好，下次如何改进"></textarea></label><footer><button type="button" class="secondary" @click="showInterviewForm = false">取消</button><button class="primary" type="submit">保存复盘</button></footer></form><button v-if="!showInterviewForm" class="primary review-add" @click="beginInterview()">＋ 新增复盘</button></section></div>
  </main>
</template>
