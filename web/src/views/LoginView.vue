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
      <div class="login-brand"><img class="brand-mark" :src="logoUrl" alt="NetDesk Logo" /><div><strong>NetDesk</strong><small>办公室网络资产台</small></div></div>
      <div class="intro-copy"><span class="eyebrow">NETWORK ASSET WORKSPACE</span><h1>每一台设备，<br />每一个服务入口，<br /><em>都清晰可见。</em></h1><p>用一张实时拓扑图管理办公室网络资产，快速找到服务器、管理后台与部署服务。</p></div>
      <div class="login-topology" aria-hidden="true"><i class="pulse p1"></i><i class="pulse p2"></i><i class="pulse p3"></i><span class="line l1"></span><span class="line l2"></span><span class="line l3"></span><b class="node n1"><img :src="deviceIcons.internet" alt="" /></b><b class="node n2"><img :src="deviceIcons.router" alt="" /></b><b class="node n3"><img :src="deviceIcons.switch" alt="" /></b><b class="node n4"><img :src="deviceIcons.server" alt="" /></b><b class="node n5"><img :src="deviceIcons.desktop" alt="" /></b></div>
      <small class="login-foot">PRIVATE · SECURE · SELF-HOSTED</small>
    </section>
    <section class="login-form-wrap">
      <form class="login-form" @submit.prevent="submit">
        <div class="login-form-title"><small>欢迎回来</small><h2>登录到 NetDesk</h2><p>请输入管理员账号以访问办公室网络资产。</p></div>
        <label>用户名<div class="login-input"><span>♙</span><input v-model.trim="username" autocomplete="username" autofocus required /></div></label>
        <label>密码<div class="login-input"><span>⌾</span><input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required /><button type="button" @click="showPassword = !showPassword">{{ showPassword ? '隐藏' : '显示' }}</button></div></label>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button class="login-submit" :disabled="loading">{{ loading ? '正在验证…' : '登录' }}<span>→</span></button>
        <div class="login-tip"><span>⌁</span><p><strong>首次登录</strong><br />默认账号为 admin，初始密码由服务端环境变量配置。</p></div>
      </form>
    </section>
  </main>
</template>
