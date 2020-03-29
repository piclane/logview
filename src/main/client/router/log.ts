import Vue from 'vue'
import VueRouter from 'vue-router'
import File from '../views/File.vue'

Vue.use(VueRouter);

const router = new VueRouter({
  mode: 'history',
  routes: [{
    path: '*',
    name: 'File',
    component: File,
    props: true
  }]
});

export default router
