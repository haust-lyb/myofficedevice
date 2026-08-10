<script setup>import DeviceNodeShell from './DeviceNodeShell.vue'; defineProps({ id: String, data: Object })</script>
<template><DeviceNodeShell :id="id" :data="data" icon="◎" color="#8b5cf6" /></template>
