<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import logoUrl from '../assets/logo.svg'
import { currentUser, logout } from '../stores/auth'

const router = useRouter()
const users = ref([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const showCreate = ref(false)
const createForm = reactive({ username: '', displayName: '', password: '', role: 'USER' })

const roleLabels = { USER: '普通用户', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }
const roleDescriptions = {
  USER: '登录并查看拓扑',
  ADMIN: '登录、查看并编辑拓扑',
  SUPER_ADMIN: '拥有全部权限',
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

async function signOut() {
  await logout()
  await router.replace('/login')
}

onMounted(loadUsers)
</script>

<template>
  <main class="settings-page">
    <header class="settings-header">
      <div class="brand"><img class="brand-mark" :src="logoUrl" alt="NetDesk Logo" /><div><strong>NetDesk</strong><small>系统设置</small></div></div>
      <div class="settings-header-actions"><button @click="router.push('/flow')">← 返回拓扑</button><span>{{ currentUser?.displayName }}</span><button @click="signOut">退出登录</button></div>
    </header>
    <section class="settings-content">
      <div class="settings-title"><div><small>SYSTEM SETTINGS</small><h1>用户与权限</h1><p>维护可访问 NetDesk 的账号及其拓扑权限。</p></div><button class="primary" @click="showCreate = true">＋ 新增用户</button></div>
      <div class="permission-guide"><article v-for="(label, role) in roleLabels" :key="role"><strong>{{ label }}</strong><span>{{ roleDescriptions[role] }}</span></article></div>
      <p v-if="error" class="settings-message error">{{ error }}</p>
      <p v-else-if="notice" class="settings-message success">{{ notice }}</p>
      <div v-if="loading" class="settings-loading">正在加载用户…</div>
      <div v-else class="user-table">
        <div class="user-row table-head"><span>用户</span><span>角色</span><span>状态</span><span>重置密码</span><span>操作</span></div>
        <div v-for="user in users" :key="user.id" class="user-row">
          <div class="user-identity"><span>{{ user.displayName.slice(0, 2) }}</span><label><input v-model.trim="user.displayName" /><small>@{{ user.username }}</small></label></div>
          <div><select v-model="user.role" :disabled="user.role === 'SUPER_ADMIN'"><option value="USER">普通用户</option><option value="ADMIN">管理员</option><option v-if="user.role === 'SUPER_ADMIN'" value="SUPER_ADMIN">超级管理员</option></select><small>{{ roleDescriptions[user.role] }}</small></div>
          <label class="status-switch"><input v-model="user.enabled" type="checkbox" :disabled="user.role === 'SUPER_ADMIN'" /><span>{{ user.enabled ? '已启用' : '已停用' }}</span></label>
          <input v-model="user.newPassword" type="password" autocomplete="new-password" placeholder="留空则不修改" />
          <div class="user-actions"><button class="save" :disabled="user.saving" @click="saveUser(user)">{{ user.saving ? '保存中' : '保存' }}</button><button class="delete" :disabled="user.role !== 'USER'" :title="user.role !== 'USER' ? '管理员不可删除' : '删除用户'" @click="deleteUser(user)">删除</button></div>
        </div>
      </div>
    </section>

    <div v-if="showCreate" class="modal-backdrop" @mousedown.self="showCreate = false">
      <form class="modal" @submit.prevent="createUser"><div class="modal-title"><div><small>用户管理</small><h2>新增用户</h2></div><button type="button" @click="showCreate = false">×</button></div><label>用户名<input v-model.trim="createForm.username" autocomplete="off" maxlength="64" required /></label><label>显示名称<input v-model.trim="createForm.displayName" maxlength="80" required /></label><label>初始密码<input v-model="createForm.password" type="password" autocomplete="new-password" minlength="6" required /></label><label>角色<select v-model="createForm.role"><option value="USER">普通用户 · 仅查看</option><option value="ADMIN">管理员 · 可编辑拓扑</option></select></label><div class="modal-actions"><button type="button" @click="showCreate = false">取消</button><button class="primary">创建用户</button></div></form>
    </div>
  </main>
</template>
