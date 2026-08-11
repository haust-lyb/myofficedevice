<script setup>import DeviceNodeShell from './DeviceNodeShell.vue'; defineProps({ id: String, data: Object })</script>
<template><DeviceNodeShell :id="id" :data="data" color="#0891b2" /></template>
