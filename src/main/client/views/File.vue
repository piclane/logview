<template>
    <file-viewer ref="fileViewer"></file-viewer>
</template>

<script>
    import FileViewer from '@/components/FileViewer.vue';
    import Path from "@/utils/Path";

    export default {
        mounted() {
            this.$refs.fileViewer.$emit('show-tail', this.currentPath);
        },
        computed: {
            currentPath: function() {
                let path = '/' + this.$route.params._path;
                if(this.$route.query.path) {
                    path = this.$route.query.path;
                }
                return Path.of(path).normalize();
            }
        },
        components: {
            FileViewer
        }
    }
</script>

<style scoped>
    .file-viewer {
        position: absolute;
        top: 0;
        left: 0;
        bottom: 0;
        right: 0;
    }
</style>
