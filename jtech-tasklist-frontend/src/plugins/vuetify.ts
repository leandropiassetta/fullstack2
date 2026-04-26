import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

import { createVuetify } from 'vuetify'

export const vuetify = createVuetify({
  theme: {
    defaultTheme: 'tasklist',
    themes: {
      tasklist: {
        dark: false,
        colors: {
          primary: '#28536b',
          secondary: '#c2944a',
          surface: '#fffaf4',
          background: '#f4efe6',
          success: '#4f7c5a',
          error: '#9f3d32',
        },
      },
    },
  },
})
