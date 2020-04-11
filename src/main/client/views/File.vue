<template>
    <file-viewer ref="fileViewer"></file-viewer>
</template>

<script>
    import FileViewer from '@/components/FileViewer.vue';
    import Path from "@/utils/Path";
    import Range from "@/utils/Range";

    export default {
        mounted() {
            if(this.currentRange.isValid) {
                this.$refs.fileViewer.$emit('show-there', this.currentPath, this.currentRange);
            } else {
                this.$refs.fileViewer.$emit('show-tail', this.currentPath);
            }
        },
        computed: {
            currentPath: function() {
                return Path.of('/' + this.$route.params._path).normalize();
            },
            currentRange: function() {
                return Range.parse(this.$route.hash);
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
