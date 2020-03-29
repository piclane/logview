import Vue from 'vue';
import App from './App.vue';
import router from './router/index';
import ElementUI from 'element-ui';
// import locale from 'element-ui/lib/locale/lang/ja';
import 'element-ui/lib/theme-chalk/index.css';
import 'element-ui/lib/theme-chalk/display.css';

Vue.config.productionTip = false;
// Vue.use(ElementUI, { locale });
Vue.use(ElementUI);

new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
