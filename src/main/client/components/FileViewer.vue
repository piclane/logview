<template>
    <div class="file-viewer">
        <div class="toolbar">
            <el-tooltip
                effect="dark"
                content="Back"
                v-show="$route.name === 'File'">
                <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-back"
                    @click="doBack"/>
            </el-tooltip>
            <i class="el-icon-document"></i>
            <span class="path">{{ path.toString().trim() }}</span>
            <div class="space"></div>
            <el-input
                ref="searchInput"
                placeholder="search"
                size="mini"
                clearable
                v-model="search"
                :class="{search: true, inactive: search === ''}"
                :prefix-icon="searching ? 'icon-loading' : 'el-icon-search'"
                @keypress.enter.native="doSearch"
                @clear="doSearch"/>
            <el-tooltip
                effect="dark"
                :content="searchSmart ? 'Smart search is enabled' : 'Smart search is disabled'">
                <el-button
                    size="mini"
                    icon="el-icon-sunny"
                    :type="searchSmart ? 'gray' : 'text'"
                    @click="toggleSearchSmart" />
            </el-tooltip>
            <el-divider direction="vertical"></el-divider>
            <el-tooltip
                    effect="dark"
                    content="View the head">
                <el-button
                        type="text"
                        size="mini"
                        icon="el-icon-arrow-up"
                        @click="$emit('show-head', path)"/>
            </el-tooltip>
            <el-tooltip
                    effect="dark"
                    content="View the tail">
                <el-button
                        type="text"
                        size="mini"
                        icon="el-icon-arrow-down"
                        @click="$emit('show-tail', path)"/>
            </el-tooltip>
            <el-tooltip
                effect="dark"
                content="Download">
                <el-button
                    type="text"
                    size="mini"
                    icon="el-icon-download"
                    @click="doDownload"/>
            </el-tooltip>
            <el-tooltip
                effect="dark"
                :content="scrollLock ? 'Scroll lock is enabled' : 'Scroll lock is disabled'">
                <el-button
                    size="mini"
                    icon="el-icon-lock"
                    :type="scrollLock ? 'gray' : 'text'"
                    @click="toggleScrollLock" />
            </el-tooltip>
        </div>
        <file-renderer
            ref="fileRenderer"
            :scrollLock="scrollLock"
            @searching="searching = $event"/>
        <div class="wrapper" v-show="path === ''"></div>
    </div>
</template>

<script>
    import FileRenderer from "@/components/FileRenderer.vue";
    import {downloadFile} from "@/utils/api/FileApiClient";
    import Path from "@/utils/Path";
    import Range from "@/utils/Range";

    export default {
        name: 'file-viewer',
        components: {FileRenderer},
        data() {
            return {
                path: Path.empty(),
                search: '',
                searching: false,
                searchSmart: true,
                scrollLock: false
            };
        },
        created() {
            this.$on('show-tail', path => {
                this.path = path;
                this.search = '';
                this.searching = false;
                this.scrollLock = false;
                this.$refs.fileRenderer.$emit('show-tail', path);
            });

            this.$on('show-head', path => {
                this.path = path;
                this.search = '';
                this.searching = false;
                this.scrollLock = false;
                this.$refs.fileRenderer.$emit('show-head', path);
            });

            this.$on('show-there', (path, range) => {
                this.path = path;
                this.search = '';
                this.searching = false;
                this.scrollLock = true;
                this.$refs.fileRenderer.$emit('show-there', path, range);
            });
        },
        methods: {
            doBack: function() {
                this.$router.push({
                    path: this.path.parent().toString(),
                    query: {
                        file: this.path.toString(),
                        range: Range.parse(this.$route.hash).toString()
                    }
                });
            },
            toggleScrollLock: function() {
                this.scrollLock = !this.scrollLock;
            },
            toggleSearchSmart: function() {
                this.searchSmart = !this.searchSmart;
                if(this.search) {
                    this.doSearch();
                }
            },
            doDownload: function() {
                downloadFile(this.path);
            },
            doSearch: function() {
                this.$refs.searchInput.blur();
                if(this.search) {
                    this.$refs.fileRenderer.$emit('search', this.search, this.searchSmart);
                } else {
                    this.$emit('show-tail', this.path);
                }
            }
        }
    }
</script>

<style scoped>
    .file-viewer {
        display: flex;
        flex-direction: column;
    }

    .toolbar {
        border-bottom: 1px solid #eee;
        font-size: 12px;
        background-color: royalblue;
        color: white;
    }

    .toolbar .path {
        white-space: nowrap;
        text-align: left;
    }

    .toolbar .el-button.el-button--gray {
        color: #FFF;
        background-color: rgba(0,0,0,.35);
        border: none;
    }

    .toolbar .el-input >>> i.icon-loading {
        width: 12px;
        height: 12px;
        line-height: 24px;
    }

    .toolbar .el-divider {
        background-color: rgba(255, 255, 255, 0.3);
    }

    .wrapper {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        background-color: rgba(0,0,0,.4);
        color: white;
    }
</style>
<style src="../utils/Toolbar.css" scoped></style>
<style src="../utils/Icon.css"></style>
