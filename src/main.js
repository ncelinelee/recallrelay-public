import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles.css'
import './theme-ppt.css'
import { tr } from './i18n'

const app = createApp(App)
app.config.globalProperties.$tr = tr
app.use(router).mount('#app')
