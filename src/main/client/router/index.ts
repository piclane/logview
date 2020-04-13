import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import File from '../views/File.vue'

Vue.use(VueRouter);

const router = new VueRouter({
  base: process.env.BASE_URL,
  mode: 'history',
  routes: [{
    path: '/$/:_path(.*)',
    name: 'File',
    component: File,
    props: true
  }, {
    path: '/@/:_path(.*)',
    name: 'Home',
    component: Home,
    props: true
  }, {
    path: '/*',
    redirect: '/@/'
  }]
});

export default router
