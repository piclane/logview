<template>
    <div class="file-table">
        <div class="toolbar">
          <el-tooltip
              effect="dark"
              content="Up">
            <el-button
                size="mini"
                type="text"
                icon="el-icon-top"
                :disabled="!currentPath.hasParent"
                @click="moveUp" />
          </el-tooltip>
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
                v-loading="loading"
                :data="viewRows"
                :default-sort="{prop: 'lastModified', order: 'descending'}"
                @cell-click="passthroughEvent('cell-click', arguments)"
                @sort-change="onChangeSort"
                border
                height="auto"
                fixed-header="true">
            <el-table-column
                    sortable="true"
                    width="160"
                    fixed="left"
                    :formatter="renderCell"
                    prop="name"
                    label="Filename" />
            <el-table-column
                    sortable="true"
                    width="140"
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
    import Swal from 'sweetalert2'
    import {showLoadFailed} from "@/utils/Alert";

    const defaultRenderer = (record, column, value) => {
        return value ? '' + value : '';
    };

    export default {
      mixins: [
        FileTableRenderers,
        ReemittableMixin
      ],
      props: {
        currentPath: {
          type: Path,
          required: true
        }
      },
      data() {
        return {
          loading: false,
          rows: [],
          search: '',
          sortProp: 'lastModified',
          sortOrder: 'descending'
        };
      },
      created: function () {
        this.$on('clickLink', this.onClickLink);
        this.reload();
      },
      computed: {
        /**
         * 表示用のレコード配列を取得します
         *
         * @return {[]} 表示用のレコード配列
         */
        viewRows: function () {
          const q = this.search.toLowerCase(),
              sortProp = this.sortProp,
              sortOrder = this.sortOrder;
          let rows = this.rows
              .filter(row => row['name'].toLowerCase().indexOf(q) !== -1);
          if (sortProp && sortOrder) {
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
          if (this.currentPath.hasParent) {
            rows.unshift({
              name: '..',
              type: 'dir',
              readable: true
            });
          }
          return rows;
        }
      },
      methods: {
        /**
         * 再読み込みを行います
         */
        reload() {
          this.loading = true;
          listDir(this.currentPath).then(rows => {
            this.loading = false;
            this.rows = rows;
          }, err => {
            this.loading = false;
            return showLoadFailed(`${err.response.status} ${err.response.statusText}\n\n${err.response.data}`);
          });
        },

        /**
         * 上階層に移動します
         */
        moveUp() {
          this.$router.push(`/@${this.currentPath.parent()}`);
        },

        /**
         * リンクをクリックした場合に呼び出されます
         */
        onClickLink(event) {
          const el = event.currentTarget;
          if (event.shiftKey || event.metaKey) { // 新規タブ・新規ウィンドウで開く場合
            // nop
          } else if (el.classList.contains('dir')) { // ディレクトリの場合
            const resource = el.resource;
            this.rows = [];
            this.$router.push(`/@${resource}`);
            event.stopPropagation();
            event.preventDefault();
          } else { // ファイルの場合
            const resource = el.resource;
            this.$emit('select-resource', resource);
            event.stopPropagation();
            event.preventDefault();
          }
        },

        onChangeSort(e) {
          this.sortProp = e.prop;
          this.sortOrder = e.order;
        },

        renderCell(record, column, value) {
          switch (column.property) {
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
      watch: {
        'currentPath': 'reload'
      },
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
        color: mediumblue;
        text-decoration: none;
    }

    a.logfile:visited {
        color: mediumblue;
    }

    a.logfile.unreadable {
        text-decoration: line-through;
    }

    a.logfile i {
        margin-right: 7px;
        vertical-align: middle;
        position: relative;
        top: -2px;
    }

    .toolbar {
        background: mediumorchid;
    }
</style>
<style src="../utils/Toolbar.css" scoped></style>
