<template>
  <el-container id="main-container">
    <el-header id="header" height="">
      <main-header :current-path="currentPath" />
    </el-header>
    <el-main>
      <multipane>
        <file-table @select-resource="showTail" :current-path="currentPath" />
        <multipane-resizer></multipane-resizer>
        <file-viewer ref="fileViewer"></file-viewer>
      </multipane>
    </el-main>
  </el-container>
</template>

<script>
import MainHeader from '@/components/MainHeader.vue';
import FileTable from '@/components/FileTable.vue';
import FileViewer from '@/components/FileViewer.vue';
import { Multipane, MultipaneResizer } from 'vue-multipane';
import Path from "@/utils/Path";
import Range from "@/utils/Range";

export default {
  data: function() {
    return {
      selectedPath: Path.empty()
    };
  },
  computed: {
    currentPath: function() {
      return Path.of(this.$route.path).normalize();
    }
  },
  watch: {
    '$route': {
      handler: function() {
        const file = this.$route.query.file;
        if(file) {
          const path = Path.of(file).normalize();
          const range = Range.parse(this.$route.query.range);
          this.$nextTick(() => {
            if(range.isValid) {
              this.showThere(path, range);
            } else {
              this.showTail(path);
            }
          });
          this.$router.replace({path: this.$route.path});
        }
      },
      immediate: true
    }
  },
  methods: {
    showTail: function(path) {
      this.$refs.fileViewer.$emit('show-tail', path);
    },
    showThere: function(path, range) {
      this.$refs.fileViewer.$emit('show-there', path, range);
    }
  },
  components: {
    MainHeader,
    FileTable,
    FileViewer,
    Multipane, MultipaneResizer
  }
}
</script>

<style scoped>
  .el-container > * {
    padding: 0;
    position: relative;
  }

  .el-container, .multipane {
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    right: 0;
  }

  .file-table {
    width: 460px;
    min-width: 200px;
    overflow: hidden;
  }

  .file-viewer {
    flex-grow: 1;
    width: 1px;
  }

  .is-resizing .multipane-resizer {
    background: #9d9ea0;
  }

  .multipane .multipane-resizer {
    margin-left: 0;
    left: 0;
    background: #333;
    width: 7px;
    z-index: 10;
  }

  .multipane .multipane-resizer:before {
    content: '';
    position: absolute;
    display: inline-block;
    top: 50%;
    left: 50%;
    width: 1px;
    height: 80px;
    margin-left: -2px;
    border-width: 0 1px 0 1px;
    border-style: solid;
    border-color: #aaa;
  }
</style>
