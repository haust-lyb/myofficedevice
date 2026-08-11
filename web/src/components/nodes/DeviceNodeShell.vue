<script setup>
import { computed, inject } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { deviceIcons } from '../../assets/deviceIcons'

const props = defineProps({
  id: { type: String, required: true },
  data: { type: Object, required: true },
  color: { type: String, required: true },
})

const editMode = inject('netdesk-edit-mode')
const activeFilter = inject('netdesk-device-filter')
const selectedId = inject('netdesk-selected-id')
const nodeClasses = computed(() => {
  const filter = activeFilter?.value || 'all'
  const matches = filter === 'all' || props.data.type === filter
  return { dimmed: !matches, selected: selectedId?.value === props.id }
})
</script>

<template>
  <div class="device-node" :class="nodeClasses">
    <Handle v-if="editMode" id="input" class="netdesk-handle" type="target" :position="Position.Top" :connectable="true" title="拖到附近即可吸附完成连接" />
    <div class="device-icon" :style="{ background: `${color}13` }"><img :src="deviceIcons[data.type]" :alt="`${data.name}图标`" /></div>
    <div class="device-info">
      <strong>{{ data.name }}</strong>
      <span><em v-if="data.networkMode === 'dhcp'">DHCP</em>{{ data.ip || (data.networkMode === 'dhcp' ? '自动获取 IP' : 'IP 未设置') }}</span>
      <small><i class="dot" :class="data.status === 'online' ? 'green' : data.status === 'warning' ? 'amber' : 'gray'"></i>{{ data.status === 'online' ? '在线' : data.status === 'warning' ? '需关注' : '离线' }}<b v-if="data.services?.length">{{ data.services.length }} 个服务</b></small>
    </div>
    <Handle v-if="editMode" id="output" class="netdesk-handle" type="source" :position="Position.Bottom" :connectable="true" title="按住并拖动以创建连线" />
  </div>
</template>

<style>
.vue-flow__handle.netdesk-handle {
  width: 24px;
  height: 24px;
  border: 0;
  background: transparent;
  box-shadow: none;
  cursor: crosshair;
  transition: none;
}

.vue-flow__handle.netdesk-handle::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 10px;
  height: 10px;
  box-sizing: border-box;
  transform: translate(-50%, -50%);
  border: 2px solid #fff;
  border-radius: 50%;
  background: #4f7cff;
  box-shadow: 0 0 0 2px rgba(79, 124, 255, 0.25), 0 2px 6px rgba(37, 67, 145, 0.24);
  pointer-events: none;
}

.vue-flow__handle.netdesk-handle:hover::after,
.vue-flow__handle.netdesk-handle.connecting::after {
  background: #2f5ff2;
  box-shadow: 0 0 0 3px rgba(79, 124, 255, 0.14), 0 2px 6px rgba(37, 67, 145, 0.24);
}

.vue-flow__handle.netdesk-handle.valid::after {
  background: #35a773;
  box-shadow: 0 0 0 3px rgba(53, 167, 115, 0.14), 0 2px 6px rgba(27, 133, 85, 0.22);
}
</style>
