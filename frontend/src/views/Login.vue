<template>
  <div class="login-container">
    <div class="login-card">
      <div class="video-area">
        <video class="login-video" autoplay loop muted playsinline>
          <source src="/cat_happy.mp4" type="video/mp4" />
        </video>
      </div>
      <div class="form-area">
        <h2>🏪 超市进销存管理系统</h2>
        <el-radio-group v-model="form.role" class="role-box">
          <el-radio label="1">管理员</el-radio>
          <el-radio label="0">普通用户</el-radio>
        </el-radio-group>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="登录" name="login">
            <el-input v-model="form.username" placeholder="账号" style="margin-bottom:10px" @input="truncateLoginUsername" />
            <el-input v-model="form.password" placeholder="密码" show-password style="margin-bottom:10px" />
            <el-button type="primary" @click="login" style="width:100%">登录</el-button>
          </el-tab-pane>
          <el-tab-pane label="注册" name="reg">
            <el-input v-model="regForm.username" placeholder="账号" style="margin-bottom:10px" @input="truncateRegUsername" />
            <el-input v-model="regForm.password" placeholder="密码" show-password style="margin-bottom:5px" />
            <div class="password-strength">
              <div class="strength-label">密码强度：</div>
              <div class="strength-bars">
                <div class="bar" :class="{ weak: passwordStrength >= 1, medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
                <div class="bar" :class="{ weak: passwordStrength >= 1, medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
                <div class="bar" :class="{ medium: passwordStrength >= 2, strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
                <div class="bar" :class="{ strong: passwordStrength >= 3, veryStrong: passwordStrength >= 4 }"></div>
              </div>
              <div class="strength-text" :class="passwordStrengthClass">{{ passwordStrengthText }}</div>
              <div class="password-hints">
                <div :class="{ valid: hasLetter }">✓ 包含至少一个字母（大写或小写）</div>
                <div :class="{ valid: hasNumber }">✓ 包含数字</div>
                <div :class="{ valid: hasSpecialChar }">✓ 包含特殊字符(@$!%*?&)</div>
                <div :class="{ valid: regForm.password.length >= 8 }">✓ 至少8位</div>
              </div>
            </div>
            <div class="phone-input">
              <el-select v-model="regForm.areaCode" placeholder="区号" style="width: 120px; margin-right: 10px; color: #409EFF; font-weight: bold">
                <el-option v-for="area in areaCodes" :key="area.code" :label="area.label" :value="area.code" style="color: #409EFF" />
              </el-select>
              <el-input v-model="regForm.phoneNum" :placeholder="'手机号 (' + getPhoneLengthHint() + ')'" />
            </div>
            <el-input v-model="regForm.realName" placeholder="姓名" style="margin-bottom:10px" />
            <el-button type="primary" @click="register" style="width:100%">注册</el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { login as apiLogin, register as apiRegister, checkHasEmployees } from '@/api/user'

const router = useRouter()
const activeTab = ref('login')
const form = ref({ role: '1', username: '', password: '' })
const regForm = ref({ username: '', password: '', realName: '', phoneNum: '', areaCode: '+86' })

// 切换到注册 tab 时清空注册表单
watch(activeTab, (newTab) => {
  if (newTab === 'reg') {
    regForm.value = { username: '', password: '', realName: '', phoneNum: '', areaCode: '+86' }
  }
})

const areaCodes = [
  { code: '+86', label: '+86 中国大陆' },
  { code: '+852', label: '+852 中国香港' },
  { code: '+853', label: '+853 中国澳门' },
  { code: '+886', label: '+886 中国台湾' },
  { code: '+81', label: '+81 日本' },
  { code: '+82', label: '+82 韩国' },
  { code: '+65', label: '+65 新加坡' },
  { code: '+66', label: '+66 泰国' },
  { code: '+60', label: '+60 马来西亚' },
  { code: '+84', label: '+84 越南' },
  { code: '+91', label: '+91 印度' },
  { code: '+971', label: '+971 阿联酋' },
  { code: '+966', label: '+966 沙特' },
  { code: '+62', label: '+62 印尼' },
  { code: '+63', label: '+63 菲律宾' },
  { code: '+1', label: '+1 美国/加拿大' },
  { code: '+7', label: '+7 俄罗斯' },
  { code: '+44', label: '+44 英国' },
  { code: '+49', label: '+49 德国' },
  { code: '+33', label: '+33 法国' },
  { code: '+39', label: '+39 意大利' },
  { code: '+34', label: '+34 西班牙' },
  { code: '+41', label: '+41 瑞士' },
  { code: '+46', label: '+46 瑞典' },
  { code: '+47', label: '+47 挪威' },
  { code: '+61', label: '+61 澳大利亚' },
  { code: '+64', label: '+64 新西兰' },
  { code: '+55', label: '+55 巴西' },
  { code: '+54', label: '+54 阿根廷' }
]

const hasLetter = computed(() => /[A-Za-z]/.test(regForm.value.password))
const hasNumber = computed(() => /\d/.test(regForm.value.password))
const hasSpecialChar = computed(() => /[@$!%*?&]/.test(regForm.value.password))

const passwordStrength = computed(() => {
  let strength = 0
  if (regForm.value.password.length >= 8) strength++
  if (hasLetter.value) strength++
  if (hasNumber.value) strength++
  if (hasSpecialChar.value) strength++
  return Math.min(strength, 4)
})

const passwordStrengthClass = computed(() => {
  switch (passwordStrength.value) {
    case 0: return 'weak'
    case 1: return 'weak'
    case 2: return 'medium'
    case 3: return 'strong'
    case 4: return 'veryStrong'
    default: return ''
  }
})

const passwordStrengthText = computed(() => {
  switch (passwordStrength.value) {
    case 0: return '请输入密码'
    case 1: return '弱'
    case 2: return '中等'
    case 3: return '强'
    case 4: return '非常强'
    default: return ''
  }
})

const getPhoneLengthHint = () => {
  const hints = {
    '+86': '11位，1开头',
    '+852': '8位，5/6/9开头',
    '+853': '8位，6开头',
    '+886': '10位，09开头',
    '+81': '10-11位',
    '+82': '10-11位',
    '+65': '8位',
    '+66': '10位',
    '+60': '10位',
    '+84': '10位',
    '+91': '10位',
    '+971': '9位',
    '+966': '9位',
    '+62': '10-12位',
    '+63': '10位',
    '+1': '10位',
    '+7': '10位',
    '+44': '11位',
    '+49': '10-11位',
    '+33': '9位',
    '+39': '10位',
    '+34': '9位',
    '+41': '9位',
    '+46': '9位',
    '+47': '8位',
    '+61': '9位',
    '+64': '8-9位',
    '+55': '11位',
    '+54': '10位'
  }
  return hints[regForm.value.areaCode] || ''
}

const truncateLoginUsername = () => {
  const maxLength = form.value.role === '1' ? 6 : 9
  if (form.value.username && form.value.username.length > maxLength) {
    form.value.username = form.value.username.substring(0, maxLength)
  }
}

const truncateRegUsername = () => {
  const maxLength = form.value.role === '1' ? 6 : 9
  if (regForm.value.username && regForm.value.username.length > maxLength) {
    regForm.value.username = regForm.value.username.substring(0, maxLength)
  }
}

const login = async () => {
  try {
    const res = await apiLogin(form.value.username, form.value.password)
    if (res.code === 200) {
      let adminLevel = 0
      if (form.value.username.startsWith('11')) {
        adminLevel = 1
      } else if (form.value.username.startsWith('10')) {
        adminLevel = 2
      } else if (form.value.username.startsWith('01')) {
        adminLevel = 3
      }
      localStorage.setItem('role', res.data.role !== undefined ? String(res.data.role) : form.value.role)
      localStorage.setItem('userId', res.data.id)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('adminLevel', adminLevel.toString())
      router.push('/layout/user-info')
    } else {
      alert(res.msg)
    }
  } catch (error) {
    alert('登录失败，请检查网络连接')
  }
}

const register = async () => {
  try {
    // 如果注册普通用户，先检查员工表中是否有该用户
    if (form.value.role === '0') {
      const res = await checkHasEmployees()
      if (res.code !== 200) {
        alert('系统显示管理员还未录入员工信息，该用户为非法用户，不能注册，请联系管理员或者等待管理员录入员工信息后重试')
        activeTab.value = 'login'
        return
      }
    }
    
    const phoneWithCode = regForm.value.areaCode + '|' + regForm.value.phoneNum
    const res = await apiRegister({
      username: regForm.value.username,
      password: regForm.value.password,
      realName: regForm.value.realName,
      phone: phoneWithCode,
      role: parseInt(form.value.role)
    })
    if (res.code === 200) {
      alert('注册成功！请登录')
      activeTab.value = 'login'
    } else {
      alert(res.msg)
    }
  } catch (error) {
    alert('注册失败，请检查网络连接')
  }
}

</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.login-card {
  display: flex;
  background: rgba(255,255,255,0.95);
  border-radius: 24px;
  box-shadow: 0 25px 80px rgba(0,0,0,0.3);
  overflow: hidden;
  width: 800px;
}

.video-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #000;
}

.login-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-area {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.form-area h2 {
  text-align: center;
  margin-bottom: 25px;
  font-size: 22px;
  color: #333 !important;
}

.role-box {
  margin-bottom: 18px;
  display: flex;
  justify-content: center;
  gap: 25px;
}

.el-tabs {
  margin-top: 15px;
}

.el-tab-pane {
  padding-top: 15px;
}

.password-strength {
  margin-bottom: 10px;
}

.strength-label {
  font-size: 12px;
  color: #666 !important;
  margin-bottom: 4px;
}

.strength-bars {
  display: flex;
  gap: 3px;
  margin-bottom: 4px;
}

.bar {
  flex: 1;
  height: 5px;
  background: #ddd;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.bar.weak { background: #ff4757; }
.bar.medium { background: #ffa502; }
.bar.strong { background: #2ed573; }
.bar.veryStrong { background: #1dd1a1; }

.strength-text {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 4px;
}

.strength-text.weak { color: #ff4757 !important; }
.strength-text.medium { color: #ffa502 !important; }
.strength-text.strong { color: #2ed573 !important; }
.strength-text.veryStrong { color: #1dd1a1 !important; }

.password-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 10px;
}

.password-hints div {
  color: #999 !important;
}

.password-hints div.valid {
  color: #2ed573 !important;
}

.phone-input {
  display: flex;
  margin-bottom: 10px;
}
</style>
