<script setup>
import { computed, markRaw, nextTick, onMounted, provide, ref, watch } from 'vue'
import { VueFlow, MarkerType, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { ControlButton, Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import dagre from '@dagrejs/dagre'
import { v4 as uuidv4 } from 'uuid'
import { canEditTopology } from '../stores/auth'
import http from '../api/http'
import InternetNode from '../components/nodes/InternetNode.vue'
import RouterNode from '../components/nodes/RouterNode.vue'
import SwitchNode from '../components/nodes/SwitchNode.vue'
import ServerNode from '../components/nodes/ServerNode.vue'
import VirtualMachineNode from '../components/nodes/VirtualMachineNode.vue'
import DesktopNode from '../components/nodes/DesktopNode.vue'
import LaptopNode from '../components/nodes/LaptopNode.vue'
import AccountMenu from '../components/AccountMenu.vue'
import logoUrl from '../assets/logo.svg'
import autoLayoutIcon from '../assets/自动整理布局.svg'
import { allDevicesIcon, deviceIcons } from '../assets/deviceIcons'

const { project, fitView, updateEdge } = useVueFlow()

const typeMeta = {
  internet: { label: '公网', icon: deviceIcons.internet, color: '#8b5cf6' },
  router: { label: '路由器', icon: deviceIcons.router, color: '#2563eb' },
  switch: { label: '交换机', icon: deviceIcons.switch, color: '#0891b2' },
  server: { label: '服务器', icon: deviceIcons.server, color: '#0f766e' },
  virtualMachine: { label: '虚拟机', icon: deviceIcons.virtualMachine, color: '#7c3aed' },
  desktop: { label: '台式机', icon: deviceIcons.desktop, color: '#d97706' },
  laptop: { label: '笔记本', icon: deviceIcons.laptop, color: '#db2777' },
}
const nodeTypes = {
  internet: markRaw(InternetNode),
  router: markRaw(RouterNode),
  switch: markRaw(SwitchNode),
  server: markRaw(ServerNode),
  virtualMachine: markRaw(VirtualMachineNode),
  desktop: markRaw(DesktopNode),
  laptop: markRaw(LaptopNode),
}
const defaultEdgeOptions = { type: 'bezier', interactionWidth: 44, markerEnd: MarkerType.ArrowClosed, data: { lineStyle: 'solid' } }
const systemDeviceTypes = ['server', 'virtualMachine', 'desktop', 'laptop']

const nodes = ref([])
const edges = ref([])
const selectedId = ref(null)
const selectedEdgeId = ref(null)
const search = ref('')
const filter = ref('all')
const showAddDevice = ref(false)
const showAddService = ref(false)
const showPassword = ref({})
const editingServiceId = ref(null)
const editingService = ref(null)
const showDeleteConfirm = ref(false)
const serviceToDelete = ref(null)
const saveState = ref('正在连接…')
const editMode = ref(false)
const loadingTopology = ref(true)
const topologyError = ref('')
const topologyVersion = ref(0)
let saveTimer
let saveSequence = Promise.resolve()
let topologyReady = false
provide('netdesk-edit-mode', editMode)
provide('netdesk-device-filter', filter)
provide('netdesk-selected-id', selectedId)
const newDevice = ref(emptyDeviceForm())
const newService = ref({ name: '', url: '', username: '', password: '', category: '运维入口', description: '' })

const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedId.value))
const selectedEdge = computed(() => edges.value.find((edge) => edge.id === selectedEdgeId.value))
const onlineCount = computed(() => nodes.value.filter((node) => node.data.status === 'online').length)
const saveStateClass = computed(() => ({
  saving: ['正在连接…', '等待保存…', '保存中…'].includes(saveState.value),
  error: ['连接失败', '保存失败', '保存冲突'].includes(saveState.value),
}))
const searchResults = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return []
  return nodes.value.flatMap((node) => {
    const deviceMatch = [node.data.name, node.data.ip, node.data.note, node.data.hostName, node.data.os, node.data.platform]
      .some((value) => value?.toLowerCase().includes(term))
    const services = (node.data.services || []).filter((service) => [service.name, service.url, service.username].some((value) => value?.toLowerCase().includes(term)))
    return deviceMatch || services.length ? [{ node, services }] : []
  }).slice(0, 6)
})

function serializeTopology() {
  const cleanNodes = nodes.value.map(({ id, type, position, data }) => ({
    id, type, position: { x: position.x, y: position.y }, data: JSON.parse(JSON.stringify(data)),
  }))
  const cleanEdges = edges.value.map(({ id, source, target, sourceHandle, targetHandle, type, animated, markerEnd, data, style }) => ({
    id, source, target, sourceHandle, targetHandle, type, animated, markerEnd, data, style,
  }))
  return { nodes: cleanNodes, edges: cleanEdges }
}

async function persist(force = false) {
  if ((!editMode.value && !force) || !topologyReady) return
  window.clearTimeout(saveTimer)
  // Queue writes so a slow earlier autosave cannot conflict with the user's
  // own later autosave before its new version has reached the browser.
  saveSequence = saveSequence.then(async () => {
    saveState.value = '保存中…'
    try {
      const result = await http.put('/topology', { topology: serializeTopology(), version: topologyVersion.value })
      topologyVersion.value = result.data.version
      saveState.value = '已保存到服务器'
      topologyError.value = ''
    } catch (error) {
      if (error.response?.status === 409) {
        saveState.value = '保存冲突'
        topologyError.value = '拓扑已被其他用户修改，请刷新后再编辑。'
        editMode.value = false
      } else {
        saveState.value = '保存失败'
        topologyError.value = error.response?.data?.message || '无法连接服务器，修改尚未保存'
      }
    }
  })
  await saveSequence
}

onMounted(async () => {
  try {
    const result = await http.get('/topology')
    const { topology, version } = result.data
    topologyVersion.value = version
    nodes.value = (topology.nodes || []).map((node) => ({ ...node, type: node.data?.type || node.type || 'server', data: normalizeDeviceData(node.data) }))
    edges.value = (topology.edges || []).map((edge) => normalizeEdge(edge))
    topologyReady = true
    saveState.value = '已连接服务器'
    await nextTick()
    if (nodes.value.length) fitView({ padding: .18, duration: 300 })
  } catch (error) {
    topologyError.value = error.response?.data?.message || '拓扑数据加载失败'
    saveState.value = '连接失败'
  } finally {
    loadingTopology.value = false
  }
})

watch([nodes, edges], () => {
  if (!editMode.value || !topologyReady) return
  window.clearTimeout(saveTimer)
  saveState.value = '等待保存…'
  saveTimer = window.setTimeout(() => persist(), 600)
}, { deep: true })

function selectNode(node) {
  selectedId.value = node.id
  selectedEdgeId.value = null
  search.value = ''
}

function selectEdge(edge) {
  selectedEdgeId.value = edge.id
  selectedId.value = null
}

function onConnect(connection) {
  if (!editMode.value) return
  if (!connection.source || !connection.target || connection.source === connection.target) return
  const duplicate = edges.value.some((edge) => edge.source === connection.source && edge.target === connection.target && edge.sourceHandle === connection.sourceHandle && edge.targetHandle === connection.targetHandle)
  if (duplicate) return
  edges.value.push(normalizeEdge({ ...connection, id: createEdgeId() }))
}

function createEdgeId() {
  return `edge-${uuidv4()}`
}

function onEdgeUpdate({ edge, connection }) {
  if (!editMode.value) return
  if (!connection.source || !connection.target || connection.source === connection.target) return
  const duplicate = edges.value.some((candidate) => candidate.id !== edge.id
    && candidate.source === connection.source
    && candidate.target === connection.target
    && candidate.sourceHandle === connection.sourceHandle
    && candidate.targetHandle === connection.targetHandle)
  if (duplicate) return
  updateEdge(edge, connection, false)
}

function onDragStart(event, type) {
  if (!editMode.value) {
    event.preventDefault()
    return
  }
  event.dataTransfer.setData('application/vueflow', type)
  event.dataTransfer.effectAllowed = 'move'
}

function onDrop(event) {
  if (!editMode.value) return
  const type = event.dataTransfer.getData('application/vueflow')
  if (!type) return
  const bounds = event.currentTarget.getBoundingClientRect()
  const position = project({ x: event.clientX - bounds.left, y: event.clientY - bounds.top })
  const id = `${type}-${Date.now()}`
  nodes.value.push({ id, type, position, data: deviceData({ name: `新建${typeMeta[type].label}`, type }) })
  selectedId.value = id
}

function createDevice() {
  if (!editMode.value) return
  if (!newDevice.value.name.trim()) return
  const id = `${newDevice.value.type}-${Date.now()}`
  nodes.value.push({ id, type: newDevice.value.type, position: { x: 250 + Math.random() * 220, y: 250 + Math.random() * 160 }, data: deviceData({ ...newDevice.value, name: newDevice.value.name.trim() }) })
  selectedId.value = id
  showAddDevice.value = false
  newDevice.value = emptyDeviceForm()
  nextTick(() => fitView({ padding: 0.2, duration: 400 }))
}

function createService() {
  if (!editMode.value) return
  if (!selectedNode.value || !newService.value.name.trim()) return
  selectedNode.value.data.services.push({ ...newService.value, id: `service-${Date.now()}` })
  showAddService.value = false
  newService.value = { name: '', url: '', username: '', password: '', category: '运维入口', description: '' }
}

function startEditService(service) {
  editingServiceId.value = service.id
  editingService.value = { ...service }
}

function saveEditService() {
  if (!selectedNode.value || !editingService.value) return
  const idx = selectedNode.value.data.services.findIndex((s) => s.id === editingServiceId.value)
  if (idx !== -1) {
    selectedNode.value.data.services[idx] = { ...editingService.value }
  }
  editingServiceId.value = null
  editingService.value = null
}

function cancelEditService() {
  editingServiceId.value = null
  editingService.value = null
}

function copyService(service) {
  if (!editMode.value || !selectedNode.value) return
  const newId = `service-${Date.now()}`
  const copied = { ...service, id: newId, name: `${service.name}（副本）` }
  selectedNode.value.data.services.push(copied)
}

function confirmRemoveService(service) {
  serviceToDelete.value = service
  showDeleteConfirm.value = true
}

function removeServiceConfirmed() {
  if (!editMode.value || !selectedNode.value || !serviceToDelete.value) return
  selectedNode.value.data.services = selectedNode.value.data.services.filter(
    (s) => s.id !== serviceToDelete.value.id
  )
  showDeleteConfirm.value = false
  serviceToDelete.value = null
}

function removeDevice() {
  if (!editMode.value) return
  if (!selectedNode.value || !window.confirm(`确定删除「${selectedNode.value.data.name}」和它的连线吗？`)) return
  const id = selectedNode.value.id
  nodes.value = nodes.value.filter((node) => node.id !== id)
  edges.value = edges.value.filter((edge) => edge.source !== id && edge.target !== id)
  selectedId.value = null
}

function copyText(value) {
  if (value) navigator.clipboard?.writeText(value)
}

function openService(url) {
  if (/^https?:\/\//i.test(url)) window.open(url, '_blank', 'noopener,noreferrer')
  else copyText(url)
}

function emptyDeviceForm() {
  return { name: '', type: 'server', networkMode: 'dhcp', ip: '', note: '', hostName: '', os: '', platform: '', cpu: '', memory: '', disk: '' }
}

function deviceData(values) {
  return { ...emptyDeviceForm(), ...values, status: 'offline', services: [] }
}

function normalizeDeviceData(data = {}) {
  return { ...emptyDeviceForm(), status: 'offline', services: [], ...data }
}

function normalizeEdge(edge) {
  const lineStyle = edge.data?.lineStyle || (edge.animated ? 'animated' : edge.style?.strokeDasharray ? 'dashed' : 'solid')
  return { ...defaultEdgeOptions, ...edge, type: edge.type || 'bezier', interactionWidth: 44, data: { ...edge.data, lineStyle }, animated: lineStyle === 'animated', style: lineStyle === 'dashed' ? { strokeDasharray: '8 6' } : {} }
}

async function autoLayout() {
  if (!editMode.value || !nodes.value.length) return

  const graph = new dagre.graphlib.Graph()
  graph.setDefaultEdgeLabel(() => ({}))
  graph.setGraph({ rankdir: 'TB', nodesep: 54, ranksep: 86, marginx: 30, marginy: 30 })

  nodes.value.forEach((node) => {
    const width = node.dimensions?.width || 190
    const height = node.dimensions?.height || 66
    graph.setNode(node.id, { width, height })
  })
  edges.value.forEach((edge) => {
    if (graph.hasNode(edge.source) && graph.hasNode(edge.target)) graph.setEdge(edge.source, edge.target)
  })

  dagre.layout(graph)
  nodes.value = nodes.value.map((node) => {
    const layout = graph.node(node.id)
    const width = node.dimensions?.width || 190
    const height = node.dimensions?.height || 66
    return { ...node, position: { x: layout.x - width / 2, y: layout.y - height / 2 } }
  })

  await nextTick()
  fitView({ padding: .18, duration: 500 })
}

function updateEdgeStyle() {
  if (!selectedEdge.value) return
  const normalized = normalizeEdge(selectedEdge.value)
  Object.assign(selectedEdge.value, normalized)
}

function removeSelectedEdge() {
  if (!editMode.value || !selectedEdge.value) return
  edges.value = edges.value.filter((edge) => edge.id !== selectedEdge.value.id)
  selectedEdgeId.value = null
}

function updateSelectedNodeType() {
  if (selectedNode.value) selectedNode.value.type = selectedNode.value.data.type
}

async function toggleEditMode() {
  if (!canEditTopology.value) return
  if (editMode.value) {
    await persist(true)
    if (topologyError.value) return
    editMode.value = false
  } else {
    editMode.value = true
  }
  showAddDevice.value = false
  showAddService.value = false
  editingServiceId.value = null
  editingService.value = null
  showDeleteConfirm.value = false
  serviceToDelete.value = null
}

</script>

<template>
  <main class="workspace">
    <header class="topbar">
      <div class="brand"><img class="brand-mark" :src="logoUrl" alt="OfficeMesh Logo" /><div><strong>OfficeMesh</strong><small>办公室网络资产台</small></div></div>
      <div class="search-wrap">
        <span>⌕</span><input v-model="search" placeholder="搜索设备、IP、服务或账号…" />
        <div v-if="searchResults.length" class="search-panel">
          <button v-for="result in searchResults" :key="result.node.id" @click="selectNode(result.node)">
            <span class="result-icon"><img :src="typeMeta[result.node.data.type].icon" alt="" /></span>
            <span><strong>{{ result.node.data.name }}</strong><small>{{ result.node.data.ip || '未设置 IP' }}<template v-if="result.services.length"> · {{ result.services.map(s => s.name).join('、') }}</template></small></span>
            <em>查看</em>
          </button>
        </div>
      </div>
      <div class="top-actions"><AccountMenu /></div>
    </header>

    <aside class="sidebar">
      <div class="sidebar-head">
        <span>设备库</span>
        <button v-if="canEditTopology" class="edit-mode-switch" :class="{ active: editMode }" role="switch" :aria-checked="editMode" @click="toggleEditMode">
          <span class="switch-track"><i></i></span><b>{{ editMode ? '编辑中' : '启用编辑' }}</b>
        </button>
      </div>
      <p class="helper">{{ editMode ? '拖到画布中添加设备' : '启用编辑后可添加设备' }}</p>
      <div class="device-palette" :class="{ disabled: !editMode }">
        <button v-for="(meta, type) in typeMeta" :key="type" :draggable="editMode" @dragstart="onDragStart($event, type)">
          <span :style="{ background: `${meta.color}13` }"><img :src="meta.icon" alt="" /></span>{{ meta.label }}<i>⠿</i>
        </button>
      </div>
      <div class="sidebar-section">
        <span>视图</span>
        <button :class="{ active: filter === 'all' }" @click="filter = 'all'"><i><img :src="allDevicesIcon" alt="" /></i>全部设备<em>{{ nodes.length }}</em></button>
        <button v-for="(meta, type) in typeMeta" :key="type" :class="{ active: filter === type }" @click="filter = type">
          <i><img :src="meta.icon" alt="" /></i>{{ meta.label }}<em>{{ nodes.filter(node => node.data.type === type).length }}</em>
        </button>
      </div>
      <div class="network-summary"><span>网络概览</span><div><strong>{{ onlineCount }}/{{ nodes.length }}</strong><small>设备在线</small></div><div class="meter"><i :style="{ width: `${nodes.length ? onlineCount / nodes.length * 100 : 0}%` }"></i></div><p><span><i class="dot green"></i>在线 {{ onlineCount }}</span><span><i class="dot gray"></i>离线 {{ nodes.length - onlineCount }}</span></p></div>
    </aside>

    <section class="canvas" :class="{ editing: editMode }" @dragover.prevent @drop="onDrop">
      <div class="canvas-status topology-status" :class="[{ editing: editMode }, saveStateClass]" aria-live="polite">
        <i></i><span><strong>{{ editMode ? '编辑模式' : '查看模式' }}</strong><small>{{ saveState }}</small></span>
      </div>
      <VueFlow v-model:nodes="nodes" v-model:edges="edges" :node-types="nodeTypes" :default-edge-options="defaultEdgeOptions" fit-view-on-init :min-zoom="0.3" :max-zoom="1.8" :nodes-draggable="editMode" :nodes-connectable="editMode" :edges-updatable="editMode" :delete-key-code="editMode ? ['Backspace', 'Delete'] : null" :connection-radius="64" :connect-on-click="true" @connect="onConnect" @edge-update="onEdgeUpdate" @node-click="({ node }) => selectNode(node)" @edge-click="({ edge }) => selectEdge(edge)" @pane-click="selectedId = selectedEdgeId = null" class="vue-flow">
        <Background pattern-color="#d8dee8" :gap="22" :size="1" />
        <Controls position="bottom-left" :show-interactive="false" :fit-view-params="{ padding: .18, duration: 400 }">
          <ControlButton class="auto-layout-control" :disabled="!editMode" title="自动整理" @click="autoLayout"><img :src="autoLayoutIcon" alt="" /></ControlButton>
        </Controls>
        <MiniMap position="bottom-right" :node-color="node => typeMeta[node.data.type]?.color || '#94a3b8'" pannable zoomable />
      </VueFlow>
      <div v-if="loadingTopology" class="topology-state"><span class="state-spinner"></span><strong>正在从服务器加载拓扑</strong></div>
      <div v-else-if="topologyError" class="topology-state error"><span>!</span><strong>{{ topologyError }}</strong><button @click="$router.go(0)">重新加载</button></div>
      <div v-else-if="!nodes.length" class="topology-state empty-state"><span>◇</span><strong>还没有网络设备</strong><p>打开设备库顶部的编辑开关，再拖入你的第一台设备。</p><button v-if="!editMode && canEditTopology" @click="toggleEditMode">启用编辑</button></div>
      <div class="canvas-hint" :class="{ editing: editMode }">
        <span>{{ editMode ? '✦' : '◉' }}</span>
        <p><strong>{{ editMode ? '编辑提示' : '当前为查看模式' }}</strong><small>{{ editMode ? '拖动设备调整位置；完成后关闭设备库顶部的编辑开关即可保存。' : canEditTopology ? '打开设备库顶部的编辑开关，即可修改拓扑。' : '你拥有只读权限，可浏览设备与服务信息。' }}</small></p>
      </div>
    </section>

    <aside v-if="selectedNode" class="detail-panel">
      <div class="detail-head"><div class="large-icon" :style="{ background: `${typeMeta[selectedNode.data.type].color}13` }"><img :src="typeMeta[selectedNode.data.type].icon" alt="" /></div><div><small>{{ typeMeta[selectedNode.data.type].label }}</small><h2>{{ selectedNode.data.name }}</h2></div><button @click="selectedId = null">×</button></div>
      <div class="status-line"><span><i class="dot" :class="selectedNode.data.status === 'online' ? 'green' : selectedNode.data.status === 'warning' ? 'amber' : 'gray'"></i>{{ selectedNode.data.status === 'online' ? '设备在线' : selectedNode.data.status === 'warning' ? '设备需关注' : '设备离线' }}</span><select v-model="selectedNode.data.status" :disabled="!editMode"><option value="online">在线</option><option value="warning">需关注</option><option value="offline">离线</option></select></div>
      <section class="detail-section"><div class="section-title"><span>设备信息</span><em class="readonly-label">{{ editMode ? '可编辑' : '只读' }}</em></div><label>设备名称<input v-model="selectedNode.data.name" :disabled="!editMode" /></label><label>设备类型<select v-model="selectedNode.data.type" :disabled="!editMode" @change="updateSelectedNodeType"><option v-for="(meta, type) in typeMeta" :key="type" :value="type">{{ meta.label }}</option></select></label><div class="field-row"><label>网络配置<select v-model="selectedNode.data.networkMode" :disabled="!editMode"><option value="dhcp">DHCP 自动获取</option><option value="static">固定 IP</option></select></label><label>{{ selectedNode.data.networkMode === 'static' ? '固定 IP 地址' : '当前 IP（可选）' }}<input v-model="selectedNode.data.ip" :disabled="!editMode" placeholder="192.168.1.10" /></label></div><label v-if="systemDeviceTypes.includes(selectedNode.data.type) && selectedNode.data.type !== 'virtualMachine'">操作系统<input v-model="selectedNode.data.os" :disabled="!editMode" placeholder="例如：Windows 11 / Ubuntu 24.04" /></label><label>备注<textarea v-model="selectedNode.data.note" :disabled="!editMode" rows="2" placeholder="位置、系统、用途…"></textarea></label></section>
      <section v-if="selectedNode.data.type === 'virtualMachine'" class="detail-section vm-section"><div class="section-title"><span>虚拟机配置</span><em class="readonly-label">{{ editMode ? '可编辑' : '只读' }}</em></div><div class="field-row"><label>宿主机<input v-model="selectedNode.data.hostName" :disabled="!editMode" placeholder="例如：PVE-01" /></label><label>虚拟化平台<select v-model="selectedNode.data.platform" :disabled="!editMode"><option value="">未设置</option><option>Proxmox VE</option><option>VMware ESXi</option><option>Hyper-V</option><option>KVM</option><option>VirtualBox</option><option>其他</option></select></label></div><label>操作系统<input v-model="selectedNode.data.os" :disabled="!editMode" placeholder="例如：Ubuntu Server 24.04" /></label><div class="vm-resource-grid"><label>CPU<input v-model="selectedNode.data.cpu" :disabled="!editMode" placeholder="4 vCPU" /></label><label>内存<input v-model="selectedNode.data.memory" :disabled="!editMode" placeholder="8 GB" /></label><label>磁盘<input v-model="selectedNode.data.disk" :disabled="!editMode" placeholder="120 GB" /></label></div></section>
      <section class="detail-section services"><div class="section-title"><span>Web 服务 <em>{{ selectedNode.data.services?.length || 0 }}</em></span><button v-if="editMode" @click="showAddService = true">＋ 添加</button></div>
        <div v-if="!selectedNode.data.services?.length" class="empty"><span>⌁</span><strong>还没有服务</strong><small>添加管理后台、面板或业务系统</small></div>
        <article v-for="service in selectedNode.data.services" :key="service.id" class="service-card">
          <template v-if="editingServiceId === service.id">
            <div class="service-card-edit">
              <label>服务名称<input v-model="editingService.name" placeholder="例如：Portainer" /></label>
              <label>访问地址<input v-model="editingService.url" placeholder="https://192.168.10.30:9443" /></label>
              <div class="field-row"><label>账号<input v-model="editingService.username" autocomplete="off" /></label><label>密码<input v-model="editingService.password" type="password" autocomplete="new-password" /></label></div>
              <label>分类<select v-model="editingService.category"><option>运维入口</option><option>研发服务</option><option>文件服务</option><option>容器服务</option><option>远程访问</option><option>其他</option></select></label>
              <label>说明<textarea v-model="editingService.description" rows="2" placeholder="服务用途、注意事项…"></textarea></label>
            </div>
            <div class="service-actions"><button class="service-action-save" @click="saveEditService">保存</button><button @click="cancelEditService">取消</button></div>
          </template>
          <template v-else>
            <div class="service-top"><span class="service-favicon">↗</span><div><strong>{{ service.name }}</strong><small>{{ service.category }}</small></div></div>
            <p v-if="service.description" class="service-desc">{{ service.description }}</p>
            <button class="service-url" @click="openService(service.url)"><span>{{ service.url }}</span><i>{{ /^https?:/.test(service.url) ? '↗' : '复制' }}</i></button>
            <div class="credential"><span><small>账号</small><b>{{ service.username || '—' }}</b></span><button @click="copyText(service.username)">复制</button></div>
            <div class="credential"><span><small>密码</small><b>{{ showPassword[service.id] ? (service.password || '未保存') : '••••••••••' }}</b></span><button @click="showPassword[service.id] = !showPassword[service.id]">{{ showPassword[service.id] ? '隐藏' : '显示' }}</button><button @click="copyText(service.password)">复制</button></div>
            <div v-if="editMode" class="service-actions"><button @click="startEditService(service)">编辑</button><button @click="copyService(service)">复制</button><button class="service-action-delete" @click="confirmRemoveService(service)">删除</button></div>
          </template>
        </article>
      </section>
      <div class="security-note">🔒 设备、服务与凭据均保存到服务器，拓扑数据使用 AES-GCM 加密后写入 SQLite。</div>
      <button v-if="editMode" class="delete-device" @click="removeDevice">删除此设备</button>
    </aside>

    <aside v-else-if="selectedEdge" class="detail-panel edge-panel">
      <div class="detail-head"><div class="large-icon edge-icon">⌁</div><div><small>网络连接</small><h2>连线设置</h2></div><button @click="selectedEdgeId = null">×</button></div>
      <section class="detail-section"><div class="section-title"><span>连接信息</span><em class="readonly-label">{{ editMode ? '可编辑' : '只读' }}</em></div><div class="edge-endpoints"><span>{{ nodes.find(n => n.id === selectedEdge.source)?.data.name || selectedEdge.source }}</span><i>→</i><span>{{ nodes.find(n => n.id === selectedEdge.target)?.data.name || selectedEdge.target }}</span></div></section>
      <section class="detail-section"><div class="section-title"><span>路径风格</span></div><div class="edge-style-grid"><label v-for="option in [{ value: 'smoothstep', label: '圆角折线', icon: '⌁' }, { value: 'bezier', label: '贝塞尔曲线', icon: '∿' }, { value: 'straight', label: '直线', icon: '╱' }, { value: 'step', label: '阶梯线', icon: '⌜' }]" :key="option.value" :class="{ active: selectedEdge.type === option.value }"><input v-model="selectedEdge.type" type="radio" :value="option.value" :disabled="!editMode" /><b>{{ option.icon }}</b><span>{{ option.label }}</span></label></div></section>
      <section class="detail-section"><div class="section-title"><span>线条样式</span></div><select v-model="selectedEdge.data.lineStyle" :disabled="!editMode" @change="updateEdgeStyle"><option value="solid">实线</option><option value="dashed">虚线</option><option value="animated">流动线</option></select></section>
      <button v-if="editMode" class="delete-device" @click="removeSelectedEdge">删除此连线</button>
    </aside>

    <div v-if="showAddDevice || showAddService" class="modal-backdrop" @mousedown.self="showAddDevice = showAddService = false">
      <form v-if="showAddDevice" class="modal" @submit.prevent="createDevice"><div class="modal-title"><div><small>资产管理</small><h2>添加新设备</h2></div><button type="button" @click="showAddDevice = false">×</button></div><label>设备名称<input v-model="newDevice.name" autofocus placeholder="例如：业务系统虚拟机" required /></label><label>设备类型<select v-model="newDevice.type"><option v-for="(meta, type) in typeMeta" :key="type" :value="type">{{ meta.label }}</option></select></label><div class="field-row"><label>网络配置<select v-model="newDevice.networkMode"><option value="dhcp">DHCP 自动获取</option><option value="static">固定 IP</option></select></label><label>{{ newDevice.networkMode === 'static' ? '固定 IP 地址' : '当前 IP（可选）' }}<input v-model="newDevice.ip" placeholder="192.168.10.10" /></label></div><label v-if="systemDeviceTypes.includes(newDevice.type) && newDevice.type !== 'virtualMachine'">操作系统<input v-model="newDevice.os" placeholder="例如：Windows 11 / Ubuntu 24.04" /></label><template v-if="newDevice.type === 'virtualMachine'"><div class="field-row"><label>宿主机<input v-model="newDevice.hostName" placeholder="例如：PVE-01" /></label><label>虚拟化平台<select v-model="newDevice.platform"><option value="">未设置</option><option>Proxmox VE</option><option>VMware ESXi</option><option>Hyper-V</option><option>KVM</option><option>VirtualBox</option><option>其他</option></select></label></div><label>操作系统<input v-model="newDevice.os" placeholder="例如：Ubuntu Server 24.04" /></label><div class="vm-resource-grid"><label>CPU<input v-model="newDevice.cpu" placeholder="4 vCPU" /></label><label>内存<input v-model="newDevice.memory" placeholder="8 GB" /></label><label>磁盘<input v-model="newDevice.disk" placeholder="120 GB" /></label></div></template><label>备注<textarea v-model="newDevice.note" rows="3" placeholder="设备位置、用途或系统信息"></textarea></label><div class="modal-actions"><button type="button" @click="showAddDevice = false">取消</button><button class="primary">添加设备</button></div></form>
      <form v-else class="modal" @submit.prevent="createService"><div class="modal-title"><div><small>{{ selectedNode?.data.name }}</small><h2>添加服务入口</h2></div><button type="button" @click="showAddService = false">×</button></div><label>服务名称<input v-model="newService.name" autofocus placeholder="例如：Portainer" required /></label><label>访问地址<input v-model="newService.url" placeholder="https://192.168.10.30:9443" /></label><div class="field-row"><label>账号<input v-model="newService.username" autocomplete="off" /></label><label>密码<input v-model="newService.password" type="password" autocomplete="new-password" /></label></div><label>分类<select v-model="newService.category"><option>运维入口</option><option>研发服务</option><option>文件服务</option><option>容器服务</option><option>远程访问</option><option>其他</option></select></label><label>说明<textarea v-model="newService.description" rows="2" placeholder="服务用途、注意事项…"></textarea></label><p class="form-warning">凭据会随拓扑加密保存到服务器，不写入浏览器 LocalStorage。</p><div class="modal-actions"><button type="button" @click="showAddService = false">取消</button><button class="primary">保存服务</button></div></form>
    </div>

    <div v-if="showDeleteConfirm" class="modal-backdrop" @mousedown.self="showDeleteConfirm = false">
      <div class="modal confirm-modal">
        <div class="modal-title"><div><small>确认删除</small><h2>删除服务</h2></div><button type="button" @click="showDeleteConfirm = false">×</button></div>
        <p class="confirm-message">确定要删除服务「<strong>{{ serviceToDelete?.name }}</strong>」吗？此操作不可恢复。</p>
        <div class="modal-actions"><button @click="showDeleteConfirm = false">取消</button><button class="danger" @click="removeServiceConfirmed">确认删除</button></div>
      </div>
    </div>
  </main>
</template>

<style>
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
@import '@vue-flow/controls/dist/style.css';
@import '@vue-flow/minimap/dist/style.css';
</style>
