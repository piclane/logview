<template>
    <div class="file-table">
        <div class="toolbar">
            <el-tooltip
                effect="dark"
                content="Reload">
                <el-button
                    size="mini"
                    type="text"
                    icon="el-icon-refresh"
                    @click="reload" />
            </el-tooltip>
            <el-input
                :class="{inactive: search === ''}"
                placeholder="search"
                prefix-icon="el-icon-search"
                v-model="search"
                size="mini"
                clearable />
        </div>
        <el-table
                ref="table"
                :data="viewRows"
                :default-sort="{prop: 'lastModified', order: 'descending'}"
                @cell-click="passthroughEvent('cell-click', arguments)"
                @sort-change="onChangeSort"
                border
                height="auto"
                fixed-header="true">
            <el-table-column
                    sortable="true"
                    width="150"
                    fixed="left"
                    :formatter="renderCell"
                    prop="name"
                    label="Filename" />
            <el-table-column
                    sortable="true"
                    width="150"
                    :formatter="renderCell"
                    prop="lastModified"
                    label="Last Modified" />
            <el-table-column
                    align="right"
                    sortable="true"
                    prop="size"
                    :formatter="renderCell"
                    label="Size" />
            <el-table-column
                    prop="owner"
                    :formatter="renderCell"
                    label="Owner" />
            <el-table-column
                    prop="group"
                    :formatter="renderCell"
                    label="Group" />
            <el-table-column
                    prop="permissions"
                    :formatter="renderCell"
                    label="Permissions" />
        </el-table>
    </div>
</template>

<script>
    import {FileTableRenderers} from "./FileTableRenderers";
    import Path from "@/utils/Path";
    import ReemittableMixin from "@/utils/ReemittableMixin";
    import {listDir} from "@/utils/api/DirApiClient";

    const defaultRenderer = (record, column, value) => {
        return value ? '' + value : '';
    };

    export default {
        data() {
            return {
                rows: [],
                search: '',
                sortProp: 'lastModified',
                sortOrder: 'descending'
            };
        },
        props: {
            currentPath: {
                type: Path,
                required: true
            }
        },
        created: function() {
            this.$on('clickLink', this.onClickLink);
            this.reload();
        },
        watch: {
            'currentPath': 'reload'
        },
        methods: {
            /**
             * 再読み込みを行います
             */
            reload: function() {
                listDir(this.currentPath).then(rows => {
                    this.rows = rows;
                });
            },

            /**
             * リンクをクリックした場合に呼び出されます
             */
            onClickLink: function(event) {
                const el = event.target;
                if(el.classList.contains('dir')) { // ディレクトリの場合
                    this.rows = [];
                    return;
                } else if(event.shiftKey || event.metaKey) { // 新規タブ・新規ウィンドウで開く場合
                    return;
                }
                const resource = el.resource;
                this.$emit('select-resource', resource);
                event.stopPropagation();
                event.preventDefault();
            },

            onChangeSort: function(e) {
                this.sortProp = e.prop;
                this.sortOrder = e.order;
            },

            renderCell: function(record, column, value) {
                switch(column.property) {
                    case 'name':
                        return this.renderName(record, column, value);
                    case 'lastModified':
                        return this.renderLastModified(record, column, value);
                    case 'size':
                        return this.renderSize(record, column, value);
                    case 'permissions':
                        return this.renderPermission(record, column, value);
                    default:
                        return defaultRenderer(record, column, value);
                }
            },
        },
        computed: {
            /**
             * 表示用のレコード配列を取得します
             *
             * @return {[]} 表示用のレコード配列
             */
            viewRows: function() {
                const q = this.search.toLowerCase(),
                    sortProp = this.sortProp,
                    sortOrder = this.sortOrder;
                let rows = this.rows
                    .filter(row => row['name'].toLowerCase().indexOf(q) !== -1);
                if(sortProp && sortOrder) {
                    rows = rows.map((value, index) => {
                            return {value, index};
                        })
                        .sort((a, b) => {
                            const av = a.value[sortProp],
                                bv = b.value[sortProp];
                            let r;
                            if (av < bv) {
                                r = -1;
                            } else if (av > bv) {
                                r = 1;
                            } else {
                                r = a.index - b.index;
                            }
                            return r * (sortOrder === 'descending' ? -1 : 1);
                        })
                        .map(v => v.value);
                }
                if(this.currentPath.hasParent) {
                    rows.unshift({
                        name: '..',
                        type: 'dir',
                        readable: true
                    });
                }
                return rows;
            }
        },
        mixins: [
            FileTableRenderers,
            ReemittableMixin
        ]
    }
</script>

<style scoped>
    .file-table {
        display: flex;
        flex-direction: column;
    }

    .file-table >>> .el-table {
        flex-grow: 1;
    }

    .file-table >>> .el-table th {
        background: #f4f5f8;
        color: #6f7276;
    }

    .file-table >>> .el-table td,
    .file-table >>> .el-table th {
        padding: 3px 0;
    }

    .file-table >>> .el-table .cell {
        white-space: nowrap;
        padding: 0 5px;
        font-family: Verdana, Arial, sans-serif;
    }

    .file-table >>> .el-table th .cell {
        font-size: 11px;
    }

    .file-table >>> .el-table td .cell {
        font-size: 12px;
    }

    a.logfile {
        text-decoration: none;
    }

    a.logfile i {
        font-size: 13px;
        margin-right: 5px;
        vertical-align: middle;
        color: midnightblue;
    }

    .toolbar {
        background: mediumorchid;
    }
</style>
<style src="../utils/Toolbar.css" scoped></style>
