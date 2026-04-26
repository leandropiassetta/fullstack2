import './assets/main.css'

import { createApp } from 'vue'

import App from './App.vue'
import { appPinia } from './plugins/pinia'
import { vuetify } from './plugins/vuetify'
import router from './router'

const app = createApp(App)

app.use(appPinia)
app.use(vuetify)
app.use(router)

app.mount('#app')
