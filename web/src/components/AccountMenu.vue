<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { canManageSystem, currentUser, logout } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const open = ref(false)
const avatarText = computed(() => {
  const value = currentUser.value?.displayName?.trim() || currentUser.value?.username?.trim() || '我'
  return Array.from(value)[0]
})

async function goToSettings() {
  open.value = false
  if (route.name !== 'settings') await router.push('/settings')
}

async function signOut() {
  open.value = false
  await logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="account-menu-wrap">
    <button class="avatar" :title="currentUser?.displayName" @click="open = !open">{{ avatarText }}</button>
    <div v-if="open" class="account-menu">
      <div><strong>{{ currentUser?.displayName }}</strong><small>{{ currentUser?.username }}</small></div>
      <button v-if="canManageSystem && route.name !== 'settings'" @click="goToSettings">⚙ 系统设置</button>
      <button class="logout-action" @click="signOut">退出登录</button>
    </div>
  </div>
</template>
