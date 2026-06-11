<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :before-close="handleClose"
  >
    <div class="confirm-content">
      <div class="confirm-icon" :class="iconClass">
        <el-icon :size="48">
          <component :is="iconComponent" />
        </el-icon>
      </div>
      <div class="confirm-message">
        <p class="message-text">{{ message }}</p>
        <p v-if="description" class="message-desc">{{ description }}</p>
      </div>
    </div>
    
    <template #footer>
      <div class="confirm-footer">
        <el-button @click="handleCancel">
          {{ cancelText }}
        </el-button>
        <el-button 
          type="primary" 
          :type="confirmType"
          @click="handleConfirm"
          :loading="loading"
        >
          {{ confirmText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Warning, Info, Success, QuestionFilled, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认'
  },
  message: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'warning',
    validator: (value) => ['warning', 'info', 'success', 'danger', 'question'].includes(value)
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const iconClass = computed(() => `icon-${props.type}`)

const iconComponent = computed(() => {
  const iconMap = {
    warning: Warning,
    info: Info,
    success: Success,
    danger: Delete,
    question: QuestionFilled
  }
  return iconMap[props.type] || Warning
})

const confirmType = computed(() => {
  const typeMap = {
    warning: 'primary',
    info: 'primary',
    success: 'success',
    danger: 'danger',
    question: 'primary'
  }
  return typeMap[props.type] || 'primary'
})

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
  dialogVisible.value = false
}

const handleClose = (done) => {
  if (!props.loading) {
    emit('cancel')
    done()
  }
}
</script>

<style scoped>
.confirm-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 10px 0;
}

.confirm-icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-warning {
  background-color: rgba(230, 162, 60, 0.12);
  color: #fbbf24;
}

.icon-info {
  background-color: var(--bg-glass);
  color: var(--text-secondary);
}

.icon-success {
  background-color: rgba(103, 194, 58, 0.12);
  color: #4ade80;
}

.icon-danger {
  background-color: rgba(245, 108, 108, 0.12);
  color: #f87171;
}

.icon-question {
  background-color: rgba(64, 158, 255, 0.12);
  color: #60a5fa;
}

.confirm-message {
  flex: 1;
}

.message-text {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 500;
}

.message-desc {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
