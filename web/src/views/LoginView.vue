<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../stores/auth'
import logoUrl from '../assets/logo.svg'
import { deviceIcons } from '../assets/deviceIcons'

const route = useRoute()
const router = useRouter()
const username = ref('admin')
const password = ref('')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await login(username.value, password.value)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/flow')
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '暂时无法登录，请检查后端服务'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand"><img class="brand-mark" :src="logoUrl" alt="OfficeMesh Logo" /><div><strong>OfficeMesh</strong><small>办公室网络资产台</small></div></div>
      <div class="intro-copy"><span class="eyebrow">NETWORK ASSET WORKSPACE</span><h1>每一台设备，<br />每一个服务入口，<br /><em>都清晰可见。</em></h1><p>用一张实时拓扑图管理办公室网络资产，快速找到服务器、管理后台与部署服务。</p></div>
      <div class="login-topology" aria-hidden="true"><i class="pulse p1"></i><i class="pulse p2"></i><i class="pulse p3"></i><span class="line l1"></span><span class="line l2"></span><span class="line l3"></span><b class="node n1"><img :src="deviceIcons.internet" alt="" /></b><b class="node n2"><img :src="deviceIcons.router" alt="" /></b><b class="node n3"><img :src="deviceIcons.switch" alt="" /></b><b class="node n4"><img :src="deviceIcons.server" alt="" /></b><b class="node n5"><img :src="deviceIcons.desktop" alt="" /></b></div>
      <div class="login-foot"><small>PRIVATE · SECURE · SELF-HOSTED</small><a href="https://github.com/haust-lyb/myofficedevice" target="_blank" rel="noopener noreferrer" aria-label="在 GitHub 查看 OfficeMesh 项目"><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 .7a11.5 11.5 0 0 0-3.64 22.41c.58.11.79-.25.79-.56v-2.23c-3.22.7-3.9-1.37-3.9-1.37-.53-1.34-1.29-1.7-1.29-1.7-1.05-.72.08-.7.08-.7 1.17.08 1.78 1.2 1.78 1.2 1.04 1.77 2.72 1.26 3.38.96.1-.75.4-1.26.74-1.55-2.57-.29-5.27-1.28-5.27-5.68 0-1.25.45-2.28 1.19-3.08-.12-.29-.52-1.46.11-3.04 0 0 .97-.31 3.16 1.18a10.94 10.94 0 0 1 5.76 0c2.2-1.49 3.16-1.18 3.16-1.18.63 1.58.23 2.75.11 3.04.74.8 1.19 1.83 1.19 3.08 0 4.41-2.7 5.38-5.28 5.67.42.36.79 1.06.79 2.14v3.18c0 .31.21.67.8.56A11.5 11.5 0 0 0 12 .7Z"/></svg> GitHub</a></div>
    </section>
    <section class="login-form-wrap">
      <form class="login-form" @submit.prevent="submit">
        <div class="login-form-title"><small>欢迎回来</small><h2>登录到 OfficeMesh</h2><p>请输入管理员账号以访问办公室网络资产。</p></div>
        <label>用户名<div class="login-input"><span>♙</span><input v-model.trim="username" autocomplete="username" autofocus required /></div></label>
        <label>密码<div class="login-input"><span>⌾</span><input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required /><button type="button" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></div></label>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button class="login-submit" :disabled="loading">{{ loading ? '正在验证…' : '登录' }}<span>→</span></button>
      </form>
    </section>
  </main>
</template>
