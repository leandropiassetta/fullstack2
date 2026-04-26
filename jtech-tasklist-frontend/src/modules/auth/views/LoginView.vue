<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

type FormRef = {
  validate: () => Promise<{ valid: boolean }>
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormRef | null>(null)
const registerFormRef = ref<FormRef | null>(null)
const isLoginFormValid = ref(false)
const isRegisterFormValid = ref(false)
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showRegisterConfirmPassword = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
})

const registerForm = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const requiredRule = (value: string) => !!value?.trim() || 'Campo obrigatório.'
const emailRule = (value: string) => /.+@.+\..+/.test(value) || 'E-mail inválido.'
const minLengthRule = (min: number) => (value: string) =>
  value.length >= min || `Mínimo de ${min} caracteres.`
const passwordMatchRule = (value: string) =>
  value === registerForm.password || 'As senhas precisam ser iguais.'

function resolveRedirectTarget() {
  const redirectTo = route.query.redirectTo

  if (typeof redirectTo === 'string' && redirectTo.startsWith('/app')) {
    return redirectTo
  }

  return { name: 'app-tasklists' }
}

function switchMode(next: 'login' | 'register') {
  mode.value = next
  authStore.error = null
}

async function handleLogin() {
  authStore.error = null
  const result = await loginFormRef.value?.validate()
  if (!result?.valid) return

  try {
    await authStore.login({ email: loginForm.email, password: loginForm.password })
    void router.push(resolveRedirectTarget())
  } catch {}
}

async function handleRegister() {
  authStore.error = null
  const result = await registerFormRef.value?.validate()
  if (!result?.valid) return

  try {
    await authStore.register({
      name: registerForm.name,
      email: registerForm.email,
      password: registerForm.password,
    })
    void router.push(resolveRedirectTarget())
  } catch {}
}
</script>

<template>
  <div class="login-page">
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="auth-showcase__signals" aria-hidden="true">
          <span class="signal signal--1"></span>
          <span class="signal signal--2"></span>
          <span class="signal signal--3"></span>
        </div>
        <div class="auth-showcase__inner">
          <div class="brand-lockup">
            <div class="brand-mark">T</div>
            <div class="brand-info">
              <p class="brand-name">Listas de<br />Tarefas</p>
            </div>
          </div>

          <h1 class="showcase-title">
            Sua mente foi feita para ter ideias, não para guardá-las.
          </h1>

          <div class="showcase-points">
            <div class="showcase-point">
              <v-icon icon="mdi-lightning-bolt" size="20" />
              <span>Crie listas para organizar trabalho, rotina e prioridades</span>
            </div>
            <div class="showcase-point">
              <v-icon icon="mdi-shield-check" size="20" />
              <span>Acesse sua conta com segurança e retome seu fluxo</span>
            </div>
            <div class="showcase-point">
              <v-icon icon="mdi-check-circle" size="20" />
              <span>Adicione tarefas e acompanhe o que ainda precisa de atenção</span>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-form-column">
          <v-card rounded="xl" elevation="14" class="login-card">
            <v-card-text class="pa-8 pa-md-10">
              <p class="panel-label">Acesso</p>
              <h2 class="text-h3 font-weight-bold text-high-emphasis">
                {{ mode === 'login' ? 'Entre na sua conta' : 'Crie sua conta' }}
              </h2>
              <p class="intro-copy">
                {{
                  mode === 'login'
                    ? 'Acesse suas listas e continue de onde parou.'
                    : 'Comece agora a organizar suas listas e tarefas.'
                }}
              </p>

            <v-tabs
              v-model="mode"
              class="mt-6 auth-tabs"
              color="primary"
              density="compact"
              grow
            >
              <v-tab value="login" @click="switchMode('login')">Entrar</v-tab>
              <v-tab value="register" @click="switchMode('register')">Cadastrar</v-tab>
            </v-tabs>

            <v-alert
              v-if="authStore.error"
              class="mt-5"
              type="error"
              variant="tonal"
              density="compact"
              :text="authStore.error"
            />

            <v-form
              v-if="mode === 'login'"
              ref="loginFormRef"
              v-model="isLoginFormValid"
              class="mt-6"
              @submit.prevent="handleLogin"
            >
              <v-text-field
                v-model="loginForm.email"
                class="auth-field"
                label="E-mail"
                type="email"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-email-outline"
                autocomplete="email"
                :rules="[requiredRule, emailRule]"
              />

              <v-text-field
                v-model="loginForm.password"
                class="mt-3 auth-field"
                label="Senha"
                :type="showLoginPassword ? 'text' : 'password'"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-lock-outline"
                :append-inner-icon="showLoginPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                autocomplete="current-password"
                :rules="[requiredRule]"
                @click:append-inner="showLoginPassword = !showLoginPassword"
              />

              <v-btn
                class="mt-6 auth-submit"
                size="x-large"
                block
                append-icon="mdi-arrow-right"
                type="submit"
                :disabled="!isLoginFormValid || authStore.isLoading"
                :loading="authStore.isLoading"
              >
                Entrar
              </v-btn>
            </v-form>

            <v-form
              v-else
              ref="registerFormRef"
              v-model="isRegisterFormValid"
              class="mt-6"
              @submit.prevent="handleRegister"
            >
              <v-text-field
                v-model="registerForm.name"
                class="auth-field"
                label="Nome"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-account-outline"
                autocomplete="name"
                :rules="[requiredRule]"
              />

              <v-text-field
                v-model="registerForm.email"
                class="mt-3 auth-field"
                label="E-mail"
                type="email"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-email-outline"
                autocomplete="email"
                :rules="[requiredRule, emailRule]"
              />

              <v-text-field
                v-model="registerForm.password"
                class="mt-3 auth-field"
                label="Senha"
                :type="showRegisterPassword ? 'text' : 'password'"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-lock-outline"
                :append-inner-icon="showRegisterPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                autocomplete="new-password"
                :rules="[requiredRule, minLengthRule(6)]"
                @click:append-inner="showRegisterPassword = !showRegisterPassword"
              />

              <v-text-field
                v-model="registerForm.confirmPassword"
                class="mt-3 auth-field"
                label="Confirmar senha"
                :type="showRegisterConfirmPassword ? 'text' : 'password'"
                variant="solo"
                flat
                color="primary"
                prepend-inner-icon="mdi-shield-check-outline"
                :append-inner-icon="
                  showRegisterConfirmPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'
                "
                autocomplete="new-password"
                :rules="[requiredRule, passwordMatchRule]"
                @click:append-inner="
                  showRegisterConfirmPassword = !showRegisterConfirmPassword
                "
              />

              <v-btn
                class="mt-6 auth-submit"
                size="x-large"
                block
                append-icon="mdi-arrow-right"
                type="submit"
                :disabled="!isRegisterFormValid || authStore.isLoading"
                :loading="authStore.isLoading"
              >
                Cadastrar
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f5f0;
  padding: 24px;
}

.auth-shell {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  width: min(1200px, 100%);
  min-height: 720px;
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgb(13 43 77 / 0.15);
  background: #ffffff;
}

.auth-showcase {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px;
  background: linear-gradient(180deg, #143253 0%, #0d2b4d 100%);
  color: #ffffff;
  overflow: hidden;
}

.auth-showcase::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.42;
  z-index: 1;
  background-image:
    radial-gradient(circle at 18% 12%, rgb(255 255 255 / 0.22) 0 2px, transparent 2.6px),
    radial-gradient(circle at 72% 10%, rgb(255 255 255 / 0.2) 0 2px, transparent 2.6px),
    radial-gradient(circle at 84% 56%, rgb(255 255 255 / 0.08) 0 2px, transparent 2.6px),
    radial-gradient(circle at 24% 62%, rgb(255 255 255 / 0.08) 0 2px, transparent 2.6px),
    linear-gradient(135deg, rgb(255 255 255 / 0.06) 0, rgb(255 255 255 / 0.06) 1px, transparent 1px),
    linear-gradient(45deg, rgb(255 255 255 / 0.05) 0, rgb(255 255 255 / 0.05) 1px, transparent 1px),
    url("data:image/svg+xml,%3Csvg width='520' height='520' viewBox='0 0 520 520' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' stroke='%23ffffff' stroke-width='1.5' stroke-opacity='0.56' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M38 108h76l34 34h64l40-40h78l42 42h84M52 258h96l42-42h86l36 36h74l40-40h48M228 34v82l42 42v66l-56 56v72M356 56l-36 36v74l48 48v88M124 392l58-58h84l44 44h98M92 168l34 34h78l46-46h66l36 36h86M164 76l32 32h44l28-28h44l34 34M146 286l34 34h58l40-40h82l28 28h66'/%3E%3Cpath d='M260 158l58-58h66M260 224l72 72h74M270 280l-54 54h-68M198 204l-52-52H82' stroke-opacity='0.48'/%3E%3C/g%3E%3Cg fill='%23ffffff' fill-opacity='0.8'%3E%3Ccircle cx='38' cy='108' r='3.2'/%3E%3Ccircle cx='52' cy='258' r='3.2'/%3E%3Ccircle cx='228' cy='34' r='3.2'/%3E%3Ccircle cx='356' cy='56' r='3.2'/%3E%3Ccircle cx='124' cy='392' r='3.2'/%3E%3Ccircle cx='92' cy='168' r='3.2'/%3E%3Ccircle cx='372' cy='144' r='2.8'/%3E%3Ccircle cx='426' cy='218' r='2.8'/%3E%3Ccircle cx='408' cy='378' r='2.8'/%3E%3Ccircle cx='268' cy='334' r='2.8'/%3E%3Ccircle cx='146' cy='152' r='2.8'/%3E%3Ccircle cx='318' cy='100' r='2.8'/%3E%3Ccircle cx='332' cy='296' r='2.8'/%3E%3Ccircle cx='216' cy='334' r='2.8'/%3E%3C/g%3E%3C/svg%3E"),
    url("data:image/svg+xml,%3Csvg width='560' height='560' viewBox='0 0 560 560' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' stroke='%23ffffff' stroke-width='1.2' stroke-opacity='0.34' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M44 214h98l30-30h80l42 42h72l34-34h72M122 94l46 46h66l32-32h88l30 30M88 390l54-54h76l48 48h90M280 42v76l34 34v76l54 54v92M422 78l-34 34v62l40 40v78M196 248l46 46h68l44-44h78'/%3E%3Cpath d='M280 228l-58 58h-74M310 188l58-58h54M236 330l-44 44h-64M344 280l52 52h72' stroke-opacity='0.28'/%3E%3C/g%3E%3Cg fill='%23ffffff' fill-opacity='0.52'%3E%3Ccircle cx='44' cy='214' r='2.6'/%3E%3Ccircle cx='122' cy='94' r='2.6'/%3E%3Ccircle cx='88' cy='390' r='2.6'/%3E%3Ccircle cx='280' cy='42' r='2.6'/%3E%3Ccircle cx='422' cy='78' r='2.6'/%3E%3Ccircle cx='196' cy='248' r='2.6'/%3E%3Ccircle cx='294' cy='226' r='2.3'/%3E%3Ccircle cx='266' cy='336' r='2.3'/%3E%3Ccircle cx='400' cy='332' r='2.3'/%3E%3Ccircle cx='154' cy='336' r='2.3'/%3E%3C/g%3E%3C/svg%3E");
  background-size:
    220px 220px,
    220px 220px,
    260px 260px,
    140px 140px,
    180px 180px,
    420px 420px,
    500px 500px;
  background-position:
    16% 10%,
    78% 8%,
    84% 60%,
    26% 64%,
    center,
    center,
    left 12% top 2%,
    right 0 top 16%;
  background-repeat: repeat;
  mix-blend-mode: screen;
  mask-image:
    linear-gradient(
      to bottom,
      black 0%,
      black 26%,
      rgb(0 0 0 / 0.42) 48%,
      rgb(0 0 0 / 0.26) 66%,
      transparent 100%
    ),
    radial-gradient(circle at 50% 16%, black 0%, rgb(0 0 0 / 0.98) 48%, transparent 96%);
}

.auth-showcase::after {
  content: '';
  position: absolute;
  inset: -18% -8%;
  z-index: 1;
  background:
    linear-gradient(
      110deg,
      transparent 0%,
      transparent 38%,
      rgb(255 255 255 / 0.02) 46%,
      rgb(255 255 255 / 0.12) 50%,
      rgb(255 255 255 / 0.03) 54%,
      transparent 62%,
      transparent 100%
    ),
    radial-gradient(circle at 22% 26%, rgb(255 255 255 / 0.08) 0 0.8%, transparent 3.8%),
    radial-gradient(circle at 68% 18%, rgb(255 255 255 / 0.08) 0 0.9%, transparent 4.2%),
    radial-gradient(circle at 78% 72%, rgb(255 255 255 / 0.06) 0 0.8%, transparent 3.8%),
    radial-gradient(circle at 32% 74%, rgb(255 255 255 / 0.06) 0 0.8%, transparent 4%);
  filter: blur(16px);
  opacity: 0.9;
  pointer-events: none;
  mix-blend-mode: screen;
  animation: neuralSweep 10s linear infinite;
}

.auth-showcase__signals {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.signal {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: radial-gradient(circle, rgb(255 255 255 / 0.98) 0%, rgb(255 255 255 / 0.9) 32%, rgb(147 197 253 / 0.72) 58%, rgb(147 197 253 / 0) 100%);
  box-shadow:
    0 0 8px rgb(255 255 255 / 0.88),
    0 0 18px rgb(147 197 253 / 0.58),
    0 0 26px rgb(125 211 252 / 0.24);
  opacity: 0;
  filter: saturate(1.15);
  offset-rotate: 0deg;
  will-change: offset-distance, opacity, transform;
}

.signal::before {
  content: '';
  position: absolute;
  inset: -8px;
  border-radius: inherit;
  background: radial-gradient(circle, rgb(255 255 255 / 0.2) 0%, transparent 72%);
}

.signal--1 {
  top: 0;
  left: 0;
  offset-path: path('M 40 108 L 116 108 L 150 142 L 214 142 L 254 102 L 332 102 L 374 144 L 458 144');
  animation: signalTravel 4.8s linear infinite;
}

.signal--2 {
  top: 0;
  left: 0;
  width: 6px;
  height: 6px;
  offset-path: path('M 228 34 L 228 116 L 270 158 L 270 224 L 214 280 L 214 352');
  animation: signalTravel 5.2s linear infinite 0.9s;
}

.signal--3 {
  top: 0;
  left: 0;
  width: 6px;
  height: 6px;
  offset-path: path('M 92 168 L 126 202 L 204 202 L 250 156 L 316 156 L 352 192 L 438 192');
  animation: signalTravel 4.4s linear infinite 1.6s;
}

.auth-showcase__inner {
  text-shadow: 0 2px 20px rgb(4 18 34 / 0.18);
}

@keyframes neuralSweep {
  0% {
    transform: translateX(-12%) translateY(-1%);
    opacity: 0.55;
  }

  45% {
    opacity: 0.9;
  }

  100% {
    transform: translateX(12%) translateY(1%);
    opacity: 0.55;
  }
}

@keyframes signalTravel {
  0% {
    offset-distance: 0%;
    transform: scale(0.72);
    opacity: 0;
  }

  10% {
    opacity: 1;
  }

  30% {
    transform: scale(1);
    opacity: 1;
  }

  58% {
    transform: scale(0.96);
    opacity: 0.95;
  }

  82% {
    transform: scale(0.84);
    opacity: 0.86;
  }

  100% {
    offset-distance: 100%;
    transform: scale(0.64);
    opacity: 0;
  }
}

.auth-showcase__inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 20px;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 40px;
}

.brand-mark {
  font-family:
    'Inter',
    'Segoe UI',
    system-ui,
    sans-serif;
  font-size: 8.5rem;
  line-height: 0.7;
  font-weight: 900;
  background: linear-gradient(180deg, #fde68a 0%, #d97706 50%, #78350f 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 15px 30px rgb(0 0 0 / 0.4));
  margin-right: 8px;
}

.brand-info {
  display: flex;
  align-items: center;
}

.brand-name {
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.1;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.showcase-title {
  font-size: clamp(3.2rem, 4.8vw, 4.8rem) !important;
  line-height: 0.92 !important;
  font-weight: 950 !important;
  color: #ffffff;
  margin-top: 32px;
  margin-bottom: 56px;
  letter-spacing: -0.06em !important;
  max-width: 12ch;
  -webkit-text-stroke: 1.2px rgb(196 154 78 / 0.62);
  text-shadow:
    0 1px 0 rgb(8 24 43 / 0.22),
    0 10px 24px rgb(5 18 34 / 0.16),
    0 0 18px rgb(6 25 45 / 0.1);
}

.showcase-points {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showcase-point {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgb(255 255 255 / 0.85);
  font-size: 0.95rem;
  font-weight: 600;
}

.showcase-point :deep(.v-icon) {
  color: rgb(244 225 173 / 0.98) !important;
  opacity: 1;
  filter:
    drop-shadow(0 0 6px rgb(196 154 78 / 0.28))
    drop-shadow(0 1px 0 rgb(108 73 21 / 0.24));
}

.auth-form-column {
  background-color: #f8f5f0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 72px 64px;
}

.login-card {
  width: 100%;
  max-width: 480px;
  background: #ffffff !important;
  border-radius: 32px !important;
  border: 1px solid rgb(255 255 255 / 0.8) !important;
  box-shadow: 0 50px 100px -20px rgb(0 0 0 / 0.12) !important;
  backdrop-filter: blur(10px);
}

.panel-label {
  color: #64748b;
  font-weight: 800;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.intro-copy {
  color: #334155;
  font-weight: 500;
}

.auth-submit {
  background: linear-gradient(180deg, #2d3e50 0%, #0d2b4d 100%) !important;
  color: #ffffff !important;
  font-weight: 700 !important;
  text-transform: none !important;
  letter-spacing: -0.02em !important;
  height: 54px !important;
  border-radius: 14px !important;
  box-shadow: 0 10px 20px -5px rgb(13 43 77 / 0.4) !important;
  transition: all 0.3s ease !important;
}

.auth-submit:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 12px 24px rgb(13 43 77 / 0.3) !important;
}

.login-card :deep(.auth-field .v-field) {
  border-radius: 12px !important;
  background: linear-gradient(180deg, #fcfdfc 0%, #f7f8f6 100%) !important;
  border: 1px solid rgb(196 154 78 / 0.2) !important;
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 0.82),
    0 2px 8px rgb(15 23 42 / 0.03) !important;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease !important;
}

.login-card :deep(.auth-field .v-field:hover) {
  border-color: rgb(196 154 78 / 0.3) !important;
}

.login-card :deep(.auth-field .v-field--focused) {
  border-color: rgb(196 154 78 / 0.58) !important;
  box-shadow:
    0 0 0 4px rgb(196 154 78 / 0.12),
    0 10px 24px rgb(13 43 77 / 0.08) !important;
  transform: translateY(-1px);
}

.login-card :deep(.auth-field .v-field__overlay) {
  opacity: 0 !important;
}

.login-card :deep(.auth-field .v-field__loader) {
  display: none !important;
}

.login-card :deep(.v-field__input) {
  padding-top: 20px !important;
  padding-bottom: 15px !important;
  padding-inline: 0.35rem !important;
}

.login-card :deep(.v-label) {
  font-weight: 600;
  color: #64748b;
  margin-inline-start: 0.18rem;
}

.login-card :deep(.auth-field .v-field__prepend-inner),
.login-card :deep(.auth-field .v-field__append-inner) {
  color: rgb(71 85 105 / 0.9);
  opacity: 1;
}

.login-card :deep(.auth-field .v-input__details) {
  padding-inline: 0.2rem;
}

.auth-tabs :deep(.v-tab) {
  font-weight: 700;
  transition:
    color 0.2s ease,
    opacity 0.2s ease;
}

.auth-tabs :deep(.v-slide-group__content) {
  gap: 0.25rem;
}

.auth-tabs :deep(.v-tab--selected) {
  font-weight: 700;
}

.login-card :deep(.v-btn:hover) {
  transform: translateY(-1px);
  box-shadow: var(--app-shadow-hover);
}

@media (max-width: 960px) {
  .auth-shell {
    grid-template-columns: 1fr;
    width: min(680px, 100%);
    min-height: auto;
  }

  .auth-showcase {
    min-height: 320px;
    padding: 36px 32px;
  }

  .showcase-title {
    max-width: 12ch;
    margin-bottom: 2rem;
  }

  .auth-form-column {
    padding: 32px;
  }
}

@media (max-width: 640px) {
  .auth-shell {
    border-radius: 24px;
  }

  .auth-showcase {
    padding: 24px 20px;
  }

  .brand-mark {
    font-size: 5.2rem;
  }

  .showcase-title {
    font-size: 2.7rem;
  }

  .auth-form-column {
    padding: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-showcase::after {
    animation: none;
  }

  .signal {
    animation: none;
    opacity: 0;
  }
}
</style>
