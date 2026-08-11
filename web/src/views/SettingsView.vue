<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import logoUrl from '../assets/logo.svg'
import AccountMenu from '../components/AccountMenu.vue'

const router = useRouter()
const users = ref([])
const loginLogs = ref([])
const logPage = ref(0)
const logTotalPages = ref(0)
const logTotalElements = ref(0)
const loading = ref(true)
const logsLoading = ref(true)
const activeTab = ref('users')
const error = ref('')
const notice = ref('')
const showCreate = ref(false)
const importInput = ref(null)
const dataBusy = ref(false)
const successToast = ref('')
let toastTimer
const createForm = reactive({ username: '', displayName: '', password: '', role: 'USER' })

const pageTitle = computed(() => ({ users: '用户与权限', logs: '登录日志', data: '数据管理' })[activeTab.value])
const pageDescription = computed(() => ({
  users: '维护可访问 OfficeMesh 的账号及其拓扑权限。',
  logs: `共 ${logTotalElements.value} 条登录记录，每页显示 20 条。`,
  data: '导出拓扑备份，或从 JSON 备份文件恢复拓扑。',
})[activeTab.value])

const roleLabels = { USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
const roleDescriptions = {
  USER: '登录并查看拓扑',
  ADMIN: '登录、查看并编辑拓扑',
  SUPER_ADMIN: '拥有全部权限',
}
const loginStatusLabels = {
  SUCCESS: '登录成功',
  BAD_CREDENTIALS: '密码错误',
  LOCKED: '账号锁定',
  DISABLED: '账号停用',
}

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const result = await http.get('/users')
    users.value = result.data.map(user => ({ ...user, newPassword: '', saving: false }))
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadLoginLogs(page = logPage.value) {
  logsLoading.value = true
  error.value = ''
  try {
    const result = await http.get('/login-logs', { params: { page, size: 20 } })
    loginLogs.value = result.data.content
    logPage.value = result.data.page
    logTotalPages.value = result.data.totalPages
    logTotalElements.value = result.data.totalElements
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '登录日志加载失败'
  } finally {
    logsLoading.value = false
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

function isLocked(user) {
  return user.lockedUntil && new Date(user.lockedUntil) > new Date()
}

function avatarText(user) {
  return Array.from(user.displayName?.trim() || user.username || '用')[0]
}

async function createUser() {
  error.value = ''
  try {
    await http.post('/users', createForm)
    Object.assign(createForm, { username: '', displayName: '', password: '', role: 'USER' })
    showCreate.value = false
    notice.value = '用户已创建'
    await loadUsers()
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '创建用户失败'
  }
}

async function saveUser(user) {
  user.saving = true
  error.value = ''
  notice.value = ''
  try {
    await http.put(`/users/${user.id}`, {
      displayName: user.displayName,
      password: user.newPassword || null,
      role: user.role,
      enabled: user.enabled,
    })
    user.newPassword = ''
    notice.value = `已保存 ${user.displayName}`
    await loadUsers()
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '保存用户失败'
  } finally {
    user.saving = false
  }
}

async function deleteUser(user) {
  if (!window.confirm(`确定删除用户「${user.displayName}」吗？`)) return
  error.value = ''
  try {
    await http.delete(`/users/${user.id}`)
    notice.value = '用户已删除'
    await loadUsers()
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '删除用户失败'
  }
}

async function exportTopology() {
  dataBusy.value = true
  error.value = ''
  notice.value = ''
  try {
    const blob = await http.get('/topology/export', { responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `officemesh-topology-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    notice.value = '拓扑备份已导出'
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '拓扑导出失败'
  } finally {
    dataBusy.value = false
  }
}

function chooseImportFile() {
  importInput.value?.click()
}

async function importTopology(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    error.value = '导入文件不能超过 10 MB'
    return
  }

  let payload
  try {
    payload = JSON.parse(await file.text())
  } catch {
    error.value = '所选文件不是有效的 JSON 文件'
    return
  }
  if (!window.confirm(`导入「${file.name}」将覆盖服务器上的当前拓扑，确定继续吗？`)) return

  dataBusy.value = true
  error.value = ''
  notice.value = ''
  try {
    await http.post('/topology/import', payload)
    notice.value = '拓扑已导入，返回拓扑页面后将加载新数据'
    successToast.value = '拓扑导入成功'
    window.clearTimeout(toastTimer)
    toastTimer = window.setTimeout(() => { successToast.value = '' }, 4000)
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '拓扑导入失败'
  } finally {
    dataBusy.value = false
  }
}

onMounted(() => Promise.all([loadUsers(), loadLoginLogs()]))
</script>

<template>
  <main class="settings-page">
    <Transition name="toast">
      <div v-if="successToast" class="success-toast" role="status" aria-live="assertive"><span>✓</span><div><strong>{{ successToast }}</strong><small>返回拓扑页面后将自动加载新数据</small></div><button aria-label="关闭提示" @click="successToast = ''">×</button></div>
    </Transition>
    <header class="settings-header">
      <div class="brand"><img class="brand-mark" :src="logoUrl" alt="OfficeMesh Logo" /><div><strong>OfficeMesh</strong><small>系统设置</small></div></div>
      <AccountMenu />
    </header>
    <section class="settings-content">
      <button class="settings-back" @click="router.push('/flow')">← 返回拓扑</button>
      <div class="settings-title"><div><small>SYSTEM SETTINGS</small><h1>{{ pageTitle }}</h1><p>{{ pageDescription }}</p></div><button v-if="activeTab === 'users'" class="primary" @click="showCreate = true">＋ 新增用户</button><button v-else-if="activeTab === 'logs'" class="secondary" @click="loadLoginLogs(logPage)">↻ 刷新日志</button></div>
      <div class="settings-tabs"><button :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">用户管理</button><button :class="{ active: activeTab === 'logs' }" @click="activeTab = 'logs'">登录日志</button><button :class="{ active: activeTab === 'data' }" @click="activeTab = 'data'">导入与导出</button></div>
      <div v-if="activeTab === 'users'" class="permission-guide"><article v-for="(label, role) in roleLabels" :key="role"><strong>{{ label }}</strong><span>{{ roleDescriptions[role] }}</span></article></div>
      <p v-if="error" class="settings-message error">{{ error }}</p>
      <p v-else-if="notice" class="settings-message success">{{ notice }}</p>
      <div v-if="activeTab === 'users' && loading" class="settings-loading">正在加载用户…</div>
      <div v-else-if="activeTab === 'users'" class="user-table">
        <div class="user-row table-head"><span>用户</span><span>角色</span><span>状态</span><span>重置密码</span><span>操作</span></div>
        <div v-for="user in users" :key="user.id" class="user-row">
          <div class="user-identity"><span>{{ avatarText(user) }}</span><label><input v-model.trim="user.displayName" /><small>@{{ user.username }}</small></label></div>
          <div><select v-model="user.role" :disabled="user.role === 'SUPER_ADMIN'"><option value="USER">普通用户</option><option value="ADMIN">管理员</option><option v-if="user.role === 'SUPER_ADMIN'" value="SUPER_ADMIN">超级管理员</option></select><small>{{ roleDescriptions[user.role] }}</small></div>
          <label class="status-switch"><input v-model="user.enabled" type="checkbox" :disabled="user.role === 'SUPER_ADMIN'" /><span :class="{ locked: isLocked(user) }">{{ isLocked(user) ? '锁定中' : user.enabled ? '已启用' : '已停用' }}</span></label>
          <input v-model="user.newPassword" type="password" autocomplete="new-password" :placeholder="isLocked(user) ? '重置密码可解锁' : '留空则不修改'" />
          <div class="user-actions"><button class="save" :disabled="user.saving" @click="saveUser(user)">{{ user.saving ? '保存中' : '保存' }}</button><button class="delete" :disabled="user.role !== 'USER'" :title="user.role !== 'USER' ? '管理员不可删除' : '删除用户'" @click="deleteUser(user)">删除</button></div>
        </div>
      </div>
      <div v-else-if="activeTab === 'logs' && logsLoading" class="settings-loading">正在加载登录日志…</div>
      <div v-else-if="activeTab === 'logs'" class="login-log-table">
        <div class="login-log-row table-head"><span>时间</span><span>账号</span><span>结果</span><span>IP 地址</span><span>客户端</span></div>
        <div v-for="log in loginLogs" :key="log.id" class="login-log-row"><span>{{ formatTime(log.createdAt) }}</span><strong>{{ log.username }}</strong><span class="login-result" :class="log.status.toLowerCase()">{{ loginStatusLabels[log.status] }}</span><code>{{ log.ipAddress }}</code><span class="user-agent" :title="log.userAgent">{{ log.userAgent || '—' }}</span></div>
        <div v-if="!loginLogs.length" class="settings-loading">暂无登录日志</div>
        <div v-if="logTotalPages > 0" class="log-pagination"><span>第 {{ logPage + 1 }} / {{ logTotalPages }} 页</span><div><button :disabled="logPage === 0 || logsLoading" @click="loadLoginLogs(logPage - 1)">上一页</button><button :disabled="logPage + 1 >= logTotalPages || logsLoading" @click="loadLoginLogs(logPage + 1)">下一页</button></div></div>
      </div>
      <div v-else class="data-management">
        <article><span class="data-icon export">↓</span><div><strong>导出拓扑备份</strong><p>下载包含全部设备、连线、服务入口及凭据的 JSON 文件。备份内容为明文，请妥善保管。</p><small>格式：OfficeMesh Topology JSON · 可用于迁移或灾难恢复</small></div><button :disabled="dataBusy" @click="exportTopology">{{ dataBusy ? '处理中…' : '导出 JSON' }}</button></article>
        <article><span class="data-icon import">↑</span><div><strong>导入拓扑备份</strong><p>从 OfficeMesh 备份或包含 nodes、edges 的 JSON 文件恢复数据，现有拓扑将被覆盖。</p><small>导入前建议先导出当前拓扑，文件大小上限 10 MB</small></div><button class="danger-outline" :disabled="dataBusy" @click="chooseImportFile">选择文件并导入</button><input ref="importInput" type="file" accept="application/json,.json" hidden @change="importTopology" /></article>
        <div class="data-warning"><span>!</span><p><strong>安全提示</strong>导出的服务账号与密码未经额外文件加密，请仅在可信环境中存储和传输备份。</p></div>
      </div>
    </section>

    <div v-if="showCreate" class="modal-backdrop" @mousedown.self="showCreate = false">
      <form class="modal" @submit.prevent="createUser"><div class="modal-title"><div><small>用户管理</small><h2>新增用户</h2></div><button type="button" @click="showCreate = false">×</button></div><label>用户名<input v-model.trim="createForm.username" autocomplete="off" maxlength="64" required /></label><label>显示名称<input v-model.trim="createForm.displayName" maxlength="80" required /></label><label>初始密码<input v-model="createForm.password" type="password" autocomplete="new-password" minlength="6" required /></label><label>角色<select v-model="createForm.role"><option value="USER">普通用户 · 仅查看</option><option value="ADMIN">管理员 · 可编辑拓扑</option></select></label><div class="modal-actions"><button type="button" @click="showCreate = false">取消</button><button class="primary">创建用户</button></div></form>
    </div>
  </main>
</template>
