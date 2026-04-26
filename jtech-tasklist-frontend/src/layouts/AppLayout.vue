<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const sessionCaption = computed(() => authStore.user?.email ?? authStore.user?.id ?? 'sem sessão')

function handleLogout() {
  authStore.logout()
  void router.push('/login')
}
</script>

<template>
  <v-app class="app-layout">
    <v-navigation-drawer permanent width="268" color="primary">
      <div class="drawer-top">
        <div class="brand-mark">
          <span class="brand-glyph">T</span>
          <div class="brand-copy">
            <h2 class="drawer-title">Listas de tarefas</h2>
          </div>
        </div>
        <p class="drawer-subtitle">Crie listas e acompanhe as tarefas de cada uma.</p>
      </div>

      <div class="drawer-neural" aria-hidden="true">
        <svg class="drawer-neural__svg" viewBox="0 0 268 900" preserveAspectRatio="none">
          <defs>
            <filter id="drawer-neural-glow" x="-40%" y="-40%" width="180%" height="180%">
              <feGaussianBlur stdDeviation="1.8" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>

            <path
              id="drawer-neural-path-1"
              d="M 18 168 L 92 168 L 124 200 L 180 200 L 220 160 L 250 160"
            />
            <path
              id="drawer-neural-path-2"
              d="M 18 248 L 110 248 L 154 292 L 154 372 L 220 438 L 220 612"
            />
            <path
              id="drawer-neural-path-3"
              d="M 18 330 L 92 330 L 148 386 L 148 516 L 206 574 L 206 790"
            />
            <path id="drawer-neural-path-4" d="M 94 706 L 94 624 L 148 570 L 206 490 L 242 460" />
          </defs>

          <g class="drawer-neural__layer drawer-neural__layer--primary">
            <use href="#drawer-neural-path-1" />
            <use href="#drawer-neural-path-2" />
            <use href="#drawer-neural-path-3" />
            <path d="M 18 408 L 116 408 L 174 466 L 174 664" />
            <path d="M 94 706 L 94 636" />
            <path d="M 148 654 L 148 584" />
            <path d="M 206 790 L 206 590" />
            <path d="M 242 812 L 242 480" />
            <path d="M 124 200 L 184 260" />
            <path d="M 154 372 L 214 432" />
            <path d="M 148 516 L 220 588" />
            <path d="M 160 842 H 228" />
            <path d="M 58 540 H 138 L 176 578" />
            <path d="M 104 226 L 150 272" />
          </g>

          <g class="drawer-neural__layer drawer-neural__layer--secondary">
            <path d="M 138 132 L 138 188 L 182 232 L 182 286" />
            <path d="M 212 156 L 178 190 L 178 248 L 220 290 L 220 372" />
            <path d="M 182 286 H 232" />
            <path d="M 178 248 L 224 202" />
            <path d="M 148 570 L 206 490 L 242 460" />
            <path d="M 58 540 L 104 494 H 160" />
            <path d="M 120 756 H 194" />
            <path d="M 84 614 L 126 572" />
          </g>

          <g class="drawer-neural__nodes">
            <circle cx="18" cy="168" r="2.6" />
            <circle cx="18" cy="248" r="2.6" />
            <circle cx="18" cy="330" r="2.6" />
            <circle cx="18" cy="408" r="2.6" />
            <circle cx="94" cy="706" r="2.4" />
            <circle cx="148" cy="654" r="2.4" />
            <circle cx="206" cy="574" r="2.4" />
            <circle cx="242" cy="460" r="2.4" />
            <circle cx="250" cy="160" r="2.2" />
            <circle cx="228" cy="842" r="2.2" />
          </g>

          <g class="drawer-neural__signals">
            <circle class="drawer-neural__signal" r="2.6" filter="url(#drawer-neural-glow)">
              <animateMotion dur="7.6s" repeatCount="indefinite" rotate="auto">
                <mpath href="#drawer-neural-path-1" />
              </animateMotion>
            </circle>
            <circle
              class="drawer-neural__signal drawer-neural__signal--small"
              r="2.2"
              filter="url(#drawer-neural-glow)"
            >
              <animateMotion dur="8.8s" begin="2.4s" repeatCount="indefinite" rotate="auto">
                <mpath href="#drawer-neural-path-4" />
              </animateMotion>
            </circle>
          </g>
        </svg>
      </div>
    </v-navigation-drawer>

    <v-app-bar flat color="surface">
      <v-app-bar-title class="text-high-emphasis">Meu painel</v-app-bar-title>
      <template #append>
        <div class="header-session">
          <div class="session-copy">
            <strong>{{ authStore.currentUserLabel }}</strong>
            <span>{{ sessionCaption }}</span>
          </div>
          <v-btn variant="text" color="primary" prepend-icon="mdi-logout" @click="handleLogout">
            Sair
          </v-btn>
        </div>
      </template>
    </v-app-bar>

    <v-main>
      <RouterView />
    </v-main>
  </v-app>
</template>

<style scoped>
.app-layout {
  background:
    linear-gradient(180deg, rgb(40 83 107 / 0.03), transparent 24%),
    radial-gradient(circle at top left, rgb(215 189 143 / 0.1), transparent 26%),
    linear-gradient(160deg, #fdf8f0, #fffaf4 58%, #f4ead8);
}

.app-layout :deep(.v-navigation-drawer) {
  background:
    radial-gradient(circle at 20% 18%, rgb(255 255 255 / 0.06) 0, transparent 34%),
    linear-gradient(180deg, #112f4e, #0c2744 78%);
  background-size:
    auto,
    auto;
  border-right: 1px solid rgb(255 255 255 / 0.05);
  overflow: hidden;
}

.drawer-top {
  position: relative;
  z-index: 2;
  padding: 2.15rem 1.75rem 1.35rem;
  color: #fffaf4;
}

.brand-mark {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
}

.brand-glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  font-family:
    'Inter',
    'Segoe UI',
    system-ui,
    sans-serif;
  font-size: 5rem;
  line-height: 0.7;
  font-weight: 900;
  letter-spacing: -0.06em;
  background: linear-gradient(180deg, #fde68a 0%, #d97706 50%, #78350f 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 15px 30px rgb(0 0 0 / 0.4));
}

.drawer-title {
  font-size: 1.8rem;
  font-weight: 700;
  line-height: 1.08;
  text-transform: lowercase;
}

.drawer-subtitle {
  margin-top: 1.15rem;
  font-size: 1rem;
  line-height: 1.65;
  opacity: 0.88;
  max-width: 16rem;
}

.drawer-neural {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  opacity: 0.58;
  mask-image:
    linear-gradient(to bottom, transparent 0%, transparent 18%, rgb(0 0 0 / 0.18) 32%, rgb(0 0 0 / 0.72) 52%, black 72%, black 100%),
    radial-gradient(circle at 46% 74%, black 0%, rgb(0 0 0 / 0.9) 34%, transparent 92%);
}

.drawer-neural__svg {
  width: 100%;
  height: 100%;
  display: block;
  opacity: 0.94;
}

.drawer-neural__layer {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.drawer-neural__layer--primary {
  stroke: rgb(255 255 255 / 0.22);
  stroke-width: 1;
}

.drawer-neural__layer--secondary {
  stroke: rgb(255 255 255 / 0.13);
  stroke-width: 0.82;
}

.drawer-neural__nodes {
  fill: rgb(255 255 255 / 0.28);
}

.drawer-neural__signal {
  fill: rgb(255 255 255 / 0.98);
  opacity: 0.98;
}

.drawer-neural__signal--small {
  fill: rgb(248 250 252 / 0.88);
}

@media (prefers-reduced-motion: reduce) {
  .drawer-neural__signals {
    display: none;
  }
}

.app-layout :deep(.v-list-item) {
  margin-bottom: 0.45rem;
  color: rgb(255 250 244 / 0.92);
}

.app-layout :deep(.v-list-item--active) {
  background: rgb(255 255 255 / 0.1);
  box-shadow: inset 3px 0 0 #f0d389;
}

.app-layout :deep(.v-list-item-title) {
  font-weight: 600;
}

.app-layout :deep(.v-toolbar) {
  background:
    linear-gradient(180deg, rgb(255 252 247 / 0.96), rgb(249 243 233 / 0.9)) !important;
  border-bottom: 1px solid rgb(40 83 107 / 0.1);
  box-shadow:
    0 10px 24px rgb(13 43 77 / 0.05),
    0 1px 0 rgb(255 255 255 / 0.72) inset;
  backdrop-filter: blur(14px);
  padding-inline: 1.15rem !important;
}

.app-layout :deep(.v-toolbar-title__placeholder) {
  font-size: 1.16rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: rgb(18 41 61 / 0.96);
}

.app-layout :deep(.v-app-bar-title) {
  padding-inline-start: 0.2rem;
}

.app-layout :deep(.v-main__wrap) {
  padding: 1.35rem 1.5rem 1.85rem;
}

.header-session {
  display: flex;
  align-items: center;
  gap: 1.15rem;
  padding-right: 0.45rem;
}

.session-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
  font-size: 0.84rem;
  color: rgb(40 83 107 / 0.7);
}

.session-copy strong {
  color: rgb(18 41 61 / 0.94);
  font-size: 1rem;
}

.header-session :deep(.v-btn) {
  padding-inline: 0.9rem !important;
  min-width: auto;
}
</style>
