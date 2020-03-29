import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import File from '../views/File.vue'

Vue.use(VueRouter);

const router = new VueRouter({
  routes: [{
    path: '/$/file/:_path(.*)',
    alias: '/log.html',
    name: 'File',
    component: File,
    props: true
  }, {
    path: '/*',
    name: 'Home',
    component: Home,
    props: true
  }]
});

export default router
