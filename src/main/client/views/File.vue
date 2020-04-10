<template>
    <file-viewer ref="fileViewer"></file-viewer>
</template>

<script>
    import FileViewer from '@/components/FileViewer.vue';
    import Path from "@/utils/Path";

    export default {
        mounted() {
            if(this.currentPosition !== null) {
                this.$refs.fileViewer.$emit('show-there', this.currentPath, this.currentPosition);
            } else {
                this.$refs.fileViewer.$emit('show-tail', this.currentPath);
            }
        },
        computed: {
            currentPath: function() {
                return Path.of('/' + this.$route.params._path).normalize();
            },
            currentPosition: function() {
                let m = /B(\d+)(-(\d+))?/.exec(this.$route.hash);
                if(m) {
                    return {
                        start: parseInt(m[1]),
                        end: parseInt(m[2] ? m[3] : m[1])
                    }
                }
                return null;
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
